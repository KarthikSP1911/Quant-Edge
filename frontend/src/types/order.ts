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

// Mirrors the backend's GraphQL PendingOrder type (a chat-agent-staged order awaiting confirmation).
export interface PendingOrder {
  symbol: string
  side: OrderSide
  type: OrderKind
  quantity: number
  limitPrice: number | null
  stopPrice: number | null
  estimatedCost: number | null
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
