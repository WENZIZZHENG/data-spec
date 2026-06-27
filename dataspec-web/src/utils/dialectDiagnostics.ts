import type { DialectDiagnostic } from '@/types'

type DiagnosticTagType = 'success' | 'warning' | 'danger' | 'info'

export function dialectName(dialect?: string) {
  if (dialect === 'postgresql') {
    return 'PostgreSQL'
  }
  if (dialect === 'mysql') {
    return 'MySQL'
  }
  return '未知方言'
}

export function dialectSummary(diagnostics: DialectDiagnostic[] = []) {
  const first = diagnostics.find((item) => item.dialect)
  const riskCount = diagnostics.filter((item) =>
    ['WARNING', 'PARTIAL', 'UNSUPPORTED'].includes(item.level ?? '')
  ).length
  const name = dialectName(first?.dialect)
  return riskCount > 0 ? `${name} · ${riskCount} 个兼容提示` : `${name} · 已识别`
}

export function diagnosticTagType(level?: string): DiagnosticTagType {
  if (level === 'SUPPORTED') {
    return 'success'
  }
  if (level === 'WARNING' || level === 'PARTIAL') {
    return 'warning'
  }
  if (level === 'UNSUPPORTED') {
    return 'danger'
  }
  return 'info'
}

export function diagnosticSummaryTagType(diagnostics: DialectDiagnostic[] = []): DiagnosticTagType {
  if (diagnostics.some((item) => item.level === 'UNSUPPORTED')) {
    return 'danger'
  }
  if (diagnostics.some((item) => item.level === 'WARNING' || item.level === 'PARTIAL')) {
    return 'warning'
  }
  if (diagnostics.some((item) => item.level === 'SUPPORTED')) {
    return 'success'
  }
  return 'info'
}

export function diagnosticLevelLabel(level?: string) {
  if (level === 'SUPPORTED') {
    return '支持'
  }
  if (level === 'PARTIAL') {
    return '部分'
  }
  if (level === 'WARNING') {
    return '注意'
  }
  if (level === 'UNSUPPORTED') {
    return '不支持'
  }
  return '提示'
}
