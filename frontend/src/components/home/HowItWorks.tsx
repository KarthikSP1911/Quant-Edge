const steps = [
  {
    number: '01',
    title: 'Create an account',
    description: 'Sign up and receive a simulated $10,000 balance to trade with immediately.',
  },
  {
    number: '02',
    title: 'Research a company',
    description:
      'Pull live prices, indicators, and filings, or ask the AI agent to summarize a company for you.',
  },
  {
    number: '03',
    title: 'Place an order',
    description:
      'Submit market, limit, stop-loss, or stop-limit orders and watch them fill in real time.',
  },
]

export default function HowItWorks() {
  return (
    <section className="mx-auto max-w-7xl px-6 py-20">
      <h2 className="text-3xl font-semibold tracking-tight text-[var(--color-text-primary)]">
        How it works
      </h2>

      <div className="mt-12 grid grid-cols-1 gap-10 sm:grid-cols-3 sm:gap-8">
        {steps.map((step) => (
          <div key={step.number} className="border-t border-[var(--color-border)] pt-6">
            <p className="text-sm font-semibold text-[var(--color-accent-blue)]">{step.number}</p>
            <h3 className="mt-3 text-lg font-semibold text-[var(--color-text-primary)]">
              {step.title}
            </h3>
            <p className="mt-2 text-sm text-[var(--color-text-secondary)]">{step.description}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
