import { SetMetadata } from '@nestjs/common';

export const REQUIRE_BEARER_AUTH_KEY = 'requireBearerAuth';

/**
 * Skips the dev / signup no-Bearer fallbacks in {@link JwtAuthGuard}: missing or invalid
 * `Authorization: Bearer …` yields 401. Use for member-owned data that must never run as an
 * inferred user when the client omits the token.
 */
export const RequireBearerAuth = () => SetMetadata(REQUIRE_BEARER_AUTH_KEY, true);
