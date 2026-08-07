import Link from 'next/link'
import Logo from '@/components/shared/Logo'

const columns = [
  {
    heading: 'Product',
    links: [
      { label: 'Features', href: '#features' },
      { label: 'FAQ', href: '#faq' },
      { label: 'Docs', href: '#docs' },
    ],
  },
  {
    heading: 'Company',
    links: [
      { label: 'About', href: '#' },
      { label: 'Contact', href: '#' },
    ],
  },
  {
    heading: 'Legal',
    links: [
      { label: 'Privacy', href: '#' },
      { label: 'Terms', href: '#' },
      { label: 'Data disclaimer', href: '#' },
    ],
  },
]

export default function Footer() {
  return (
    <footer className="bg-[var(--color-text-primary)]">
      <div className="mx-auto max-w-7xl px-8 py-16 sm:px-12 lg:px-20">
        <div className="flex flex-col gap-12 sm:flex-row sm:justify-between">
          <div className="max-w-xs">
            <div className="flex items-center gap-2">
              <Logo variant="mono" size={22} className="text-white" />
              <span className="text-lg font-semibold tracking-tight text-white">QuantEdge</span>
            </div>
            <p className="mt-4 text-sm text-white/60">
              AI-powered stock research and simulated trading, built on real market data.
            </p>
            <a
              href="#"
              className="group mt-6 inline-flex items-center gap-2 text-sm font-medium text-white/70 transition-colors hover:text-white"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="currentColor"
                aria-hidden="true"
                className="transition-transform duration-200 group-hover:scale-110"
              >
                <path d="M16 3.04a6.6 6.6 0 0 1-1.89.52 3.3 3.3 0 0 0 1.45-1.82c-.63.38-1.34.65-2.09.8A3.28 3.28 0 0 0 7.86 5.6 9.3 9.3 0 0 1 1.11 2.2a3.28 3.28 0 0 0 1.02 4.38A3.25 3.25 0 0 1 .64 6.2v.04a3.28 3.28 0 0 0 2.63 3.22 3.3 3.3 0 0 1-1.48.06 3.29 3.29 0 0 0 3.07 2.28A6.59 6.59 0 0 1 0 13.14a9.28 9.28 0 0 0 5.03 1.47c6.04 0 9.34-5 9.34-9.34l-.01-.42A6.7 6.7 0 0 0 16 3.04z" />
              </svg>
              Follow us on Twitter
            </a>
          </div>

          <div className="grid grid-cols-3 gap-8 sm:gap-16">
            {columns.map((column) => (
              <div key={column.heading}>
                <p className="text-sm font-semibold text-white">{column.heading}</p>
                <ul className="mt-4 space-y-3">
                  {column.links.map((link) => (
                    <li key={link.label}>
                      <Link
                        href={link.href}
                        className="group relative inline-block text-sm text-white/60 transition-colors hover:text-white"
                      >
                        {link.label}
                        <span className="absolute -bottom-0.5 left-0 h-px w-full origin-left scale-x-0 bg-white transition-transform duration-200 ease-out group-hover:scale-x-100" />
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-14 flex flex-col gap-4 border-t border-white/10 pt-8 text-sm text-white/40 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} QuantEdge. All rights reserved.</p>
          <p>All simulated trades. No real capital at risk.</p>
        </div>
      </div>
    </footer>
  )
}
