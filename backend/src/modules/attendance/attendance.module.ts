import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { RbacModule } from '../rbac/rbac.module';
import { PrismaModule } from '../prisma/prisma.module';
import { AttendanceController } from './attendance.controller';
import { AttendanceService } from './attendance.service';

@Module({
  imports: [PrismaModule, RbacModule],
  controllers: [AttendanceController],
  providers: [AttendanceService, GymAccessService],
  exports: [AttendanceService],
})
export class AttendanceModule {}
