import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { EventEmitterModule } from '@nestjs/event-emitter';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerModule } from '@nestjs/throttler';
import { AppController } from './app.controller';
import { validateEnv } from './config/env.validation';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';
import { AnalyticsModule } from './modules/analytics/analytics.module';
import { AttendanceModule } from './modules/attendance/attendance.module';
import { AttendanceQrModule } from './modules/attendance-qr/attendance-qr.module';
import { AuthModule } from './modules/auth/auth.module';
import { DashboardModule } from './modules/dashboard/dashboard.module';
import { EnquiriesModule } from './modules/enquiries/enquiries.module';
import { ExpensesModule } from './modules/expenses/expenses.module';
import { FinanceModule } from './modules/finance/finance.module';
import { GymConfigModule } from './modules/gym-config/gym-config.module';
import { GymFeaturesModule } from './modules/gym-features/gym-features.module';
import { GymProfileModule } from './modules/gym-profile/gym-profile.module';
import { GymsModule } from './modules/gyms/gyms.module';
import { MembersModule } from './modules/members/members.module';
import { MealsModule } from './modules/meals/meals.module';
import { DietModule } from './modules/diet/diet.module';
import { MessagingModule } from './modules/messaging/messaging.module';
import { NotificationsModule } from './modules/notifications/notifications.module';
import { OnboardingModule } from './modules/onboarding/onboarding.module';
import { OwnerAppModule } from './modules/owner-app/owner-app.module';
import { PlatformModule } from './platform/platform.module';
import { PlansModule } from './modules/plans/plans.module';
import { ProductsModule } from './modules/products/products.module';
import { RbacModule } from './modules/rbac/rbac.module';
import { PrismaModule } from './modules/prisma/prisma.module';
import { SaasModule } from './modules/saas/saas.module';
import { SearchModule } from './modules/search/search.module';
import { SystemConfigModule } from './modules/system-config/system-config.module';
import { AuditModule } from './modules/audit/audit.module';
import { AdminModule } from './modules/admin/admin.module';
import { HealthModule } from './modules/health/health.module';
import { ProfileModule } from './modules/profile/profile.module';
import { SubscriptionsModule } from './modules/subscriptions/subscriptions.module';
import { TrainerLeavesModule } from './modules/trainer-leaves/trainer-leaves.module';
import { TrainersModule } from './modules/trainers/trainers.module';
import { UploadsModule } from './modules/uploads/uploads.module';
import { WorkoutsModule } from './modules/workouts/workouts.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      validate: validateEnv,
    }),
    ThrottlerModule.forRoot([
      {
        ttl: 60_000,
        limit: 120,
        name: 'default',
      },
    ]),
    EventEmitterModule.forRoot(),
    PrismaModule,
    HealthModule,
    AuthModule,
    PlatformModule,
    AdminModule,
    OnboardingModule,
    OwnerAppModule,
    DashboardModule,
    AnalyticsModule,
    NotificationsModule,
    SubscriptionsModule,
    MembersModule,
    MealsModule,
    DietModule,
    GymFeaturesModule,
    TrainersModule,
    WorkoutsModule,
    PlansModule,
    ProductsModule,
    RbacModule,
    SearchModule,
    ProfileModule,
    AuditModule,
    SaasModule,
    EnquiriesModule,
    ExpensesModule,
    FinanceModule,
    MessagingModule,
    GymProfileModule,
    GymConfigModule,
    SystemConfigModule,
    GymsModule,
    AttendanceModule,
    AttendanceQrModule,
    TrainerLeavesModule,
    UploadsModule,
  ],
  controllers: [AppController],
  providers: [{ provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule {}
