import {
  IsIn,
  IsNotEmpty,
  IsOptional,
  IsString,
  Matches,
} from 'class-validator';

export class MarkAttendanceDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsString()
  @IsNotEmpty()
  date: string;

  /** HH:mm or HH:mm:ss — combined with `date` for checkedInAt when status is present */
  @IsOptional()
  @IsString()
  @Matches(/^([01]?\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/)
  time?: string;

  @IsString()
  @IsIn(['present', 'absent'])
  status: 'present' | 'absent';
}
