import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class ListMemberPersonalExercisesQueryDto {
  @ApiPropertyOptional({
    description: 'Case-insensitive substring match on exercise name',
  })
  @IsOptional()
  @IsString()
  search?: string;
}
