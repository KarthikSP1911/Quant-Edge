const CHART_POINTS = [
  [0, 92],
  [30, 84],
  [60, 88],
  [90, 70],
  [120, 76],
  [150, 58],
  [180, 64],
  [210, 46],
  [240, 52],
  [270, 34],
  [300, 40],
  [330, 22],
  [360, 28],
]

const CHART_LINE = CHART_POINTS.map(([x, y]) => `${x},${y}`).join(' ')
const CHART_AREA = `0,110 ${CHART_LINE} 360,110`

export default function Hero() {
  return (
    <section className="mx-auto max-w-7xl px-6 pt-16 pb-20 sm:pt-24 sm:pb-28">
      <div className="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
        <div>
          <p className="text-xs font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
            Simulated trading platform
          </p>
          <h1 className="mt-4 text-4xl leading-tight font-semibold tracking-tight text-[var(--color-text-primary)] sm:text-5xl">
            Research stocks and trade with real market data — risk-free.
          </h1>
          <p className="mt-5 max-w-md text-lg text-[var(--color-text-secondary)]">
            QuantEdge pairs live market data with an AI research agent and a full order matching
            engine, so you can build and test strategies before risking real capital.
          </p>
          <div className="mt-8 flex flex-wrap items-center gap-6">
            <a
              href="/signup"
              className="rounded-md bg-[var(--color-accent-blue)] px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            >
              Get Started
            </a>
            <a
              href="#docs"
              className="text-sm font-medium text-[var(--color-text-primary)] underline decoration-[var(--color-border)] underline-offset-4 hover:decoration-[var(--color-text-primary)]"
            >
              View Docs
            </a>
          </div>
        </div>

        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6 shadow-sm">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-[var(--color-text-secondary)]">NVDA</p>
              <p className="mt-1 text-2xl font-semibold text-[var(--color-text-primary)]">
                $138.42
              </p>
            </div>
            <div className="rounded-md bg-[var(--color-accent-light)] px-2 py-1 text-sm font-medium text-[var(--color-profit)]">
              +2.4%
            </div>
          </div>

          <svg
            viewBox="0 0 360 110"
            className="mt-6 h-40 w-full"
            preserveAspectRatio="none"
            aria-hidden="true"
          >
            <polygon points={CHART_AREA} fill="var(--color-accent-light)" opacity="0.5" />
            <polyline
              points={CHART_LINE}
              fill="none"
              stroke="var(--color-accent-blue)"
              strokeWidth="2"
              strokeLinejoin="round"
              strokeLinecap="round"
            />
          </svg>

          <div className="mt-4 grid grid-cols-3 gap-4 border-t border-[var(--color-border)] pt-4 text-sm">
            <div>
              <p className="text-[var(--color-text-muted)]">Open</p>
              <p className="mt-1 font-medium text-[var(--color-text-primary)]">$135.10</p>
            </div>
            <div>
              <p className="text-[var(--color-text-muted)]">Volume</p>
              <p className="mt-1 font-medium text-[var(--color-text-primary)]">42.1M</p>
            </div>
            <div>
              <p className="text-[var(--color-text-muted)]">52w Range</p>
              <p className="mt-1 font-medium text-[var(--color-text-primary)]">86–145</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
