import request from '@/api/request'
import type { RuleExemption, RuleExemptionReq } from '@/types'

export function listRuleExemptions(projectId: number) {
  return request.get<unknown, RuleExemption[]>('/rule-exemptions', {
    params: { projectId }
  })
}

export function getRuleExemption(id: number) {
  return request.get<unknown, RuleExemption>(`/rule-exemptions/${id}`)
}

export function createRuleExemption(data: RuleExemptionReq) {
  return request.post<unknown, RuleExemption>('/rule-exemptions', data)
}

export function disableRuleExemption(id: number) {
  return request.patch<unknown, void>(`/rule-exemptions/${id}/disable`)
}

export function deleteRuleExemption(id: number) {
  return request.delete<unknown, void>(`/rule-exemptions/${id}`)
}
