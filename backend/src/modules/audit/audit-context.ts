import { AsyncLocalStorage } from 'node:async_hooks';

export type AuditRequestContext = {
  ip?: string;
  userAgent?: string;
  requestId: string;
};

export const auditContextStorage = new AsyncLocalStorage<AuditRequestContext>();
