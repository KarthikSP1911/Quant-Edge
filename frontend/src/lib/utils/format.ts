export function formatPrice(price: number | null): string {
  if (price === null) return '—'
  return price.toLocaleString('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

export function formatMarketCap(marketCap: number | null): string {
  if (marketCap === null) return '—'
  if (marketCap >= 1e12) return `${(marketCap / 1e12).toFixed(1)}T`
  if (marketCap >= 1e9) return `${(marketCap / 1e9).toFixed(1)}B`
  if (marketCap >= 1e6) return `${(marketCap / 1e6).toFixed(1)}M`
  return marketCap.toLocaleString('en-US')
}

export function formatChangePercent(changePercent: number | null): string {
  if (changePercent === null) return '—'
  const sign = changePercent > 0 ? '+' : ''
  return `${sign}${changePercent.toFixed(2)}%`
}

export function formatPeRatio(peRatio: number | null): string {
  if (peRatio === null) return '—'
  return peRatio.toFixed(1)
}

export function formatSignedCurrency(value: number | null): string {
  if (value === null) return '—'
  const sign = value > 0 ? '+' : value < 0 ? '-' : ''
  return `${sign}${Math.abs(value).toLocaleString('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}
