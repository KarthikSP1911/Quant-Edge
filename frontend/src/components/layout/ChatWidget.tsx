'use client'

import { useRef, useState } from 'react'
import { usePathname } from 'next/navigation'
import ChatBody from '@/components/chat/ChatBody'
import { useOutsideClick } from '@/hooks/useOutsideClick'
import { useClearChatHistory } from '@/hooks/useChat'

export default function ChatWidget() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const [hasOpened, setHasOpened] = useState(false)
  const panelRef = useRef<HTMLDivElement>(null)
  const clearHistory = useClearChatHistory()

  useOutsideClick(panelRef, open, () => setOpen(false))

  if (pathname === '/chat') return null

  const toggleOpen = () => {
    setOpen((o) => !o)
    setHasOpened(true)
  }

  return (
    <div
      ref={panelRef}
      className="pointer-events-none fixed bottom-6 right-6 z-[60] flex flex-col items-end gap-3"
    >
      <div
        aria-hidden={!open}
        className={`flex h-[550px] max-h-[calc(100vh-6rem)] w-[400px] max-w-[calc(100vw-3rem)] origin-bottom-right flex-col gap-4 rounded-xl border border-[var(--color-border)] bg-[var(--color-card-bg)] p-4 shadow-2xl transition-all duration-200 ease-out ${
          open
            ? 'pointer-events-auto scale-100 opacity-100'
            : 'pointer-events-none scale-95 opacity-0'
        }`}
      >
        <div className="flex shrink-0 items-center justify-between">
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">
            QuantEdge Assistant
          </h2>
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => clearHistory.mutate()}
              disabled={clearHistory.isPending}
              aria-label="Clear chat"
              title="Clear chat"
              className="flex h-7 w-7 items-center justify-center rounded-md text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-sidebar-hover)] disabled:cursor-not-allowed disabled:opacity-50"
            >
              <svg
                width="15"
                height="15"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2L4 6h16Z" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => setOpen(false)}
              aria-label="Close chat"
              className="flex h-7 w-7 items-center justify-center rounded-md text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-sidebar-hover)]"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              >
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        {hasOpened && <ChatBody />}
      </div>

      <button
        type="button"
        onClick={toggleOpen}
        aria-expanded={open}
        aria-label="Toggle chat assistant"
        className="pointer-events-auto flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-[var(--color-accent-blue)] text-white shadow-lg transition-transform hover:scale-105 active:scale-95"
      >
        {open ? (
          <svg
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          >
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        ) : (
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
          </svg>
        )}
      </button>
    </div>
  )
}
