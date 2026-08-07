'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { useState } from 'react'

const faqs = [
  {
    question: 'Is QuantEdge trading with real money?',
    answer:
      'No. Every account starts with a simulated $10,000 balance. Market data is real and live, but all trades are simulated — nothing ever touches real capital.',
  },
  {
    question: 'What order types are supported?',
    answer:
      'Market, limit, stop-loss, and stop-limit orders, all matched against live prices through a Kafka-backed order matching engine, the same mechanics as a real exchange.',
  },
  {
    question: 'How does the AI research agent work?',
    answer:
      'A 5-step agent with 9 tools reads filings, earnings calls, and recent news for a company, then streams its reasoning trace to you in real time over SSE so you can see exactly how it reached its conclusion.',
  },
  {
    question: 'Can I see how my portfolio performed in the past?',
    answer:
      'Yes. The portfolio time machine lets you replay your holdings at any past date to see exactly what they were worth and what drove the change.',
  },
  {
    question: 'Is my activity tracked anywhere?',
    answer:
      'Every trade, order change, and account action is recorded automatically in an audit log via AOP-based auditing, so you always have a full history to review or export.',
  },
]

function FaqItem({
  question,
  answer,
  isOpen,
  onToggle,
}: {
  question: string
  answer: string
  isOpen: boolean
  onToggle: () => void
}) {
  return (
    <div className="border-b border-[var(--color-border)] py-6">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={isOpen}
        className="group flex w-full items-center justify-between gap-6 text-left"
      >
        <span className="text-base font-medium text-[var(--color-text-primary)] transition-colors duration-150 group-hover:text-[var(--color-accent-blue)] sm:text-lg">
          {question}
        </span>
        <motion.span
          animate={{ rotate: isOpen ? 45 : 0 }}
          whileHover={{ scale: 1.1 }}
          transition={{ duration: 0.2 }}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-light)] text-[var(--color-accent-blue)]"
          aria-hidden="true"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path
              d="M7 1V13M1 7H13"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
            />
          </svg>
        </motion.span>
      </button>
      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            className="overflow-hidden"
          >
            <p className="pt-4 text-sm leading-relaxed text-[var(--color-text-secondary)] sm:text-base">
              {answer}
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default function FAQ() {
  const [openIndex, setOpenIndex] = useState<number | null>(0)

  return (
    <section id="faq" className="mx-auto max-w-4xl px-8 py-24 sm:px-12 sm:py-32 lg:px-20">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-80px' }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="text-center"
      >
        <h2 className="text-4xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          Frequently asked questions
        </h2>
        <p className="mt-4 text-lg text-[var(--color-text-secondary)]">
          Everything you need to know before you get started.
        </p>
      </motion.div>

      <div className="mt-14">
        {faqs.map((faq, i) => (
          <FaqItem
            key={faq.question}
            question={faq.question}
            answer={faq.answer}
            isOpen={openIndex === i}
            onToggle={() => setOpenIndex((current) => (current === i ? null : i))}
          />
        ))}
      </div>
    </section>
  )
}
