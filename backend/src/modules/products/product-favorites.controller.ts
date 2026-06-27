import { Controller, Delete, Get, Param, Post, Query } from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiParam,
  ApiTags,
} from '@nestjs/swagger';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { GymIdFlexibleQueryDto } from './dto/gym-id-flexible-query.dto';
import { ProductsService } from './products.service';

@ApiTags('Favorites')
@ApiBearerAuth()
@Controller()
export class ProductFavoritesController {
  constructor(private readonly products: ProductsService) {}

  @Get('favorites')
  @ApiOperation({
    summary: 'List my favorite products',
    description:
      'Requires `gymId` or `gym_id`. Any active gym member at that gym.',
  })
  listFavorites(
    @CurrentUser() user: JwtUser,
    @Query() query: GymIdFlexibleQueryDto,
  ) {
    return this.products.listFavorites(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
    );
  }

  @Post('favorites/:product_id')
  @ApiOperation({ summary: 'Add product to favorites' })
  @ApiParam({ name: 'product_id' })
  addFavorite(
    @CurrentUser() user: JwtUser,
    @Param('product_id') productId: string,
    @Query() query: GymIdFlexibleQueryDto,
  ) {
    return this.products.addFavorite(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
      productId,
    );
  }

  @Delete('favorites/:product_id')
  @ApiOperation({ summary: 'Remove product from favorites' })
  @ApiParam({ name: 'product_id' })
  removeFavorite(
    @CurrentUser() user: JwtUser,
    @Param('product_id') productId: string,
    @Query() query: GymIdFlexibleQueryDto,
  ) {
    return this.products.removeFavorite(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
      productId,
    );
  }
}
