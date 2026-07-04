import request from '@/api/request'
import type {
  PageResult,
  StandardUsageExample,
  StandardUsageExampleSaveReq
} from '@/types'

export interface UsageExampleListParams {
  projectId: number
  scope?: string
  exampleType?: string
  status?: string
  query?: string
  current?: number
  size?: number
}

export function listUsageExamples(params: UsageExampleListParams) {
  return request.get<unknown, PageResult<StandardUsageExample>>('/usage-examples', {
    params
  })
}

export function createUsageExample(data: StandardUsageExampleSaveReq) {
  return request.post<unknown, StandardUsageExample>('/usage-examples', data)
}

export function updateUsageExample(id: number, data: StandardUsageExampleSaveReq) {
  return request.put<unknown, StandardUsageExample>(`/usage-examples/${id}`, data)
}

export function deleteUsageExample(projectId: number, id: number) {
  return request.delete<unknown, void>(`/usage-examples/${id}`, {
    params: { projectId }
  })
}
