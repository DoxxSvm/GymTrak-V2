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
import { GymIdQueryDto } from '../../common/dto/gym-id-query.dto';
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
      'Searchable per-gym library (max 500). `search` matches substring on `name` (case-insensitive).',
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
      'Adds a food template for the gym (used when building meals and optional `diet_food_id` on meal lines).',
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
    @Query() query: GymIdQueryDto,
    @Body() body: CreateDietFoodDto,
  ) {
    return this.diet.createFood(user.sub, query.gymId, body);
  }

  @Get(':foodId')
  @ApiOperation({ summary: 'Get food by id' })
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
    @Query() query: GymIdQueryDto,
  ) {
    return this.diet.getFood(user.sub, query.gymId, foodId);
  }

  @Patch(':foodId')
  @ApiOperation({ summary: 'Update catalog food' })
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
    @Query() query: GymIdQueryDto,
    @Body() body: UpdateDietFoodDto,
  ) {
    return this.diet.updateFood(user.sub, query.gymId, foodId, body);
  }

  @Delete(':foodId')
  @ApiOperation({ summary: 'Delete catalog food' })
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
    @Query() query: GymIdQueryDto,
  ) {
    return this.diet.deleteFood(user.sub, query.gymId, foodId);
  }
}
