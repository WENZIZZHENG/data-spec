import request from '@/api/request'
import type { MetricDefinitionReq, MetricDefinitionResp } from '@/types'

export function listMetricDefinitions(params: {
  projectId: number
  query?: string
  status?: string
  fieldId?: number
  metricKey?: string
  limit?: number
}) {
  return request.get<unknown, MetricDefinitionResp[]>('/metric-definitions', { params })
}

export function createMetricDefinition(data: MetricDefinitionReq) {
  return request.post<unknown, MetricDefinitionResp>('/metric-definitions', data)
}

export function updateMetricDefinition(id: number, data: MetricDefinitionReq) {
  return request.put<unknown, MetricDefinitionResp>(`/metric-definitions/${id}`, data)
}

export function deleteMetricDefinition(id: number) {
  return request.delete<unknown, void>(`/metric-definitions/${id}`)
}
