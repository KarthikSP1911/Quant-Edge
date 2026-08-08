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

export type OrderStatus =
  'PENDING' | 'OPEN' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED' | 'EXPIRED'

export type OrderKind = 'MARKET' | 'LIMIT' | 'STOP_LOSS' | 'STOP_LIMIT'

// Mirrors the backend's GraphQL Order type (openOrders/filledOrders/orderHistory).
export interface Order {
  id: string
  symbol: string
  side: OrderSide
  type: OrderKind
  status: OrderStatus
  quantity: number
  filledQuantity: number
  limitPrice: number | null
  stopPrice: number | null
  createdAt: string
  updatedAt: string
  expiresAt: string | null
}

// Mirrors the backend's TradeExecutedMessage, pushed on GET /api/orders/stream.
export interface OrderFillEvent {
  executionId: string
  orderId: string
  userId: string
  symbol: string
  side: OrderSide
  quantity: number
  price: number
  executedAt: string
}
