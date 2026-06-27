import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { AttendanceSource, GymRole } from '@prisma/client';
import { createHmac, timingSafeEqual } from 'crypto';
import * as QRCode from 'qrcode';
import { PrismaService } from '../prisma/prisma.service';

type PunchQrPayload = {
  v: 3;
  purpose: 'attendance_punch';
  gymId: string;
  gymUserId: string;
  userId: string;
  role: GymRole;
};

@Injectable()
export class AttendanceQrService {
  constructor(private readonly prisma: PrismaService) { }

  async getMemberToken(actorUserId: string, gymId: string) {

    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        logoUrl: true,
        qrSigningSecret: true,
        ownerId: true,
        owner: {
          select: {
            id: true,
            fullName: true,
            phone: true,
            avatarUrl: true,
          },
        },
      },
    });

    const gymUser = await this.prisma.gymUser.findFirst({
      where: {
        gymId,
        userId: gym?.ownerId,
        isActive: true,
        role: {
          in: [GymRole.OWNER, GymRole.STAFF, GymRole.TRAINER, GymRole.MEMBER],
        },
      },
      select: {
        id: true,
        role: true,
        attendanceBlocked: true,
        attendanceBlockedReason: true,
      },
    });
    if (!gymUser) {
      throw new ForbiddenException(
        'Owner, trainer, or member access required at this gym',
      );
    }
    if (gymUser.role === GymRole.MEMBER && gymUser.attendanceBlocked) {
      throw new ForbiddenException(
        gymUser.attendanceBlockedReason?.trim() ||
        'Attendance is blocked for this member at this gym',
      );
    }

    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    const payload: PunchQrPayload = {
      v: 3,
      purpose: 'attendance_punch',
      gymId,
      gymUserId: gymUser.id,
      userId: gym.ownerId,
      role: gymUser.role,
    };
    const token = signPayload(gym.qrSigningSecret, payload);
    const qrBase64 = await QRCode.toDataURL(token, {
      width: 280,
      margin: 1,
      errorCorrectionLevel: 'M',
    });
    return {
      gymId: gym.id,
      name: gym.name,
      slug: gym.slug,
      logoUrl: gym.logoUrl,
      owner: {
        id: gym.owner.id,
        fullName: gym.owner.fullName,
        phone: gym.owner.phone,
        avatarUrl: gym.owner.avatarUrl,
      },
      role: gymUser.role,
      token,
      qrBase64,
      hint: 'Use this same QR for both clock-in and clock-out on POST /attendance-qr/punch.',
    };
  }

  async checkInWithToken(token: string, actorUserId?: string) {
    if (/^data:image\//i.test(token.trim())) {
      throw new BadRequestException(
        'QR image base64 is not a token. Send the decoded token string from GET /attendance-qr/my-qr.',
      );
    }

    const parsed = parsePayloadFromToken(token);
    if (!parsed) {
      throw new BadRequestException('Invalid or malformed attendance token');
    }
    const gym = await this.prisma.gym.findUnique({
      where: { id: parsed.gymId },
      select: { id: true, qrSigningSecret: true },
    });
    if (!gym) {
      throw new BadRequestException('Invalid attendance token (unknown gym)');
    }
    if (!verifySignature(gym.qrSigningSecret, token)) {
      throw new BadRequestException('Invalid attendance token (bad signature)');
    }

    const gymUser = await this.prisma.gymUser.findFirst({
      where: {
        gymId: parsed.gymId,
        userId: parsed.userId,
        id: parsed.gymUserId,
        role: parsed.role,
        isActive: true,
      },
      select: {
        id: true,
        role: true,
        attendanceBlocked: true,
        attendanceBlockedReason: true,
      },
    });
    if (!gymUser) {
      const activeMembership = await this.prisma.gymUser.findFirst({
        where: {
          gymId: parsed.gymId,
          userId: parsed.userId,
          id: parsed.gymUserId,
          isActive: true,
        },
        select: { role: true },
      });
      if (activeMembership) {
        throw new BadRequestException(
          `QR role mismatch for this user (${activeMembership.role}). Regenerate QR via GET /attendance-qr/my-qr and retry.`,
        );
      }
      throw new BadRequestException(
        'Invalid or inactive attendance QR for this gym',
      );
    }
    if (gymUser.role === GymRole.MEMBER && gymUser.attendanceBlocked) {
      throw new ForbiddenException(
        gymUser.attendanceBlockedReason?.trim() ||
        'Attendance is blocked for this member at this gym',
      );
    }
    if (gymUser.role === GymRole.OWNER) {
      if (actorUserId) {
        return this.punchLoggedInUserWithToken(actorUserId, token);
      }
      return this.punchOwner(parsed.gymId, parsed.userId);
    }

    if (gymUser.role === GymRole.TRAINER || gymUser.role === GymRole.STAFF) {
      return this.punchStaffOrTrainer(
        parsed.gymId,
        parsed.gymUserId,
        parsed.userId,
        gymUser.role,
      );
    }
    if (gymUser.role !== GymRole.MEMBER) {
      throw new BadRequestException('Unsupported QR role for attendance punch');
    }

    return this.punchMember(parsed.gymId, parsed.userId);
  }

  async punchLoggedInUserWithToken(actorUserId: string, token: string) {
    if (/^data:image\//i.test(token.trim())) {
      throw new BadRequestException(
        'QR image base64 is not a token. Send the decoded token string from GET /attendance-qr/my-qr.',
      );
    }

    const parsed = parsePayloadFromToken(token);
    if (!parsed) {
      throw new BadRequestException('Invalid or malformed attendance token');
    }
    const gym = await this.prisma.gym.findUnique({
      where: { id: parsed.gymId },
      select: { id: true, qrSigningSecret: true },
    });
    if (!gym) {
      throw new BadRequestException('Invalid attendance token (unknown gym)');
    }
    if (!verifySignature(gym.qrSigningSecret, token)) {
      throw new BadRequestException('Invalid attendance token (bad signature)');
    }

    const actorGymUser = await this.prisma.gymUser.findFirst({
      where: {
        gymId: parsed.gymId,
        userId: actorUserId,
        isActive: true,
        role: { in: [GymRole.MEMBER, GymRole.TRAINER, GymRole.STAFF] },
      },
      select: {
        id: true,
        role: true,
        attendanceBlocked: true,
        attendanceBlockedReason: true,
      },
    });
    if (!actorGymUser) {
      throw new ForbiddenException(
        'Logged-in user is not an active member/trainer/staff at this gym',
      );
    }

    if (
      actorGymUser.role === GymRole.MEMBER &&
      actorGymUser.attendanceBlocked
    ) {
      throw new ForbiddenException(
        actorGymUser.attendanceBlockedReason?.trim() ||
        'Attendance is blocked for this member at this gym',
      );
    }

    if (actorGymUser.role === GymRole.MEMBER) {
      return this.punchMember(parsed.gymId, actorUserId);
    }
    if (
      actorGymUser.role !== GymRole.TRAINER &&
      actorGymUser.role !== GymRole.STAFF
    ) {
      throw new BadRequestException('Unsupported role for self punch');
    }

    return this.punchStaffOrTrainer(
      parsed.gymId,
      actorGymUser.id,
      actorUserId,
      actorGymUser.role,
    );
  }

  private async punchMember(gymId: string, memberUserId: string) {
    const attendedOn = utcDateOnly(new Date());
    const openSession = await this.prisma.attendanceRecord.findFirst({
      where: { gymId, memberUserId, attendedOn, checkedOutAt: null },
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

    const row = await this.prisma.attendanceRecord.create({
      data: {
        gymId,
        memberUserId,
        attendedOn,
        checkedInAt: new Date(),
        source: AttendanceSource.QR_TOKEN,
      },
      select: { attendedOn: true, checkedInAt: true },
    });

    return {
      ok: true as const,
      action: 'clock_in' as const,
      attendedOn: row.attendedOn,
      gymId,
      checkedInAt: row.checkedInAt.toISOString(),
    };
  }

  private async punchStaffOrTrainer(
    gymId: string,
    gymUserId: string,
    userId: string,
    role: 'TRAINER' | 'STAFF',
  ) {
    const attendedOn = utcDateOnly(new Date());
    const openSession = await this.prisma.trainerAttendanceRecord.findFirst({
      where: { gymId, trainerUserId: userId, attendedOn, checkedOutAt: null },
      orderBy: { checkedInAt: 'desc' },
    });

    if (openSession) {
      const now = new Date();
      const updated = await this.prisma.trainerAttendanceRecord.update({
        where: { id: openSession.id },
        data: { checkedOutAt: now },
        select: { attendedOn: true, checkedInAt: true, checkedOutAt: true },
      });
      return {
        ok: true as const,
        action: 'clock_out' as const,
        attendedOn: updated.attendedOn,
        gymId,
        gymUserId,
        role,
        checkedInAt: updated.checkedInAt.toISOString(),
        checkedOutAt: updated.checkedOutAt?.toISOString() ?? null,
      };
    }

    const row = await this.prisma.trainerAttendanceRecord.create({
      data: {
        gymId,
        trainerUserId: userId,
        attendedOn,
        checkedInAt: new Date(),
      },
      select: { attendedOn: true, checkedInAt: true },
    });

    return {
      ok: true as const,
      action: 'clock_in' as const,
      attendedOn: row.attendedOn,
      gymId,
      gymUserId,
      role,
      checkedInAt: row.checkedInAt.toISOString(),
    };
  }

  private async punchOwner(gymId: string, ownerUserId: string) {
    const attendedOn = utcDateOnly(new Date());
    const openSession = await this.prisma.trainerAttendanceRecord.findFirst({
      where: {
        gymId,
        trainerUserId: ownerUserId,
        attendedOn,
        checkedOutAt: null,
      },
      orderBy: { checkedInAt: 'desc' },
    });

    if (openSession) {
      const now = new Date();
      const updated = await this.prisma.trainerAttendanceRecord.update({
        where: { id: openSession.id },
        data: { checkedOutAt: now },
        select: { attendedOn: true, checkedInAt: true, checkedOutAt: true },
      });
      return {
        ok: true as const,
        action: 'clock_out' as const,
        attendedOn: updated.attendedOn,
        gymId,
        role: GymRole.OWNER,
        checkedInAt: updated.checkedInAt.toISOString(),
        checkedOutAt: updated.checkedOutAt?.toISOString() ?? null,
      };
    }

    const row = await this.prisma.trainerAttendanceRecord.create({
      data: {
        gymId,
        trainerUserId: ownerUserId,
        attendedOn,
        checkedInAt: new Date(),
      },
      select: { attendedOn: true, checkedInAt: true },
    });

    return {
      ok: true as const,
      action: 'clock_in' as const,
      attendedOn: row.attendedOn,
      gymId,
      role: GymRole.OWNER,
      checkedInAt: row.checkedInAt.toISOString(),
    };
  }
}

function utcDateOnly(d: Date): Date {
  return new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 0, 0, 0, 0),
  );
}

