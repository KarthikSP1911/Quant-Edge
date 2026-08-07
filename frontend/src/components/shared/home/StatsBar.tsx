'use client'

import { motion } from 'framer-motion'

const stats = [
  { value: '$10,000', label: 'Starting balance', delta: null },
  { value: 'Real-time', label: 'Market data', delta: null },
  { value: '4', label: 'Order types', delta: null },
  { value: '11', label: 'Tracked data tables', delta: null },
]

export default function StatsBar() {
  return (
    <section className="bg-[var(--color-text-primary)]">
      <div className="mx-auto grid max-w-7xl grid-cols-2 divide-x divide-white/10 px-8 sm:grid-cols-4 sm:px-12 lg:px-20">
        {stats.map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: '-80px' }}
            transition={{ duration: 0.5, delay: i * 0.08, ease: [0.22, 1, 0.36, 1] }}
            whileHover={{ y: -2 }}
            className="cursor-default px-4 py-8 text-center transition-colors sm:text-left"
          >
            <p className="text-2xl font-semibold text-white">{stat.value}</p>
            <p className="mt-1 text-sm text-white/60">{stat.label}</p>
          </motion.div>
        ))}
      </div>
    </section>
  )
}
