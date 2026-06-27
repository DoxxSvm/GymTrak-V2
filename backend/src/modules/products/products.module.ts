import { Module } from '@nestjs/common';
import { GymAccessService } from '../../common/services/gym-access.service';
import { RbacModule } from '../rbac/rbac.module';
import { ProductFavoritesController } from './product-favorites.controller';
import { ProductsController } from './products.controller';
import { ProductsService } from './products.service';

@Module({
  imports: [RbacModule],
  controllers: [ProductsController, ProductFavoritesController],
  providers: [ProductsService, GymAccessService],
})
export class ProductsModule {}
