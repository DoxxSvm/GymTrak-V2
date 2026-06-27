import { IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class LeaveBalanceQueryDto extends GymIdQueryDto {
  /** Owner / queue: which trainer (`GymUser.id`). Trainers omit (own balance). */
  @IsOptional()
  @IsString()
  trainerId?: string;
}
