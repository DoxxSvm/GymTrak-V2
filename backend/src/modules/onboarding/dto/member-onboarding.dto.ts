import { Type } from 'class-transformer';
import {
  IsNotEmpty,
  IsNumber,
  IsString,
  Max,
  MaxLength,
  Min,
  MinLength,
} from 'class-validator';

export class MemberOnboardingDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(1)
  @MaxLength(120)
  fullName: string;

  @Type(() => Number)
  @IsNumber()
  @Min(50)
  @Max(280)
  heightCm: number;

  @Type(() => Number)
  @IsNumber()
  @Min(20)
  @Max(500)
  weightKg: number;
}
