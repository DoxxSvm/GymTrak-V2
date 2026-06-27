import { ConfigService } from '@nestjs/config';
import { Msg91OtpService } from './msg91-otp.service';

describe('Msg91OtpService', () => {
  function makeService(env: Record<string, string | boolean | undefined>) {
    const config = {
      get: (key: string) => env[key],
    } as unknown as ConfigService;
    return new Msg91OtpService(config);
  }

  it('defaults OTP length to 6', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'true',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
    });
    expect(svc.getStatus().otpLength).toBe(6);
  });

  it('clamps MSG91_OTP_LENGTH to 4–9', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'true',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
      MSG91_OTP_LENGTH: '12',
    });
    expect(svc.getStatus().otpLength).toBe(9);
  });

  it('is disabled when IS_MSG91_ENABLED=false even with msg91 provider', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'false',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
    });
    expect(svc.isRequested()).toBe(false);
    expect(svc.isEnabled()).toBe(false);
  });

  it('reports missing template warning for India', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'true',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
      MSG91_USE_WIDGET: 'false',
    });
    const status = svc.getStatus();
    expect(status.enabled).toBe(true);
    expect(status.hasTemplateId).toBe(false);
    expect(status.warnings.some((w) => w.includes('MSG91_TEMPLATE_ID'))).toBe(
      true,
    );
  });

  it('uses widget channel when widget mode is enabled with widget id', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'true',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
      MSG91_USE_WIDGET: 'true',
      MSG91_WIDGET_ID: 'widget-1',
    });
    const status = svc.getStatus();
    expect(status.channel).toBe('widget');
  });

  it('falls back to v5 when widget mode is on but widget id is missing', () => {
    const svc = makeService({
      IS_MSG91_ENABLED: 'true',
      OTP_PROVIDER: 'msg91',
      MSG91_AUTH_KEY: 'key',
      MSG91_USE_WIDGET: 'true',
    });
    const status = svc.getStatus();
    expect(status.channel).toBe('v5');
  });
});
