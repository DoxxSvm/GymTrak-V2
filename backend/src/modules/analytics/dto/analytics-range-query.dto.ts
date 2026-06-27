import { IsIn, IsNotEmpty, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export type AnalyticsGranularity = 'day' | 'week' | 'month';

export class AnalyticsRangeQueryDto extends GymIdQueryDto {
  @IsString()
  @IsNotEmpty()
  from!: string;

  @IsString()
  @IsNotEmpty()
  to!: string;

  @IsIn(['day', 'week', 'month'])
  granularity!: AnalyticsGranularity;
}
