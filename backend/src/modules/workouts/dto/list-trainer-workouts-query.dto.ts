import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional } from 'class-validator';
import { OptionalGymIdQueryDto } from '../../../common/dto/optional-gym-id-query.dto';

/**
 * Optional `gymId`: workouts for `user_id` = JWT subject in that gym.
 * Omit `gymId`: personal workouts (`gym_id` null) for that user.
 */
export class ListTrainerWorkoutsQueryDto extends OptionalGymIdQueryDto {
  @ApiPropertyOptional({
    enum: ['all', 'member', 'trainer'],
    description:
      '`member` = logged by MEMBER; `trainer` = OWNER, TRAINER, or STAFF; `all` = no filter.',
  })
  @IsOptional()
  @IsIn(['all', 'member', 'trainer'])
  created_by?: 'all' | 'member' | 'trainer';
}
