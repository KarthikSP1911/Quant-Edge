'use client'

import { motion, type Variants } from 'framer-motion'

// Seeded PRNG (mulberry32) so the candlestick series is deterministic across server and
// client renders instead of using Math.random, which would cause a hydration mismatch.
function mulberry32(seed: number) {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const CANDLE_COUNT = 26
const CANDLE_WIDTH = 6.5
const CANDLE_GAP = 360 / CANDLE_COUNT
const GRIDLINES = [18, 46, 74, 102]

const CANDLES = (() => {
  const random = mulberry32(42)
  let price = 100
  const candles: {
    cx: number
    open: number
    close: number
    high: number
    low: number
    bullish: boolean
  }[] = []

  for (let i = 0; i < CANDLE_COUNT; i++) {
    const open = price
    // Gentle overall uptrend (drift) with per-candle noise, matching the "+2.4%" badge.
    const drift = -3.2
    const noise = (random() - 0.5) * 9
    const close = Math.max(6, Math.min(104, open + drift + noise))
    const wickUp = random() * 5
    const wickDown = random() * 5
    price = close

    candles.push({
      cx: CANDLE_WIDTH / 2 + i * CANDLE_GAP,
      open,
      close,
      high: Math.max(0, Math.min(open, close) - wickUp),
      low: Math.min(110, Math.max(open, close) + wickDown),
      // Smaller y = higher price, so a close above the open (smaller y) is bullish.
      bullish: close <= open,
    })
  }

  return candles
})()

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 24 },
  show: (delay = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] },
  }),
}

