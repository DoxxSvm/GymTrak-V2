import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import {
  EnquiryStatus,
  GymRole,
  MemberSubscriptionStatus,
  PaymentStatus,
  Prisma,
} from '@prisma/client';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import { getTrainerPermissions } from 'src/common/permissions/permission-codes';
import { balanceDueCents } from '../subscriptions/subscription-lifecycle';

@Injectable()
export class DashboardService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly permissionEngine: PermissionEngineService,
  ) {}

  async getOwnerDashboard(ownerId: string, gymId?: string): Promise<unknown> {
    const isSuperAdmin =
      await this.permissionEngine.isPlatformSuperAdmin(ownerId);
    const gyms = await this.prisma.gym.findMany({
      where: { ownerId },
      select: { id: true, name: true, slug: true, timezone: true },
      orderBy: { name: 'asc' },
    });
    if (gyms.length === 0) {
      const requestedGymId = gymId?.trim();
      if (!requestedGymId) {
        throw new ForbiddenException(
          'Only gym owners can access /dashboard/owner without gymId',
        );
      }
      const linkedMembership = await this.prisma.gymUser.findFirst({
        where: {
          userId: ownerId,
          gymId: requestedGymId,
          isActive: true,
          role: { in: [GymRole.TRAINER, GymRole.STAFF] },
        },
        select: { id: true, role: true },
      });
      if (linkedMembership || isSuperAdmin) {
        return this.getDashboardForUser(ownerId, requestedGymId);
      }
      throw new ForbiddenException(
        'Only gym owners, trainers, or staff can access this dashboard',
      );
    }
    const selected =
      gymId != null && gymId.length > 0
        ? gyms.find((g) => g.id === gymId)
        : gyms[0];
    if (!selected) {
      throw new ForbiddenException('Invalid gym for this owner');
    }
    const gid = selected.id;

    const metrics = await this.loadDashboardMetrics(gid, ownerId, true);
    const permissions = await getTrainerPermissions(this.prisma, ownerId, gid);

    return {
      viewer: 'owner' as const,
      gyms,
      selectedGymId: gid,
      gym: selected,
      permissions,
      ...metrics,
    };
  }

  async getOwnerHomeDashboard(ownerId: string, gymId?: string) {
    const gyms = await this.prisma.gym.findMany({
      where: { ownerId },
      select: { id: true, name: true },
      orderBy: { name: 'asc' },
    });
    if (gyms.length === 0) {
      throw new NotFoundException('No gyms found for this owner');
    }
    const selectedGym = gymId ? gyms.find((g) => g.id === gymId) : gyms[0];
    if (!selectedGym) {
      throw new BadRequestException('Invalid gymId');
    }

    const now = new Date();
    const start = new Date(now);
    start.setUTCHours(0, 0, 0, 0);
    const monthStart = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1),
    );

    const terminalEnquiry: EnquiryStatus[] = [
      EnquiryStatus.CONVERTED,
      EnquiryStatus.CLOSED,
      EnquiryStatus.LOST,
    ];

    const [
      active,
      inactive,
      expired,
      totalMembers,
      monthlyRevenue,
      pendingRevenue,
      recentPayments,
      attendanceRows,
      ownerProfile,
      totalEnquiry,
      enquiryConverted,
      enquiryPending,
    ] = await Promise.all([
      this.prisma.gymUser.count({
        where: {
          gymId: selectedGym.id,
          role: GymRole.MEMBER,
          isLead: false,
          isActive: true,
          membershipEndsAt: { not: null, gte: start },
        },
      }),
      this.prisma.gymUser.count({
        where: {
          gymId: selectedGym.id,
          role: GymRole.MEMBER,
          isActive: false,
        },
      }),
      this.prisma.gymUser.count({
        where: {
          gymId: selectedGym.id,
          role: GymRole.MEMBER,
          isLead: false,
          isActive: true,
          OR: [
            { membershipEndsAt: null },
            { membershipEndsAt: { lt: start } },
          ],
        },
      }),
      this.prisma.gymUser.count({
        where: { gymId: selectedGym.id, role: GymRole.MEMBER },
      }),
      this.prisma.payment.aggregate({
        where: {
          gymId: selectedGym.id,
          status: PaymentStatus.COMPLETED,
          createdAt: { gte: monthStart },
        },
        _sum: { amountCents: true },
      }),
      this.computePendingCollectionCents(selectedGym.id),
      this.prisma.payment.findMany({
        where: { gymId: selectedGym.id },
        orderBy: { createdAt: 'desc' },
        take: 10,
        select: {
          amountCents: true,
          createdAt: true,
          memberUser: { select: { fullName: true } },
        },
      }),
      this.prisma.attendanceRecord.groupBy({
        by: ['attendedOn'],
        where: {
          gymId: selectedGym.id,
          attendedOn: {
            gte: new Date(start.getTime() - 6 * 24 * 60 * 60 * 1000),
            lte: start,
          },
        },
        _count: { _all: true },
      }),
      this.prisma.user.findUnique({
        where: { id: ownerId },
        select: { fullName: true, avatarUrl: true },
      }),
      this.prisma.enquiry.count({ where: { gymId: selectedGym.id } }),
      this.prisma.enquiry.count({
        where: { gymId: selectedGym.id, status: EnquiryStatus.CONVERTED },
      }),
      this.prisma.enquiry.count({
        where: {
          gymId: selectedGym.id,
          status: { notIn: terminalEnquiry },
        },
      }),
    ]);
    const permissions = await this.buildOwnerPermissionsPayload(
      ownerId,
      selectedGym.id,
    );

    const labels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const attendanceMap = new Map(
      attendanceRows.map((r) => [
        new Date(r.attendedOn).toISOString().slice(0, 10),
        r._count._all,
      ]),
    );
    const weekly_attendance = Array.from({ length: 7 }).map((_, i) => {
      const d = new Date(start.getTime() - (6 - i) * 24 * 60 * 60 * 1000);
      const key = d.toISOString().slice(0, 10);
      return { day: labels[d.getUTCDay()], count: attendanceMap.get(key) ?? 0 };
    });

    const trafficTrend = await this.buildTrafficTrend(selectedGym.id, active);

    return {
      greeting: this.getGreeting(now),
      viewer: 'owner' as const,
      gym_name: selectedGym.name,
      selectedGymId: selectedGym.id,
      owner_name: ownerProfile?.fullName ?? '',
      owner_image: ownerProfile?.avatarUrl ?? null,
      permissions,
      total_enquiry: totalEnquiry,
      converted: enquiryConverted,
      pending: enquiryPending,
      stats: {
        active_members: active,
        inactive_members: inactive,
        expired_members: expired,
        total_members: totalMembers,
      },
      revenue: {
        monthly: monthlyRevenue._sum.amountCents ?? 0,
        pending: pendingRevenue,
      },
      recent_payments: recentPayments.map((p) => ({
        member_name: p.memberUser?.fullName ?? '',
        amount: p.amountCents,
        date: p.createdAt,
      })),
      weekly_attendance,
      traffic_trend: trafficTrend,
    };
  }

  /**
   * Unified dashboard: owners get full payload; staff get the same shape with payment
   * sections omitted when they lack `payments:access` (after guards + feature gates).
   */
  async getDashboardForUser(userId: string, gymId: string): Promise<unknown> {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: {
        id: true,
        name: true,
        slug: true,
        timezone: true,
        ownerId: true,
      },
    });
    if (!gym) {
      throw new NotFoundException('Gym not found');
    }
    if (gym.ownerId === userId) {
      return this.getOwnerDashboard(userId, gymId);
    }
    if (await this.permissionEngine.isPlatformSuperAdmin(userId)) {
      const metrics = await this.loadDashboardMetrics(gym.id, userId, true);
      return {
        viewer: 'super_admin' as const,
        selectedGymId: gym.id,
        gym: {
          id: gym.id,
          name: gym.name,
          slug: gym.slug,
          timezone: gym.timezone,
        },
        ...metrics,
      };
    }
    const trainerPermissions = await getTrainerPermissions(
      this.prisma,
      userId,
      gymId,
    );

    const eff = await this.permissionEngine.getEffective(userId, gymId);
    const metrics = await this.loadDashboardMetrics(
      gym.id,
      userId,
      eff.effective.payments,
    );
    return {
      viewer: 'staff' as const,
      selectedGymId: gym.id,
      gym: {
        id: gym.id,
        name: gym.name,
        slug: gym.slug,
        timezone: gym.timezone,
      },
      permissions: trainerPermissions,
      ...metrics,
    };
  }

  private async loadDashboardMetrics(
    gymId: string,
    notificationRecipientUserId: string,
    includePayments: boolean,
  ) {
    const now = new Date();
    const startOfToday = new Date(now);
    startOfToday.setUTCHours(0, 0, 0, 0);

    const thirtyDaysAgo = new Date(now);
    thirtyDaysAgo.setUTCDate(thirtyDaysAgo.getUTCDate() - 30);

    const day1 = new Date(startOfToday);
    day1.setUTCDate(day1.getUTCDate() + 1);
    const day3End = new Date(startOfToday);
    day3End.setUTCDate(day3End.getUTCDate() + 3);
    day3End.setUTCHours(23, 59, 59, 999);

    const day4 = new Date(startOfToday);
    day4.setUTCDate(day4.getUTCDate() + 4);
    const day7End = new Date(startOfToday);
    day7End.setUTCDate(day7End.getUTCDate() + 7);
    day7End.setUTCHours(23, 59, 59, 999);

    const memberWhere: Prisma.GymUserWhereInput = {
      gymId,
      role: GymRole.MEMBER,
    };

    const enquiryTerminal: EnquiryStatus[] = [
      EnquiryStatus.CONVERTED,
      EnquiryStatus.CLOSED,
      EnquiryStatus.LOST,
    ];

    const gymWithOwner = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: {
        owner: { select: { fullName: true, avatarUrl: true } },
      },
    });

    const [
      receivedAgg,
      pendingCollectionCents,
      recentPayments,
      attendanceToday,
      totalMembers,
      activeMembers,
      inactiveMembers,
      expiredMembers,
      expiry1to3,
      expiry4to7,
      enquiriesTotal,
      enquiriesOpen,
      enquiriesConverted,
      enquiriesPendingPipeline,
      unreadNotifications,
    ] = await Promise.all([
      includePayments
        ? this.prisma.payment.aggregate({
            where: {
              gymId,
              status: PaymentStatus.COMPLETED,
              OR: [
                { completedAt: { gte: thirtyDaysAgo } },
                {
                  completedAt: null,
                  createdAt: { gte: thirtyDaysAgo },
                },
              ],
            },
            _sum: { amountCents: true },
          })
        : Promise.resolve({ _sum: { amountCents: null as number | null } }),
      includePayments
        ? this.computePendingCollectionCents(gymId)
        : Promise.resolve(0),
      includePayments
        ? this.prisma.payment.findMany({
            where: { gymId },
            orderBy: { createdAt: 'desc' },
            take: 8,
            select: {
              id: true,
              amountCents: true,
              currency: true,
              status: true,
              reference: true,
              createdAt: true,
              completedAt: true,
              memberUser: {
                select: { id: true, fullName: true, phone: true },
              },
            },
          })
        : Promise.resolve([]),
      this.prisma.attendanceRecord.count({
        where: {
          gymId,
          attendedOn: startOfToday,
        },
      }),
      this.prisma.gymUser.count({ where: memberWhere }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
          isLead: false,
          isActive: true,
          membershipEndsAt: { not: null, gte: startOfToday },
        },
      }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
          isActive: false,
        },
      }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
          isLead: false,
          isActive: true,
          OR: [
            { membershipEndsAt: null },
            { membershipEndsAt: { lt: startOfToday } },
          ],
        },
      }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
          isLead: false,
          isActive: true,
          membershipEndsAt: {
            gte: day1,
            lte: day3End,
          },
        },
      }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
          isLead: false,
          isActive: true,
          membershipEndsAt: {
            gte: day4,
            lte: day7End,
          },
        },
      }),
      this.prisma.enquiry.count({ where: { gymId } }),
      this.prisma.enquiry.count({
        where: { gymId, status: EnquiryStatus.OPEN },
      }),
      this.prisma.enquiry.count({
        where: { gymId, status: EnquiryStatus.CONVERTED },
      }),
      this.prisma.enquiry.count({
        where: { gymId, status: { notIn: enquiryTerminal } },
      }),
      this.prisma.notification.count({
        where: {
          gymId,
          recipientUserId: notificationRecipientUserId,
          readAt: null,
        },
      }),
    ]);

    const received30 = receivedAgg._sum.amountCents ?? 0;
    const pending = pendingCollectionCents;

    const paymentHealth = !includePayments
      ? 50
      : received30 + pending === 0
        ? 55
        : Math.round((100 * received30) / (received30 + pending));

    const attendanceHealth =
      activeMembers === 0
        ? 60
        : Math.min(
            100,
            Math.round((attendanceToday / Math.max(activeMembers, 1)) * 100),
          );

    const expiring = expiry1to3 + expiry4to7;
    const expiryHealth = Math.max(
      0,
      100 -
        Math.min(100, Math.round((expiring / Math.max(activeMembers, 1)) * 40)),
    );

    const healthScore = Math.round(
      0.45 * paymentHealth + 0.3 * attendanceHealth + 0.25 * expiryHealth,
    );

    const trafficTrend = await this.buildTrafficTrend(gymId, activeMembers);

    return {
      owner_name: gymWithOwner?.owner?.fullName ?? '',
      owner_image: gymWithOwner?.owner?.avatarUrl ?? null,
      total_enquiry: enquiriesTotal,
      converted: enquiriesConverted,
      pending: enquiriesPendingPipeline,
      health: {
        score: healthScore,
        factors: {
          payments: paymentHealth,
          attendance: attendanceHealth,
          membership: expiryHealth,
        },
      },
      payments: includePayments
        ? {
            receivedLast30DaysCents: received30,
            pendingCents: pending,
            currency: 'INR',
            recent: recentPayments,
          }
        : {
            hidden: true as const,
            reason: 'payments_module_or_permission',
          },
      attendance: {
        todayCount: attendanceToday,
        date: startOfToday.toISOString(),
      },
      traffic_trend: trafficTrend,
      members: {
        total: totalMembers,
        active: activeMembers,
        inactive: inactiveMembers,
        expired: expiredMembers,
      },
      expiryAlerts: {
        days1to3: expiry1to3,
        days4to7: expiry4to7,
      },
      enquiries: {
        total: enquiriesTotal,
        open: enquiriesOpen,
        converted: enquiriesConverted,
        pending: enquiriesPendingPipeline,
      },
      notifications: {
        unreadCount: unreadNotifications,
      },
    };
  }

  /**
   * Total still to collect: outstanding per current member subscriptions
   * (`priceCents - paidCents`, same as subscription list `balanceDueCents`) plus any
   * `Payment` rows still in `PENDING` (recorded but not completed).
   */
  private async computePendingCollectionCents(gymId: string): Promise<number> {
    const [currentSubs, pendingPayments] = await Promise.all([
      this.prisma.memberSubscription.findMany({
        where: {
          isCurrentSubscription: true,
          status: {
            in: [
              MemberSubscriptionStatus.ACTIVE,
              MemberSubscriptionStatus.SCHEDULED,
              MemberSubscriptionStatus.FROZEN,
            ],
          },
          gymUser: {
            gymId,
            role: GymRole.MEMBER,
            isActive: true,
          },
        },
        select: { priceCents: true, paidCents: true },
      }),
      this.prisma.payment.aggregate({
        where: { gymId, status: PaymentStatus.PENDING },
        _sum: { amountCents: true },
      }),
    ]);

    let total = pendingPayments._sum.amountCents ?? 0;
    for (const s of currentSubs) {
      total += balanceDueCents(s.priceCents, s.paidCents);
    }
    return total;
  }

  /** Get local hour for a Date in a given IANA timezone. */
  private getLocalHour(date: Date, tz: string): number {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: tz,
      hour: 'numeric',
      hour12: false,
    }).formatToParts(date);
    const hourPart = parts.find((p) => p.type === 'hour');
    return hourPart ? parseInt(hourPart.value, 10) : date.getUTCHours();
  }

  /**
   * Traffic trend from member `AttendanceRecord`: current week (Mon–Sun),
   * unique members per day, check-in counts per 3-hour slot (gym timezone).
   */
  private async buildTrafficTrend(gymId: string, activeMembers: number) {
    const gym = await this.prisma.gym.findUnique({
      where: { id: gymId },
      select: { timezone: true },
    });
    const tz = gym?.timezone?.trim() || 'Asia/Kolkata';

    const now = new Date();
    const todayAttendedOn = this.utcDateOnly(now);
    const dayOfWeek = todayAttendedOn.getUTCDay();
    const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const weekStart = new Date(todayAttendedOn);
    weekStart.setUTCDate(weekStart.getUTCDate() + mondayOffset);
    const weekEnd = new Date(weekStart);
    weekEnd.setUTCDate(weekEnd.getUTCDate() + 6);

    const weekRecords = await this.prisma.attendanceRecord.findMany({
      where: {
        gymId,
        attendedOn: { gte: weekStart, lte: weekEnd },
      },
      select: {
        attendedOn: true,
        memberUserId: true,
        checkedInAt: true,
      },
    });

    const dateKey = (d: Date) => d.toISOString().slice(0, 10);
    const todayKey = dateKey(todayAttendedOn);

    const membersByDay = new Map<string, Set<string>>();
    for (const r of weekRecords) {
      const key = dateKey(r.attendedOn);
      if (!membersByDay.has(key)) {
        membersByDay.set(key, new Set());
      }
      membersByDay.get(key)!.add(r.memberUserId);
    }

    const todayMembers = membersByDay.get(todayKey);

    const slots = [
      { label: '6 AM to 9 AM', startHour: 6, endHour: 9 },
      { label: '9 AM to 12 PM', startHour: 9, endHour: 12 },
      { label: '12 PM to 3 PM', startHour: 12, endHour: 15 },
      { label: '3 PM to 6 PM', startHour: 15, endHour: 18 },
      { label: '6 PM to 9 PM', startHour: 18, endHour: 21 },
      { label: '9 PM to 12 AM', startHour: 21, endHour: 24 },
    ];

    const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const todayIndex = (dayOfWeek + 6) % 7;
    const todayLabel = dayLabels[todayIndex];

    const weekly = dayLabels.map((label, i) => {
      const d = new Date(weekStart);
      d.setUTCDate(d.getUTCDate() + i);
      const key = dateKey(d);
      const dayRecords = weekRecords.filter((r) => dateKey(r.attendedOn) === key);
      const count = membersByDay.get(key)?.size ?? 0;
      const hourly = slots.map((slot) => {
        const slotCount = dayRecords.filter((r) => {
          const h = this.getLocalHour(r.checkedInAt, tz);
          return h >= slot.startHour && h < slot.endHour;
        }).length;
        return { label: slot.label, count: slotCount };
      });
      return { day: label, count, hourly };
    });

    return {
      today_count: todayMembers?.size ?? 0,
      capacity: activeMembers,
      selected_day: todayLabel,
      weekly,
    };
  }

  private utcDateOnly(d: Date): Date {
    return new Date(
      Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate(), 0, 0, 0, 0),
    );
  }

  private getGreeting(now: Date): string {
    const h = now.getHours();
    if (h < 12) {
      return 'Good Morning';
    }
    if (h < 17) {
      return 'Good Afternoon';
    }
    return 'Good Evening';
  }

  private async buildOwnerPermissionsPayload(
    ownerUserId: string,
    gymId: string,
  ) {
    const [effective, trainerDefaults, staffDefaults] = await Promise.all([
      this.permissionEngine.getEffective(ownerUserId, gymId),
      this.permissionEngine.getRoleDefaults(GymRole.TRAINER),
      this.permissionEngine.getRoleDefaults(GymRole.STAFF),
    ]);

    const expandedPermissions =
      this.permissionEngine.expandEffectivePermissions(effective.effective);
    return {
      role: effective.role,
      permissions: expandedPermissions,
      effective: expandedPermissions,
      roleDefaults: {
        trainer: trainerDefaults,
        staff: staffDefaults,
      },
    };
  }
}
