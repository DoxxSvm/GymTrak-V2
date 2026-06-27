import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';
import { GymIdQueryDto } from '../../../common/dto/gym-id-query.dto';

export class ListDietMealsQueryDto extends GymIdQueryDto {
  @ApiPropertyOptional({ description: 'Filter by member GymUser id' })
  @IsOptional()
  @IsString()
  member_id?: string;
}
