import type { RuleExemption } from '@/types'

export function ruleExemptionScopeLabel(exemption: RuleExemption) {
  const table = exemption.tableName?.trim() || '*'
  const column = exemption.columnName?.trim() || '*'
  return `${table}.${column}`
}

export function isRuleExemptionExpired(exemption: RuleExemption, now = new Date()) {
  if (!exemption.expiresAt) {
    return false
  }
  const expiresAt = new Date(exemption.expiresAt)
  return Number.isFinite(expiresAt.getTime()) && expiresAt.getTime() <= now.getTime()
}

export function ruleExemptionStatusLabel(exemption: RuleExemption, now = new Date()) {
  if (!exemption.enabled) {
    return '已禁用'
  }
  if (isRuleExemptionExpired(exemption, now)) {
    return '已过期'
  }
  return '启用中'
}

export function ruleExemptionStatusTagType(exemption: RuleExemption, now = new Date()) {
  if (!exemption.enabled) {
    return 'info'
  }
  if (isRuleExemptionExpired(exemption, now)) {
    return 'warning'
  }
  return 'success'
}

export function normalizeRuleExemptionPayload(exemption: RuleExemption): RuleExemption {
  return {
    ...exemption,
    tableName: exemption.tableName?.trim() || null,
    columnName: exemption.columnName?.trim() || null,
    ruleCode: exemption.ruleCode?.trim(),
    reason: exemption.reason?.trim()
  }
}
