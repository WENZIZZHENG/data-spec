import type { DatabaseConnectionSecurityDiagnostic } from '@/types'

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
