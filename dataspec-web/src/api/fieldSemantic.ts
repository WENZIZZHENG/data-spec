import request from '@/api/request'
import type { FieldSemanticRuleReq, FieldSemanticRuleResp } from '@/types'

export function listFieldSemanticRules(params: {
  projectId: number
  fieldId?: number
  ruleType?: string
  query?: string
  limit?: number
}) {
  return request.get<unknown, FieldSemanticRuleResp[]>('/field-semantics', { params })
}

export function createFieldSemanticRule(data: FieldSemanticRuleReq) {
  return request.post<unknown, FieldSemanticRuleResp>('/field-semantics', data)
}

export function updateFieldSemanticRule(id: number, data: FieldSemanticRuleReq) {
  return request.put<unknown, FieldSemanticRuleResp>(`/field-semantics/${id}`, data)
}

export function deleteFieldSemanticRule(id: number) {
  return request.delete<unknown, void>(`/field-semantics/${id}`)
}
