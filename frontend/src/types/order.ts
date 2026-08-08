export type OrderSide = 'BUY' | 'SELL'
export type OrderType = 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'STOP_LIMIT'
export type TimeInForce = 'DAY' | 'GTC'

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

export interface PlaceOrderInput {
  symbol: string
  side: OrderSide
  type: Exclude<OrderType, 'MARKET'>
  quantity: number
  limitPrice: number | null
  stopPrice: number | null
  timeInForce: TimeInForce
}

// Mirrors the backend's PlacedOrderResponse (POST /api/orders).
export interface PlacedOrderResult {
  id: string
  symbol: string
  side: OrderSide
  type: OrderType
  quantity: number
  limitPrice: number | null
  stopPrice: number | null
  timeInForce: TimeInForce
  status: string
  expiresAt: string | null
  createdAt: string
}
