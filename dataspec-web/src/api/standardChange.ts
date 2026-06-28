import request from '@/api/request'
import type {
  FieldChangePreviewReq,
  RuleChangePreviewReq,
  StandardChangePreview
} from '@/types'

export function previewFieldChange(id: number, data: FieldChangePreviewReq) {
  return request.post<unknown, StandardChangePreview>(`/standard-changes/preview/fields/${id}`, data)
}

export function previewRuleChange(id: number, data: RuleChangePreviewReq) {
  return request.post<unknown, StandardChangePreview>(`/standard-changes/preview/rules/${id}`, data)
}

export function previewRuleToggle(id: number, projectId: number, enabled: boolean) {
  return request.post<unknown, StandardChangePreview>(`/standard-changes/preview/rules/${id}/toggle`, null, {
    params: { projectId, enabled }
  })
}
