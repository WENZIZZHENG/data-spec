import request from '@/api/request'
import type { FieldKnowledgeCardListResp, FieldKnowledgeCardResp } from '@/types'

export function listFieldKnowledgeCards(params: {
  projectId: number
  query?: string
  status?: string
  fieldId?: number
  limit?: number
}) {
  return request.get<unknown, FieldKnowledgeCardListResp>('/field-knowledge-cards', { params })
}

export function getFieldKnowledgeCard(projectId: number, fieldId: number) {
  return request.get<unknown, FieldKnowledgeCardResp>(`/field-knowledge-cards/${fieldId}`, {
    params: { projectId }
  })
}
