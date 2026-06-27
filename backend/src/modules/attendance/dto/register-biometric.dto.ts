import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class RegisterBiometricDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  label?: string | null;
}
