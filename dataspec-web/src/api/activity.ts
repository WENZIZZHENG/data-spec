import request from '@/api/request'
import type { ProjectActivityTimeline } from '@/types'

export function listProjectActivities(projectId: number, actionType?: string, limit = 20) {
  return request.get<unknown, ProjectActivityTimeline>(`/projects/${projectId}/activities`, {
    params: {
      actionType: actionType || undefined,
      limit
    }
  })
}
