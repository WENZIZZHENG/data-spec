import request from '@/api/request'
import type { DashboardSummary } from '@/types'

export function getDashboardSummary(projectId: number) {
  return request.get<unknown, DashboardSummary>('/dashboard/summary', {
    params: { projectId }
  })
}
