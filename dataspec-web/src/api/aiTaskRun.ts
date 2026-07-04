import request from '@/api/request'
import type { AiTaskRunDetail, AiTaskRunListItem, PageResult } from '@/types'

export interface AiTaskRunListParams {
  projectId: number
  current?: number
  size?: number
  status?: string
  taskType?: string
}

export function listAiTaskRuns(params: AiTaskRunListParams) {
  return request.get<unknown, PageResult<AiTaskRunListItem>>('/ai-task-runs', {
    params: {
      current: 1,
      size: 10,
      ...params
    }
  })
}

export function listRecentAiTaskFailures(projectId: number, limit = 10) {
  return request.get<unknown, AiTaskRunListItem[]>('/ai-task-runs/recent-failures', {
    params: { projectId, limit }
  })
}

export function getAiTaskRunDetail(id: number, projectId: number) {
  return request.get<unknown, AiTaskRunDetail>(`/ai-task-runs/${id}`, {
    params: { projectId }
  })
}
