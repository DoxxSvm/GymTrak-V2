import { ApiProperty } from '@nestjs/swagger';
import { IsInt, Max, Min } from 'class-validator';

export class UpdateWorkoutCompletionDto {
  @ApiProperty({
    description: 'Editable workout duration in whole minutes.',
    example: 45,
    minimum: 0,
    maximum: 1440,
  })
  @IsInt()
  @Min(0)
  @Max(1440)
  duration_minutes: number;
}
