import type { CheckoutSessionResult } from '@/types/wallet'
import { apiRequest } from './client'

export function createCheckoutSession(amountUsd: number): Promise<CheckoutSessionResult> {
  return apiRequest<CheckoutSessionResult>('/api/wallet/checkout-session', {
    method: 'POST',
    body: JSON.stringify({ amountUsd }),
  })
}
