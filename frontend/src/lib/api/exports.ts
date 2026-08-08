import { API_BASE_URL } from '@/lib/config'
import { getAccessToken } from '@/lib/auth/tokens'
import { ApiError } from './client'

export type ExportKind = 'portfolio-pdf' | 'trade-history-csv' | 'tax-pnl-pdf'

const FILENAMES: Record<ExportKind, string> = {
  'portfolio-pdf': 'portfolio-report.pdf',
  'trade-history-csv': 'trade-history.csv',
  'tax-pnl-pdf': 'tax-pnl-report.pdf',
}

// Generation and download are one call - the endpoint returns the file directly, so this
// triggers the browser's native download instead of parsing JSON like apiRequest does.
export async function downloadExport(kind: ExportKind): Promise<void> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE_URL}/api/exports/${kind}`, {
    method: 'POST',
    credentials: 'include',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })

  if (!response.ok) {
    throw new ApiError('Failed to generate export. Please try again.', response.status)
  }

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = FILENAMES[kind]
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
