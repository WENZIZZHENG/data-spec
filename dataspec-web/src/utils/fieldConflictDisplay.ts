import type { FieldConflictField, FieldConflictGroup, FieldConflictSeverity, FieldConflictType } from '@/types'

export function conflictTypeLabel(type?: FieldConflictType | string) {
  if (type === 'NAME_DUPLICATE') {
    return '字段名重复'
  }
  if (type === 'ALIAS_CONFLICT') {
    return '别名冲突'
  }
  if (type === 'DISPLAY_NAME_DUPLICATE') {
    return '显示名重复'
  }
  if (type === 'SEMANTIC_DUPLICATE') {
    return '语义疑似重复'
  }
  if (type === 'RESERVED_WORD') {
    return 'SQL 保留字'
  }
  if (type === 'DANGEROUS_SQL_NAME') {
    return 'SQL 危险命名'
  }
  if (type === 'CASE_COLLISION') {
    return '大小写碰撞'
  }
  if (type === 'AMBIGUOUS_ALIAS') {
    return 'Alias 歧义'
  }
  return '其他冲突'
}

export function isNamingRiskType(type?: FieldConflictType | string) {
  return type === 'RESERVED_WORD'
    || type === 'DANGEROUS_SQL_NAME'
    || type === 'CASE_COLLISION'
    || type === 'AMBIGUOUS_ALIAS'
}

export function conflictSeverityTagType(severity?: FieldConflictSeverity | string) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

export function filterConflictGroups(
  groups: FieldConflictGroup[],
  severity: FieldConflictSeverity | 'ALL',
  type: FieldConflictType | 'ALL'
) {
  return groups.filter((group) => {
    const severityMatched = severity === 'ALL' || group.severity === severity
    const typeMatched = type === 'ALL' || group.conflictType === type
    return severityMatched && typeMatched
  })
}

export function conflictFieldEditQuery(field: FieldConflictField) {
  const query: Record<string, string> = {
    keyword: field.name || field.displayName || ''
  }
  if (field.fieldId) {
    query.fieldId = String(field.fieldId)
  }
  return query
}

export function conflictFieldSummary(fields?: FieldConflictField[]) {
  return (fields ?? [])
    .map((field) => field.name || field.displayName)
    .filter(Boolean)
    .join('、')
}
