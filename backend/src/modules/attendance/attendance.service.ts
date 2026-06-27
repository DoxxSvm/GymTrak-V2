import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { AttendanceSource, GymRole, Prisma } from '@prisma/client';
import * as bcrypt from 'bcrypt';
import { randomBytes } from 'crypto';
import { GymAccessService } from '../../common/services/gym-access.service';
import { startOfUtcDay } from '../../common/utils/utc-date';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class AttendanceService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  async listDaily(
    actorUserId: string,
    gymId: string,
    dateIso: string,
    limit: number,
    offset: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const attendedOn = this.parseDateOnly(dateIso);
    const where: Prisma.AttendanceRecordWhereInput = {
      gymId,
      attendedOn,
    };
    const [total, rows] = await Promise.all([
      this.prisma.attendanceRecord.count({ where }),
      this.prisma.attendanceRecord.findMany({
        where,
        orderBy: { checkedInAt: 'desc' },
        take: limit,
        skip: offset,
        select: {
          id: true,
          attendedOn: true,
          checkedInAt: true,
          source: true,
          memberUser: {
            select: {
              id: true,
              fullName: true,
              phone: true,
            },
          },
        },
      }),
    ]);
    return { gymId, date: dateIso, total, limit, offset, items: rows };
  }

  async markAttendanceByOwner(
    actorUserId: string,
    dto: {
      member_id: string;
      date: string;
      time?: string;
      status: 'present' | 'absent';
    },
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        id: true,
        gymId: true,
        role: true,
        userId: true,
        isActive: true,
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, member.gymId);
    if (!member.isActive) {
      throw new BadRequestException('Inactive member cannot be marked');
    }

    const attendedOn = this.parseDateOnly(dto.date);
    if (dto.status === 'present') {
      const checkedInAt = dto.time?.trim()
        ? this.combineDateAndTime(attendedOn, dto.time)
        : new Date();
      const existingRecord = await this.prisma.attendanceRecord.findFirst({
        where: {
          gymId: member.gymId,
          memberUserId: member.userId,
          attendedOn,
          checkedOutAt: null,
        },
        orderBy: { checkedInAt: 'desc' },
        select: { id: true },
      });
      if (existingRecord) {
        await this.prisma.attendanceRecord.update({
          where: { id: existingRecord.id },
          data: { checkedInAt, source: AttendanceSource.MANUAL },
        });
      } else {
        await this.prisma.attendanceRecord.create({
          data: {
            gymId: member.gymId,
            memberUserId: member.userId,
            attendedOn,
            source: AttendanceSource.MANUAL,
            checkedInAt,
          },
        });
      }
    } else {
      await this.prisma.attendanceRecord.deleteMany({
        where: {
          gymId: member.gymId,
          memberUserId: member.userId,
          attendedOn,
        },
      });
    }
    return { success: true as const };
  }

  /**
   * Owner-marked check-in: one record per member per gym per calendar day.
   */
  async memberCheckInByOwner(
    actorUserId: string,
    dto: { member_id: string; date?: string; time?: string },
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        id: true,
        gymId: true,
        role: true,
        userId: true,
        isActive: true,
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
    if (!member.isActive) {
      throw new BadRequestException('Inactive member cannot check in');
    }
    await this.assertMemberAttendanceAllowed(member.userId, member.gymId);

    const attendedOn = dto.date?.trim()
      ? this.parseDateOnly(dto.date)
      : utcDateOnly(new Date());
    const existing = await this.prisma.attendanceRecord.findFirst({
      where: {
        gymId: member.gymId,
        memberUserId: member.userId,
        attendedOn,
        checkedOutAt: null,
      },
      orderBy: { checkedInAt: 'desc' },
      select: { id: true, checkedInAt: true },
    });
    if (existing) {
      throw new ConflictException('Already checked in for this day');
    }
    const checkedInAt = dto.time?.trim()
      ? this.combineDateAndTime(attendedOn, dto.time)
      : new Date();
    const created = await this.prisma.attendanceRecord.create({
      data: {
        gymId: member.gymId,
        memberUserId: member.userId,
        attendedOn,
        checkedInAt,
        source: AttendanceSource.BIOMETRIC,
      },
      select: { id: true, checkedInAt: true, attendedOn: true },
    });
    return { success: true as const, ...created };
  }

  async memberCheckOutByOwner(
    actorUserId: string,
    dto: { member_id: string; date?: string; time?: string },
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        id: true,
        gymId: true,
        role: true,
        userId: true,
        isActive: true,
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
    const attendedOn = dto.date?.trim()
      ? this.parseDateOnly(dto.date)
      : utcDateOnly(new Date());
    const row = await this.prisma.attendanceRecord.findFirst({
      where: {
        gymId: member.gymId,
        memberUserId: member.userId,
        attendedOn,
        checkedOutAt: null,
      },
      orderBy: { checkedInAt: 'desc' },
      select: { id: true, checkedOutAt: true },
    });
    if (!row) {
      throw new NotFoundException('No open check-in found for this day');
    }
    const checkedOutAt = dto.time?.trim()
      ? this.combineDateAndTime(attendedOn, dto.time)
      : new Date();
    await this.prisma.attendanceRecord.update({
      where: { id: row.id },
      data: { checkedOutAt },
    });
    return { success: true as const, checkedOutAt };
  }

  /**
   * Industry-standard punch flow:
   * - no record today => check-in
   * - open record today => check-out
   * - already checked out today => conflict
   */
  async memberPunchByOwner(
    actorUserId: string,
    dto: { member_id: string; date?: string; time?: string },
  ) {
    const member = await this.prisma.gymUser.findUnique({
      where: { id: dto.member_id },
      select: {
        gymId: true,
        role: true,
        userId: true,
        isActive: true,
      },
    });
    if (!member || member.role !== GymRole.MEMBER) {
      throw new NotFoundException('Member not found');
    }
    await this.gymAccess.assertCanManageGym(actorUserId, member.gymId);
    if (!member.isActive) {
      throw new BadRequestException('Inactive member cannot punch attendance');
    }

    const attendedOn = dto.date?.trim()
      ? this.parseDateOnly(dto.date)
      : utcDateOnly(new Date());
    const punchedAt = dto.time?.trim()
      ? this.combineDateAndTime(attendedOn, dto.time)
      : new Date();

    const existing = await this.prisma.attendanceRecord.findFirst({
      where: {
        gymId: member.gymId,
        memberUserId: member.userId,
        attendedOn,
        checkedOutAt: null,
      },
      orderBy: { checkedInAt: 'desc' },
      select: { id: true, checkedInAt: true, checkedOutAt: true },
    });

    if (!existing) {
      await this.assertMemberAttendanceAllowed(member.userId, member.gymId);
      const created = await this.prisma.attendanceRecord.create({
        data: {
          gymId: member.gymId,
          memberUserId: member.userId,
          attendedOn,
          checkedInAt: punchedAt,
          source: AttendanceSource.MANUAL,
        },
        select: { checkedInAt: true, attendedOn: true },
      });
      return {
        success: true as const,
        action: 'clock_in' as const,
        attendedOn: created.attendedOn,
        checkedInAt: created.checkedInAt,
      };
    }

    if (punchedAt.getTime() < existing.checkedInAt.getTime()) {
      throw new BadRequestException('check-out time cannot be before check-in');
    }

    const updated = await this.prisma.attendanceRecord.update({
      where: { id: existing.id },
      data: { checkedOutAt: punchedAt },
      select: { attendedOn: true, checkedInAt: true, checkedOutAt: true },
    });

    return {
      success: true as const,
      action: 'clock_out' as const,
      attendedOn: updated.attendedOn,
      checkedInAt: updated.checkedInAt,
      checkedOutAt: updated.checkedOutAt,
    };
  }

  async monthlyStats(
    actorUserId: string,
    gymId: string,
    year: number,
    month: number,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const start = new Date(Date.UTC(year, month - 1, 1, 0, 0, 0, 0));
    const end = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));

    const [totalCheckIns, uniqueMembers, byDayRows] = await Promise.all([
      this.prisma.attendanceRecord.count({
        where: {
          gymId,
          attendedOn: { gte: start, lte: end },
        },
      }),
      this.prisma.attendanceRecord.groupBy({
        by: ['memberUserId'],
        where: {
          gymId,
          attendedOn: { gte: start, lte: end },
        },
        _count: true,
      }),
      this.prisma.attendanceRecord.groupBy({
        by: ['attendedOn'],
        where: {
          gymId,
          attendedOn: { gte: start, lte: end },
        },
        _count: true,
      }),
    ]);

    const byDay: Record<string, number> = {};
    for (const r of byDayRows) {
      const d = r.attendedOn as Date;
      const key = d.toISOString().slice(0, 10);
      byDay[key] = r._count;
    }

    return {
      gymId,
      year,
      month,
      totalCheckIns,
      uniqueMemberCount: uniqueMembers.length,
      byDay,
    };
  }

  async memberLifetimeStats(
    actorUserId: string,
    gymId: string,
    memberUserId: string,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const member = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: memberUserId,
        role: GymRole.MEMBER,
      },
      select: { id: true },
    });
    if (!member) {
      throw new NotFoundException('Member not found at this gym');
    }

    const [totalVisits, firstVisit, lastVisit] = await Promise.all([
      this.prisma.attendanceRecord.count({
        where: { gymId, memberUserId },
      }),
      this.prisma.attendanceRecord.findFirst({
        where: { gymId, memberUserId },
        orderBy: { attendedOn: 'asc' },
        select: { attendedOn: true },
      }),
      this.prisma.attendanceRecord.findFirst({
        where: { gymId, memberUserId },
        orderBy: { attendedOn: 'desc' },
        select: { attendedOn: true },
      }),
    ]);

    return {
      gymId,
      memberUserId,
      totalVisits,
      firstVisitOn: firstVisit?.attendedOn?.toISOString().slice(0, 10) ?? null,
      lastVisitOn: lastVisit?.attendedOn?.toISOString().slice(0, 10) ?? null,
    };
  }

  async gymSummary(actorUserId: string, gymId: string) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const now = new Date();
    const monthStart = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1),
    );
    const today = startOfUtcDay(now);

    const [lifetime, thisMonth, todayCount] = await Promise.all([
      this.prisma.attendanceRecord.count({ where: { gymId } }),
      this.prisma.attendanceRecord.count({
        where: {
          gymId,
          attendedOn: { gte: monthStart },
        },
      }),
      this.prisma.attendanceRecord.count({
        where: { gymId, attendedOn: today },
      }),
    ]);

    return {
      gymId,
      lifetimeCheckIns: lifetime,
      monthToDateCheckIns: thisMonth,
      todayCheckIns: todayCount,
    };
  }

  async setAttendanceBlock(
    actorUserId: string,
    gymId: string,
    gymUserId: string,
    blocked: boolean,
    reason?: string | null,
  ) {
    await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    const gu = await this.prisma.gymUser.findFirst({
      where: {
        id: gymUserId,
        gymId,
        role: GymRole.MEMBER,
      },
      select: { id: true },
    });
    if (!gu) {
      throw new NotFoundException('Member gym profile not found');
    }
    await this.prisma.gymUser.update({
      where: { id: gymUserId },
      data: {
        attendanceBlocked: blocked,
        attendanceBlockedReason: blocked ? reason?.trim() || null : null,
        attendanceBlockedAt: blocked ? new Date() : null,
      },
    });
    return { gymUserId, attendanceBlocked: blocked };
  }

  async registerBiometric(
    actorUserId: string,
    gymId: string,
    label?: string | null,
  ) {
    await this.gymAccess.assertMemberAtGym(actorUserId, gymId);
    await this.assertMemberAttendanceAllowed(actorUserId, gymId);

    const apiKey = randomBytes(32).toString('base64url');
    const secretHash = await bcrypt.hash(apiKey, 10);
    const deviceId = `dev_${randomBytes(12).toString('hex')}`;

    await this.prisma.memberBiometricCredential.create({
      data: {
        gymId,
        memberUserId: actorUserId,
        deviceId,
        secretHash,
        label: label?.trim() || null,
      },
    });

    return {
      deviceId,
      apiKey,
      warning: 'Store apiKey securely; it cannot be retrieved again.',
    };
  }

  async listBiometricCredentials(actorUserId: string, gymId: string) {
    await this.gymAccess.assertMemberAtGym(actorUserId, gymId);
    const rows = await this.prisma.memberBiometricCredential.findMany({
      where: {
        gymId,
        memberUserId: actorUserId,
        revokedAt: null,
      },
      select: {
        id: true,
        deviceId: true,
        label: true,
        createdAt: true,
      },
      orderBy: { createdAt: 'desc' },
    });
    return { gymId, items: rows };
  }

  async revokeBiometric(
    actorUserId: string,
    gymId: string,
    credentialId: string,
  ) {
    const row = await this.prisma.memberBiometricCredential.findFirst({
      where: { id: credentialId, gymId },
      select: { id: true, memberUserId: true },
    });
    if (!row) {
      throw new NotFoundException('Credential not found');
    }
    if (row.memberUserId !== actorUserId) {
      await this.gymAccess.assertCanManageGym(actorUserId, gymId);
    }
    await this.prisma.memberBiometricCredential.update({
      where: { id: credentialId },
      data: { revokedAt: new Date() },
    });
    return { ok: true as const };
  }

  async checkInBiometric(gymId: string, deviceId: string, apiKey: string) {
    const row = await this.prisma.memberBiometricCredential.findFirst({
      where: {
        deviceId,
        gymId,
        revokedAt: null,
      },
      select: {
        id: true,
        secretHash: true,
        memberUserId: true,
      },
    });
    if (!row) {
      throw new BadRequestException('Unknown or revoked device');
    }
    const ok = await bcrypt.compare(apiKey, row.secretHash);
    if (!ok) {
      throw new BadRequestException('Invalid credentials');
    }

    const member = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: row.memberUserId,
        role: GymRole.MEMBER,
        isActive: true,
      },
      select: { id: true },
    });
    if (!member) {
      throw new BadRequestException('Member not active at this gym');
    }

    const attendedOn = utcDateOnly(new Date());
    const openSession = await this.prisma.attendanceRecord.findFirst({
      where: {
        gymId,
        memberUserId: row.memberUserId,
        attendedOn,
        checkedOutAt: null,
      },
      orderBy: { checkedInAt: 'desc' },
    });

    if (openSession) {
      const now = new Date();
      const updated = await this.prisma.attendanceRecord.update({
        where: { id: openSession.id },
        data: { checkedOutAt: now },
        select: { checkedInAt: true, checkedOutAt: true },
      });
      return {
        ok: true as const,
        action: 'clock_out' as const,
        attendedOn,
        gymId,
        checkedInAt: updated.checkedInAt.toISOString(),
        checkedOutAt: updated.checkedOutAt?.toISOString() ?? null,
      };
    }

    await this.assertMemberAttendanceAllowed(row.memberUserId, gymId);

    const created = await this.prisma.attendanceRecord.create({
      data: {
        gymId,
        memberUserId: row.memberUserId,
        attendedOn,
        checkedInAt: new Date(),
        source: AttendanceSource.BIOMETRIC,
      },
      select: { checkedInAt: true },
    });

    return {
      ok: true as const,
      action: 'clock_in' as const,
      attendedOn,
      gymId,
      checkedInAt: created.checkedInAt.toISOString(),
    };
  }

  private async assertMemberAttendanceAllowed(
    memberUserId: string,
    gymId: string,
  ): Promise<void> {
    const gu = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: memberUserId,
        role: GymRole.MEMBER,
        isActive: true,
      },
      select: { attendanceBlocked: true, attendanceBlockedReason: true },
    });
    if (!gu) {
      throw new ForbiddenException('Not an active member at this gym');
    }
    if (gu.attendanceBlocked) {
      throw new ForbiddenException(
        gu.attendanceBlockedReason?.trim() ||
          'Attendance is blocked for this member at this gym',
      );
    }
  }

  private parseDateOnly(iso: string): Date {
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim());
    if (!m) {
      throw new BadRequestException('date must be YYYY-MM-DD');
    }
    const y = parseInt(m[1], 10);
    const mo = parseInt(m[2], 10) - 1;
    const d = parseInt(m[3], 10);
    return new Date(Date.UTC(y, mo, d, 0, 0, 0, 0));
  }

  private combineDateAndTime(attendedOn: Date, time: string): Date {
    const parts = time
      .trim()
      .split(':')
      .map((p) => parseInt(p, 10));
    const hh = parts[0] ?? 0;
    const mm = parts[1] ?? 0;
    const ss = parts[2] ?? 0;
    return new Date(
      Date.UTC(
        attendedOn.getUTCFullYear(),
        attendedOn.getUTCMonth(),
        attendedOn.getUTCDate(),
        hh,
        mm,
        ss,
        0,
      ),
    );
  }
}

function utcDateOnly(d: Date): Date {
  return new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 0, 0, 0, 0),
  );
}
