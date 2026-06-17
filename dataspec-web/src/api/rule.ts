import request from '@/api/request'
import type { RuleConfig, RuleConfigReq } from '@/types'

export function listRuleConfigs(projectId: number) {
  return request.get<unknown, RuleConfig[]>('/rules', {
    params: { projectId }
  })
}

export function getRuleConfig(id: number) {
  return request.get<unknown, RuleConfig>(`/rules/${id}`)
}

export function createRuleConfig(data: RuleConfigReq) {
  return request.post<unknown, RuleConfig>('/rules', data)
}

export function updateRuleConfig(id: number, data: RuleConfigReq) {
  return request.put<unknown, RuleConfig>(`/rules/${id}`, data)
}

export function toggleRuleConfig(id: number, enabled: boolean) {
  return request.patch<unknown, void>(`/rules/${id}/toggle`, null, {
    params: { enabled }
  })
}

export function deleteRuleConfig(id: number) {
  return request.delete<unknown, void>(`/rules/${id}`)
}
