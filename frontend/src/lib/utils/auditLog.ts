const ACTION_LABELS: Record<string, string> = {
  BUY: 'Bought',
  SELL: 'Sold',
  PLACE_ORDER: 'Placed order',
  CANCEL_ORDER: 'Cancelled order',
  WATCHLIST_ADD: 'Added to watchlist',
  WATCHLIST_REMOVE: 'Removed from watchlist',
}

export function formatAuditAction(action: string): string {
  return (
    ACTION_LABELS[action] ?? action.charAt(0) + action.slice(1).toLowerCase().replace(/_/g, ' ')
  )
}

function formatDetailValue(value: unknown): string {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function humanizeKey(key: string): string {
  const spaced = key.replace(/([a-z0-9])([A-Z])/g, '$1 $2')
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase()
}

// details is a raw JSON string of the audited method's arguments - never rendered as a raw dump.
export function summarizeAuditDetails(details: string | null): { key: string; value: string }[] {
  if (!details) return []
  try {
    const parsed: unknown = JSON.parse(details)
    if (typeof parsed !== 'object' || parsed === null) return []
    return Object.entries(parsed as Record<string, unknown>)
      .filter(([, value]) => value !== null && value !== undefined)
      .map(([key, value]) => ({ key: humanizeKey(key), value: formatDetailValue(value) }))
  } catch {
    return []
  }
}
