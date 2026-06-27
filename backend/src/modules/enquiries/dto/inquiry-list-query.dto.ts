import { Type } from 'class-transformer';
import { IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class InquiryListQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsIn(['pending', 'converted', 'lost'])
  status?: 'pending' | 'converted' | 'lost';

  @IsOptional()
  @IsString()
  q?: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(0)
  offset?: number = 0;
}
