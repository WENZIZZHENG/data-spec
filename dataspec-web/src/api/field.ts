import request from '@/api/request'
import type {
  Field,
  FieldImpactReport,
  FieldQualityReport,
  FieldReq,
  FieldSourceDetail,
  FieldSuggestion,
  PageResult
} from '@/types'

export function pageFields(projectId: number, current = 1, size = 20) {
  return request.get<unknown, PageResult<Field>>('/fields', {
    params: { projectId, current, size }
  })
}

export function listFields(projectId: number) {
  return request.get<unknown, Field[]>('/fields/all', {
    params: { projectId }
  })
}

export function suggestFields(projectId: number, query: string, limit = 5) {
  return request.get<unknown, FieldSuggestion[]>('/fields/suggest', {
    params: { projectId, query, limit }
  })
}

export function getFieldQualityReport(projectId: number) {
  return request.get<unknown, FieldQualityReport>('/fields/quality', {
    params: { projectId }
  })
}

export function getFieldImpactReport(id: number, projectId: number) {
  return request.get<unknown, FieldImpactReport>(`/fields/${id}/impact`, {
    params: { projectId }
  })
}

export function getField(id: number) {
  return request.get<unknown, Field>(`/fields/${id}`)
}

export function listFieldSources(id: number) {
  return request.get<unknown, FieldSourceDetail[]>(`/fields/${id}/sources`)
}

export function createField(data: FieldReq) {
  return request.post<unknown, Field>('/fields', data)
}

export function updateField(id: number, data: FieldReq) {
  return request.put<unknown, Field>(`/fields/${id}`, data)
}

export function deleteField(id: number) {
  return request.delete<unknown, void>(`/fields/${id}`)
}
