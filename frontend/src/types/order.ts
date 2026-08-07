export type OrderSide = 'BUY' | 'SELL'

export interface TradeOrderInput {
  symbol: string
  side: OrderSide
  quantity: number
}

// Mirrors the backend's OrderResponse (POST /api/orders/buy|sell).
export interface TradeOrderResult {
  id: string
  symbol: string
  side: OrderSide
  quantity: number
  executionPrice: number
  status: string
  executedAt: string
}
