import { ExerciseEquipment, Muscle } from '@prisma/client';
import { IsEnum, IsOptional, IsString } from 'class-validator';
import { OptionalGymIdQueryDto } from '../../../common/dto/optional-gym-id-query.dto';

export class ListTrainerExercisesQueryDto extends OptionalGymIdQueryDto {
  @IsOptional()
  @IsString()
  search?: string;

  @IsOptional()
  @IsEnum(ExerciseEquipment)
  equipment?: ExerciseEquipment;

  @IsOptional()
  @IsEnum(Muscle)
  muscle?: Muscle;
}
