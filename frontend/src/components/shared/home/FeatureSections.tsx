'use client'

import { motion } from 'framer-motion'
import type { ReactNode } from 'react'

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
  { label: '52w Range', value: '$86–145' },
]

function TwitterMark() {
  return (
    <svg viewBox="0 0 20 20" className="h-full w-full" aria-hidden="true">
      <circle cx="10" cy="10" r="10" fill="#1DA1F2" />
      <path
        d="M15.5 7.3c-.4.2-.8.3-1.2.4a2.2 2.2 0 0 0 1-1.2 4.3 4.3 0 0 1-1.4.5 2.2 2.2 0 0 0-3.7 2 6.2 6.2 0 0 1-4.5-2.3 2.2 2.2 0 0 0 .7 2.9c-.3 0-.7-.1-1-.3v.1a2.2 2.2 0 0 0 1.8 2.1 2.2 2.2 0 0 1-1 0 2.2 2.2 0 0 0 2 1.5 4.4 4.4 0 0 1-3.3.9 6.2 6.2 0 0 0 3.3 1c4 0 6.2-3.4 6.2-6.2v-.3c.4-.3.8-.7 1.1-1.1z"
        fill="#fff"
      />
    </svg>
  )
}

function AppleMark() {
  return (
    <svg viewBox="0 0 20 20" className="h-full w-full" aria-hidden="true">
      <circle cx="10" cy="10" r="10" fill="#0F172A" />
      <path
        d="M12.6 6.2c.5-.6.9-1.4.8-2.2-.7 0-1.6.5-2.1 1.1-.5.5-.9 1.4-.8 2.1.8.1 1.6-.4 2.1-1zM14.6 10.5c0-1.4 1.1-2.1 1.2-2.1-.6-.9-1.6-1.1-2-1.1-.8-.1-1.6.5-2 .5-.4 0-1.1-.5-1.8-.5-.9 0-1.8.5-2.3 1.4-1 1.7-.2 4.2.7 5.6.4.7 1 1.5 1.6 1.4.7 0 .9-.4 1.7-.4.8 0 1 .4 1.7.4.7 0 1.2-.7 1.6-1.3.5-.7.7-1.4.7-1.4-.1 0-1.4-.5-1.4-2.1z"
        fill="#fff"
      />
    </svg>
  )
}

function MicrosoftMark() {
  return (
    <svg
      viewBox="0 0 20 20"
      className="h-full w-full rounded-full bg-white p-1.5"
      aria-hidden="true"
    >
      <rect x="1" y="1" width="8.5" height="8.5" fill="#F35325" />
      <rect x="10.5" y="1" width="8.5" height="8.5" fill="#81BC06" />
      <rect x="1" y="10.5" width="8.5" height="8.5" fill="#05A6F0" />
      <rect x="10.5" y="10.5" width="8.5" height="8.5" fill="#FFBA08" />
    </svg>
  )
}

function GoogleMark() {
  return (
    <svg
      viewBox="0 0 18 18"
      className="h-full w-full rounded-full border border-[var(--color-border)] bg-white p-1"
      aria-hidden="true"
    >
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332C2.438 15.983 5.482 18 9 18z"
      />
      <path
        fill="#FBBC05"
        d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0 5.482 0 2.438 2.017.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z"
      />
    </svg>
  )
}

function NikeMark() {
  return (
    <svg viewBox="0 0 20 20" className="h-full w-full" aria-hidden="true">
      <circle cx="10" cy="10" r="10" fill="#111827" />
      <path d="M3 12.5 15 6.2c1 .1 1.7.6 2 1L6.5 14.5c-1.7.6-3-.6-3.5-2z" fill="#fff" />
    </svg>
  )
}

