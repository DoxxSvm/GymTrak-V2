import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { GymAccessService } from '../../common/services/gym-access.service';
import { AttendanceQrController } from './attendance-qr.controller';
import { AttendanceQrService } from './attendance-qr.service';

@Module({
  imports: [PrismaModule],
  controllers: [AttendanceQrController],
  providers: [AttendanceQrService, GymAccessService],
})
export class AttendanceQrModule {}
