import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class AttendanceCheckInDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(8000)
  token!: string;
}