const TOP_STOCKS = [
  {
    ticker: 'TWTR',
    name: 'Twitter Inc.',
    price: '$63.98',
    change: '−0.23%',
    up: false,
    points: '0,6 8,10 16,8 24,16 32,14 40,20',
    Logo: TwitterMark,
  },
  {
    ticker: 'GOOGL',
    name: 'Alphabet Inc.',
    price: '$2.84k',
    change: '+0.58%',
    up: true,
    points: '0,20 8,16 16,18 24,10 32,12 40,4',
    Logo: GoogleMark,
  },
  {
    ticker: 'MSFT',
    name: 'Microsoft',
    price: '$302.1',
    change: '+0.23%',
    up: true,
    points: '0,14 8,18 16,10 24,12 32,6 40,8',
    Logo: MicrosoftMark,
  },
  {
    ticker: 'NKE',
    name: 'Nike, Inc.',
    price: '$169.8',
    change: '−0.82%',
    up: false,
    points: '0,4 8,8 16,6 24,14 32,12 40,18',
    Logo: NikeMark,
  },
]

function TopStocksVisual() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-gradient-to-b from-[var(--color-accent-light)]/60 to-[var(--color-page-bg)] p-6 shadow-xl shadow-slate-900/5">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-[var(--color-text-primary)]">Top Stocks</h3>
        <a
          href="#"
          className="text-sm font-medium text-[var(--color-accent-blue)] underline-offset-2 hover:underline"
        >
          View all
        </a>
      </div>

      <div className="mt-5 space-y-1">
        {TOP_STOCKS.map((stock) => (
          <div
            key={stock.ticker}
            className="flex items-center gap-3 rounded-xl px-2 py-3 transition-colors hover:bg-white/70"
          >
            <div className="h-9 w-9 shrink-0 overflow-hidden rounded-full">
              <stock.Logo />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                {stock.ticker}
              </p>
              <p className="truncate text-xs text-[var(--color-text-muted)]">{stock.name}</p>
            </div>
            <svg width="44" height="24" viewBox="0 0 40 24" className="shrink-0" aria-hidden="true">
              <polyline
                points={stock.points}
                fill="none"
                stroke={stock.up ? 'var(--color-profit)' : 'var(--color-loss)'}
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <div className="shrink-0 text-right">
              <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                {stock.price}
              </p>
              <p
                className={`text-xs font-medium ${stock.up ? 'text-[var(--color-profit)]' : 'text-[var(--color-loss)]'}`}
              >
                {stock.change}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function AlternativeDataVisual() {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-8 shadow-xl shadow-slate-900/5">
      <svg
        className="absolute top-6 left-6 h-16 w-16 text-[var(--color-border)]"
        viewBox="0 0 64 64"
        fill="none"
        aria-hidden="true"
      >
        <circle cx="12" cy="10" r="2.5" fill="currentColor" />
        <circle cx="28" cy="10" r="2.5" fill="currentColor" />
        <circle cx="20" cy="24" r="2.5" fill="currentColor" />
        <path
          d="M12 12.5V18M28 12.5V18M12 20L20 22M28 20L20 22"
          stroke="currentColor"
          strokeWidth="1.2"
        />
      </svg>

      <div className="relative flex min-h-[260px] items-center justify-center">
        <svg viewBox="0 0 160 160" className="h-40 w-40" aria-hidden="true">
          <circle cx="80" cy="80" r="70" fill="var(--color-accent-light)" />
          <path d="M80 80 L80 10 A70 70 0 0 1 143 110 Z" fill="var(--color-accent-blue)" />
          <path d="M80 80 L143 110 A70 70 0 0 1 80 150 Z" fill="#60A5FA" />
          <circle cx="80" cy="80" r="30" fill="var(--color-card-bg)" />
        </svg>

        <svg
          className="absolute top-2 right-4 h-16 w-24 text-[var(--color-accent-blue)]"
          viewBox="0 0 96 64"
          fill="none"
          aria-hidden="true"
        >
          <path
            d="M2 50 L22 30 L38 40 L60 14 L94 2"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M78 2 H94 V18"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        <motion.div
          initial={{ opacity: 0, y: -8 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          whileHover={{ scale: 1.05 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="absolute top-4 -right-2 flex items-center gap-2 rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 shadow-lg shadow-slate-900/10 sm:right-2"
        >
          <div className="h-7 w-7 shrink-0 overflow-hidden rounded-full">
            <MicrosoftMark />
          </div>
          <div>
            <p className="text-[11px] font-medium text-[var(--color-text-secondary)]">MSFT</p>
            <p className="text-xs font-semibold text-[var(--color-profit)]">$365.51 ▲0.59%</p>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          whileHover={{ scale: 1.05 }}
          transition={{ duration: 0.5, delay: 0.35 }}
          className="absolute bottom-2 -left-2 flex items-center gap-2 rounded-xl border border-[var(--color-border)] bg-white px-3 py-2 shadow-lg shadow-slate-900/10 sm:left-2"
        >
          <div className="h-7 w-7 shrink-0 overflow-hidden rounded-full">
            <AppleMark />
          </div>
          <div>
            <p className="text-[11px] font-medium text-[var(--color-text-secondary)]">AAPL</p>
            <p className="text-xs font-semibold text-[var(--color-loss)]">$149.62 ▼0.19%</p>
          </div>
        </motion.div>
      </div>
    </div>
  )
}

function PriceBreakdownVisual() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-page-bg)] shadow-xl shadow-slate-900/5">
      <div className="flex items-center gap-2 border-b border-[var(--color-border)] px-4 py-3">
        <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
        <span className="h-2.5 w-2.5 rounded-full bg-[var(--color-border)]" />
        <span className="ml-3 text-xs font-medium text-[var(--color-text-muted)]">
          AAPL · Apple Inc.
        </span>
      </div>
      <div className="p-6">
        <div className="flex items-baseline gap-3">
          <p className="text-3xl font-semibold text-[var(--color-text-primary)]">$214.36</p>
          <p className="text-sm font-medium text-[var(--color-loss)]">−1.12 (−0.52%)</p>
        </div>
        <svg viewBox="0 0 168 70" className="mt-6 h-44 w-full" aria-hidden="true">
          <line x1="0" y1="17.5" x2="168" y2="17.5" stroke="var(--color-border)" strokeWidth="1" />
          <line x1="0" y1="35" x2="168" y2="35" stroke="var(--color-border)" strokeWidth="1" />
          <line x1="0" y1="52.5" x2="168" y2="52.5" stroke="var(--color-border)" strokeWidth="1" />
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
    </div>
  )
}

function ResearchAgentVisual() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6 shadow-xl shadow-slate-900/5">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
          AI Research Agent
        </h3>
        <span className="rounded-full bg-[var(--color-accent-light)] px-2.5 py-1 text-xs font-semibold text-[var(--color-accent-blue)]">
          87% confidence
        </span>
      </div>
      <p className="mt-4 text-sm leading-relaxed text-[var(--color-text-secondary)]">
        Revenue growth remains steady on services expansion, though hardware margins narrowed this
        quarter. Analyst sentiment is mixed heading into the next earnings call.
      </p>
      <div className="mt-5 space-y-2 border-t border-[var(--color-border)] pt-5">
        {['Parsed 10-K filing', 'Scanned earnings call transcript', 'Reviewed 30d of news'].map(
          (step) => (
            <div
              key={step}
              className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-accent-blue)]" />
              {step}
            </div>
          ),
        )}
      </div>
      <div className="mt-5 flex flex-wrap gap-2 border-t border-[var(--color-border)] pt-5">
        <span className="rounded-md border border-[var(--color-border)] bg-[var(--color-page-bg)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
          10-K filing
        </span>
        <span className="rounded-md border border-[var(--color-border)] bg-[var(--color-page-bg)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
          Earnings call
        </span>
        <span className="rounded-md border border-[var(--color-border)] bg-[var(--color-page-bg)] px-2 py-1 text-xs text-[var(--color-text-muted)]">
          News (30d)
        </span>
      </div>
    </div>
  )
}

