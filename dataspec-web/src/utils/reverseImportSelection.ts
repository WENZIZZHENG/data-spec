export interface DatabaseTableLike {
  schemaName?: string
  tableName?: string
  comment?: string
}

export interface FieldCandidateLike {
  tableName?: string
  columnName?: string
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