function signPayload(secret: string, payload: PunchQrPayload): string {
  const body = Buffer.from(JSON.stringify(payload), 'utf8');
  const sig = createHmac('sha256', secret).update(body).digest('base64url');
  return `${body.toString('base64url')}.${sig}`;
}

function parsePayloadFromToken(token: string): PunchQrPayload | null {
  const parts = token.split('.');
  if (parts.length !== 2) {
    return null;
  }
  const [a] = parts;
  try {
    const body = Buffer.from(a, 'base64url');
    const parsed = JSON.parse(body.toString('utf8')) as PunchQrPayload;
    if (
      parsed.v !== 3 ||
      parsed.purpose !== 'attendance_punch' ||
      !parsed.gymId ||
      !parsed.gymUserId ||
      !parsed.userId
    ) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function verifySignature(secret: string, token: string): boolean {
  const parts = token.split('.');
  if (parts.length !== 2) {
    return false;
  }
  const [a, b] = parts;
  const body = Buffer.from(a, 'base64url');
  const expected = createHmac('sha256', secret)
    .update(body)
    .digest('base64url');
  const bBuf = Buffer.from(b, 'utf8');
  const eBuf = Buffer.from(expected, 'utf8');
  if (bBuf.length !== eBuf.length) {
    return false;
  }
  return timingSafeEqual(bBuf, eBuf);
}
