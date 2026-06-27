import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional, IsString } from 'class-validator';
import { OptionalGymIdQueryDto } from '../../../common/dto/optional-gym-id-query.dto';

export class ListDietMealsQueryDto extends OptionalGymIdQueryDto {
  @ApiPropertyOptional({ description: 'Filter by member GymUser id' })
  @IsOptional()
  @IsString()
  member_id?: string;

  @ApiPropertyOptional({
    enum: ['all', 'member', 'trainer'],
    description:
      '`member` = created by a gym MEMBER; `trainer` = OWNER, TRAINER, or STAFF; `all` = no filter.',
  })
  @IsOptional()
  @IsIn(['all', 'member', 'trainer'])
  created_by?: 'all' | 'member' | 'trainer';
}
