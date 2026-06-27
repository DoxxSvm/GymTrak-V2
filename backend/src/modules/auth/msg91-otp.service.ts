import {
  BadGatewayException,
  Injectable,
  Logger,
  OnModuleInit,
  ServiceUnavailableException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

const MSG91_BASE = 'https://control.msg91.com/api/v5';
/** Widget send/verify (default template) — uses `authkey` header, not body `tokenAuth`. */
const MSG91_WIDGET_BASE = 'https://api.msg91.com/api/v5';

type Msg91Json = {
  type?: string;
  message?: string;
  request_id?: string;
  reqId?: string;
  hasError?: boolean;
  status?: string;
  code?: string;
};

export type Msg91SendResult = {
  requestId: string;
  channel: 'v5' | 'widget';
};

export type Msg91Status = {
  enabled: boolean;
  isMsg91Enabled: boolean;
  skipSend: boolean;
  channel: 'v5' | 'widget' | 'none';
  hasAuthKey: boolean;
  hasTemplateId: boolean;
  hasDltTemplateId: boolean;
  hasSenderId: boolean;
  hasWidgetId: boolean;
  hasTokenAuth: boolean;
  templateId: string | null;
  otpLength: number;
  warnings: string[];
};

@Injectable()
export class Msg91OtpService implements OnModuleInit {
  private readonly logger = new Logger(Msg91OtpService.name);

  constructor(private readonly config: ConfigService) {}

  onModuleInit(): void {
    if (!this.isEnabled()) {
      return;
    }
    const status = this.getStatus();
    this.logger.log(
      `MSG91 OTP ready (channel=${status.channel}, otpLength=${status.otpLength}, template=${status.hasTemplateId ? 'set' : 'MISSING'})`,
    );
    for (const warning of status.warnings) {
      this.logger.warn(warning);
    }
  }

  /** Whether MSG91 SMS is requested (`IS_MSG91_ENABLED` or legacy `OTP_PROVIDER=msg91`). */
  isRequested(): boolean {
    const flag = this.config.get<string | boolean>('IS_MSG91_ENABLED');
    if (flag === true || flag === 'true' || flag === '1') {
      return true;
    }
    if (flag === false || flag === 'false' || flag === '0') {
      return false;
    }
    const provider = this.config
      .get<string>('OTP_PROVIDER')
      ?.trim()
      .toLowerCase();
    return provider === 'msg91';
  }

  isEnabled(): boolean {
    return this.isRequested() && !!this.authKey();
  }

  getStatus(): Msg91Status {
    const hasAuthKey = !!this.authKey();
    const hasTemplateId = !!this.templateId();
    const hasDltTemplateId = !!this.dltTemplateId();
    const hasSenderId = !!this.senderId();
    const hasWidgetId = !!this.widgetId();
    const hasTokenAuth = !!this.widgetTokenAuth();
    const widgetRequested = this.isWidgetFlagEnabled();
    const channel = this.resolveActiveChannel();
    const msg91Requested = this.isRequested();
    const warnings: string[] = [];

    if (!msg91Requested) {
      warnings.push(
        'IS_MSG91_ENABLED=false — MSG91 SMS is off; login uses static OTP_STATIC_CODE (default 123456).',
      );
    }
    if (!hasAuthKey && msg91Requested) {
      warnings.push('MSG91_AUTH_KEY is missing.');
    }
    if (!hasTemplateId && channel === 'v5') {
      warnings.push(
        'MSG91_TEMPLATE_ID is not set. For India, SMS usually will NOT deliver without an approved OTP template from MSG91 dashboard (OTP → Templates). API may still return success.',
      );
    }
    if (widgetRequested && !hasWidgetId) {
      warnings.push(
        'MSG91_USE_WIDGET=true but MSG91_WIDGET_ID is missing — using v5 /otp API.',
      );
    }
    if (widgetRequested && hasWidgetId && channel === 'widget' && !hasTemplateId) {
      warnings.push(
        'Using MSG91 Widget API (default template). Set MSG91_TEMPLATE_ID and MSG91_USE_WIDGET=false for custom DLT template via v5 /otp.',
      );
    }
    if (channel === 'widget') {
      warnings.push(
        `Widget OTP length: set ${this.otpLength()} digits in MSG91 dashboard (OTP Widget → your widget → OTP length). API also sends otp_length=${this.otpLength()}.`,
      );
    }

    return {
      enabled: this.isEnabled(),
      isMsg91Enabled: msg91Requested,
      skipSend: this.shouldSkipSend(),
      channel: !hasAuthKey ? 'none' : channel,
      hasAuthKey,
      hasTemplateId,
      hasDltTemplateId,
      hasSenderId,
      hasWidgetId,
      hasTokenAuth,
      templateId: this.templateId() ?? null,
      otpLength: this.otpLength(),
      warnings,
    };
  }

  /** E.164 `+91…` → MSG91 `91…` (digits only, no `+`). */
  toMsg91Mobile(e164Phone: string): string {
    return e164Phone.replace(/\D/g, '');
  }

  async sendTestOtp(e164Phone: string): Promise<
    Msg91Status & {
      skipped: boolean;
      e164: string;
      mobile: string;
      sendChannel?: Msg91SendResult['channel'];
      requestId?: string;
      message: string;
    }
  > {
    const status = this.getStatus();
    const mobile = this.toMsg91Mobile(e164Phone);

    if (!status.isMsg91Enabled) {
      throw new ServiceUnavailableException(
        'MSG91 is disabled (IS_MSG91_ENABLED=false). Use static OTP_STATIC_CODE instead.',
      );
    }

    if (!status.enabled) {
      throw new ServiceUnavailableException(
        'MSG91 is not enabled (set IS_MSG91_ENABLED=true and MSG91_AUTH_KEY)',
      );
    }

    if (this.shouldSkipSend()) {
      return {
        ...status,
        skipped: true,
        e164: e164Phone,
        mobile,
        message:
          'MSG91_SKIP_SEND=true — no SMS sent. Set MSG91_SKIP_SEND=false to test live delivery.',
      };
    }

    const sent = await this.sendOtp(e164Phone);
    return {
      ...status,
      skipped: false,
      e164: e164Phone,
      mobile,
      requestId: sent.requestId,
      sendChannel: sent.channel,
      message:
        'MSG91 accepted the OTP request. If SMS does not arrive within 1–2 minutes, set MSG91_TEMPLATE_ID from MSG91 dashboard (DLT-approved OTP template).',
    };
  }

  async sendOtp(e164Phone: string): Promise<Msg91SendResult> {
    if (this.shouldSkipSend()) {
      this.logger.log(
        `[MSG91 skip] send OTP → ${this.toMsg91Mobile(e164Phone)}`,
      );
      return { requestId: `skip-${Date.now()}`, channel: 'v5' };
    }

    const mobile = this.toMsg91Mobile(e164Phone);
    if (this.useWidgetApi()) {
      return this.sendViaWidget(mobile);
    }
    return this.sendViaV5(mobile);
  }

  async verifyOtp(
    e164Phone: string,
    otp: string,
    opts?: { requestId?: string; channel?: 'v5' | 'widget' },
  ): Promise<boolean> {
    if (this.shouldSkipSend()) {
      return true;
    }

    const mobile = this.toMsg91Mobile(e164Phone);
    if (opts?.channel === 'widget' && opts.requestId) {
      return this.verifyViaWidget(opts.requestId, otp);
    }
    return this.verifyViaV5(mobile, otp);
  }

  async retryOtp(
    e164Phone: string,
    opts?: {
      requestId?: string;
      channel?: 'v5' | 'widget';
      retryChannel?: number;
      retryType?: 'text' | 'voice';
    },
  ): Promise<void> {
    if (this.shouldSkipSend()) {
      return;
    }

    const mobile = this.toMsg91Mobile(e164Phone);
    if (opts?.channel === 'widget' && opts.requestId) {
      await this.retryViaWidget(opts.requestId, opts.retryChannel);
      return;
    }
    await this.retryViaV5(mobile, opts?.retryType ?? 'text');
  }

  private authKey(): string | undefined {
    return this.config.get<string>('MSG91_AUTH_KEY')?.trim() || undefined;
  }

  private templateId(): string | undefined {
    const raw = this.config.get<string>('MSG91_TEMPLATE_ID')?.trim();
    if (!raw || raw === "''" || raw === '""') {
      return undefined;
    }
    return raw;
  }

  private dltTemplateId(): string | undefined {
    return (
      this.config.get<string>('MSG91_DLT_TEMPLATE_ID')?.trim() || undefined
    );
  }

  private senderId(): string | undefined {
    return this.config.get<string>('MSG91_SENDER_ID')?.trim() || undefined;
  }

  private widgetId(): string | undefined {
    return this.config.get<string>('MSG91_WIDGET_ID')?.trim() || undefined;
  }

  private tokenAuth(): string {
    return this.widgetTokenAuth() || this.authKey() || '';
  }

  private otpExpiryMinutes(): number | undefined {
    const raw = this.config.get<number | string>('MSG91_OTP_EXPIRY_MINUTES');
    if (raw == null || raw === '') {
      return undefined;
    }
    const n = typeof raw === 'number' ? raw : Number(raw);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  /** MSG91 OTP digit count (4–9 per API; default 6). */
  private otpLength(): number {
    const raw = this.config.get<number | string>('MSG91_OTP_LENGTH');
    const n =
      raw == null || raw === ''
        ? 6
        : typeof raw === 'number'
          ? raw
          : Number(raw);
    if (!Number.isFinite(n)) {
      return 6;
    }
    return Math.min(9, Math.max(4, Math.trunc(n)));
  }

  private widgetTokenAuth(): string | undefined {
    const raw = this.config.get<string>('MSG91_TOKEN_AUTH')?.trim();
    return raw || undefined;
  }

  private isWidgetFlagEnabled(): boolean {
    const flag = this.config.get<string | boolean>('MSG91_USE_WIDGET');
    return flag === true || flag === 'true' || flag === '1';
  }

  /** Widget when flag + widget id + auth key (MSG91 `sendOtp` uses authkey header). */
  private resolveActiveChannel(): 'v5' | 'widget' {
    if (this.isWidgetFlagEnabled() && this.widgetId() && this.authKey()) {
      return 'widget';
    }
    return 'v5';
  }

  private useWidgetApi(): boolean {
    return this.resolveActiveChannel() === 'widget';
  }

  private shouldSkipSend(): boolean {
    const v = this.config.get<string | boolean>('MSG91_SKIP_SEND');
    return v === true || v === 'true' || v === '1';
  }

  private requireTemplateId(): boolean {
    const v = this.config.get<string | boolean>('MSG91_REQUIRE_TEMPLATE_ID');
    return v === true || v === 'true' || v === '1';
  }

  private headers(): Record<string, string> {
    const authkey = this.authKey();
    if (!authkey) {
      throw new ServiceUnavailableException('MSG91 is not configured');
    }
    return {
      authkey,
      accept: 'application/json',
      'Content-Type': 'application/json',
    };
  }

  private buildV5SendBody(mobile: string): Record<string, unknown> {
    const templateId = this.templateId();
    if (this.requireTemplateId() && !templateId) {
      throw new BadGatewayException(
        'MSG91_TEMPLATE_ID is required (MSG91_REQUIRE_TEMPLATE_ID=true). Add your DLT-approved OTP template id from MSG91 dashboard.',
      );
    }

    const body: Record<string, unknown> = {
      mobile,
      otp_length: this.otpLength(),
    };

    if (templateId) {
      body.template_id = templateId;
    }
    const dltId = this.dltTemplateId();
    if (dltId) {
      body.dlt_template_id = dltId;
    }
    const sender = this.senderId();
    if (sender) {
      body.sender = sender;
    }
    const expiry = this.otpExpiryMinutes();
    if (expiry != null) {
      body.otp_expiry = expiry;
    }

    return body;
  }

  private async sendViaV5(mobile: string): Promise<Msg91SendResult> {
    const body = this.buildV5SendBody(mobile);

    const data = await this.postJson(`${MSG91_BASE}/otp`, body);
    const requestId = data.request_id ?? data.reqId;
    if (data.type !== 'success' || !requestId) {
      const detail =
        data.message ??
        (typeof data === 'object' ? JSON.stringify(data) : 'unknown error');
      this.logger.error(
        `MSG91 send failed for ***${mobile.slice(-4)}: ${detail}`,
      );
      throw new BadGatewayException(
        this.mapSendFailureMessage(detail, body.template_id as string | undefined),
      );
    }
    if (!body.template_id) {
      this.logger.warn(
        `MSG91 OTP accepted without MSG91_TEMPLATE_ID for ***${mobile.slice(-4)} — SMS may not deliver in India until a DLT template is configured.`,
      );
    }
    this.logger.log(
      `MSG91 OTP sent (v5) to ***${mobile.slice(-4)} request_id=${requestId} otp_length=${body.otp_length} template=${body.template_id ?? 'default'}`,
    );
    return { requestId, channel: 'v5' };
  }

  private mapSendFailureMessage(
    detail: string,
    templateId?: string,
  ): string {
    const lower = detail.toLowerCase();
    if (
      !templateId &&
      (lower.includes('template') || lower.includes('dlt'))
    ) {
      return 'MSG91 requires MSG91_TEMPLATE_ID (DLT-approved OTP template from MSG91 dashboard → OTP → Templates).';
    }
    if (lower.includes('authentication')) {
      return 'MSG91 authentication failed — check MSG91_AUTH_KEY in .env.';
    }
    return detail || 'MSG91 failed to send OTP';
  }

  private async sendViaWidget(mobile: string): Promise<Msg91SendResult> {
    const widgetId = this.widgetId();
    if (!widgetId) {
      throw new ServiceUnavailableException('MSG91_WIDGET_ID is not set');
    }

    const data = await this.postJson(`${MSG91_WIDGET_BASE}/widget/sendOtp`, {
      widgetId,
      identifier: mobile,
      otp_length: this.otpLength(),
    });

    if (data.type === 'error' || data.hasError || data.status === 'fail') {
      throw new BadGatewayException(
        data.message ??
          'MSG91 widget failed to send OTP. Check MSG91_WIDGET_ID and Mobile Integration on the widget in MSG91 dashboard.',
      );
    }

    const reqId = this.extractWidgetReqId(data);
    if (!reqId) {
      throw new BadGatewayException(
        'MSG91 widget did not return a request id (reqId). OTP may not have been sent.',
      );
    }
    this.logger.log(
      `MSG91 widget OTP sent to ***${mobile.slice(-4)} request_id=${reqId} otp_length=${this.otpLength()}`,
    );
    return { requestId: reqId, channel: 'widget' };
  }

  private extractWidgetReqId(data: Msg91Json): string | null {
    if (data.reqId?.trim()) {
      return data.reqId.trim();
    }
    if (data.request_id?.trim()) {
      return data.request_id.trim();
    }
    if (data.type === 'success' && data.message?.trim()) {
      return data.message.trim();
    }
    return null;
  }

  private async verifyViaV5(mobile: string, otp: string): Promise<boolean> {
    const url = new URL(`${MSG91_BASE}/otp/verify`);
    url.searchParams.set('mobile', mobile);
    url.searchParams.set('otp', otp.trim());

    const data = await this.getJson(url.toString());
    const ok = this.isVerifySuccess(data);
    if (!ok) {
      this.logger.warn(
        `MSG91 verify failed for ***${mobile.slice(-4)}: ${data.message ?? data.type ?? 'unknown'}`,
      );
    } else {
      this.logger.log(`MSG91 verify succeeded for ***${mobile.slice(-4)}`);
    }
    return ok;
  }

  private async verifyViaWidget(reqId: string, otp: string): Promise<boolean> {
    const widgetId = this.widgetId();
    if (!widgetId) {
      return false;
    }

    const data = await this.postJson(`${MSG91_WIDGET_BASE}/widget/verifyOtp`, {
      widgetId,
      reqId,
      otp: otp.trim(),
    });
    return this.isVerifySuccess(data);
  }

  private async retryViaV5(
    mobile: string,
    retryType: 'text' | 'voice',
  ): Promise<void> {
    const url = new URL(`${MSG91_BASE}/otp/retry`);
    url.searchParams.set('mobile', mobile);
    url.searchParams.set('retrytype', retryType);
    const data = await this.getJson(url.toString());
    if (data.type !== 'success') {
      throw new BadGatewayException(data.message ?? 'MSG91 retry failed');
    }
    this.logger.log(
      `MSG91 OTP retry (${retryType}) for ***${mobile.slice(-4)}`,
    );
  }

  private async retryViaWidget(
    reqId: string,
    retryChannel?: number,
  ): Promise<void> {
    const widgetId = this.widgetId();
    if (!widgetId) {
      throw new ServiceUnavailableException('MSG91 widget is not configured');
    }

    const body: Record<string, unknown> = {
      widgetId,
      reqId,
    };
    if (retryChannel != null) {
      body.retryChannel = retryChannel;
    }

    const data = await this.postJson(
      `${MSG91_WIDGET_BASE}/widget/retryOtp`,
      body,
    );
    if (data.hasError || data.status === 'fail' || data.type === 'error') {
      throw new BadGatewayException(data.message ?? 'MSG91 widget retry failed');
    }
  }

  private isVerifySuccess(data: Msg91Json): boolean {
    if (data.type === 'success') {
      return true;
    }
    const msg = (data.message ?? '').toLowerCase();
    return (
      msg.includes('verified') ||
      msg.includes('number_verified') ||
      msg === 'otp verified success'
    );
  }

  private async postJson(url: string, body: unknown): Promise<Msg91Json> {
    const res = await fetch(url, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(body),
    });
    return this.parseResponse(res, 'POST', url, body);
  }

  private async getJson(url: string): Promise<Msg91Json> {
    const res = await fetch(url, {
      method: 'GET',
      headers: this.headers(),
    });
    return this.parseResponse(res, 'GET', url);
  }

  private redactUrlForLog(url: string): string {
    try {
      const parsed = new URL(url);
      if (parsed.searchParams.has('otp')) {
        parsed.searchParams.set('otp', '***');
      }
      return parsed.toString();
    } catch {
      return url;
    }
  }

  private redactBodyForLog(body: unknown): unknown {
    if (!body || typeof body !== 'object') {
      return body;
    }
    const copy = { ...(body as Record<string, unknown>) };
    if ('tokenAuth' in copy) {
      copy.tokenAuth = '***';
    }
    if ('otp' in copy) {
      copy.otp = '***';
    }
    return copy;
  }

  private logApiExchange(
    method: string,
    url: string,
    status: number,
    data: Msg91Json,
    requestBody?: unknown,
  ): void {
    const requestPart =
      requestBody !== undefined
        ? ` request=${JSON.stringify(this.redactBodyForLog(requestBody))}`
        : '';
    this.logger.log(
      `MSG91 API ${method} ${this.redactUrlForLog(url)}${requestPart} → HTTP ${status} response=${JSON.stringify(data)}`,
    );
  }

  private async parseResponse(
    res: Response,
    method: string,
    url: string,
    requestBody?: unknown,
  ): Promise<Msg91Json> {
    let data: Msg91Json = {};
    try {
      data = (await res.json()) as Msg91Json;
    } catch {
      this.logger.error(
        `MSG91 API ${method} ${this.redactUrlForLog(url)} → HTTP ${res.status} invalid JSON body`,
      );
      throw new BadGatewayException('MSG91 returned an invalid response');
    }

    this.logApiExchange(method, url, res.status, data, requestBody);

    if (
      res.status === 401 ||
      data.message === 'AuthenticationFailure' ||
      data.code === '401'
    ) {
      throw new BadGatewayException(
        'MSG91 authentication failed — check MSG91_AUTH_KEY or MSG91_TOKEN_AUTH',
      );
    }
    return data;
  }
}
