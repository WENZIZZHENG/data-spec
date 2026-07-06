import { typedGet } from '@/api/typedClient'

export interface AiTaskRunListParams {
  projectId: number
  current?: number
  size?: number
  status?: string
  taskType?: string
}

export function listAiTaskRuns(params: AiTaskRunListParams) {
  return typedGet('/api/ai-task-runs', {
    params: {
      current: 1,
      size: 10,
      ...params
    }
  })
}

export function listRecentAiTaskFailures(projectId: number, limit = 10) {
  return typedGet('/api/ai-task-runs/recent-failures', {
    params: { projectId, limit }
  })
}

export function getAiTaskRunDetail(id: number, projectId: number) {
  return typedGet('/api/ai-task-runs/{id}', {
    path: { id },
    params: { projectId }
  })
}
