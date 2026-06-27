export type FieldImpactTypeLike =
  | 'TEMPLATE'
  | 'IMPORT_SOURCE'
  | 'SQL_CHECK'
  | 'STANDARD_SNAPSHOT'
  | 'CODE_SET'
  | string

export type FieldImpactSeverityLike = 'HIGH' | 'WARNING' | 'INFO' | string

export interface FieldImpactSummaryLike {
  totalImpactCount?: number
  templateImpactCount?: number
  importSourceImpactCount?: number
  sqlCheckImpactCount?: number
  snapshotImpactCount?: number
  codeSetImpactCount?: number
}

export interface FieldEditWarningLike {
  attribute?: string
}

export interface FieldCriticalValueLike {
  name?: string
  dataType?: string
  status?: string
  codeSetId?: number
  sensitive?: boolean
}

const ATTRIBUTE_LABELS: Record<string, string> = {
  name: '字段名',
  dataType: '数据类型',
  status: '状态',
  codeSetId: '代码集',
  sensitive: '敏感标记'
}

export function impactTypeLabel(type?: FieldImpactTypeLike) {
  if (type === 'TEMPLATE') {
    return '表模板'
  }
  if (type === 'IMPORT_SOURCE') {
    return '导入来源'
  }
  if (type === 'SQL_CHECK') {
    return 'SQL 检查'
  }
  if (type === 'STANDARD_SNAPSHOT') {
    return '标准快照'
  }
  if (type === 'CODE_SET') {
    return '代码集'
  }
  return '其他影响'
}

export function impactSeverityTagType(severity?: FieldImpactSeverityLike) {
  if (severity === 'HIGH') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

export function fieldImpactSummaryText(summary?: FieldImpactSummaryLike) {
  const total = summary?.totalImpactCount ?? 0
  if (total === 0) {
    return '暂无已知影响'
  }
  const parts = [
    ['模板', summary?.templateImpactCount],
    ['导入来源', summary?.importSourceImpactCount],
    ['SQL', summary?.sqlCheckImpactCount],
    ['快照', summary?.snapshotImpactCount],
    ['代码集', summary?.codeSetImpactCount]
  ]
    .filter(([, value]) => Number(value ?? 0) > 0)
    .map(([label, value]) => `${label} ${value}`)
  return `共 ${total} 项影响：${parts.join('，')}`
}

export function warningSummaryText(warnings: FieldEditWarningLike[]) {
  return warnings
    .map((warning) => ATTRIBUTE_LABELS[warning.attribute ?? ''] ?? warning.attribute)
    .filter(Boolean)
    .join('、')
}

export function criticalFieldChanged(before: FieldCriticalValueLike, after: FieldCriticalValueLike) {
  return ['name', 'dataType', 'status', 'codeSetId', 'sensitive'].some((key) =>
    normalizeValue(before[key as keyof FieldCriticalValueLike]) !== normalizeValue(after[key as keyof FieldCriticalValueLike])
  )
}

function normalizeValue(value: unknown) {
  return value ?? null
}
