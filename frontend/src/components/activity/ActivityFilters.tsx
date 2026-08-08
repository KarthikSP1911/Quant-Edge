const ACTIONS = [
  { value: '', label: 'All actions' },
  { value: 'BUY', label: 'Bought' },
  { value: 'SELL', label: 'Sold' },
  { value: 'PLACE_ORDER', label: 'Placed order' },
  { value: 'CANCEL_ORDER', label: 'Cancelled order' },
  { value: 'WATCHLIST_ADD', label: 'Added to watchlist' },
  { value: 'WATCHLIST_REMOVE', label: 'Removed from watchlist' },
]

const ENTITY_TYPES = [
  { value: '', label: 'All entities' },
  { value: 'ORDER', label: 'Order' },
  { value: 'WATCHLIST', label: 'Watchlist' },
]

const selectClasses =
  'rounded-md border border-[var(--color-border)] bg-[var(--color-page-bg)] px-3 py-2 text-sm text-[var(--color-text-primary)] focus:border-[var(--color-accent-blue)] focus:outline-none'

export interface ActivityFiltersValue {
  action: string
  entityType: string
  startDate: string
  endDate: string
}

export default function ActivityFilters({
  value,
  onChange,
}: {
  value: ActivityFiltersValue
  onChange: (value: ActivityFiltersValue) => void
}) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <select
        className={selectClasses}
        value={value.action}
        onChange={(e) => onChange({ ...value, action: e.target.value })}
      >
        {ACTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <select
        className={selectClasses}
        value={value.entityType}
        onChange={(e) => onChange({ ...value, entityType: e.target.value })}
      >
        {ENTITY_TYPES.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      <input
        type="date"
        className={selectClasses}
        value={value.startDate}
        onChange={(e) => onChange({ ...value, startDate: e.target.value })}
        aria-label="From date"
      />
      <input
        type="date"
        className={selectClasses}
        value={value.endDate}
        onChange={(e) => onChange({ ...value, endDate: e.target.value })}
        aria-label="To date"
      />
    </div>
  )
}
