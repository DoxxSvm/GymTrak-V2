#!/usr/bin/env node
/**
 * CLI: check MSG91 config and optionally send a test OTP.
 * Usage:
 *   node scripts/msg91-test-otp.js
 *   node scripts/msg91-test-otp.js 6353491081
 *   node scripts/msg91-test-otp.js 6353491081 --voice-retry
 */
const fs = require('fs');
const path = require('path');

function loadEnv() {
  const envPath = path.join(__dirname, '..', '.env');
  if (!fs.existsSync(envPath)) return;
  for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const t = line.trim();
    if (!t || t.startsWith('#') || !t.includes('=')) continue;
    const i = t.indexOf('=');
    const key = t.slice(0, i).trim();
    let val = t.slice(i + 1).trim();
    if (
      (val.startsWith('"') && val.endsWith('"')) ||
      (val.startsWith("'") && val.endsWith("'"))
    ) {
      val = val.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = val;
  }
}

function toE164(phone, cc = '+91') {
  const ccDigits = cc.replace(/\D/g, '');
  let digits = String(phone).replace(/\D/g, '').replace(/^0+/, '');
  if (digits.startsWith(ccDigits) && digits.length > ccDigits.length) {
    const national = digits.slice(ccDigits.length);
    if (national.length >= 6) digits = national;
  }
  return `+${ccDigits}${digits}`;
}

loadEnv();

const phoneArg = process.argv[2];
const voiceRetry = process.argv.includes('--voice-retry');
const base = process.env.APP_PUBLIC_URL?.replace(/\/$/, '') || 'http://localhost:3000';
const api = `${base}/api/v1`;

async function main() {
  console.log('=== MSG91 status ===');
  const statusRes = await fetch(`${api}/auth/msg91/status`);
  const status = await statusRes.json();
  console.log(JSON.stringify(status, null, 2));

  if (!phoneArg) {
    console.log('\nTo send a test OTP: node scripts/msg91-test-otp.js <10-digit-phone>');
    return;
  }

  const body = { phone: phoneArg.replace(/\D/g, ''), country_code: '+91' };
  console.log('\n=== MSG91 test send ===', body);
  const sendRes = await fetch(`${api}/auth/msg91/test-send`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const sendJson = await sendRes.json();
  console.log(sendRes.status, JSON.stringify(sendJson, null, 2));

  if (voiceRetry && sendRes.ok) {
    console.log('\n=== MSG91 voice retry ===');
    const retryRes = await fetch(`${api}/auth/msg91/retry-voice`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    console.log(retryRes.status, await retryRes.text());
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
