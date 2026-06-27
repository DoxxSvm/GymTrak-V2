import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

/** `userId` must equal JWT `sub` (common member API). */
export class UnifiedWorkoutUserIdBodyDto {
  @ApiProperty({ description: 'Must match JWT subject (`sub`).' })
  @IsString()
  @IsNotEmpty()
  userId!: string;
}
