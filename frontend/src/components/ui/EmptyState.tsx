import Link from 'next/link'

interface EmptyStateAction {
  label: string
  href?: string
  onClick?: () => void
}

interface EmptyStateProps {
  title: string
  message?: string
  action?: EmptyStateAction
}

export default function EmptyState({ title, message, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] py-10 text-center">
      <p className="text-sm font-medium text-[var(--color-text-primary)]">{title}</p>
      {message && <p className="text-xs text-[var(--color-text-secondary)]">{message}</p>}
      {action &&
        (action.href ? (
          <Link
            href={action.href}
            className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
          >
            {action.label}
          </Link>
        ) : (
          <button
            type="button"
            onClick={action.onClick}
            className="text-sm font-medium text-[var(--color-accent-blue)] hover:underline"
          >
            {action.label}
          </button>
        ))}
    </div>
  )
}
