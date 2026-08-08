'use client'

import { useEffect, useRef } from 'react'
import {
  ColorType,
  createChart,
  LineSeries,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import type { ComparisonEntry } from '@/types/comparison'

/**
 * Series colours for up to three stocks. Deliberately avoids the profit/loss green and red from the
 * design tokens — those already mean "up" and "down" everywhere else in the app, and reusing them
 * for a stock's identity would read as a gain/loss judgement on that stock.
 */
export const SERIES_COLORS = ['#2563EB', '#F59E0B', '#64748B'] as const

function toTime(isoString: string): UTCTimestamp {
  return (new Date(isoString).getTime() / 1000) as UTCTimestamp
}

/**
 * Rebases a price series to percent change from its first bar.
 *
 * Comparing raw prices is meaningless across different price scales — a $700 stock and a $30 stock
 * share no usable y-axis. Rebasing every series to 0% at the common start makes the shapes
 * comparable, which is the whole point of the overlay.
 */
export function toPercentChangeSeries(entry: ComparisonEntry): { time: string; value: number }[] {
  const base = entry.candles[0]?.close
  if (base === undefined || base === 0) return []

  return entry.candles.map((candle) => ({
    time: candle.time,
    value: ((candle.close - base) / base) * 100,
  }))
}

export default function ComparisonOverlayChart({ entries }: { entries: ComparisonEntry[] }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Line'>[]>([])

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, {
      height: 380,
      layout: {
        background: { type: ColorType.Solid, color: '#FFFFFF' },
        textColor: '#64748B',
        fontFamily: 'var(--font-sans), Inter, sans-serif',
      },
      grid: {
        vertLines: { color: '#E2E8F0' },
        horzLines: { color: '#E2E8F0' },
      },
      rightPriceScale: { borderColor: '#E2E8F0' },
      timeScale: { borderColor: '#E2E8F0' },
      localization: {
        priceFormatter: (value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(1)}%`,
      },
    })

    chartRef.current = chart

    let lastWidth = container.clientWidth
    const resizeObserver = new ResizeObserver((observed) => {
      const { width } = observed[0].contentRect
      // Same guard as PriceChart: redundant applyOptions calls can cascade into a
      // ResizeObserver feedback loop.
      if (Math.abs(width - lastWidth) < 1) return
      lastWidth = width
      chart.applyOptions({ width })
    })
    resizeObserver.observe(container)

    return () => {
      resizeObserver.disconnect()
      chart.remove()
      chartRef.current = null
      seriesRef.current = []
    }
  }, [])

  useEffect(() => {
    const chart = chartRef.current
    if (!chart) return

    seriesRef.current.forEach((series) => {
      try {
        chart.removeSeries(series)
      } catch {
        // chart already disposed
      }
    })

    seriesRef.current = entries.map((entry, index) => {
      const series = chart.addSeries(LineSeries, {
        color: SERIES_COLORS[index % SERIES_COLORS.length],
        lineWidth: 2,
        title: entry.symbol,
      })
      series.setData(
        toPercentChangeSeries(entry).map((p) => ({ time: toTime(p.time), value: p.value })),
      )
      return series
    })

    chart.timeScale().fitContent()
  }, [entries])

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-4">
        {entries.map((entry, index) => (
          <div key={entry.symbol} className="flex items-center gap-2 text-sm">
            <span
              aria-hidden
              className="h-0.5 w-4 rounded-full"
              style={{ backgroundColor: SERIES_COLORS[index % SERIES_COLORS.length] }}
            />
            <span className="font-medium text-[var(--color-text-primary)]">{entry.symbol}</span>
          </div>
        ))}
      </div>
      <div ref={containerRef} className="w-full" />
      <p className="text-xs text-[var(--color-text-muted)]">
        Normalized to percent change from the start of the range, so stocks at different price
        levels can be compared directly.
      </p>
    </div>
  )
}
