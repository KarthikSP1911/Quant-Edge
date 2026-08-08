import type { ActivityTimelineFilters, AuditLogPage } from '@/types/auditLog'
import { graphqlRequest } from './client'

const ACTIVITY_TIMELINE_QUERY = /* GraphQL */ `
  query ActivityTimeline(
    $action: String
    $entityType: String
    $startDate: String
    $endDate: String
    $page: Int
    $size: Int
  ) {
    activityTimeline(
      action: $action
      entityType: $entityType
      startDate: $startDate
      endDate: $endDate
      page: $page
      size: $size
    ) {
      content {
        id
        action
        entityType
        entityId
        details
        ipAddress
        createdAt
      }
      totalElements
      totalPages
      page
      size
    }
  }
`

export async function fetchActivityTimeline(
  filters: ActivityTimelineFilters,
): Promise<AuditLogPage> {
  const data = await graphqlRequest<{ activityTimeline: AuditLogPage }>(ACTIVITY_TIMELINE_QUERY, {
    ...filters,
  })
  return data.activityTimeline
}
