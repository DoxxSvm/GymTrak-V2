import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

export class BiometricCheckInDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(512)
  apiKey: string;
}
