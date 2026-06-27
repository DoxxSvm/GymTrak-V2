import { toE164Phone } from './phone.util';

describe('toE164Phone', () => {
  it('formats 10-digit Indian national number', () => {
    expect(toE164Phone('6353491081', '+91')).toBe('+916353491081');
  });

  it('strips duplicate country code when phone includes 91 prefix', () => {
    expect(toE164Phone('916353491081', '+91')).toBe('+916353491081');
  });

  it('handles values that already include +', () => {
    expect(toE164Phone('+916353491081', '+91')).toBe('+916353491081');
  });

  it('strips leading trunk zero', () => {
    expect(toE164Phone('06353491081', '+91')).toBe('+916353491081');
  });
});
