import { isValidElement, type ReactNode } from 'react'
import ReactMarkdown, { type Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'

// Matches a signed price/percentage change, e.g. "+2.34%", "-$12.30", "-4.1"
const CHANGE_PATTERN = /^[+-]\$?[\d,]+(\.\d+)?%?$/

function flattenText(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node)
  if (Array.isArray(node)) return node.map(flattenText).join('')
  if (isValidElement(node)) {
    const props = node.props as { children?: ReactNode }
    return flattenText(props.children)
  }
  return ''
}

function changeColorClass(node: ReactNode): string {
  const text = flattenText(node).trim()
  if (!CHANGE_PATTERN.test(text)) return ''
  return text.startsWith('-') ? 'text-[var(--color-loss)]' : 'text-[var(--color-profit)]'
}

const components: Components = {
  p: ({ children }) => <p className="mb-2 leading-relaxed last:mb-0">{children}</p>,
  strong: ({ children }) => (
    <strong className="font-semibold text-[var(--color-text-primary)]">{children}</strong>
  ),
  em: ({ children }) => <em className="italic">{children}</em>,
  ul: ({ children }) => <ul className="mb-2 list-disc space-y-1 pl-5 last:mb-0">{children}</ul>,
  ol: ({ children }) => <ol className="mb-2 list-decimal space-y-1 pl-5 last:mb-0">{children}</ol>,
  li: ({ children }) => <li className={changeColorClass(children)}>{children}</li>,
  a: ({ children, href }) => (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-[var(--color-accent-blue)] underline underline-offset-2 hover:opacity-80"
    >
      {children}
    </a>
  ),
  h1: ({ children }) => (
    <h1 className="mt-3 mb-1.5 text-base font-semibold text-[var(--color-text-primary)] first:mt-0">
      {children}
    </h1>
  ),
  h2: ({ children }) => (
    <h2 className="mt-3 mb-1.5 text-sm font-semibold text-[var(--color-text-primary)] first:mt-0">
      {children}
    </h2>
  ),
  h3: ({ children }) => (
    <h3 className="mt-2 mb-1 text-sm font-semibold text-[var(--color-text-primary)] first:mt-0">
      {children}
    </h3>
  ),
  hr: () => <hr className="my-3 border-[var(--color-border)]" />,
  code: ({ children }) => (
    <code className="rounded bg-[var(--color-sidebar-hover)] px-1 py-0.5 font-mono text-xs text-[var(--color-text-primary)]">
      {children}
    </code>
  ),
  pre: ({ children }) => (
    <pre className="mb-2 overflow-x-auto rounded-md bg-[var(--color-sidebar-hover)] p-3 text-xs last:mb-0">
      {children}
    </pre>
  ),
  table: ({ children }) => (
    <div className="mb-2 overflow-x-auto rounded-md border border-[var(--color-border)] last:mb-0">
      <table className="w-full border-collapse text-sm">{children}</table>
    </div>
  ),
  thead: ({ children }) => (
    <thead className="bg-[var(--color-sidebar-hover)] text-[var(--color-text-secondary)]">
      {children}
    </thead>
  ),
  tbody: ({ children }) => <tbody>{children}</tbody>,
  tr: ({ children }) => (
    <tr className="border-t border-[var(--color-border)] first:border-t-0">{children}</tr>
  ),
  th: ({ children }) => (
    <th className="px-3 py-1.5 text-left text-xs font-semibold whitespace-nowrap">{children}</th>
  ),
  td: ({ children }) => (
    <td className={`px-3 py-1.5 whitespace-nowrap tabular-nums ${changeColorClass(children)}`}>
      {children}
    </td>
  ),
}

export default function ChatMarkdown({ content }: { content: string }) {
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
      {content}
    </ReactMarkdown>
  )
}