function OrderTicketVisual() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-page-bg)] p-6 shadow-xl shadow-slate-900/5">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-[var(--color-text-primary)]">NVDA · Order</p>
        <span className="rounded-full bg-[var(--color-accent-light)] px-2.5 py-1 text-xs font-semibold text-[var(--color-accent-blue)]">
          Stop-Limit
        </span>
      </div>
      <div className="mt-5 grid grid-cols-2 gap-4">
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
          <p className="text-xs text-[var(--color-text-muted)]">Stop price</p>
          <p className="mt-1 text-lg font-semibold text-[var(--color-text-primary)]">$132.00</p>
        </div>
        <div className="rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4">
          <p className="text-xs text-[var(--color-text-muted)]">Limit price</p>
          <p className="mt-1 text-lg font-semibold text-[var(--color-text-primary)]">$130.50</p>
        </div>
      </div>
      <div className="mt-5 flex gap-3">
        <div className="flex-1 cursor-pointer rounded-md bg-[var(--color-accent-blue)] py-2.5 text-center text-sm font-semibold text-white transition-transform duration-150 hover:bg-blue-700 active:scale-95">
          Buy
        </div>
        <div className="flex-1 cursor-pointer rounded-md border border-[var(--color-border)] py-2.5 text-center text-sm font-semibold text-[var(--color-text-secondary)] transition-colors duration-150 hover:bg-[var(--color-sidebar-hover)] active:scale-95">
          Sell
        </div>
      </div>
      <div className="mt-5 border-t border-[var(--color-border)] pt-5 text-xs text-[var(--color-text-muted)]">
        Filled through the Kafka-backed matching engine the instant your trigger condition is met.
      </div>
    </div>
  )
}

