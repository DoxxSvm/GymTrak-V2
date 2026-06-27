import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { MembersModule } from '../members/members.module';
import { SaasModule } from '../saas/saas.module';
import { EnquiriesController } from './enquiries.controller';
import { InquiriesCompatController } from './inquiries-compat.controller';
import { EnquiriesService } from './enquiries.service';

@Module({
  imports: [MembersModule, SaasModule],
  controllers: [EnquiriesController, InquiriesCompatController],
  providers: [EnquiriesService, GymAccessService],
})
export class EnquiriesModule {}
