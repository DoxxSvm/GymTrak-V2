import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

/** Query `gymId` — omit to scope by user memberships / JWT default (see service). */
export class OptionalGymIdQueryDto {
  @ApiPropertyOptional({
    description:
      'Target gym id. When omitted, reads aggregate across gyms you manage; writes resolve via JWT default gym or when you manage exactly one gym.',
  })
  @IsOptional()
  @IsString()
  gymId?: string;
}
