import type { Candle } from '@/types/stock'

export interface LinePoint {
  time: string
  value: number
}

function ema(values: number[], period: number): number[] {
  const k = 2 / (period + 1)
  const result: number[] = []
  values.forEach((value, i) => {
    result.push(i === 0 ? value : value * k + result[i - 1] * (1 - k))
  })
  return result
}

export function computeRsi(candles: Candle[], period = 14): LinePoint[] {
  if (candles.length <= period) return []

  const gains: number[] = []
  const losses: number[] = []
  for (let i = 1; i < candles.length; i++) {
    const change = candles[i].close - candles[i - 1].close
    gains.push(Math.max(change, 0))
    losses.push(Math.max(-change, 0))
  }

  const points: LinePoint[] = []
  let avgGain = gains.slice(0, period).reduce((a, b) => a + b, 0) / period
  let avgLoss = losses.slice(0, period).reduce((a, b) => a + b, 0) / period

  for (let i = period; i < gains.length; i++) {
    avgGain = (avgGain * (period - 1) + gains[i]) / period
    avgLoss = (avgLoss * (period - 1) + losses[i]) / period
    const rs = avgLoss === 0 ? 100 : avgGain / avgLoss
    const rsi = avgLoss === 0 ? 100 : 100 - 100 / (1 + rs)
    points.push({ time: candles[i + 1].time, value: rsi })
  }
  return points
}

export interface MacdResult {
  macd: LinePoint[]
  signal: LinePoint[]
}

export function computeMacd(
  candles: Candle[],
  fastPeriod = 12,
  slowPeriod = 26,
  signalPeriod = 9,
): MacdResult {
  if (candles.length <= slowPeriod) return { macd: [], signal: [] }

  const closes = candles.map((c) => c.close)
  const fastEma = ema(closes, fastPeriod)
  const slowEma = ema(closes, slowPeriod)
  const macdLine = closes.map((_, i) => fastEma[i] - slowEma[i])
  const signalLine = ema(macdLine.slice(slowPeriod - 1), signalPeriod)

  const macd = candles
    .slice(slowPeriod - 1)
    .map((c, i) => ({ time: c.time, value: macdLine[slowPeriod - 1 + i] }))
  const signal = candles
    .slice(slowPeriod - 1)
    .map((c, i) => ({ time: c.time, value: signalLine[i] }))

  return { macd, signal }
}

export interface BollingerBands {
  upper: LinePoint[]
  middle: LinePoint[]
  lower: LinePoint[]
}

export function computeBollingerBands(
  candles: Candle[],
  period = 20,
  stdDevMultiplier = 2,
): BollingerBands {
  if (candles.length < period) return { upper: [], middle: [], lower: [] }

  const upper: LinePoint[] = []
  const middle: LinePoint[] = []
  const lower: LinePoint[] = []

  for (let i = period - 1; i < candles.length; i++) {
    const window = candles.slice(i - period + 1, i + 1).map((c) => c.close)
    const mean = window.reduce((a, b) => a + b, 0) / period
    const variance = window.reduce((a, b) => a + (b - mean) ** 2, 0) / period
    const stdDev = Math.sqrt(variance)

    middle.push({ time: candles[i].time, value: mean })
    upper.push({ time: candles[i].time, value: mean + stdDevMultiplier * stdDev })
    lower.push({ time: candles[i].time, value: mean - stdDevMultiplier * stdDev })
  }

  return { upper, middle, lower }
}
