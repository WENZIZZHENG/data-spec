export interface DatabaseMetadataBrowserIndexLike {
  indexName?: string
  columnName?: string
  nonUnique?: boolean
}

export interface DatabaseMetadataBrowserColumnLike {
  schemaName?: string
  tableName?: string
  columnName?: string
  dataType?: string
  comment?: string
  standardFieldName?: string
  standardDisplayName?: string
  matchStatus?: string
  matchReason?: string
  candidateKey?: string
  importCandidate?: boolean
  selectedByDefault?: boolean
  missingComment?: boolean
  typeChanged?: boolean
  unmanaged?: boolean
  indexNames?: string[]
}

export interface DatabaseMetadataBrowserTableLike {
  schemaName?: string
  tableName?: string
  comment?: string
  indexes?: DatabaseMetadataBrowserIndexLike[]
  columns?: DatabaseMetadataBrowserColumnLike[]
}

export interface DatabaseMetadataBrowserLike {
  databaseType?: string
  databaseName?: string
  schemaName?: string
  selectedTableNames?: string[]
  aiReadableSummary?: string
  summary?: {
    tableCount?: number
    columnCount?: number
    indexCount?: number
    candidateCount?: number
    missingCommentCount?: number
    changedCount?: number
    unmanagedCount?: number
  }
  tables?: DatabaseMetadataBrowserTableLike[]
}

export interface DatabaseMetadataBrowserRow extends DatabaseMetadataBrowserColumnLike {
  schemaName?: string
  tableName?: string
  tableComment?: string
  indexNames: string[]
}

export function flattenMetadataBrowserRows(browser?: DatabaseMetadataBrowserLike | null): DatabaseMetadataBrowserRow[] {
  return (browser?.tables ?? []).flatMap((table) => {
    const tableIndexNames = table.indexes ?? []
    return (table.columns ?? []).map((column) => ({
      ...column,
      schemaName: column.schemaName ?? table.schemaName,
      tableName: column.tableName ?? table.tableName,
      tableComment: table.comment,
      indexNames: column.indexNames?.length
        ? column.indexNames
        : tableIndexNames
          .filter((index) => sameText(index.columnName, column.columnName))
          .map((index) => index.indexName)
          .filter((indexName): indexName is string => Boolean(indexName))
    }))
  })
}

export function filterMetadataBrowserRows<T extends DatabaseMetadataBrowserRow>(rows: T[], query: string): T[] {
  const keyword = query.trim().toLowerCase()
  if (!keyword) {
    return rows
  }
  return rows.filter((row) =>
    [
      row.schemaName,
      row.tableName,
      row.tableComment,
      row.columnName,
      row.dataType,
      row.comment,
      row.standardFieldName,
      row.standardDisplayName,
      row.matchStatus,
      row.matchReason,
      ...(row.indexNames ?? [])
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  )
}

export function buildBrowserCandidateKeySet(browser?: DatabaseMetadataBrowserLike | null): Set<string> {
  return new Set(
    flattenMetadataBrowserRows(browser)
      .filter((row) => row.importCandidate && row.selectedByDefault)
      .map((row) => row.candidateKey || `${row.tableName ?? ''}.${row.columnName ?? ''}`)
      .filter((key) => key !== '.')
  )
}

export function buildMetadataBrowserAiSummary(browser?: DatabaseMetadataBrowserLike | null): string {
  const backendSummary = redactSensitiveText(browser?.aiReadableSummary ?? '')
  if (backendSummary) {
    return backendSummary
  }
  const summary = browser?.summary
  const lines = [
    'DataSpec database metadata browser',
    `database=${browser?.databaseType ?? '-'} / ${browser?.databaseName ?? '-'} / ${browser?.schemaName ?? '-'}`,
    `summary: tables=${summary?.tableCount ?? 0}, columns=${summary?.columnCount ?? 0}, indexes=${summary?.indexCount ?? 0}, candidates=${summary?.candidateCount ?? 0}`
  ]
  for (const table of browser?.tables ?? []) {
    lines.push(`- table ${table.tableName ?? '-'} ${table.comment ?? ''}`.trim())
    const indexes = (table.indexes ?? []).map((index) => `${index.indexName ?? '-'}(${index.columnName ?? '-'})`)
    if (indexes.length) {
      lines.push(`  indexes: ${indexes.join(', ')}`)
    }
    for (const column of table.columns ?? []) {
      lines.push(`  - ${column.columnName ?? '-'} ${column.dataType ?? '-'} status=${column.matchStatus ?? 'UNKNOWN'}`)
    }
  }
  return redactSensitiveText(lines.join('\n'))
}

export function metadataBrowserStatusLabel(status?: string): string {
  const labels: Record<string, string> = {
    MATCHED: '标准命中',
    CHANGED: '类型差异',
    NEW: '候选字段',
    MISSING_COMMENT: '缺注释',
    NON_STANDARD: '非标准',
    UNMANAGED: '未纳管',
    ALIAS_MATCH: '别名命中',
    STANDARD_MATCH: '标准命中',
    POSSIBLE_DUPLICATE: '疑似重复'
  }
  return status ? labels[status] ?? status : '未知'
}

export function metadataBrowserStatusTagType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'MATCHED' || status === 'STANDARD_MATCH' || status === 'ALIAS_MATCH') {
    return 'success'
  }
  if (status === 'CHANGED' || status === 'MISSING_COMMENT') {
    return 'warning'
  }
  if (status === 'POSSIBLE_DUPLICATE') {
    return 'info'
  }
  return 'danger'
}

function sameText(left?: string, right?: string) {
  return Boolean(left && right && left.toLowerCase() === right.toLowerCase())
}

function redactSensitiveText(text: string): string {
  return text
    .replace(/jdbc:[^\s]+/gi, '[REDACTED]')
    .replace(/password\s*=\s*[^\s,;]+/gi, 'password=[REDACTED]')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, 'Bearer [REDACTED]')
    .replace(/token\s*[:=]\s*[^\s,;]+/gi, 'token=[REDACTED]')
}
