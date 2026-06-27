import { ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsNumber,
  IsOptional,
  IsString,
  Max,
  Min,
  MinLength,
} from 'class-validator';

/** Required when the caller does not yet have a member persona profile row. */
export class SwitchToMemberDto {
  @ApiPropertyOptional({ minLength: 1, maxLength: 120 })
  @IsOptional()
  @IsString()
  @MinLength(1)
  name?: string;

  @ApiPropertyOptional({ minimum: 1, maximum: 120 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(120)
  age?: number;

  @ApiPropertyOptional({ minLength: 1, maxLength: 16 })
  @IsOptional()
  @IsString()
  @MinLength(1)
  gender?: string;

  /** Height in centimetres */
  @ApiPropertyOptional({ minimum: 50, maximum: 280 })
  @IsOptional()
  @IsNumber()
  @Min(50)
  @Max(280)
  height?: number;

  /** Weight in kilograms */
  @ApiPropertyOptional({ minimum: 15, maximum: 400 })
  @IsOptional()
  @IsNumber()
  @Min(15)
  @Max(400)
  weight?: number;
}
