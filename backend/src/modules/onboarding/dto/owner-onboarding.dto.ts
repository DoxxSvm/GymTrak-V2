import { IsNotEmpty, IsString, MaxLength, MinLength } from 'class-validator';

export class OwnerOnboardingDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(2)
  @MaxLength(120)
  gymName: string;
}
