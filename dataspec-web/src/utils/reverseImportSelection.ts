export interface DatabaseTableLike {
  schemaName?: string
  tableName?: string
  comment?: string
}

export interface FieldCandidateLike {
  tableName?: string
  columnName?: string
  matchReason?: string
  confidence?: number
  ignoreReason?: string
  confirmReason?: string
}

export interface CandidateGroup<T extends FieldCandidateLike> {
  tableName: string
  candidates: T[]
}

export function filterDatabaseTables<T extends DatabaseTableLike>(tables: T[], query: string): T[] {
  const keyword = query.trim().toLowerCase()
  if (!keyword) {
    return tables
  }
  return tables.filter((table) =>
    [table.schemaName, table.tableName, table.comment]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
}

export function selectAllTableNames(tables: DatabaseTableLike[]): string[] {
  return tables
    .map((table) => table.tableName?.trim())
    .filter((tableName): tableName is string => Boolean(tableName))
}

export function mergeSelectedTableNames(current: string[] | undefined, tables: DatabaseTableLike[]): string[] {
  const selected = new Set((current ?? []).filter(Boolean))
  for (const tableName of selectAllTableNames(tables)) {
    selected.add(tableName)
  }
  return Array.from(selected)
}

/** 统计当前可见表中已选择的数量，避免跨批次累计选择误当成当前页选择。 */
export function countSelectedVisibleTableNames(current: string[] | undefined, tables: DatabaseTableLike[]): number {
  const visible = new Set(selectAllTableNames(tables))
  return (current ?? [])
    .map((tableName) => tableName.trim())
    .filter((tableName) => visible.has(tableName))
    .length
}

export function buildCandidateKey(candidate: FieldCandidateLike): string {
  return `${candidate.tableName ?? ''}.${candidate.columnName ?? ''}`
}

export function groupFieldCandidatesByTable<T extends FieldCandidateLike>(candidates: T[]): CandidateGroup<T>[] {
  const groups = new Map<string, T[]>()
  for (const candidate of candidates) {
    const tableName = candidate.tableName || '未识别表'
    const tableCandidates = groups.get(tableName) ?? []
    tableCandidates.push(candidate)
    groups.set(tableName, tableCandidates)
  }
  return Array.from(groups, ([tableName, tableCandidates]) => ({
    tableName,
    candidates: tableCandidates
  }))
}

export function pickSelectedCandidates<T extends FieldCandidateLike>(
  candidates: T[],
  selectedKeys: Set<string>
): T[] {
  return candidates.filter((candidate) => selectedKeys.has(buildCandidateKey(candidate)))
}

export function defaultCandidateConfirmReason(candidate: FieldCandidateLike): string {
  const current = candidate.confirmReason?.trim()
  if (current) {
    return current
  }
  const reason = candidate.matchReason?.trim()
  if (reason) {
    return reason
  }
  return `确认将 ${candidate.columnName || '该字段'} 作为新标准字段导入`
}

export function defaultCandidateIgnoreReason(candidate: FieldCandidateLike): string {
  return candidate.ignoreReason?.trim() || '本次未选择导入'
}

export function attachCandidateConfirmReasons<T extends FieldCandidateLike>(
  candidates: T[],
  reasonByKey: Record<string, string>
): Array<T & { confirmReason: string }> {
  return candidates.map((candidate) => {
    const key = buildCandidateKey(candidate)
    const reason = reasonByKey[key]?.trim() || defaultCandidateConfirmReason(candidate)
    return { ...candidate, confirmReason: reason }
  })
}

export function buildIgnoredCandidates<T extends FieldCandidateLike>(
  candidates: T[],
  selectedKeys: Set<string>
): Array<T & { ignoreReason: string }> {
  return candidates
    .filter((candidate) => !selectedKeys.has(buildCandidateKey(candidate)))
    .map((candidate) => ({
      ...candidate,
      ignoreReason: defaultCandidateIgnoreReason(candidate)
    }))
}
