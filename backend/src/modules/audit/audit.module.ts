import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PrismaModule } from '../prisma/prisma.module';
import { RbacModule } from '../rbac/rbac.module';
import { AuditController } from './audit.controller';
import { AuditService } from './audit.service';

@Module({
  imports: [PrismaModule, RbacModule],
  controllers: [AuditController],
  providers: [AuditService, GymAccessService],
  exports: [AuditService],
})
export class AuditModule {}
