import { ConsoleLogger } from '@nestjs/common';

/** Nest bootstrap contexts that flood the console with hundreds of lines. */
const QUIET_NEST_CONTEXTS = new Set([
  'InstanceLoader',
  'RoutesResolver',
  'RouterExplorer',
]);

/**
 * Default Nest logger minus per-route mapping noise.
 * App-level logs (Startup, Msg91OtpService, etc.) are unchanged.
 */
export class QuietNestLogger extends ConsoleLogger {
  log(message: unknown, context?: string): void {
    if (context && QUIET_NEST_CONTEXTS.has(context)) {
      return;
    }
    super.log(message, context);
  }
}

export function createNestLogger(): QuietNestLogger {
  return new QuietNestLogger();
}
