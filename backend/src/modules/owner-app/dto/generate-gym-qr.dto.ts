import { Type } from 'class-transformer';
import {
  IsNotEmpty,
  IsNumber,
  IsString,
  MaxLength,
  Min,
} from 'class-validator';

export class GenerateGymQrDto {
  @IsString()
  @IsNotEmpty()
  gymId!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(2000)
  address!: string;

  @Type(() => Number)
  @IsNumber()
  latitude!: number;

  @Type(() => Number)
  @IsNumber()
  longitude!: number;

  /** Radius in meters (e.g. geofence around the gym). */
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  gymRadius!: number;
}
