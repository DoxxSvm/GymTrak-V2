import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { EnquiryStatus, GymRole, PaymentStatus, Prisma } from '@prisma/client';
import {
  PermissionEngineService,
} from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class DashboardService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly permissionEngine: PermissionEngineService,
  ) {}

  async getOwnerDashboard(
    ownerId: string,
    gymId?: string,
  ): Promise<unknown> {
    const isSuperAdmin = await this.permissionEngine.isPlatformSuperAdmin(
      ownerId,
    );
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
    const permissions = await this.buildOwnerPermissionsPayload(ownerId, gid);

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
          isActive: true,
          OR: [
            { membershipEndsAt: null },
            { membershipEndsAt: { gte: start } },
          ],
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
          isActive: true,
          membershipEndsAt: { not: null, lt: start },
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
      this.prisma.payment.aggregate({
        where: { gymId: selectedGym.id, status: PaymentStatus.PENDING },
        _sum: { amountCents: true },
      }),
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
        monthly: Math.round((monthlyRevenue._sum.amountCents ?? 0) / 100),
        pending: Math.round((pendingRevenue._sum.amountCents ?? 0) / 100),
      },
      recent_payments: recentPayments.map((p) => ({
        member_name: p.memberUser?.fullName ?? '',
        amount: Math.round(p.amountCents / 100),
        date: p.createdAt,
      })),
      weekly_attendance,
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
      effectivePermissions: this.permissionEngine.expandEffectivePermissions(
        eff.effective,
      ),
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
      pendingAgg,
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
        ? this.prisma.payment.aggregate({
            where: {
              gymId,
              status: PaymentStatus.PENDING,
            },
            _sum: { amountCents: true },
          })
        : Promise.resolve({ _sum: { amountCents: null as number | null } }),
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
          isActive: true,
          OR: [
            { membershipEndsAt: null },
            { membershipEndsAt: { gte: startOfToday } },
          ],
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
          isActive: true,
          membershipEndsAt: { not: null, lt: startOfToday },
        },
      }),
      this.prisma.gymUser.count({
        where: {
          ...memberWhere,
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
    const pending = pendingAgg._sum.amountCents ?? 0;

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
            currency: 'USD',
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

    return {
      role: effective.role,
      effective: this.permissionEngine.expandEffectivePermissions(
        effective.effective,
      ),
      roleDefaults: {
        trainer: trainerDefaults,
        staff: staffDefaults,
      },
    };
  }
}
