import {
  IsInt,
  IsOptional,
  IsString,
  Matches,
  Max,
  Min,
} from 'class-validator';

export class GymPlanShiftInputDto {
  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  startTime: string;

  @IsString()
  @Matches(/^\d{1,2}:\d{2}$/)
  endTime: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  @Max(100)
  sortOrder?: number;
}
