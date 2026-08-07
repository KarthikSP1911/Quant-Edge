export type OrderSide = 'BUY' | 'SELL'

export interface TradeOrderInput {
  symbol: string
  side: OrderSide
  quantity: number
}

export interface TradeOrderResult {
  orderId: string
  symbol: string
  side: OrderSide
  quantity: number
  price: number
  totalValue: number
  executedAt: string
}
