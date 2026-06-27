import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';

import {
  ApiBearerAuth,
  ApiCreatedResponse,
  ApiExcludeEndpoint,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';

import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateExpenseDto } from './dto/create-expense.dto';
import {
  ExpenseDeleteOkSwagger,
  ExpenseListResponseSwagger,
  ExpenseResponseSwagger,
} from './dto/expense-api.swagger';

import { ExpenseListQueryDto } from './dto/expense-list-query.dto';
import { MonthlySummaryQueryDto } from './dto/monthly-summary-query.dto';
import { UpdateExpenseDto } from './dto/update-expense.dto';
import { ExpensesService } from './expenses.service';

@ApiTags('Expenses')
@ApiBearerAuth()
@Controller('expenses')
export class ExpensesController {
  constructor(private readonly expenses: ExpensesService) {}

  @Get('monthly-summary')
  @ApiExcludeEndpoint()
  monthlySummary(
    @CurrentUser() user: JwtUser,
    @Query() query: MonthlySummaryQueryDto,
  ) {
    const now = new Date();
    const year = query.year ?? now.getUTCFullYear();
    const month = query.month ?? now.getUTCMonth() + 1;
    return this.expenses.monthlySummary(user.sub, query.gymId, year, month);
  }

  @Get()
  @ApiOperation({ summary: 'List expenses' })
  @ApiOkResponse({ type: ExpenseListResponseSwagger })
  list(@CurrentUser() user: JwtUser, @Query() query: ExpenseListQueryDto) {
    const limit = query.limit ?? 20;
    const offset = query.offset ?? 0;
    return this.expenses.list(
      user.sub,
      query.gymId,
      query.category,
      query.month,
      query.dateFrom,
      query.dateTo,
      limit,
      offset,
      query.filter,
      query.format,
      query.sortBy,
      query.sortOrder,
      query.year,
    );
  }

  @Post()
  @HttpCode(201)
  @ApiOperation({ summary: 'Create expense' })
  @ApiCreatedResponse({ type: ExpenseResponseSwagger })
  create(@CurrentUser() user: JwtUser, @Body() body: CreateExpenseDto) {
    return this.expenses.create(user.sub, body);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get expense by id' })
  @ApiOkResponse({ type: ExpenseResponseSwagger })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.expenses.getOne(user.sub, query.gymId, id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update expense' })
  @ApiOkResponse({ type: ExpenseResponseSwagger })
  update(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateExpenseDto,
  ) {
    return this.expenses.update(user.sub, query.gymId, id, body);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete expense' })
  @ApiOkResponse({ type: ExpenseDeleteOkSwagger })
  remove(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdQueryDto,
  ) {
    return this.expenses.remove(user.sub, query.gymId, id);
  }
}
