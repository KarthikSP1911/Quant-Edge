'use client'

import { useEffect, useRef } from 'react'
import {
  CandlestickSeries,
  ColorType,
  createChart,
  LineSeries,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import type { Candle } from '@/types/stock'
import { computeBollingerBands } from '@/lib/utils/indicators'

export interface ChartOverlays {
  bollinger: boolean
}

function toTime(isoString: string): UTCTimestamp {
  return (new Date(isoString).getTime() / 1000) as UTCTimestamp
}

export default function PriceChart({
  candles,
  overlays,
}: {
  candles: Candle[]
  overlays: ChartOverlays
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null)
  const bollingerSeriesRef = useRef<ISeriesApi<'Line'>[]>([])

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, {
      height: 360,
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
      // timeVisible lets the axis show HH:MM for intraday (1D) candles instead of
      // collapsing every bar in the day down to the same date label.
      timeScale: { borderColor: '#E2E8F0', timeVisible: true, secondsVisible: false },
    })

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#16A34A',
      downColor: '#DC2626',
      borderVisible: false,
      wickUpColor: '#16A34A',
      wickDownColor: '#DC2626',
    })

    chartRef.current = chart
    candleSeriesRef.current = candleSeries

    let lastWidth = container.clientWidth
    const resizeObserver = new ResizeObserver((entries) => {
      const { width } = entries[0].contentRect
      // Guard against redundant applyOptions calls, which can otherwise cascade into a
      // ResizeObserver feedback loop when several charts on the page resize at once.
      if (Math.abs(width - lastWidth) < 1) return
      lastWidth = width
      chart.applyOptions({ width })
    })
    resizeObserver.observe(container)

    return () => {
      resizeObserver.disconnect()
      chart.remove()
      chartRef.current = null
      candleSeriesRef.current = null
      bollingerSeriesRef.current = []
    }
  }, [])

  useEffect(() => {
    const candleSeries = candleSeriesRef.current
    if (!candleSeries) return

    candleSeries.setData(
      candles.map((c) => ({
        time: toTime(c.time),
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      })),
    )
    chartRef.current?.timeScale().fitContent()
  }, [candles])

  useEffect(() => {
    const chart = chartRef.current
    if (!chart) return

    bollingerSeriesRef.current.forEach((series) => {
      try {
        chart.removeSeries(series)
      } catch {
        // chart already disposed
      }
    })
    bollingerSeriesRef.current = []

    if (!overlays.bollinger) return

    const bands = computeBollingerBands(candles)
    const lines: { data: { time: string; value: number }[]; color: string }[] = [
      { data: bands.upper, color: '#2563EB' },
      { data: bands.middle, color: '#94A3B8' },
      { data: bands.lower, color: '#2563EB' },
    ]

    bollingerSeriesRef.current = lines.map(({ data, color }) => {
      const series = chart.addSeries(LineSeries, {
        color,
        lineWidth: 1,
        crosshairMarkerVisible: false,
      })
      series.setData(data.map((p) => ({ time: toTime(p.time), value: p.value })))
      return series
    })
  }, [candles, overlays.bollinger])

  return <div ref={containerRef} className="w-full" />
}
