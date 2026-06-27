import request from '@/api/request'
import type { PageResult, StandardChangeLog } from '@/types'

export function listChangeLogs(
  projectId: number,
  targetType?: string,
  targetId?: number,
  current = 1,
  size = 10
) {
  return request.get<unknown, PageResult<StandardChangeLog>>('/change-logs', {
    params: {
      projectId,
      targetType,
      targetId,
      current,
      size
    }
  })
}
