import type {
  DatabaseConnectionHealthDiagnostic,
  DatabaseConnectionSecurityDiagnostic
} from '@/types'

type SecurityTagType = 'success' | 'warning' | 'danger' | 'info'

export function securityRiskLabel(riskLevel?: string) {
  if (riskLevel === 'SAFE') {
    return '只读安全'
  }
  if (riskLevel === 'WARNING') {
    return '需要确认'
  }
  if (riskLevel === 'DANGER') {
    return '高权限风险'
  }
  return '未知'
}

export function securityRiskTagType(riskLevel?: string): SecurityTagType {
  if (riskLevel === 'SAFE') {
    return 'success'
  }
  if (riskLevel === 'WARNING') {
    return 'warning'
  }
  if (riskLevel === 'DANGER') {
    return 'danger'
  }
  return 'info'
}

export function readOnlyLabel(value?: boolean) {
  if (value === true) {
    return '只读：是'
  }
  if (value === false) {
    return '只读：未确认'
  }
  return '只读：未知'
}

export function writeRiskLabel(value?: boolean) {
  if (value === true) {
    return '写风险：有'
  }
  if (value === false) {
    return '写风险：未发现'
  }
  return '写风险：未知'
}

export function databaseSecuritySummary(security?: DatabaseConnectionSecurityDiagnostic | null) {
  if (!security) {
    return '未测试'
  }
  const databaseType = security.databaseType || 'UNKNOWN'
  const currentUser = security.currentUser || '未知用户'
  const tableCount = security.accessibleTableCount ?? 0
  return `${databaseType} · ${currentUser} · ${securityRiskLabel(security.riskLevel)} · ${tableCount} 张表`
}

export function connectionStatusLabel(status?: string) {
  if (status === 'CONNECTED') {
    return '连接可用'
  }
  if (status === 'FAILED') {
    return '连接失败'
  }
  return '未确认'
}

export function connectionStatusTagType(status?: string): SecurityTagType {
  if (status === 'CONNECTED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

export function failureCategoryLabel(category?: string) {
  if (category === 'AUTHENTICATION') {
    return '认证失败'
  }
  if (category === 'NETWORK') {
    return '网络不可达'
  }
  if (category === 'SCHEMA_NOT_FOUND') {
    return '库或 Schema 不存在'
  }
  if (category === 'PERMISSION_DENIED') {
    return '权限不足'
  }
  if (category === 'UNSUPPORTED_DIALECT') {
    return '方言不支持'
  }
  if (category === 'UNKNOWN') {
    return '未知失败'
  }
  return '未分类'
}

export function capabilitySupportLabel(value?: string) {
  if (value === 'SUPPORTED') {
    return '支持'
  }
  if (value === 'UNSUPPORTED') {
    return '不支持'
  }
  if (value === 'PARTIAL') {
    return '部分支持'
  }
  return '未知'
}

export function capabilitySupportTagType(value?: string): SecurityTagType {
  if (value === 'SUPPORTED') {
    return 'success'
  }
  if (value === 'UNSUPPORTED') {
    return 'danger'
  }
  if (value === 'PARTIAL') {
    return 'warning'
  }
  return 'info'
}

export function metadataReadableLabel(value?: boolean) {
  if (value === true) {
    return 'Metadata：可读'
  }
  if (value === false) {
    return 'Metadata：未确认'
  }
  return 'Metadata：未知'
}

export function retryableLabel(value?: boolean) {
  if (value === true) {
    return '可重试'
  }
  if (value === false) {
    return '不建议重试'
  }
  return '重试未知'
}

export function databaseHealthSummary(health?: DatabaseConnectionHealthDiagnostic | null) {
  if (!health) {
    return '未测试'
  }
  const latency = health.latencyMs !== undefined ? `${health.latencyMs}ms` : '耗时未知'
  if (health.connectionStatus === 'FAILED') {
    return `${connectionStatusLabel(health.connectionStatus)} · ${failureCategoryLabel(health.failureCategory)} · ${latency}`
  }
  const database = [
    health.dialect || health.capability?.dialect,
    health.databaseProduct,
    health.version
  ].filter(Boolean).join(' / ') || 'UNKNOWN'
  return `${connectionStatusLabel(health.connectionStatus)} · ${database} · ${latency}`
}
