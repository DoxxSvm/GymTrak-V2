import { IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

/** List member workouts in a gym; omit `member_id` to list all members’ workouts. */
export class ListTrainerWorkoutsQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsString()
  member_id?: string;
}
