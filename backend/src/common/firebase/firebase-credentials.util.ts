import * as admin from 'firebase-admin';
import { createPrivateKey } from 'crypto';
import { existsSync, readFileSync } from 'fs';
import { isAbsolute, join } from 'path';
import type { ConfigService } from '@nestjs/config';

export type FirebaseCredentialSource =
  | 'FIREBASE_SERVICE_ACCOUNT_PATH'
  | 'FIREBASE_SERVICE_ACCOUNT_JSON'
  | 'FIREBASE_CLIENT_EMAIL+FIREBASE_PRIVATE_KEY';

export type PrivateKeyDiagnostics = {
  set: boolean;
  rawLen: number;
  normalizedLen: number;
  hasBegin: boolean;
  hasEnd: boolean;
  literalBackslashN: number;
  realNewlines: number;
  pemParsable: boolean;
};

export function resolveServiceAccountPath(raw: string): string {
  const trimmed = raw.trim();
  return isAbsolute(trimmed) ? trimmed : join(process.cwd(), trimmed);
}

/**
 * Service account keys from .env, Docker, EasyPanel, or Coolify often store PEM newlines
 * as the two-character sequence backslash + n (`\\n`). Panels may use spaces, `%0A`, or
 * truncate multiline values — this normalizes before Firebase Admin init.
 */
export function normalizeFirebasePrivateKey(
  raw: string | undefined,
): string | undefined {
  if (raw === undefined || raw === '') return undefined;

  let s = String(raw).replace(/^\uFEFF/, '');
  s = s.trim();

  if (
    (s.startsWith('"') && s.endsWith('"') && s.length > 2) ||
    (s.startsWith("'") && s.endsWith("'") && s.length > 2)
  ) {
    s = s.slice(1, -1).trim();
  }

  s = tryUrlDecodePem(s);
  s = extractPemBlock(s);

  for (let i = 0; i < 6; i++) {
    const next = s
      .replace(/\\\\r\\\\n/g, '\n')
      .replace(/\\\\n/g, '\n')
      .replace(/\\\\r/g, '\n')
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\r/g, '\n');
    if (next === s) break;
    s = next;
  }

  s = s.replace(/\r\n/g, '\n').replace(/\r/g, '\n').trim();

  return reformatPemBodyIfOneLine(s);
}

/** Panel URL-encoding for newlines only — do not use decodeURIComponent on full PEM (+ breaks base64). */
function tryUrlDecodePem(s: string): string {
  if (!/%0[aAdD]/.test(s)) return s;
  return s
    .replace(/%0[dD]/gi, '')
    .replace(/%0[aA]/gi, '\n');
}

/** Pulls a PEM block when extra text was copied around the key. */
function extractPemBlock(s: string): string {
  const m = s.match(
    /-----BEGIN (?:RSA |EC |OPENSSH |)?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC |OPENSSH |)?PRIVATE KEY-----/i,
  );
  return m ? m[0] : s;
}

/** Validates PKCS PEM before Firebase; clearer than generic PEM parse errors. */
export function assertParsablePemPrivateKey(privateKey: string): void {
  try {
    createPrivateKey({ key: privateKey, format: 'pem' });
  } catch (e) {
    const hint =
      'For Coolify/EasyPanel/Docker prefer FIREBASE_SERVICE_ACCOUNT_JSON (minified JSON one line) ' +
      'or FIREBASE_SERVICE_ACCOUNT_PATH to a mounted JSON file. ' +
      'If using FIREBASE_PRIVATE_KEY, use a single line with literal \\n between PEM lines.';
    const msg = e instanceof Error ? e.message : String(e);
    throw new Error(`FIREBASE_PRIVATE_KEY is not valid PEM (${msg}). ${hint}`);
  }
}

function reformatPemBodyIfOneLine(s: string): string {
  const t = s
    .trim()
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n');
  const beginM = t.match(/^(-----BEGIN (?:[A-Z0-9 ]+?)-----)([\s\S]*?)$/i);
  if (!beginM) return t;
  const startHeader = beginM[1];
  const afterBegin = beginM[2].trim();
  const endM = /([\s\S]*?)(-----END (?:[A-Z0-9 ]+?)-----)\s*$/i.exec(
    afterBegin,
  );
  if (!endM) return t;
  const bodyRaw = endM[1].trim();
  const end = endM[2];
  if (bodyRaw.includes('\n')) return t;
  const b64 = bodyRaw.replace(/\s/g, '');
  if (b64.length === 0) return t;
  const lines = b64.match(/.{1,64}/g) ?? [b64];
  return `${startHeader}\n${lines.join('\n')}\n${end}\n`;
}

