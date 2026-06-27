import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { GymProductUnit, Prisma } from '@prisma/client';
import { GymAccessService } from '../../common/services/gym-access.service';
import { PermissionEngineService } from '../rbac/permission-engine.service';
import { PrismaService } from '../prisma/prisma.service';
import type { CreateProductDto } from './dto/create-product.dto';
import type { ProductListQueryDto } from './dto/product-list-query.dto';
import type { UpdateProductDto } from './dto/update-product.dto';

function num(d: Prisma.Decimal): number {
  return Number(d);
}

function unitApi(u: string): string {
  return u.toLowerCase();
}

@Injectable()
export class ProductsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly gymAccess: GymAccessService,
    private readonly perm: PermissionEngineService,
  ) {}

  private assertPriceRule(price: number, discountPrice: number) {
    if (discountPrice > price) {
      throw new BadRequestException(
        'discount_price cannot be greater than price',
      );
    }
  }

  private parseUnit(raw: string): GymProductUnit {
    const u = raw.trim().toUpperCase();
    if (u === 'KG' || u === 'PCS') {
      return u as GymProductUnit;
    }
    throw new BadRequestException('unit must be kg or pcs');
  }

  private normalizeImages(urls: string[] | undefined): string[] {
    if (!urls?.length) {
      return [];
    }
    const out = urls.map((u) => u.trim()).filter(Boolean);
    if (out.length > 10) {
      throw new BadRequestException('At most 10 images allowed');
    }
    for (const u of out) {
      if (u.length > 2048) {
        throw new BadRequestException('Each image URL max 2048 characters');
      }
    }
    return out;
  }

  async create(actorUserId: string, dto: CreateProductDto) {
    this.assertPriceRule(dto.price, dto.discount_price);
    const images = this.normalizeImages(dto.images);
    const isActive = dto.is_active !== false;

    const row = await this.prisma.gymProduct.create({
      data: {
        gymId: dto.gym_id.trim(),
        name: dto.name.trim(),
        category: dto.category.trim(),
        stockQuantity: dto.stock,
        price: new Prisma.Decimal(dto.price),
        discountPrice: new Prisma.Decimal(dto.discount_price),
        unit: this.parseUnit(dto.unit),
        description: dto.description?.trim() || null,
        images,
        isActive,
      },
      select: { id: true },
    });

    return {
      success: true as const,
      message: 'Product created successfully',
      data: { id: row.id },
    };
  }

  async list(actorUserId: string, query: ProductListQueryDto) {
    const gymId = query.gymId?.trim() || query.gym_id?.trim();
    if (!gymId) {
      throw new BadRequestException('gymId or gym_id is required');
    }

    const page = Math.max(1, Number(query.page) || 1);
    const limit = Math.min(100, Math.max(1, Number(query.limit) || 10));
    const skip = (page - 1) * limit;

    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, gymId);

    const canManage = await this.perm.hasProductManagement(
      actorUserId,
      gymId,
    );
    const inc = query.include_inactive?.trim().toLowerCase();
    const includeInactive =
      (inc === 'true' || inc === '1') && canManage;

    const where: Prisma.GymProductWhereInput = {
      gymId,
      isDeleted: false,
    };
    if (!includeInactive) {
      where.isActive = true;
    }

    const q = query.search?.trim();
    if (q) {
      where.name = { contains: q, mode: 'insensitive' };
    }
    const cat = query.category?.trim();
    if (cat) {
      where.category = { equals: cat, mode: 'insensitive' };
    }

    const [total, rows] = await Promise.all([
      this.prisma.gymProduct.count({ where }),
      this.prisma.gymProduct.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
        select: {
          id: true,
          name: true,
          category: true,
          price: true,
          discountPrice: true,
          stockQuantity: true,
          unit: true,
          images: true,
          isActive: true,
        },
      }),
    ]);

    const ids = rows.map((r) => r.id);
    const favSet = new Set(
      ids.length
        ? (
            await this.prisma.gymProductFavorite.findMany({
              where: { userId: actorUserId, productId: { in: ids } },
              select: { productId: true },
            })
          ).map((f) => f.productId)
        : [],
    );

    const data = rows.map((r) => ({
      id: r.id,
      name: r.name,
      category: r.category,
      price: num(r.price),
      discount_price: num(r.discountPrice),
      stock: r.stockQuantity,
      unit: unitApi(r.unit),
      image: r.images[0] ?? null,
      is_active: r.isActive,
      is_favorite: favSet.has(r.id),
    }));

    return {
      success: true as const,
      data,
      pagination: { page, limit, total },
    };
  }

  async getOne(
    actorUserId: string,
    gymId: string | undefined,
    productId: string,
  ) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }
    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, g);

    const row = await this.prisma.gymProduct.findFirst({
      where: { id: productId, gymId: g, isDeleted: false },
    });
    if (!row) {
      throw new NotFoundException('Product not found');
    }

    const canManage = await this.perm.hasProductManagement(actorUserId, g);
    if (!row.isActive && !canManage) {
      throw new NotFoundException('Product not found');
    }

    const fav = await this.prisma.gymProductFavorite.findUnique({
      where: {
        userId_productId: { userId: actorUserId, productId: row.id },
      },
      select: { id: true },
    });

    return {
      success: true as const,
      data: {
        id: row.id,
        name: row.name,
        category: row.category,
        price: num(row.price),
        discount_price: num(row.discountPrice),
        stock: row.stockQuantity,
        unit: unitApi(row.unit),
        description: row.description,
        images: row.images,
        is_active: row.isActive,
        is_favorite: Boolean(fav),
      },
    };
  }

  async update(
    actorUserId: string,
    gymId: string | undefined,
    productId: string,
    dto: UpdateProductDto,
  ) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }

    const keys = Object.keys(dto).filter(
      (k) => (dto as Record<string, unknown>)[k] !== undefined,
    );
    if (keys.length === 0) {
      throw new BadRequestException('At least one field is required');
    }

    const existing = await this.prisma.gymProduct.findFirst({
      where: { id: productId, gymId: g, isDeleted: false },
    });
    if (!existing) {
      throw new NotFoundException('Product not found');
    }

    const nextPrice =
      dto.price != null ? dto.price : num(existing.price);
    const nextDiscount =
      dto.discount_price != null
        ? dto.discount_price
        : num(existing.discountPrice);
    this.assertPriceRule(nextPrice, nextDiscount);

    const data: Prisma.GymProductUpdateInput = {};
    if (dto.name !== undefined) {
      data.name = dto.name.trim();
    }
    if (dto.category !== undefined) {
      data.category = dto.category.trim();
    }
    if (dto.price !== undefined) {
      data.price = new Prisma.Decimal(dto.price);
    }
    if (dto.discount_price !== undefined) {
      data.discountPrice = new Prisma.Decimal(dto.discount_price);
    }
    if (dto.stock !== undefined) {
      data.stockQuantity = dto.stock;
    }
    if (dto.unit !== undefined) {
      data.unit = this.parseUnit(dto.unit);
    }
    if (dto.description !== undefined) {
      data.description = dto.description?.trim() || null;
    }
    if (dto.images !== undefined) {
      data.images = this.normalizeImages(dto.images);
    }
    if (dto.is_active !== undefined) {
      data.isActive = dto.is_active;
    }

    await this.prisma.gymProduct.update({
      where: { id: productId },
      data,
    });

    return this.getOne(actorUserId, g, productId);
  }

  async remove(
    _actorUserId: string,
    gymId: string | undefined,
    productId: string,
  ) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }
    const row = await this.prisma.gymProduct.findFirst({
      where: { id: productId, gymId: g, isDeleted: false },
    });
    if (!row) {
      throw new NotFoundException('Product not found');
    }

    await this.prisma.gymProduct.update({
      where: { id: productId },
      data: { isDeleted: true },
    });

    return {
      success: true as const,
      message: 'Product deleted successfully',
    };
  }

  async addFavorite(
    actorUserId: string,
    gymId: string | undefined,
    productId: string,
  ) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }
    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, g);

    const p = await this.prisma.gymProduct.findFirst({
      where: {
        id: productId,
        gymId: g,
        isDeleted: false,
        isActive: true,
      },
      select: { id: true },
    });
    if (!p) {
      throw new NotFoundException('Product not found');
    }

    try {
      await this.prisma.gymProductFavorite.create({
        data: {
          userId: actorUserId,
          productId,
          gymId: g,
        },
      });
    } catch (e) {
      if (
        e instanceof Prisma.PrismaClientKnownRequestError &&
        e.code === 'P2002'
      ) {
        return { success: true as const, message: 'Added to favorites' };
      }
      throw e;
    }

    return { success: true as const, message: 'Added to favorites' };
  }

  async removeFavorite(
    actorUserId: string,
    gymId: string | undefined,
    productId: string,
  ) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }
    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, g);

    const del = await this.prisma.gymProductFavorite.deleteMany({
      where: { userId: actorUserId, gymId: g, productId },
    });
    if (del.count === 0) {
      throw new NotFoundException('Favorite not found');
    }

    return { success: true as const, message: 'Removed from favorites' };
  }

  async listFavorites(actorUserId: string, gymId: string | undefined) {
    const g = gymId?.trim();
    if (!g) {
      throw new BadRequestException('gymId or gym_id is required');
    }
    await this.gymAccess.assertCanBrowseGymCatalog(actorUserId, g);

    const rows = await this.prisma.gymProductFavorite.findMany({
      where: {
        userId: actorUserId,
        gymId: g,
        product: { isDeleted: false, isActive: true },
      },
      orderBy: { createdAt: 'desc' },
      include: {
        product: {
          select: {
            id: true,
            name: true,
            discountPrice: true,
            price: true,
            images: true,
          },
        },
      },
    });

    const data = rows.map((r) => ({
      id: r.product.id,
      name: r.product.name,
      price: num(r.product.discountPrice),
      image: r.product.images[0] ?? null,
    }));

    return { success: true as const, data };
  }
}