function PortfolioVisual() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-6 shadow-xl shadow-slate-900/5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-[var(--color-text-muted)]">Portfolio value</p>
          <p className="mt-1 text-2xl font-semibold text-[var(--color-text-primary)]">$14,902</p>
        </div>
        <div className="rounded-full bg-[var(--color-accent-light)] px-3 py-1 text-sm font-semibold text-[var(--color-accent-blue)]">
          +49.0%
        </div>
      </div>
      <div className="mt-6 flex h-32 items-end gap-2">
        {[40, 55, 48, 62, 58, 74, 68, 82, 90, 76, 94, 100].map((h, i) => (
          <div
            key={i}
            className="flex-1 origin-bottom rounded-t-sm bg-[var(--color-accent-blue)] transition-transform duration-200 ease-out hover:scale-y-105"
            style={{ height: `${h}%`, opacity: 0.35 + (i / 12) * 0.65 }}
          />
        ))}
      </div>
      <div className="mt-6 grid grid-cols-2 gap-4 border-t border-[var(--color-border)] pt-5">
        <div>
          <p className="text-xs text-[var(--color-text-muted)]">Today&apos;s gain</p>
          <p className="mt-1 text-sm font-semibold text-[var(--color-accent-blue)]">+$220.42</p>
        </div>
        <div>
          <p className="text-xs text-[var(--color-text-muted)]">Overall gain</p>
          <p className="mt-1 text-sm font-semibold text-[var(--color-accent-blue)]">+$5,200.31</p>
        </div>
      </div>
    </div>
  )
}

type Row = {
  eyebrow: string
  title: string
  description: string
  bullets: string[]
  visual: ReactNode
  reverse?: boolean
}

