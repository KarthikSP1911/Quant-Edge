// MOCK — buy/sell orders are a REST write per CLAUDE.md's API design rules, but the backend
// `/orders` endpoint doesn't exist yet (Phase 3 order matching engine). This fakes a market
// order fill so the trade modals can be built end-to-end now; swapping in a real fetch() call
// later is a one-file change. Cash balance and owned quantity are held in module state (reset
// on page reload) since this branch doesn't have the portfolio slice's data layer.

import type { OrderSide, TradeOrderInput, TradeOrderResult } from '@/types/order'

const SIMULATED_LATENCY_MS = 400

let mockCashBalance = 10_000
const mockOwnedQuantities: Record<string, number> = {
  AAPL: 4,
  NVDA: 3,
}

export function getMockCashBalance(): number {
  return mockCashBalance
}

export function getMockOwnedQuantity(symbol: string): number {
  return mockOwnedQuantities[symbol] ?? 0
}

export async function placeOrder(input: TradeOrderInput, price: number): Promise<TradeOrderResult> {
  await new Promise((resolve) => setTimeout(resolve, SIMULATED_LATENCY_MS))

  const totalValue = input.quantity * price

  if (input.side === 'BUY' && totalValue > mockCashBalance) {
    throw new Error('Insufficient cash balance for this order.')
  }
  if (input.side === 'SELL' && input.quantity > getMockOwnedQuantity(input.symbol)) {
    throw new Error('Cannot sell more shares than you own.')
  }

  const signedCash: Record<OrderSide, number> = { BUY: -totalValue, SELL: totalValue }
  mockCashBalance += signedCash[input.side]

  const signedQuantity: Record<OrderSide, number> = { BUY: input.quantity, SELL: -input.quantity }
  mockOwnedQuantities[input.symbol] =
    (mockOwnedQuantities[input.symbol] ?? 0) + signedQuantity[input.side]

  return {
    orderId: crypto.randomUUID(),
    symbol: input.symbol,
    side: input.side,
    quantity: input.quantity,
    price,
    totalValue,
    executedAt: new Date().toISOString(),
  }
}
