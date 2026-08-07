import type { NewsItem } from '@/types/stock'

function formatRelativeTime(iso: string): string {
  const hours = Math.round((Date.now() - new Date(iso).getTime()) / (60 * 60 * 1000))
  if (hours < 1) return 'just now'
  if (hours < 24) return `${hours}h ago`
  return `${Math.round(hours / 24)}d ago`
}

export default function NewsList({ news }: { news: NewsItem[] }) {
  if (news.length === 0) {
    return <p className="text-sm text-[var(--color-text-secondary)]">No recent news.</p>
  }

  return (
    <ul className="flex flex-col divide-y divide-[var(--color-border)]">
      {news.map((item) => (
        <li key={item.id} className="py-3 first:pt-0 last:pb-0">
          <a
            href={item.url}
            className="text-sm font-medium text-[var(--color-text-primary)] hover:underline"
          >
            {item.headline}
          </a>
          <div className="mt-1 text-xs text-[var(--color-text-muted)]">
            {item.source} &middot; {formatRelativeTime(item.publishedAt)}
          </div>
        </li>
      ))}
    </ul>
  )
}
