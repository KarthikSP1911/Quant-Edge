'use client'

import { motion } from 'framer-motion'

const testimonials = [
  {
    quote:
      'The order matching engine feels like a real exchange. I finally understand how stop-limit orders actually fill instead of just reading about it.',
    name: 'Priya Nair',
    role: 'CS student, backtesting strategies',
  },
  {
    quote:
      'The AI research agent streaming its reasoning trace is the feature I show everyone. It reads filings faster than I ever could and shows its work.',
    name: 'Daniel Cho',
    role: 'Self-taught investor',
  },
  {
    quote:
      'Portfolio time machine let me see exactly why a bad week happened instead of guessing. It changed how I review my own trades.',
    name: 'Marcus Webb',
    role: 'Part-time swing trader',
  },
]

function Stars() {
  return (
    <div className="flex gap-1 text-[var(--color-accent-blue)]" aria-hidden="true">
      {Array.from({ length: 5 }).map((_, i) => (
        <svg key={i} width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
          <path d="M8 0.5l2.163 4.382 4.837.703-3.5 3.412.826 4.815L8 11.5l-4.326 2.312.826-4.815-3.5-3.412 4.837-.703L8 0.5z" />
        </svg>
      ))}
    </div>
  )
}

export default function Testimonials() {
  return (
    <section className="bg-[var(--color-card-bg)] py-24 sm:py-32">
      <div className="mx-auto max-w-7xl px-8 sm:px-12 lg:px-20">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="mx-auto max-w-2xl text-center"
        >
          <h2 className="text-4xl font-semibold tracking-tight text-[var(--color-text-primary)]">
            What early users say about QuantEdge
          </h2>
          <p className="mt-4 text-lg text-[var(--color-text-secondary)]">
            Built to help you research and trade with confidence, before real capital is on the
            line.
          </p>
        </motion.div>

        <div className="mt-16 grid grid-cols-1 gap-6 md:grid-cols-3">
          {testimonials.map((t, i) => (
            <motion.figure
              key={t.name}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-80px' }}
              transition={{ duration: 0.5, delay: i * 0.1, ease: [0.22, 1, 0.36, 1] }}
              whileHover={{ y: -6, scale: 1.015 }}
              className="group flex flex-col rounded-2xl border border-[var(--color-border)] bg-[var(--color-page-bg)] p-7 shadow-sm transition-shadow duration-200 hover:border-[var(--color-accent-blue)]/30 hover:shadow-lg hover:shadow-slate-900/5"
            >
              <Stars />
              <blockquote className="mt-4 flex-1 text-sm leading-relaxed text-[var(--color-text-primary)]">
                &ldquo;{t.quote}&rdquo;
              </blockquote>
              <figcaption className="mt-6 flex items-center gap-3 border-t border-[var(--color-border)] pt-5">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-xs font-semibold text-[var(--color-accent-blue)] transition-transform duration-200 group-hover:scale-110">
                  {t.name
                    .split(' ')
                    .map((n) => n[0])
                    .join('')}
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)]">{t.name}</p>
                  <p className="text-xs text-[var(--color-text-muted)]">{t.role}</p>
                </div>
              </figcaption>
            </motion.figure>
          ))}
        </div>
      </div>
    </section>
  )
}