export function describePrivateKeyDiagnostics(
  raw: string | undefined,
): PrivateKeyDiagnostics {
  if (raw === undefined || String(raw).trim() === '') {
    return {
      set: false,
      rawLen: 0,
      normalizedLen: 0,
      hasBegin: false,
      hasEnd: false,
      literalBackslashN: 0,
      realNewlines: 0,
      pemParsable: false,
    };
  }
  const t = String(raw);
  const normalized = normalizeFirebasePrivateKey(raw) ?? '';
  let pemParsable = false;
  if (normalized) {
    try {
      createPrivateKey({ key: normalized, format: 'pem' });
      pemParsable = true;
    } catch {
      pemParsable = false;
    }
  }
  return {
    set: true,
    rawLen: t.length,
    normalizedLen: normalized.length,
    hasBegin: /BEGIN (?:RSA |EC |OPENSSH |)?PRIVATE KEY|BEGIN RSA PRIVATE KEY/.test(
      t,
    ),
    hasEnd: /END (?:RSA |EC |OPENSSH |)?PRIVATE KEY|END RSA PRIVATE KEY/.test(t),
    literalBackslashN: (t.match(/\\n/g) ?? []).length,
    realNewlines: (t.match(/\n/g) ?? []).length,
    pemParsable,
  };
}

export function toServiceAccountForCert(
  input: Record<string, unknown> | admin.ServiceAccount,
): admin.ServiceAccount {
  const o = input as Record<string, unknown>;
  const projectId = String(
    o['projectId'] ?? o['project_id'] ?? '',
  ).trim();
  const clientEmail = String(
    o['clientEmail'] ?? o['client_email'] ?? '',
  ).trim();
  const rawKey =
    typeof o['privateKey'] === 'string' ? o['privateKey'] : undefined;
  const rawKeySnake =
    typeof o['private_key'] === 'string' ? o['private_key'] : undefined;
  const privateKey = normalizeFirebasePrivateKey(rawKey ?? rawKeySnake);
  if (!clientEmail || !privateKey) {
    throw new Error('Service account: missing client_email or private_key');
  }
  assertParsablePemPrivateKey(privateKey);
  return {
    projectId: projectId || undefined,
    clientEmail,
    privateKey,
  };
}

function serviceAccountFromDiscreteEnv(
  config: ConfigService,
): admin.ServiceAccount | null {
  const clientEmail = config.get<string>('FIREBASE_CLIENT_EMAIL')?.trim();
  const privateKey = normalizeFirebasePrivateKey(
    config.get<string>('FIREBASE_PRIVATE_KEY'),
  );
  if (!clientEmail || !privateKey) {
    return null;
  }
  const projectId = config.get<string>('FIREBASE_PROJECT_ID')?.trim();
  return {
    projectId: projectId || undefined,
    privateKey,
    clientEmail,
  };
}

export function isJsonStringParseable(s: string): boolean {
  try {
    JSON.parse(s);
    return true;
  } catch {
    return false;
  }
}

export type FirebaseCredentialResolveResult = {
  account: admin.ServiceAccount;
  source: FirebaseCredentialSource;
};

/**
 * Tries credential sources in Docker-friendly order: mounted file → JSON env → discrete vars.
 */
export function resolveFirebaseServiceAccount(
  config: ConfigService,
): { result: FirebaseCredentialResolveResult | null; attemptErrors: string[] } {
  const attemptErrors: string[] = [];

  const pathRaw = config.get<string>('FIREBASE_SERVICE_ACCOUNT_PATH')?.trim();
  if (pathRaw) {
    try {
      const resolved = resolveServiceAccountPath(pathRaw);
      if (!existsSync(resolved)) {
        attemptErrors.push(
          `PATH: file not found (${resolved})`,
        );
      } else {
        const parsed = JSON.parse(readFileSync(resolved, 'utf8')) as Record<
          string,
          unknown
        >;
        const account = toServiceAccountForCert(parsed);
        return {
          result: {
            account,
            source: 'FIREBASE_SERVICE_ACCOUNT_PATH',
          },
          attemptErrors,
        };
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      attemptErrors.push(`PATH: ${msg}`);
    }
  }

  const jsonRaw = config.get<string>('FIREBASE_SERVICE_ACCOUNT_JSON')?.trim();
  if (jsonRaw) {
    try {
      const parsed = JSON.parse(jsonRaw) as Record<string, unknown>;
      const account = toServiceAccountForCert(parsed);
      return {
        result: {
          account,
          source: 'FIREBASE_SERVICE_ACCOUNT_JSON',
        },
        attemptErrors,
      };
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      attemptErrors.push(`JSON: ${msg}`);
    }
  }

  const fromDiscrete = serviceAccountFromDiscreteEnv(config);
  if (fromDiscrete) {
    try {
      const account = toServiceAccountForCert(fromDiscrete);
      return {
        result: {
          account,
          source: 'FIREBASE_CLIENT_EMAIL+FIREBASE_PRIVATE_KEY',
        },
        attemptErrors,
      };
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      attemptErrors.push(`FIREBASE_* env: ${msg}`);
    }
  }

  return { result: null, attemptErrors };
}
