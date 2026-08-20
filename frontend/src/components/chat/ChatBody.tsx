'use client'

import { useEffect, useRef, useState } from 'react'
import ChatMarkdown from '@/components/chat/ChatMarkdown'
import PendingOrderCard from '@/components/chat/PendingOrderCard'
import {
  useCancelPendingOrder,
  useChatHistory,
  useConfirmPendingOrder,
  usePendingOrder,
  useSendChatMessage,
} from '@/hooks/useChat'

function ChatSkeleton() {
  return (
    <div className="flex flex-1 flex-col gap-4 overflow-hidden">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className={`flex ${i % 2 === 0 ? 'justify-end' : 'justify-start'}`}>
          <div className="h-10 w-2/3 max-w-md animate-pulse rounded-lg bg-[var(--color-sidebar-hover)]" />
        </div>
      ))}
    </div>
  )
}

function ChatError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-[var(--color-loss)]/40 bg-red-50/40 py-16 text-center">
      <p className="font-medium text-[var(--color-text-primary)]">
        Couldn&apos;t load chat history
      </p>
      <p className="text-sm text-[var(--color-text-secondary)]">
        Something went wrong. Please try again.
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
      >
        Retry
      </button>
    </div>
  )
}

export default function ChatBody() {
  const { data: rawMessages, isPending, isError, refetch } = useChatHistory()
  const messages = rawMessages
    ? Array.from(new Map(rawMessages.map((m) => [m.id, m])).values())
    : rawMessages
  const sendMessage = useSendChatMessage()
  const { data: pendingOrder } = usePendingOrder()
  const confirmOrder = useConfirmPendingOrder()
  const cancelOrder = useCancelPendingOrder()
  const [input, setInput] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, sendMessage.isPending, pendingOrder])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = input.trim()
    if (!trimmed || sendMessage.isPending) return
    setInput('')
    sendMessage.mutate(trimmed)
  }

  return (
    <>
      {isPending && <ChatSkeleton />}
      {isError && !isPending && <ChatError onRetry={() => void refetch()} />}

      {messages && !isPending && !isError && (
        <>
          <div ref={scrollRef} className="flex flex-1 flex-col gap-4 overflow-y-auto pr-1">
            {messages.length === 0 && (
              <div className="flex flex-1 flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-[var(--color-border)] text-center">
                <p className="text-sm font-medium text-[var(--color-text-primary)]">
                  No messages yet
                </p>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Say hello, or ask something like &quot;What&apos;s my portfolio balance?&quot;
                </p>
              </div>
            )}

            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'USER' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-lg rounded-lg px-4 py-2 text-sm ${
                    message.role === 'USER'
                      ? 'bg-[var(--color-accent-blue)] text-white'
                      : 'border border-[var(--color-border)] bg-[var(--color-card-bg)] text-[var(--color-text-primary)]'
                  }`}
                >
                  {message.role === 'USER' ? (
                    <span className="whitespace-pre-wrap">{message.content}</span>
                  ) : (
                    <ChatMarkdown content={message.content} />
                  )}
                </div>
              </div>
            ))}

            {sendMessage.isPending && (
              <div className="flex justify-start">
                <div className="flex items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)] px-4 py-3">
                  <div className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]" />
                  <div
                    className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]"
                    style={{ animationDelay: '150ms' }}
                  />
                  <div
                    className="h-1.5 w-1.5 animate-bounce rounded-full bg-[var(--color-text-secondary)]"
                    style={{ animationDelay: '300ms' }}
                  />
                </div>
              </div>
            )}

            {pendingOrder && !sendMessage.isPending && (
              <PendingOrderCard
                order={pendingOrder}
                onAccept={() => confirmOrder.mutate()}
                onReject={() => cancelOrder.mutate()}
                isSubmitting={confirmOrder.isPending || cancelOrder.isPending}
              />
            )}
          </div>

          <form onSubmit={handleSubmit} className="flex shrink-0 items-center gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask about your portfolio, a stock, or place a trade..."
              disabled={sendMessage.isPending}
              className="flex-1 rounded-md border border-[var(--color-border)] bg-white px-4 py-2 text-sm text-[var(--color-text-primary)] placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-accent-blue)] focus:outline-none disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={!input.trim() || sendMessage.isPending}
              className="rounded-md bg-[var(--color-accent-blue)] px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
            >
              Send
            </button>
          </form>
        </>
      )}
    </>
  )
}
