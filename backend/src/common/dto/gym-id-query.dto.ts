import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

/** Reusable `gymId` query param for gym-scoped resources */
export class GymIdQueryDto {
  @ApiProperty({ description: 'Target gym id (caller must have access)' })
  @IsString()
  @IsNotEmpty()
  gymId: string;
}
