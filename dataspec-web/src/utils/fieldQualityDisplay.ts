export type FieldQualityLevelLike = 'GOOD' | 'WARNING' | 'POOR' | string
export type FieldQualitySeverityLike = 'ERROR' | 'WARNING' | 'SUGGESTION' | string

export interface FieldQualityIssueLike {
  code?: string
  severity?: FieldQualitySeverityLike
}

export interface FieldQualityItemLike {
  fieldId?: number
  name?: string
  displayName?: string
  level?: FieldQualityLevelLike
  issues?: FieldQualityIssueLike[]
}

export function qualityLevelLabel(level?: FieldQualityLevelLike) {
  if (level === 'GOOD') {
    return '良好'
  }
  if (level === 'WARNING') {
    return '待完善'
  }
  if (level === 'POOR') {
    return '低质量'
  }
  return '未知'
}

export function qualityLevelTagType(level?: FieldQualityLevelLike) {
  if (level === 'GOOD') {
    return 'success'
  }
  if (level === 'WARNING') {
    return 'warning'
  }
  if (level === 'POOR') {
    return 'danger'
  }
  return 'info'
}

export function qualitySeverityTagType(severity?: FieldQualitySeverityLike) {
  if (severity === 'ERROR') {
    return 'danger'
  }
  if (severity === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

export function filterQualityItems(
  items: FieldQualityItemLike[],
  level: FieldQualityLevelLike | 'ALL',
  issueCode: string
) {
  return items.filter((item) => {
    const levelMatched = level === 'ALL' || item.level === level
    const issueMatched = issueCode === 'ALL' || (item.issues ?? []).some((issue) => issue.code === issueCode)
    return levelMatched && issueMatched
  })
}

export function issueOptions(items: FieldQualityItemLike[]) {
  const codes = new Set<string>()
  for (const item of items) {
    for (const issue of item.issues ?? []) {
      if (issue.code) {
        codes.add(issue.code)
      }
    }
  }
  return Array.from(codes).sort()
}

export function fieldQualityEditQuery(item: FieldQualityItemLike) {
  const query: Record<string, string> = {
    keyword: item.name || item.displayName || ''
  }
  if (item.fieldId) {
    query.fieldId = String(item.fieldId)
  }
  return query
}
