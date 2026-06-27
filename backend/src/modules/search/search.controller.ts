import { Controller, Get, Query } from '@nestjs/common';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import type { JwtUser } from '../auth/types/jwt-user.type';
import { GlobalSearchQueryDto } from './dto/global-search-query.dto';
import { SearchService } from './search.service';

/**
 * Gym-scoped global search (members, trainers, plans). Uses pg_trgm-backed indexes
 * for ILIKE; keep queries short and limit low for sub-300ms typical latency.
 */
@Controller('search')
export class SearchController {
  constructor(private readonly search: SearchService) {}

  @Get()
  global(@CurrentUser() user: JwtUser, @Query() query: GlobalSearchQueryDto) {
    return this.search.globalSearch(
      user.sub,
      query.gymId,
      query.q,
      query.limit,
    );
  }
}
