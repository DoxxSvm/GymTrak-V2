import { Controller, Get, Query } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { LeaderboardQueryDto } from './dto/leaderboard-query.dto';
import {
  LeaderboardService,
  type LeaderboardResponse,
} from './leaderboard.service';

@ApiTags('Leaderboard')
@ApiBearerAuth()
@Controller('leaderboard')
export class LeaderboardController {
  constructor(private readonly leaderboard: LeaderboardService) {}

  @Get()
  @ApiOperation({
    summary: 'Gym member leaderboard (attendance or completed workouts)',
    description:
      'Ranks active enrolled members (`GymUser` role MEMBER, not lead). **Attendance:** distinct check-in days are counted via `AttendanceRecord` rows per gym. **Workout:** count of `MemberWorkoutPlan` rows with `completed=true` for that gym. Ties are broken by `userId` for stable ordering; ranks are sequential (1,2,3,…). Caller must have any active membership at the gym, be owner, or super-admin.',
  })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({
    name: 'type',
    required: true,
    enum: ['attendance', 'workout'],
  })
  @ApiQuery({ name: 'page', required: false, example: '1' })
  @ApiQuery({ name: 'limit', required: false, example: '20' })
  get(
    @CurrentUser() user: JwtUser,
    @Query() query: LeaderboardQueryDto,
  ): Promise<LeaderboardResponse> {
    return this.leaderboard.get(user.sub, query);
  }
}
