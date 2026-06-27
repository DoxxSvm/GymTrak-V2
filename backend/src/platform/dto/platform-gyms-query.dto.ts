import { GymStatus } from '@prisma/client';
import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { PaginationQueryDto } from '../../common/dto/pagination-query.dto';

export class PlatformGymsQueryDto extends PaginationQueryDto {
  @IsOptional()
  @IsEnum(GymStatus)
  status?: GymStatus;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  q?: string;
}
