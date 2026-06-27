import { IsNotEmpty, IsOptional, IsString, Matches } from 'class-validator';

export class MemberAttendanceCheckOutDto {
  @IsString()
  @IsNotEmpty()
  member_id: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date?: string;

  @IsOptional()
  @IsString()
  @Matches(/^([01]?\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/)
  time?: string;
}
