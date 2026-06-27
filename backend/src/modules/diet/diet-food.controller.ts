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
} from '@nestjs/common';
import {
  ApiBadRequestResponse,
  ApiBearerAuth,
  ApiCreatedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiQuery,
  ApiTags,
  ApiUnauthorizedResponse,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import {
  DietFoodResponseSwagger,
  DietMutationSuccessSwagger,
} from './dto/diet-api.swagger';
import { CreateDietFoodDto } from './dto/create-diet-food.dto';
import { ListDietFoodQueryDto } from './dto/list-diet-food-query.dto';
import { UpdateDietFoodDto } from './dto/update-diet-food.dto';
import { DietService } from './diet.service';

@ApiTags('Diet')
@ApiBearerAuth()
@Controller('diet/food')
export class DietFoodController {
  constructor(private readonly diet: DietService) {}

  @Get()
  @ApiOperation({
    summary: 'List food catalog',
    description:
      '**`gymId` set:** rows for that gym with `userId` = you (max 500). Requires gym manage access. **`gymId` omitted:** your personal items (`gymId` null). `search` matches `name` (case-insensitive).',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Gym context; omit for personal (user-only) foods',
  })
  @ApiQuery({
    name: 'search',
    required: false,
    description: 'Filter by name substring',
  })
  @ApiOkResponse({
    description: 'Catalog foods',
    type: DietFoodResponseSwagger,
    isArray: true,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  list(@CurrentUser() user: JwtUser, @Query() query: ListDietFoodQueryDto) {
    return this.diet.listFoods(user.sub, query);
  }

  @Post()
  @HttpCode(201)
  @ApiOperation({
    summary: 'Create catalog food',
    description:
      '**`?gymId=`** → row with that `gymId` and `userId` = you (gym must be manageable). **No `gymId`** → personal row (`gymId` null, `userId` = you). JWT `gymId` is not used; pass `gymId` in the query to target a gym.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Omit to create a personal (non-gym) food',
  })
  @ApiCreatedResponse({
    description: 'Created food',
    type: DietFoodResponseSwagger,
  })
  @ApiBadRequestResponse({ description: 'Invalid body' })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  create(
    @CurrentUser() user: JwtUser,
    @Body() body: CreateDietFoodDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.createFood(user.sub, body, gymId);
  }

  @Get(':foodId')
  @ApiOperation({
    summary: 'Get food by id',
    description:
      'Gym row: pass `gymId` to match. Personal row (`gymId` null on record): do not pass `gymId`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the food is gym-scoped',
  })
  @ApiParam({ name: 'foodId', description: 'Catalog food id' })
  @ApiOkResponse({
    description: 'Catalog food',
    type: DietFoodResponseSwagger,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Food not found' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('foodId') foodId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.getFood(user.sub, foodId, gymId);
  }

  @Patch(':foodId')
  @ApiOperation({
    summary: 'Update catalog food',
    description: 'Same `gymId` rules as `GET` / `DELETE` — owner-only.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the food is gym-scoped',
  })
  @ApiParam({ name: 'foodId', description: 'Catalog food id' })
  @ApiOkResponse({
    description: 'Updated food',
    type: DietFoodResponseSwagger,
  })
  @ApiBadRequestResponse({ description: 'No fields to update' })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Food not found' })
  update(
    @CurrentUser() user: JwtUser,
    @Param('foodId') foodId: string,
    @Body() body: UpdateDietFoodDto,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.updateFood(user.sub, foodId, body, gymId);
  }

  @Delete(':foodId')
  @ApiOperation({
    summary: 'Delete catalog food',
    description: 'Same `gymId` rules as `GET`.',
  })
  @ApiQuery({
    name: 'gymId',
    required: false,
    description: 'Required when the food is gym-scoped',
  })
  @ApiParam({ name: 'foodId', description: 'Catalog food id' })
  @ApiOkResponse({
    description: 'Deletion acknowledged',
    type: DietMutationSuccessSwagger,
  })
  @ApiUnauthorizedResponse({ description: 'Missing or invalid Bearer token' })
  @ApiForbiddenResponse({ description: 'No access to this gym' })
  @ApiNotFoundResponse({ description: 'Food not found' })
  remove(
    @CurrentUser() user: JwtUser,
    @Param('foodId') foodId: string,
    @Query('gymId') gymId?: string,
  ) {
    return this.diet.deleteFood(user.sub, foodId, gymId);
  }
}
