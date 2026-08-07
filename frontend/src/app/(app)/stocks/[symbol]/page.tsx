'use client'

import { useMemo, useState } from 'react'
import { useParams } from 'next/navigation'
import { useCandles, useStockDetail } from '@/hooks/useStockDetail'
import { computeMacd, computeRsi } from '@/lib/utils/indicators'
import type { ChartRange } from '@/types/stock'
import StockHeader from '@/components/stocks/StockHeader'
import PriceChart from '@/components/stocks/PriceChart'
import IndicatorPanel from '@/components/stocks/IndicatorPanel'
import RangeToggle from '@/components/stocks/RangeToggle'
import OverlayToggles, { type OverlayState } from '@/components/stocks/OverlayToggles'
import ActionButtons from '@/components/stocks/ActionButtons'
import NewsList from '@/components/stocks/NewsList'
import FinancialsTable from '@/components/stocks/FinancialsTable'
import {
  StockDetailError,
  StockDetailSkeleton,
  StockNotFound,
} from '@/components/stocks/StockDetailStates'

export default function StockDetailPage() {
  const params = useParams<{ symbol: string }>()
  const symbol = params.symbol.toUpperCase()

  const [range, setRange] = useState<ChartRange>('3M')
  const [overlays, setOverlays] = useState<OverlayState>({
    bollinger: false,
    rsi: false,
    macd: false,
  })

  const {
    data: stock,
    isPending: stockPending,
    isError: stockError,
    refetch: refetchStock,
  } = useStockDetail(symbol)

  const {
    data: candles,
    isPending: candlesPending,
    isError: candlesError,
  } = useCandles(symbol, range, stock?.price)

  const rsi = useMemo(() => (candles ? computeRsi(candles) : []), [candles])
  const macd = useMemo(() => (candles ? computeMacd(candles) : { macd: [], signal: [] }), [candles])
  const rsiSeries = useMemo(() => [{ data: rsi, color: '#2563EB' }], [rsi])
  const macdSeries = useMemo(
    () => [
      { data: macd.macd, color: '#2563EB' },
      { data: macd.signal, color: '#F59E0B' },
    ],
    [macd],
  )

  if (stockPending) return <StockDetailSkeleton />
  if (stockError) return <StockDetailError onRetry={() => refetchStock()} />
  if (!stock) return <StockNotFound symbol={symbol} />

  return (
    <div className="flex flex-col gap-6">
      <StockHeader stock={stock} />
      <ActionButtons symbol={stock.symbol} price={stock.price} />

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <RangeToggle value={range} onChange={setRange} />
          <OverlayToggles value={overlays} onChange={setOverlays} />
        </div>

        {candlesPending && (
          <div className="h-[360px] w-full animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
        )}
        {candlesError && <StockDetailError onRetry={() => undefined} />}
        {candles && (
          <div className="rounded-lg border border-[var(--color-border)] p-2">
            <PriceChart candles={candles} overlays={{ bollinger: overlays.bollinger }} />
          </div>
        )}

        {overlays.rsi && candles && <IndicatorPanel title="RSI (14)" series={rsiSeries} />}
        {overlays.macd && candles && (
          <IndicatorPanel title="MACD (12, 26, 9)" series={macdSeries} />
        )}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="rounded-lg border border-[var(--color-border)] p-4">
          <h2 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
            Financials
          </h2>
          <FinancialsTable financials={stock.financials} />
        </section>

        <section className="rounded-lg border border-[var(--color-border)] p-4">
          <h2 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">News</h2>
          <NewsList news={stock.news} />
        </section>
      </div>

      {stock.description && (
        <section className="rounded-lg border border-[var(--color-border)] p-4">
          <h2 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">About</h2>
          <p className="text-sm text-[var(--color-text-secondary)]">{stock.description}</p>
        </section>
      )}
    </div>
  )
}
