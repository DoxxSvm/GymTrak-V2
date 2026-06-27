import { ApiProperty } from '@nestjs/swagger';
import { IsIn, IsNotEmpty, IsOptional, IsString } from 'class-validator';

/** Reusable `gymId` query param for gym-scoped resources */
export class GymIdQueryDto {
  @ApiProperty({ description: 'Target gym id (caller must have access)' })
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsOptional()
  @IsString()
  @IsIn(['TRAINER', 'STAFF'])
  role?: string;
}
