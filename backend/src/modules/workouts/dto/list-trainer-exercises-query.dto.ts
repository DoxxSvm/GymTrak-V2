import { ExerciseEquipment, Muscle } from '@prisma/client';
import { IsEnum, IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class ListTrainerExercisesQueryDto extends GymIdQueryDto {
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
