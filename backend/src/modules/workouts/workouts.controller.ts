import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
} from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AddExerciseToWorkoutDto } from './dto/add-exercise-to-workout.dto';
import { AddSetDto } from './dto/add-set.dto';
import { CreateTrainerExerciseDto } from './dto/create-trainer-exercise.dto';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { ListTrainerExercisesQueryDto } from './dto/list-trainer-exercises-query.dto';
import { UpdateTrainerExerciseDto } from './dto/update-trainer-exercise.dto';
import { CreateWorkoutDto } from './dto/create-workout.dto';
import { UpdateSetDto } from './dto/update-set.dto';
import { WorkoutsService } from './workouts.service';

@Controller()
export class WorkoutsController {
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
  deleteCatalogExercise(
    @CurrentUser() user: JwtUser,
    @Param('exerciseId') exerciseId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.workouts.deleteExercise(user.sub, query.gymId, exerciseId);
  }

  @Post('workouts')
  createWorkout(@CurrentUser() user: JwtUser, @Body() body: CreateWorkoutDto) {
    return this.workouts.createWorkout(user.sub, body);
  }

  @Get('members/:memberId/workouts')
  memberWorkouts(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
  ) {
    return this.workouts.listMemberWorkouts(user.sub, memberId);
  }

  @Get('workouts/:workoutId')
  details(@CurrentUser() user: JwtUser, @Param('workoutId') workoutId: string) {
    return this.workouts.getWorkoutDetails(user.sub, workoutId);
  }

  @Post('workouts/:workoutId/exercises')
  addExercise(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Body() body: AddExerciseToWorkoutDto,
  ) {
    return this.workouts.addExerciseToWorkout(user.sub, workoutId, body);
  }

  @Delete('workouts/:workoutId/exercises/:exerciseId')
  removeExercise(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
    @Param('exerciseId') exerciseId: string,
  ) {
    return this.workouts.deleteExerciseFromWorkout(
      user.sub,
      workoutId,
      exerciseId,
    );
  }

  @Put('sets/:setId')
  updateSet(
    @CurrentUser() user: JwtUser,
    @Param('setId') setId: string,
    @Body() body: UpdateSetDto,
  ) {
    return this.workouts.updateSet(user.sub, setId, body);
  }

  @Post('exercise-sets')
  addSet(@CurrentUser() user: JwtUser, @Body() body: AddSetDto) {
    return this.workouts.addSet(user.sub, body);
  }

  @Post('workouts/:workoutId/complete')
  complete(
    @CurrentUser() user: JwtUser,
    @Param('workoutId') workoutId: string,
  ) {
    return this.workouts.completeWorkout(user.sub, workoutId);
  }
}
