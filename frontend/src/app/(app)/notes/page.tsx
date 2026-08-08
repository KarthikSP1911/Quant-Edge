'use client'

import { useState } from 'react'
import { useResearchNotes } from '@/hooks/useResearchNotes'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import Link from 'next/link'

export default function NotesPage() {
  const { data: notes, isPending, isError } = useResearchNotes()
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null)

  if (isPending) {
    return <div className="p-4 text-[var(--color-text-secondary)]">Loading notes...</div>
  }

  if (isError) {
    return <div className="p-4 text-[var(--color-loss)]">Failed to load notes</div>
  }

  const selectedNote = notes?.find((n) => n.id === selectedNoteId)

  return (
    <div className="flex h-[calc(100vh-4rem)] gap-4">
      {/* Sidebar: Note List */}
      <div className="flex w-1/3 flex-col overflow-y-auto border-r border-[var(--color-border)] pr-4">
        <h1 className="mb-4 text-xl font-bold text-[var(--color-text-primary)]">Research Notes</h1>

        {!notes || notes.length === 0 ? (
          <p className="text-sm text-[var(--color-text-secondary)]">No notes saved yet.</p>
        ) : (
          <div className="flex flex-col gap-2">
            {notes.map((note) => (
              <button
                key={note.id}
                onClick={() => setSelectedNoteId(note.id)}
                className={`flex flex-col items-start rounded-lg border p-3 text-left transition-colors ${
                  selectedNoteId === note.id
                    ? 'border-[var(--color-accent-blue)] bg-[var(--color-accent-blue)]/10'
                    : 'border-[var(--color-border)] hover:bg-[var(--color-sidebar-hover)]'
                }`}
              >
                <div className="flex w-full items-center justify-between">
                  <span className="font-semibold text-[var(--color-text-primary)]">
                    {note.company.symbol}
                  </span>
                  <span className="text-xs text-[var(--color-text-secondary)]">
                    {new Date(note.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <h3 className="mt-1 line-clamp-1 text-sm font-medium text-[var(--color-text-primary)]">
                  {note.title}
                </h3>
                <span className="mt-2 rounded bg-[var(--color-bg-secondary)] px-2 py-0.5 text-[10px] font-medium uppercase text-[var(--color-text-secondary)]">
                  By {note.generatedBy}
                </span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Main Content: Note Reader */}
      <div className="flex-1 overflow-y-auto pl-4">
        {selectedNote ? (
          <div className="prose prose-invert max-w-none pb-12">
            <div className="mb-8 border-b border-[var(--color-border)] pb-4">
              <h1 className="mb-2 text-3xl font-bold">{selectedNote.title}</h1>
              <div className="flex items-center gap-4 text-sm text-[var(--color-text-secondary)]">
                <Link
                  href={`/stocks/${selectedNote.company.symbol}`}
                  className="text-[var(--color-accent-blue)] hover:underline"
                >
                  {selectedNote.company.symbol} - {selectedNote.company.name}
                </Link>
                <span>•</span>
                <span>{new Date(selectedNote.createdAt).toLocaleString()}</span>
              </div>
            </div>

            <div className="text-[var(--color-text-primary)]">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{selectedNote.content}</ReactMarkdown>
            </div>
          </div>
        ) : (
          <div className="flex h-full items-center justify-center text-[var(--color-text-secondary)]">
            Select a note to read
          </div>
        )}
      </div>
    </div>
  )
}
