import {
  BadRequestException,
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
  UseGuards,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiForbiddenResponse,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { AddMemberSubscriptionDto } from './dto/add-subscription.dto';
import { CreateMemberDto } from './dto/create-member.dto';
import { MemberListQueryDto } from './dto/member-list-query.dto';
import { MemberTabQueryDto } from './dto/member-tab-query.dto';
import { ReceivePaymentDto } from './dto/receive-payment.dto';
import { UpdateMemberDto } from './dto/update-member.dto';
import { MemberListFilter } from './member-list-filter';
import { AddDietEntryDto } from './dto/add-diet-entry.dto';
import {
  MemberAttendanceHistoryQueryDto,
  MemberAttendanceSummaryQueryDto,
} from './dto/member-attendance-query.dto';
import {
  MemberDetailResponseSwagger,
  MemberProfileCardSwagger,
} from './dto/member-detail.swagger';
import { MembersService } from './members.service';

@ApiTags('Members')
@ApiBearerAuth()
@Controller('members')
@UseGuards(PermissionsGuard)
@RequirePermissions(PERMISSION_CODES.MEMBERS)
export class MembersController {
  constructor(private readonly members: MembersService) {}

  @Get()
  @ApiOperation({
    summary: 'List members',
    description:
      'Requires Bearer auth. Gym owner is allowed automatically; trainer/staff need `members:manage`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description:
      'Gym context (may be inferred when the owner has exactly one gym).',
  })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @ApiQuery({ name: 'search', required: false })
  @ApiQuery({ name: 'q', required: false, description: 'Alias for search' })
  @ApiQuery({
    name: 'status',
    required: false,
    enum: ['all', 'active', 'expired', 'inactive'],
  })
  list(@CurrentUser() user: JwtUser, @Query() query: MemberListQueryDto) {
    const limit = query.limit ?? 20;
    const page = query.page ?? 1;
    const offset = query.offset ?? (page - 1) * limit;
    const q = query.search ?? query.q;
    const filter = this.resolveMemberListFilter(query);
    return this.members.list(user.sub, query.gymId, q, filter, limit, offset);
  }

  private resolveMemberListFilter(
    query: MemberListQueryDto,
  ): MemberListFilter | undefined {
    if (query.active === true && query.expired === true) {
      throw new BadRequestException(
        'Use only one of active=true or expired=true (or use status=active|expired).',
      );
    }
    if (query.status != null && query.status !== 'all') {
      return query.status === 'active'
        ? MemberListFilter.ACTIVE
        : query.status === 'expired'
          ? MemberListFilter.EXPIRED
          : query.status === 'inactive'
            ? MemberListFilter.INACTIVE
            : undefined;
    }
    if (query.expired === true) {
      return MemberListFilter.EXPIRED;
    }
    if (query.active === true) {
      return MemberListFilter.ACTIVE;
    }
    return query.filter;
  }

  @Post()
  create(@CurrentUser() user: JwtUser, @Body() body: CreateMemberDto) {
    return this.members.create(user.sub, body);
  }

  @Get(':memberId')
  @ApiOperation({
    summary: 'Member detail profile',
    description:
      '**Path:** `memberId` = GymUser id from `GET /members` (`members[].id` or `items[].gymUserId`). **Query:** `gymId` (required). Returns `summary` (list-card shape), `subscription` (`stats` + `current_subscription`), membership fields, `user` (incl. `avatarUrl`), `contact`, `tabs`. Bearer required; owner allowed; trainer/staff need `members:manage`. OpenAPI: `MemberDetailResponse` in `docs/gymtrak-api.openapi.json` and `docs/member-detail-profile.openapi.yaml`.',
  })
  @ApiParam({
    name: 'memberId',
    description: 'GymUser id (member row id from the list)',
  })
  @ApiQuery({
    name: 'gymId',
    required: true,
    description: 'Gym context',
  })
  @ApiOkResponse({
    description: 'Member detail',
    type: MemberDetailResponseSwagger,
  })
  @ApiForbiddenResponse({
    description: 'Insufficient permissions for this gym',
  })
  @ApiNotFoundResponse({ description: 'Member not found' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.getDetail(user.sub, query.gymId, memberId);
  }

  @Get(':memberId/profile')
  @ApiOperation({
    summary: 'Member profile (legacy flat shape)',
    description:
      'Flat `stats` + `current_subscription`. Prefer `GET /members/:memberId` for full detail. Schema: `MemberProfileCardResponse` in `docs/gymtrak-api.openapi.json` / `docs/member-detail-profile.openapi.yaml`.',
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiOkResponse({
    description: 'Profile card',
    type: MemberProfileCardSwagger,
  })
  @ApiForbiddenResponse({ description: 'Insufficient permissions' })
  @ApiNotFoundResponse({ description: 'Member not found' })
  profile(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.getProfile(user.sub, query.gymId, memberId);
  }

  @Patch(':memberId')
  update(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.update(user.sub, query.gymId, memberId, body);
  }

  @Put(':memberId')
  replace(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateMemberDto,
  ) {
    return this.members.update(user.sub, query.gymId, memberId, body);
  }

  @Delete(':memberId')
  remove(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.softDelete(user.sub, query.gymId, memberId);
  }

  @Get(':memberId/subscriptions')
  listSubscriptions(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listSubscriptions(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
    );
  }

  @Post(':memberId/subscriptions')
  addSubscription(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: AddMemberSubscriptionDto,
  ) {
    return this.members.addSubscription(user.sub, query.gymId, memberId, body);
  }

  @Get(':memberId/attendance/summary')
  @ApiOperation({
    summary: 'Member attendance — month summary',
    description:
      'Calendar month (UTC grid) + stats + punctuality-labelled recent_logs + months_overview. Default month from gym timezone when month/year omitted. Query: gymId (required), month, year, monthsOverviewLimit, recentLimit.',
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'month', required: false, description: '1–12 with year' })
  @ApiQuery({ name: 'year', required: false })
  @ApiQuery({
    name: 'monthsOverviewLimit',
    required: false,
    description: 'Max months_overview rows (default 24, max 60)',
  })
  @ApiQuery({
    name: 'recentLimit',
    required: false,
    description: 'Max recent_logs (default 20, max 50)',
  })
  attendanceSummary(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberAttendanceSummaryQueryDto,
  ) {
    return this.members.getAttendanceSummary(user.sub, query.gymId, memberId, {
      month: query.month,
      year: query.year,
      monthsOverviewLimit: query.monthsOverviewLimit,
      recentLimit: query.recentLimit,
    });
  }

  @Get(':memberId/attendance/history')
  @ApiOperation({
    summary: 'Member attendance — paginated history',
    description:
      'Optional from/to (YYYY-MM-DD UTC on attendedOn). OpenAPI: MemberAttendanceHistoryResponse in docs/gymtrak-api.openapi.json.',
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'from', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'to', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'offset', required: false })
  attendanceHistory(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberAttendanceHistoryQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.getAttendanceHistory(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
      query.from,
      query.to,
    );
  }

  /** @deprecated Prefer `GET .../attendance/summary` or `.../history`. */
  @Get(':memberId/attendance')
  @ApiOperation({
    summary: 'Member attendance (legacy)',
    description:
      'Deprecated. With month+year → same as attendance/summary; else same as attendance/history.',
    deprecated: true,
  })
  @ApiParam({ name: 'memberId', description: 'GymUser id' })
  @ApiQuery({ name: 'gymId', required: true })
  @ApiQuery({ name: 'month', required: false })
  @ApiQuery({ name: 'year', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'offset', required: false })
  listAttendance(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listAttendance(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
      query.month,
      query.year,
    );
  }

  @Get(':memberId/payments')
  listPayments(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: MemberTabQueryDto,
  ) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.members.listPayments(
      user.sub,
      query.gymId,
      memberId,
      limit,
      offset,
    );
  }

  @Get(':memberId/payment-summary')
  paymentSummary(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.getPaymentSummary(user.sub, query.gymId, memberId);
  }

  @Post(':memberId/payments')
  receivePayment(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
    @Body() body: ReceivePaymentDto,
  ) {
    return this.members.receivePayment(user.sub, query.gymId, memberId, body);
  }

  @Get(':memberId/diet')
  diet(
    @CurrentUser() user: JwtUser,
    @Param('memberId') memberId: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.members.listDiet(user.sub, query.gymId, memberId);
  }

  @Post('/diet')
  addDiet(@CurrentUser() user: JwtUser, @Body() body: AddDietEntryDto) {
    return this.members.addDietEntry(user.sub, body);
  }
}
