import type { RazorpayOrderResult, VerifyPaymentPayload } from '@/types/wallet'
import { apiRequest } from './client'

export function createCheckoutSession(amountUsd: number): Promise<RazorpayOrderResult> {
  return apiRequest<RazorpayOrderResult>('/api/wallet/checkout-session', {
    method: 'POST',
    body: JSON.stringify({ amountUsd }),
  })
}

export function verifyPayment(payload: VerifyPaymentPayload): Promise<void> {
  return apiRequest<void>('/api/wallet/verify-payment', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
