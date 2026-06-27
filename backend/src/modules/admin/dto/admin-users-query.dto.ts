import { Type } from 'class-transformer';
import { IsEnum, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';

/** Filters gym-scoped roles (TRAINER includes STAFF in queries). */
export enum AdminUserGymRoleFilter {
  OWNER = 'OWNER',
  TRAINER = 'TRAINER',
  MEMBER = 'MEMBER',
}

export class AdminUsersQueryDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit?: number = 20;

  @IsOptional()
  @IsEnum(AdminUserGymRoleFilter)
  role?: AdminUserGymRoleFilter;

  @IsOptional()
  @IsString()
  gymId?: string;
}
