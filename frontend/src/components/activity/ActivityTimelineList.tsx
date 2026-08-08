import type { AuditLogEntry } from '@/types/auditLog'
import { formatAuditAction, summarizeAuditDetails } from '@/lib/utils/auditLog'

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function ActivityRow({ entry }: { entry: AuditLogEntry }) {
  const details = summarizeAuditDetails(entry.details)

  return (
    <li className="flex flex-col gap-2 border-b border-[var(--color-border)] px-4 py-3 last:border-b-0">
      <div className="flex items-center justify-between gap-3">
        <div className="flex flex-col">
          <span className="text-sm font-medium text-[var(--color-text-primary)]">
            {formatAuditAction(entry.action)}
            {entry.entityId ? ` — ${entry.entityId}` : ''}
          </span>
          <span className="text-xs text-[var(--color-text-secondary)]">
            {entry.entityType} · {formatTimestamp(entry.createdAt)}
          </span>
        </div>
      </div>
      {details.length > 0 && (
        <dl className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
          {details.map(({ key, value }) => (
            <div key={key} className="flex gap-1">
              <dt className="font-medium text-[var(--color-text-primary)]">{key}:</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      )}
    </li>
  )
}

export default function ActivityTimelineList({ entries }: { entries: AuditLogEntry[] }) {
  return (
    <ul className="overflow-hidden rounded-lg border border-[var(--color-border)] bg-[var(--color-card-bg)]">
      {entries.map((entry) => (
        <ActivityRow key={entry.id} entry={entry} />
      ))}
    </ul>
  )
}
