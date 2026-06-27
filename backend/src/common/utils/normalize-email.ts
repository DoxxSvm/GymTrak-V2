/**
 * Normalizes `User.email` for storage and conflict checks.
 * `User.email` is unique; lowercase avoids duplicate accounts like `a@b.c` vs `A@b.c`.
 */
export function normalizeEmailForStorage(
  raw: string | null | undefined,
): string | null {
  if (raw == null) return null;
  const s = String(raw).trim().toLowerCase();
  return s === '' ? null : s;
}
