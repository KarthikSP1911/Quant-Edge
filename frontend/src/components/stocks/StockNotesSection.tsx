'use client'

import Link from 'next/link'
import { useResearchNotes } from '@/hooks/useResearchNotes'

export default function StockNotesSection({ symbol }: { symbol: string }) {
  const { data: notes, isPending, isError } = useResearchNotes(symbol)

  return (
    <section className="rounded-lg border border-[var(--color-border)] p-4">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Research notes</h2>
        <Link
          href="/research?tab=notes"
          className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
        >
          View all notes
        </Link>
      </div>

      {isPending && (
        <div className="h-16 animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
      )}
      {isError && (
        <p className="text-sm text-[var(--color-text-secondary)]">Couldn&apos;t load notes.</p>
      )}
      {!isPending &&
        !isError &&
        (!notes || notes.length === 0 ? (
          <p className="text-sm text-[var(--color-text-secondary)]">
            No research notes for {symbol} yet. Run AI research to generate one.
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {notes.slice(0, 3).map((note) => (
              <li key={note.id}>
                <Link
                  href="/research?tab=notes"
                  className="block rounded-lg border border-[var(--color-border)] p-3 transition-colors hover:bg-[var(--color-sidebar-hover)]"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="line-clamp-1 text-sm font-medium text-[var(--color-text-primary)]">
                      {note.title}
                    </span>
                    <span className="shrink-0 text-xs text-[var(--color-text-secondary)]">
                      {new Date(note.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        ))}
    </section>
  )
}
