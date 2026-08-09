'use client'

import ChatBody from '@/components/chat/ChatBody'

export default function ChatPage() {
  return (
    <div className="flex h-[calc(100vh-9rem)] flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">Chat</h1>
        <p className="text-sm text-[var(--color-text-secondary)]">
          Ask about your portfolio, watchlist, orders, or any stock — the assistant can look things
          up and stage trades for you to confirm.
        </p>
      </div>

      <ChatBody />
    </div>
  )
}
