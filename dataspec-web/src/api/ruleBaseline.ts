import request from '@/api/request'
import type {
  RuleBaselineApplyReq,
  RuleBaselineApplyResult,
  RuleBaselineImportReq,
  RuleBaselineInfo,
  RuleBaselinePackage,
  RuleBaselineTemplate
} from '@/types'

export function listRuleBaselineTemplates() {
  return request.get<unknown, RuleBaselineTemplate[]>('/rule-baselines/templates')
}

export function getCurrentRuleBaseline(projectId: number) {
  return request.get<unknown, RuleBaselineInfo>('/rule-baselines/current', {
    params: { projectId }
  })
}

export function applyRuleBaseline(data: RuleBaselineApplyReq) {
  return request.post<unknown, RuleBaselineApplyResult>('/rule-baselines/apply', data)
}

export function exportRuleBaseline(projectId: number) {
  return request.get<unknown, RuleBaselinePackage>('/rule-baselines/export', {
    params: { projectId }
  })
}

export function importRuleBaseline(data: RuleBaselineImportReq) {
  return request.post<unknown, RuleBaselineApplyResult>('/rule-baselines/import', data)
}
