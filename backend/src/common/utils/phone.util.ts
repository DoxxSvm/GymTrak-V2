/**
 * Normalize a national or full phone number to E.164.
 * Strips leading `0`, and removes a duplicate country prefix when the
 * client sends e.g. `916353491081` with `country_code` `+91`.
 */
export function toE164Phone(phone: string, countryCode = '+91'): string {
  const cc = countryCode.trim().replace(/[^\d+]/g, '');
  if (!/^\+[1-9]\d{0,4}$/.test(cc)) {
    throw new Error('Invalid country_code');
  }

  const ccDigits = cc.slice(1);
  let digits = phone.replace(/\D/g, '');
  if (!digits) {
    throw new Error('Invalid phone number');
  }

  digits = digits.replace(/^0+/, '');

  if (digits.startsWith(ccDigits) && digits.length > ccDigits.length) {
    const national = digits.slice(ccDigits.length);
    if (national.length >= 6 && national.length <= 15) {
      digits = national;
    }
  }

  if (digits.length < 6 || digits.length > 15) {
    throw new Error('Invalid phone number');
  }

  return `${cc}${digits}`;
}
