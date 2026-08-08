// Mirrors the backend's GraphQL AuditLogEntry type (activityTimeline).
export interface AuditLogEntry {
  id: string
  action: string
  entityType: string
  entityId: string | null
  details: string | null
  ipAddress: string | null
  createdAt: string
}

export interface AuditLogPage {
  content: AuditLogEntry[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface ActivityTimelineFilters {
  action: string | null
  entityType: string | null
  startDate: string | null
  endDate: string | null
  page: number
  size: number
}