export default function Hero() {
  return (
    <section className="relative overflow-hidden">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[520px] bg-[radial-gradient(ellipse_60%_50%_at_50%_0%,var(--color-accent-light),transparent)]"
        aria-hidden="true"
      />

      <div className="mx-auto max-w-7xl px-8 pt-10 pb-24 sm:px-12 sm:pt-14 sm:pb-32 lg:px-20">
        <div className="grid items-center gap-16 lg:grid-cols-2 lg:gap-12">
          <div>
            <motion.p
              initial="hidden"
              animate="show"
              custom={0}
              variants={fadeUp}
              className="inline-flex items-center rounded-full border border-[var(--color-border)] bg-[var(--color-card-bg)] px-3 py-1 text-xs font-semibold tracking-wide text-[var(--color-text-secondary)]"
            >
              Simulated trading, real market data
            </motion.p>

            <motion.h1
              initial="hidden"
              animate="show"
              custom={0.1}
              variants={fadeUp}
              className="mt-6 text-3xl leading-[1.1] font-semibold tracking-tight text-[var(--color-text-primary)] sm:text-4xl"
            >
              Make Better <br />
              Investment <br />
              Decisions With <span className="text-[var(--color-accent-blue)]">AI</span>
            </motion.h1>

            <motion.p
              initial="hidden"
              animate="show"
              custom={0.2}
              variants={fadeUp}
              className="mt-6 max-w-md text-lg text-[var(--color-text-secondary)]"
            >
              QuantEdge pairs live market data with an AI research agent and a full order matching
              engine, so you can build and test strategies before risking real capital.
            </motion.p>

            <motion.div
              initial="hidden"
              animate="show"
              custom={0.3}
              variants={fadeUp}
              className="mt-9 flex flex-wrap items-center gap-6"
            >
              <motion.a
                href="/register"
                whileHover={{ scale: 1.03 }}
                whileTap={{ scale: 0.97 }}
                className="rounded-full bg-[var(--color-accent-blue)] px-7 py-3.5 text-sm font-semibold text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700"
              >
                Get Started Free
              </motion.a>
              <a
                href="#features"
                className="text-sm font-semibold text-[var(--color-text-primary)] underline decoration-[var(--color-border)] underline-offset-4 transition-[text-decoration-color] hover:decoration-[var(--color-text-primary)]"
              >
                See how it works
              </a>
            </motion.div>
          </div>

          <div className="relative">
            <motion.div
              initial={{ opacity: 0, y: 30, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              whileHover={{ y: -4 }}
              transition={{ duration: 0.7, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}
              className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6 shadow-xl shadow-slate-900/5 transition-shadow hover:shadow-2xl"
            >
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-medium text-[var(--color-text-secondary)]">NVDA</p>
                  <p className="mt-1 text-3xl font-semibold text-[var(--color-text-primary)]">
                    $138.42
                  </p>
                </div>
                <div className="rounded-full bg-[var(--color-accent-light)] px-3 py-1 text-sm font-semibold text-[var(--color-accent-blue)]">
                  +2.4%
                </div>
              </div>

              <svg
                viewBox="0 0 360 110"
                className="mt-6 h-40 w-full"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                {GRIDLINES.map((y) => (
                  <line
                    key={y}
                    x1="0"
                    y1={y}
                    x2="360"
                    y2={y}
                    stroke="var(--color-border)"
                    strokeWidth="1"
                    strokeDasharray="2 4"
                  />
                ))}

                {CANDLES.map((candle, i) => {
                  const color = candle.bullish ? 'var(--color-profit)' : 'var(--color-loss)'
                  const bodyTop = Math.min(candle.open, candle.close)
                  const bodyHeight = Math.max(1.4, Math.abs(candle.close - candle.open))
                  return (
                    <g key={i}>
                      <line
                        x1={candle.cx}
                        y1={candle.high}
                        x2={candle.cx}
                        y2={candle.low}
                        stroke={color}
                        strokeWidth="1"
                      />
                      <rect
                        x={candle.cx - CANDLE_WIDTH / 2}
                        y={bodyTop}
                        width={CANDLE_WIDTH}
                        height={bodyHeight}
                        rx="0.75"
                        fill={color}
                      />
                    </g>
                  )
                })}
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
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: -10, x: -10 }}
              animate={{ opacity: 1, y: [0, -6, 0], x: 0 }}
              whileHover={{ scale: 1.06 }}
              transition={{
                opacity: { duration: 0.6, delay: 0.55, ease: [0.22, 1, 0.36, 1] },
                x: { duration: 0.6, delay: 0.55, ease: [0.22, 1, 0.36, 1] },
                y: { duration: 3.2, delay: 1.2, repeat: Infinity, ease: 'easeInOut' },
              }}
              className="absolute -top-6 -left-6 hidden cursor-default items-center gap-3 rounded-xl border border-[var(--color-border)] bg-white px-4 py-3 shadow-lg shadow-slate-900/10 sm:flex"
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-xs font-bold text-[var(--color-accent-blue)]">
                AA
              </div>
              <div>
                <p className="text-xs font-medium text-[var(--color-text-secondary)]">AAPL</p>
                <p className="text-sm font-semibold text-[var(--color-text-primary)]">$214.36</p>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 10, x: 10 }}
              animate={{ opacity: 1, y: [0, 6, 0], x: 0 }}
              whileHover={{ scale: 1.06 }}
              transition={{
                opacity: { duration: 0.6, delay: 0.7, ease: [0.22, 1, 0.36, 1] },
                x: { duration: 0.6, delay: 0.7, ease: [0.22, 1, 0.36, 1] },
                y: { duration: 3.6, delay: 1.4, repeat: Infinity, ease: 'easeInOut' },
              }}
              className="absolute -bottom-6 -right-4 hidden cursor-default items-center gap-3 rounded-xl border border-[var(--color-border)] bg-white px-4 py-3 shadow-lg shadow-slate-900/10 sm:flex"
            >
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-xs font-bold text-[var(--color-accent-blue)]">
                MS
              </div>
              <div>
                <p className="text-xs font-medium text-[var(--color-text-secondary)]">MSFT</p>
                <p className="text-sm font-semibold text-[var(--color-text-primary)]">$301.20</p>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  )
}
