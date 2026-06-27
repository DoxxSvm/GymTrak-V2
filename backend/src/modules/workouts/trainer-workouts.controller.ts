import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Query,
} from '@nestjs/common';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateWorkoutDto } from './dto/create-workout.dto';
import { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import { ListTrainerWorkoutsQueryDto } from './dto/list-trainer-workouts-query.dto';
import { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import { UpdateTrainerWorkoutDto } from './dto/update-trainer-workout.dto';
import { WorkoutsService } from './workouts.service';

@Controller('trainers/workouts')
export class TrainerWorkoutsController {
  constructor(private readonly workouts: WorkoutsService) {}

  @Post('exercises')
  createExercise(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: CreateTrainerExerciseDto,
  ) {
    return this.workouts.createExercise(user.sub, query.gymId, body);
  }

  @Get('exercises')
  listExercises(
    @CurrentUser() user: JwtUser,
    @Query() query: ListTrainerExercisesQueryDto,
  ) {
    return this.workouts.listExercises(user.sub, query);
  }

  @Patch('exercises/:exerciseId')
  updateExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerExerciseDto,
  ) {
    return this.workouts.updateExercise(user.sub, query.gymId, exerciseId, body);
  }

  @Delete('exercises/:exerciseId')
  removeExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.workouts.deleteExercise(user.sub, query.gymId, exerciseId);
  }

  @Get()
  listWorkouts(
    @CurrentUser() user: JwtUser,
    @Query() query: ListTrainerWorkoutsQueryDto,
  ) {
    return this.workouts.listTrainerWorkouts(user.sub, query);
  }

  @Post()
  createWorkout(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdQueryDto,
    @Body() body: CreateWorkoutDto,
  ) {
    return this.workouts.createTrainerWorkout(user.sub, query.gymId, body);
  }

  @Get(':workoutId')
  getWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.workouts.getWorkoutDetails(user.sub, workoutId);
  }

  @Patch(':workoutId')
  updateWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateTrainerWorkoutDto,
  ) {
    return this.workouts.updateTrainerWorkout(
      user.sub,
      query.gymId,
      workoutId,
      body,
    );
  }

  @Delete(':workoutId')
  deleteWorkout(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.workouts.deleteTrainerWorkout(user.sub, query.gymId, workoutId);
  }
}
