import request from '@/api/request'
import type {
  Field,
  FieldBulkUpdatePreview,
  FieldBulkUpdateReq,
  FieldBulkUpdateResult,
  FieldChangeUndoResult,
  FieldConflictReport,
  FieldGroupSummary,
  FieldGroupingBatchUpdateReq,
  FieldGroupingBatchUpdateResult,
  FieldImpactReport,
  FieldQualityReport,
  FieldReq,
  FieldSearchReq,
  FieldSearchResult,
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

export function searchFields(params: FieldSearchReq) {
  return request.get<unknown, FieldSearchResult>('/fields/search', {
    params
  })
}

export function getFieldGroupSummary(projectId: number) {
  return request.get<unknown, FieldGroupSummary>('/fields/groups', {
    params: { projectId }
  })
}

export function batchUpdateFieldGrouping(data: FieldGroupingBatchUpdateReq) {
  return request.post<unknown, FieldGroupingBatchUpdateResult>('/fields/groups/batch-update', data)
}

export function previewFieldBulkUpdate(data: FieldBulkUpdateReq) {
  return request.post<unknown, FieldBulkUpdatePreview>('/fields/bulk-update/preview', data)
}

export function bulkUpdateFields(data: FieldBulkUpdateReq) {
  return request.post<unknown, FieldBulkUpdateResult>('/fields/bulk-update', data)
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

export function getFieldConflictReport(projectId: number) {
  return request.get<unknown, FieldConflictReport>('/fields/conflicts', {
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

export function undoFieldChange(id: number, logId: number) {
  return request.post<unknown, FieldChangeUndoResult>(`/fields/${id}/undo`, null, {
    params: { logId }
  })
}

export function deleteField(id: number) {
  return request.delete<unknown, void>(`/fields/${id}`)
}
