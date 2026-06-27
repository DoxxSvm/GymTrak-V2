import { IsInt, IsString, Matches, Max, Min } from 'class-validator';

export class TrainerShiftDto {
  /// 0 = Sunday .. 6 = Saturday
  @IsInt()
  @Min(0)
  @Max(6)
  dayOfWeek: number;

  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  startTime: string;

  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  endTime: string;
}
