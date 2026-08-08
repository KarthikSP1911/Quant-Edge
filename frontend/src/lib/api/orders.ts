import type { TradeOrderInput, TradeOrderResult } from '@/types/order'
import { apiRequest } from './client'

export async function placeOrder(input: TradeOrderInput): Promise<TradeOrderResult> {
  const path = input.side === 'BUY' ? '/api/orders/buy' : '/api/orders/sell'
  return apiRequest<TradeOrderResult>(path, {
    method: 'POST',
    body: JSON.stringify({ symbol: input.symbol, quantity: input.quantity }),
  })
}

export function cancelOrder(orderId: string): Promise<void> {
  return apiRequest<void>(`/api/orders/${orderId}/cancel`, { method: 'POST' })
}
