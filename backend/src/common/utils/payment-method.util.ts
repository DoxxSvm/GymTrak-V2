import { BadRequestException } from '@nestjs/common';
import { PaymentMethod } from '@prisma/client';

export function paymentMethodFromApi(mode?: string): PaymentMethod | undefined {
  if (mode == null || mode === '') {
    return undefined;
  }
  const m = mode.toLowerCase();
  if (m === 'cash') {
    return PaymentMethod.CASH;
  }
  if (m === 'upi') {
    return PaymentMethod.UPI;
  }
  if (m === 'card') {
    return PaymentMethod.CARD;
  }
  throw new BadRequestException('payment_mode must be cash, upi, or card');
}

export function paymentMethodToApi(
  method: PaymentMethod | null | undefined,
): string {
  if (!method) {
    return 'unknown';
  }
  return method.toLowerCase();
}

/** Member detail / payment history UI label (`Cash`, `UPI`, `Card`). */
export function paymentMethodDisplayLabel(
  method: PaymentMethod | null | undefined,
): string {
  switch (method) {
    case PaymentMethod.UPI:
      return 'UPI';
    case PaymentMethod.CARD:
      return 'Card';
    case PaymentMethod.CASH:
    default:
      return 'Cash';
  }
}
