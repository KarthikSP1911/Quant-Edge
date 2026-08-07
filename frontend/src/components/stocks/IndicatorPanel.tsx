'use client'

import { useEffect, useRef } from 'react'
import {
  ColorType,
  createChart,
  LineSeries,
  type IChartApi,
  type UTCTimestamp,
} from 'lightweight-charts'
import type { LinePoint } from '@/lib/utils/indicators'

function toTime(isoString: string): UTCTimestamp {
  return (new Date(isoString).getTime() / 1000) as UTCTimestamp
}

export interface IndicatorSeries {
  data: LinePoint[]
  color: string
}

export default function IndicatorPanel({
  title,
  series,
}: {
  title: string
  series: IndicatorSeries[]
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const chart = createChart(container, {
      height: 120,
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
    })
    chartRef.current = chart

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
    }
  }, [])

  useEffect(() => {
    const chart = chartRef.current
    if (!chart) return

    const lineSeries = series.map(({ color }) =>
      chart.addSeries(LineSeries, { color, lineWidth: 2, crosshairMarkerVisible: false }),
    )
    series.forEach((s, i) => {
      lineSeries[i].setData(s.data.map((p) => ({ time: toTime(p.time), value: p.value })))
    })
    chart.timeScale().fitContent()

    return () => {
      // The chart may already be disposed by the mount effect's own cleanup (e.g. on unmount,
      // where cleanup order isn't guaranteed relative to this effect) — removeSeries on a
      // disposed chart throws, so this is a legitimate no-op in that case, not an error.
      lineSeries.forEach((s) => {
        try {
          chart.removeSeries(s)
        } catch {
          // chart already disposed
        }
      })
    }
  }, [series])

  return (
    <div className="rounded-lg border border-[var(--color-border)] p-2">
      <div className="px-1 text-xs font-medium text-[var(--color-text-secondary)]">{title}</div>
      <div ref={containerRef} className="w-full" />
    </div>
  )
}
