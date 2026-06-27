import { IsIn, IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class PaymentAnalyticsQueryDto extends GymIdQueryDto {
  @IsOptional()
  @IsString()
  @IsIn(['weekly', 'monthly', 'yearly'])
  range?: 'weekly' | 'monthly' | 'yearly';

  @IsString()
  @IsOptional()
  from!: string;

  @IsString()
  @IsOptional()
  to!: string;
}
