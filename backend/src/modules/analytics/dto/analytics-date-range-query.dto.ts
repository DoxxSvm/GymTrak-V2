import { IsNotEmpty, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

/** Date range without chart bucket (e.g. monthly revenue table). */
export class AnalyticsDateRangeQueryDto extends GymIdQueryDto {
  @IsString()
  @IsNotEmpty()
  from!: string;

  @IsString()
  @IsNotEmpty()
  to!: string;
}
