import * as admin from 'firebase-admin';
import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { existsSync } from 'fs';
import {
  describePrivateKeyDiagnostics,
  isJsonStringParseable,
  resolveFirebaseServiceAccount,
  resolveServiceAccountPath,
} from '../../common/firebase/firebase-credentials.util';

export type FcmSendResult =
  | { ok: true }
  | { ok: false; shouldClearToken: boolean };

@Injectable()
export class FirebasePushService implements OnModuleInit {
  private readonly logger = new Logger(FirebasePushService.name);
  private enabled = false;

  constructor(private readonly config: ConfigService) {}

  /**
   * Logs which Firebase env vars are present (no secret values).
   */
  private logFirebaseConfiguration(): void {
    const projectId = this.config.get<string>('FIREBASE_PROJECT_ID')?.trim();
    const clientEmail = this.config.get<string>('FIREBASE_CLIENT_EMAIL')?.trim();
    const privateKeyRaw = this.config.get<string>('FIREBASE_PRIVATE_KEY');
    const pk = describePrivateKeyDiagnostics(privateKeyRaw);

    const jsonRaw = this.config
      .get<string>('FIREBASE_SERVICE_ACCOUNT_JSON')
      ?.trim();
    const jsonSet = Boolean(jsonRaw);
    const jsonLen = jsonRaw?.length ?? 0;
    const jsonOk = Boolean(jsonRaw && isJsonStringParseable(jsonRaw));

    const pathRaw = this.config
      .get<string>('FIREBASE_SERVICE_ACCOUNT_PATH')
      ?.trim();
    const resolved = pathRaw
      ? resolveServiceAccountPath(pathRaw)
      : undefined;
    const pathExists = resolved ? existsSync(resolved) : undefined;

    this.logger.log(
      `Firebase configuration: NODE_ENV=${process.env.NODE_ENV ?? '(unset)'}; ` +
        `cwd=${process.cwd()}; ` +
        `projectId=${projectId || '(not set)'}; ` +
        `clientEmail=${clientEmail || '(not set)'}; ` +
        `FIREBASE_PRIVATE_KEY rawLen=${pk.rawLen} normalizedLen=${pk.normalizedLen} ` +
        `pemBegin=${pk.hasBegin} pemEnd=${pk.hasEnd} ` +
        `literalBackslashN=${pk.literalBackslashN} realNewlines=${pk.realNewlines} ` +
        `pemParsableAfterNormalize=${pk.pemParsable}; ` +
        `FIREBASE_SERVICE_ACCOUNT_JSON set=${jsonSet} len=${jsonLen} jsonOk=${jsonOk}; ` +
        `FIREBASE_SERVICE_ACCOUNT_PATH=${pathRaw || '(not set)'}` +
        (resolved
          ? ` resolved=${resolved} exists=${pathExists}`
          : '') +
        `; resolveOrder=PATH→JSON→FIREBASE_CLIENT_EMAIL+FIREBASE_PRIVATE_KEY`,
    );
  }

  onModuleInit(): void {
    this.logFirebaseConfiguration();

    if (admin.apps.length > 0) {
      this.enabled = true;
      this.logger.log('Firebase Admin: using existing default app');
      return;
    }

    const { result, attemptErrors } = resolveFirebaseServiceAccount(
      this.config,
    );

    if (!result) {
      if (attemptErrors.length > 0) {
        for (const err of attemptErrors) {
          this.logger.warn(`Firebase credential attempt failed: ${err}`);
        }
      }
      this.logger.warn(
        'Firebase push disabled: set FIREBASE_SERVICE_ACCOUNT_PATH (mounted JSON), ' +
          'or FIREBASE_SERVICE_ACCOUNT_JSON (one-line minified JSON), ' +
          'or FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY (+ optional FIREBASE_PROJECT_ID)',
      );
      return;
    }

    try {
      admin.initializeApp({
        credential: admin.credential.cert(result.account),
      });
      this.enabled = true;
      this.logger.log(
        `Firebase Admin initialized for FCM (source=${result.source}, ` +
          `projectId=${result.account.projectId ?? '(from credential)'})`,
      );
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err);
      this.logger.error(
        `Firebase Admin init failed (push disabled): ${message}`,
      );
      this.enabled = false;
    }
  }

  get isEnabled(): boolean {
    return this.enabled;
  }

  /**
   * Sends a display notification plus string `data` for the client to route (e.g. open screen).
   */
  async sendToDevice(
    token: string,
    payload: {
      title: string;
      body: string;
      data: Record<string, string>;
    },
  ): Promise<FcmSendResult> {
    if (!this.enabled || !token) {
      return { ok: false, shouldClearToken: false };
    }
    try {
      await admin.messaging().send({
        token,
        notification: {
          title: payload.title,
          body: payload.body,
        },
        data: payload.data,
        android: { priority: 'high' },
        apns: {
          payload: {
            aps: {
              sound: 'default',
            },
          },
        },
      });
      return { ok: true };
    } catch (err: unknown) {
      const code =
        err && typeof err === 'object' && 'code' in err
          ? String((err as { code: unknown }).code)
          : '';
      if (
        code === 'messaging/invalid-registration-token' ||
        code === 'messaging/registration-token-not-registered'
      ) {
        return { ok: false, shouldClearToken: true };
      }
      this.logger.warn(`FCM send failed (${code || 'unknown'})`);
      return { ok: false, shouldClearToken: false };
    }
  }
}
