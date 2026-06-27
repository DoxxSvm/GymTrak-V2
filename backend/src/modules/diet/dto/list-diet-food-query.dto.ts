import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class ListDietFoodQueryDto extends GymIdQueryDto {
  @ApiPropertyOptional({ description: 'Case-insensitive substring on `name`' })
  @IsOptional()
  @IsString()
  search?: string;
}
