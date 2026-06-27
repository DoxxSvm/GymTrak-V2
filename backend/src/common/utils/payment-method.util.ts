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
