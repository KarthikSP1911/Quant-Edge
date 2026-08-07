const CANDLES = [
  { x: 8, high: 30, low: 60, open: 50, close: 38, up: true },
  { x: 24, high: 25, low: 55, open: 38, close: 45, up: false },
  { x: 40, high: 40, low: 70, open: 45, close: 60, up: false },
  { x: 56, high: 20, low: 58, open: 60, close: 30, up: true },
  { x: 72, high: 15, low: 40, open: 30, close: 22, up: true },
  { x: 88, high: 18, low: 48, open: 22, close: 40, up: false },
  { x: 104, high: 10, low: 35, open: 40, close: 18, up: true },
  { x: 120, high: 8, low: 28, open: 18, close: 14, up: true },
  { x: 136, high: 12, low: 32, open: 14, close: 26, up: false },
  { x: 152, high: 6, low: 24, open: 26, close: 10, up: true },
]

const indicators = [
  { label: 'RSI (14)', value: '58.2' },
  { label: 'MACD', value: '+1.84' },
  { label: 'Volume', value: '42.1M' },
  { label: '52w Range', value: '$86 – $145' },
]

export default function ProductPreview() {
  return (
    <section className="bg-[var(--color-card-bg)] py-20">
      <div className="mx-auto max-w-7xl px-6">
        <div className="max-w-2xl">
          <h2 className="text-3xl font-semibold tracking-tight text-[var(--color-text-primary)]">
            Every stock, fully broken down
          </h2>
          <p className="mt-3 text-lg text-[var(--color-text-secondary)]">
            Price action, indicators, and AI-generated research in a single view.
          </p>
        </div>

        <div className="mt-12 overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-page-bg)] shadow-sm">
          <div className="flex items-center gap-2 border-b border-[var(--color-border)] px-4 py-3">
            <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
            <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
            <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
            <span className="ml-3 text-xs font-medium text-[var(--color-text-muted)]">
              AAPL · Apple Inc.
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px]">
            <div className="border-b border-[var(--color-border)] p-6 lg:border-r lg:border-b-0">
              <div className="flex items-baseline gap-3">
                <p className="text-3xl font-semibold text-[var(--color-text-primary)]">$214.36</p>
                <p className="text-sm font-medium text-[var(--color-loss)]">−1.12 (−0.52%)</p>
              </div>

              <svg viewBox="0 0 168 70" className="mt-6 h-56 w-full" aria-hidden="true">
                <line
                  x1="0"
                  y1="17.5"
                  x2="168"
                  y2="17.5"
                  stroke="var(--color-border)"
                  strokeWidth="1"
                />
                <line
                  x1="0"
                  y1="35"
                  x2="168"
                  y2="35"
                  stroke="var(--color-border)"
                  strokeWidth="1"
                />
                <line
                  x1="0"
                  y1="52.5"
                  x2="168"
                  y2="52.5"
                  stroke="var(--color-border)"
                  strokeWidth="1"
                />
                {CANDLES.map((c) => (
                  <g key={c.x}>
                    <line
                      x1={c.x}
                      y1={c.high}
                      x2={c.x}
                      y2={c.low}
                      stroke={c.up ? 'var(--color-profit)' : 'var(--color-loss)'}
                      strokeWidth="1"
                    />
                    <rect
                      x={c.x - 3}
                      y={Math.min(c.open, c.close)}
                      width="6"
                      height={Math.max(Math.abs(c.close - c.open), 2)}
                      fill={c.up ? 'var(--color-profit)' : 'var(--color-loss)'}
                    />
                  </g>
                ))}
              </svg>

              <div className="mt-6 grid grid-cols-2 gap-4 border-t border-[var(--color-border)] pt-6 sm:grid-cols-4">
                {indicators.map((indicator) => (
                  <div key={indicator.label}>
                    <p className="text-xs text-[var(--color-text-muted)]">{indicator.label}</p>
                    <p className="mt-1 text-sm font-medium text-[var(--color-text-primary)]">
                      {indicator.value}
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-6">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
                  AI Research Agent
                </h3>
                <span className="rounded-md bg-[var(--color-accent-light)] px-2 py-0.5 text-xs font-medium text-[var(--color-accent-blue)]">
                  87% confidence
                </span>
              </div>
              <p className="mt-3 text-sm leading-relaxed text-[var(--color-text-secondary)]">
                Revenue growth remains steady on services expansion, though hardware margins
                narrowed this quarter. Analyst sentiment is mixed heading into the next earnings
                call.
              </p>
              <div className="mt-4 flex flex-wrap gap-2 border-t border-[var(--color-border)] pt-4">
                <span className="rounded-md border border-[var(--color-border)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
                  10-K filing
                </span>
                <span className="rounded-md border border-[var(--color-border)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
                  Earnings call
                </span>
                <span className="rounded-md border border-[var(--color-border)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
                  News (30d)
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
