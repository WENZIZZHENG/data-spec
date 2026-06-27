export type FieldCoverageStatusLike =
  | 'STANDARD_MATCH'
  | 'ALIAS_MATCH'
  | 'MISSING_COMMENT'
  | 'POSSIBLE_DUPLICATE'
  | 'UNMANAGED'
  | string

export interface FieldCoverageItemLike {
  tableName?: string
  columnName?: string
  status?: FieldCoverageStatusLike
}

export interface FieldCoverageTableLike {
  tableName?: string
  fields?: FieldCoverageItemLike[]
}

export function coverageStatusLabel(status?: FieldCoverageStatusLike): string {
  const labels: Record<string, string> = {
    STANDARD_MATCH: '标准命中',
    ALIAS_MATCH: '别名命中',
    MISSING_COMMENT: '缺注释',
    POSSIBLE_DUPLICATE: '疑似重复',
    UNMANAGED: '未纳管'
  }
  return status ? labels[status] ?? status : '未知'
}

export function coverageStatusTagType(status?: FieldCoverageStatusLike): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'STANDARD_MATCH' || status === 'ALIAS_MATCH') {
    return 'success'
  }
  if (status === 'MISSING_COMMENT') {
    return 'warning'
  }
  if (status === 'POSSIBLE_DUPLICATE') {
    return 'info'
  }
  return 'danger'
}

export function formatCoverageRate(rate?: number): string {
  const value = Number.isFinite(rate) ? Number(rate) : 0
  return `${value.toFixed(1)}%`
}

export function filterCoverageFields<T extends FieldCoverageItemLike>(
  tables: FieldCoverageTableLike[],
  tableName: string,
  status: string
): T[] {
  return tables
    .filter((table) => tableName === 'ALL' || table.tableName === tableName)
    .flatMap((table) => (table.fields ?? []) as T[])
    .filter((field) => status === 'ALL' || field.status === status)
}
