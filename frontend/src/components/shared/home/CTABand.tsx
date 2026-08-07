export default function CTABand() {
  return (
    <section className="border-y border-[var(--color-border)] bg-[var(--color-text-primary)]">
      <div className="mx-auto flex max-w-7xl flex-col items-center gap-6 px-6 py-16 text-center sm:flex-row sm:justify-between sm:text-left">
        <h2 className="text-2xl font-semibold tracking-tight text-white">
          Start researching and trading, risk-free.
        </h2>
        <a
          href="/signup"
          className="shrink-0 rounded-md bg-[var(--color-accent-blue)] px-6 py-3 text-sm font-medium text-white transition-colors hover:bg-blue-700"
        >
          Get Started
        </a>
      </div>
    </section>
  )
}