const rows: Row[] = [
  {
    eyebrow: 'Market signals',
    title: 'What stocks are trending?',
    description:
      'Find investment opportunities and stock picks using our range of data points, including job postings, website traffic, customer sentiment, app downloads, and other critical indicators.',
    bullets: [
      'Live top-mover leaderboard',
      'Mini price sparklines at a glance',
      'Updated in real time',
    ],
    visual: <TopStocksVisual />,
    reverse: true,
  },
  {
    eyebrow: 'Alternative data',
    title: 'Stock picks powered by alternative data',
    description:
      'By tracking and comparing this data over time, as well as benchmarking it against industry peers, our AI research agent surfaces informed opportunities before they hit the mainstream.',
    bullets: [
      'Cross-referenced against industry peers',
      'Historical trend benchmarking',
      'Surfaced by the AI research agent',
    ],
    visual: <AlternativeDataVisual />,
  },
  {
    eyebrow: 'Deep research',
    title: 'Every stock, fully broken down',
    description:
      'Price action, technical indicators, and AI-generated research come together in a single view, so you never have to juggle five tabs to understand a company.',
    bullets: [
      'Live candlestick charts',
      'RSI, MACD, and volume indicators',
      '52-week range at a glance',
    ],
    visual: <PriceBreakdownVisual />,
  },
  {
    eyebrow: 'AI research agent',
    title: 'An AI analyst on every ticker',
    description:
      'A 5-step agent with 9 tools reads filings, earnings calls, and recent news, then streams its reasoning trace in real time so you can see exactly how it got there.',
    bullets: [
      '9 research tools, 5-step reasoning',
      'Sources every claim it makes',
      'Streamed live over SSE',
    ],
    visual: <ResearchAgentVisual />,
    reverse: true,
  },
  {
    eyebrow: 'Order matching engine',
    title: 'Trade every order type, risk-free',
    description:
      'Market, limit, stop-loss, and stop-limit orders are matched against live prices through a Kafka-backed engine — the same mechanics as a real exchange, without the risk.',
    bullets: [
      'Market, limit, stop-loss, stop-limit',
      'Kafka-backed matching engine',
      'Instant fill notifications',
    ],
    visual: <OrderTicketVisual />,
  },
  {
    eyebrow: 'Portfolio tracking',
    title: 'Track your portfolio like a pro',
    description:
      'Watch your simulated $10,000 balance grow, replay your portfolio at any past date, and benchmark performance against the market — all in one dashboard.',
    bullets: [
      'Portfolio time machine',
      'Daily and overall gain tracking',
      'Audit log of every action',
    ],
    visual: <PortfolioVisual />,
    reverse: true,
  },
]

function FeatureRow({ row }: { row: Row }) {
  return (
    <div
      className={`grid items-center gap-12 lg:grid-cols-2 lg:gap-20 ${
        row.reverse ? 'lg:[&>*:first-child]:order-2' : ''
      }`}
    >
      <motion.div
        initial={{ opacity: 0, x: row.reverse ? 24 : -24 }}
        whileInView={{ opacity: 1, x: 0 }}
        viewport={{ once: true, margin: '-100px' }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      >
        <p className="text-sm font-semibold tracking-wide text-[var(--color-accent-blue)] uppercase">
          {row.eyebrow}
        </p>
        <h3 className="mt-3 text-3xl font-semibold tracking-tight text-[var(--color-text-primary)] sm:text-4xl">
          {row.title}
        </h3>
        <p className="mt-4 max-w-md text-lg text-[var(--color-text-secondary)]">
          {row.description}
        </p>
        <ul className="mt-6 space-y-3">
          {row.bullets.map((bullet) => (
            <li
              key={bullet}
              className="group flex items-center gap-3 text-sm text-[var(--color-text-primary)] transition-transform duration-150 hover:translate-x-1"
            >
              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-[var(--color-accent-blue)] transition-transform duration-150 group-hover:scale-110">
                <svg width="10" height="8" viewBox="0 0 10 8" fill="none" aria-hidden="true">
                  <path
                    d="M1 4L3.5 6.5L9 1"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </span>
              {bullet}
            </li>
          ))}
        </ul>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, x: row.reverse ? -24 : 24 }}
        whileInView={{ opacity: 1, x: 0 }}
        viewport={{ once: true, margin: '-100px' }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        whileHover={{ y: -4 }}
      >
        {row.visual}
      </motion.div>
    </div>
  )
}

export default function FeatureSections() {
  return (
    <section id="features" className="mx-auto max-w-7xl px-8 py-24 sm:px-12 sm:py-32 lg:px-20">
      <div className="max-w-2xl">
        <h2 className="text-4xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          Built for serious research
        </h2>
        <p className="mt-4 text-lg text-[var(--color-text-secondary)]">
          Every feature is designed around a single question: does this help you make a better
          decision.
        </p>
      </div>

      <div className="mt-20 space-y-24 sm:mt-24 sm:space-y-32">
        {rows.map((row) => (
          <FeatureRow key={row.title} row={row} />
        ))}
      </div>
    </section>
  )
}
