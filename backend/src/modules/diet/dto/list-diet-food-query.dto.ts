import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';
import { OptionalGymIdQueryDto } from '../../../common/dto/optional-gym-id-query.dto';

export class ListDietFoodQueryDto extends OptionalGymIdQueryDto {
  @ApiPropertyOptional({ description: 'Case-insensitive substring on `name`' })
  @IsOptional()
  @IsString()
  search?: string;
}
