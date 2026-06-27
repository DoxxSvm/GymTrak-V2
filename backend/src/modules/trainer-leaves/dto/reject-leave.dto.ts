import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class RejectLeaveDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(2000)
  reason!: string;
}
