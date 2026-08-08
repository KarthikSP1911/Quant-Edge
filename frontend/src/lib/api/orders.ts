import type {
  PlaceOrderInput,
  PlacedOrderResult,
  TradeOrderInput,
  TradeOrderResult,
} from '@/types/order'
import { apiRequest } from './client'

export async function placeOrder(input: TradeOrderInput): Promise<TradeOrderResult> {
  const path = input.side === 'BUY' ? '/api/orders/buy' : '/api/orders/sell'
  return apiRequest<TradeOrderResult>(path, {
    method: 'POST',
    body: JSON.stringify({ symbol: input.symbol, quantity: input.quantity }),
  })
}

export async function placeLimitOrder(input: PlaceOrderInput): Promise<PlacedOrderResult> {
  return apiRequest<PlacedOrderResult>('/api/orders', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}
