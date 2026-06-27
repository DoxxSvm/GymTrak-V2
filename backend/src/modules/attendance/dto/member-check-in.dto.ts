import { IsNotEmpty, IsOptional, IsString, Matches } from 'class-validator';

export class MemberAttendanceCheckInDto {
  /** GymUser id for the member (same as other member routes) */
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date?: string;

  /** HH:mm or HH:mm:ss (UTC interpretation on the given date) */
  @IsOptional()
  @IsString()
  @Matches(/^([01]?\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/)
  time?: string;
}
