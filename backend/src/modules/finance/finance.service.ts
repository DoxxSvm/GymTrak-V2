import { Injectable } from '@nestjs/common';
import { PaymentStatus } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class FinanceService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
  ) {}

  /**
   * All-time completed member payments vs operational expenses (same gym currency assumption as rest of product).
   */
  async summary(actorUserId: string, gymId: string) {
    await this.gymAccess.assertGymOwnerOrSuperAdmin(actorUserId, gymId);
    const [rev, exp] = await Promise.all([
      this.prisma.payment.aggregate({
        where: { gymId, status: PaymentStatus.COMPLETED },
        _sum: { amountCents: true },
      }),
      this.prisma.expense.aggregate({
        where: { gymId },
        _sum: { amountCents: true },
      }),
    ]);
    const total_revenue = rev._sum.amountCents ?? 0;
    const total_expenses = exp._sum.amountCents ?? 0;
    return {
      total_revenue,
      total_expenses,
      net_profit: total_revenue - total_expenses,
    };
  }
}
