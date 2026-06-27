import { GymFeatureKey } from '@prisma/client';
import { IsBoolean, IsEnum, IsNotEmpty, IsString } from 'class-validator';

export class SetGymFeatureDto {
  @IsString()
  @IsNotEmpty()
  gymId: string;

  @IsEnum(GymFeatureKey)
  key: GymFeatureKey;

  @IsBoolean()
  enabled: boolean;
}
