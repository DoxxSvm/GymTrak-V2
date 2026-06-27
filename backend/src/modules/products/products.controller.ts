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
  ValidationPipe,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiParam,
  ApiTags,
} from '@nestjs/swagger';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { RequirePermissions } from '../../common/decorators/require-permissions.decorator';
import { PermissionsGuard } from '../../common/guards/permissions.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { GymIdFlexibleQueryDto } from './dto/gym-id-flexible-query.dto';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { CreateProductDto } from './dto/create-product.dto';
import { ProductListQueryDto } from './dto/product-list-query.dto';
import { UpdateProductDto } from './dto/update-product.dto';
import { ProductsService } from './products.service';

const bodyPipe = new ValidationPipe({
  transform: true,
  whitelist: false,
  forbidNonWhitelisted: false,
});

@ApiTags('Products')
@ApiBearerAuth()
@Controller()
export class ProductsController {
  constructor(private readonly products: ProductsService) {}

  @Post('products')
  @HttpCode(201)
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.PRODUCT_CREATE)
  @ApiOperation({
    summary: 'Create product',
    description:
      'Admin / staff with `product:create`. Body uses snake_case (`gym_id`, `discount_price`, …).',
  })
  create(
    @CurrentUser() user: JwtUser,
    @Body(bodyPipe) body: CreateProductDto,
  ) {
    return this.products.create(user.sub, body);
  }

  @Get('products')
  @ApiOperation({
    summary: 'List products',
    description:
      'Any gym member (or owner). Pagination: `page`, `limit`. Filters: `search`, `category`. `include_inactive=true` only for catalog managers.',
  })
  list(@CurrentUser() user: JwtUser, @Query() query: ProductListQueryDto) {
    return this.products.list(user.sub, query);
  }

  @Get('products/:id')
  @ApiOperation({ summary: 'Product details' })
  @ApiParam({ name: 'id', description: 'Product id' })
  getOne(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdFlexibleQueryDto,
  ) {
    return this.products.getOne(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
      id,
    );
  }

  @Patch('products/:id')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.PRODUCT_UPDATE)
  @ApiOperation({ summary: 'Update product' })
  @ApiParam({ name: 'id' })
  update(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdFlexibleQueryDto,
    @Body(bodyPipe) body: UpdateProductDto,
  ) {
    return this.products.update(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
      id,
      body,
    );
  }

  @Delete('products/:id')
  @UseGuards(PermissionsGuard)
  @RequirePermissions(PERMISSION_CODES.PRODUCT_DELETE)
  @ApiOperation({ summary: 'Soft-delete product' })
  @ApiParam({ name: 'id' })
  remove(
    @CurrentUser() user: JwtUser,
    @Param('id') id: string,
    @Query() query: GymIdFlexibleQueryDto,
  ) {
    return this.products.remove(
      user.sub,
      query.gymId?.trim() || query.gym_id?.trim(),
      id,
    );
  }
}
