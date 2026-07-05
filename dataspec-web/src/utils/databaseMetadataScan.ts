/** 当前扫描页的表级 metadata 子集；不包含列 metadata 或业务数据行。 */
export interface DatabaseMetadataScanTableLike {
  /** 表所在 schema；MySQL 场景可能为空。 */
  schemaName?: string
  /** 当前页返回的表名。 */
  tableName?: string
  /** 表注释，仅用于前端筛选和展示。 */
  comment?: string
}

/** 前端扫描工具依赖的最小响应形状，用于兼容 API 类型和测试 fixture。 */
export interface DatabaseMetadataScanResultLike {
  /** 当前连接可见表数量估算。 */
  estimatedTableCount?: number
  /** 下一批 cursor；为空表示无后续批次。 */
  cursor?: string | null
  /** 面向 AI 的脱敏恢复提示。 */
  resumeCommand?: string
  /** 当前页表级 metadata。 */
  tables?: DatabaseMetadataScanTableLike[]
  /** 当前扫描进度和分页状态。 */
  progress?: {
    /** 已处理表数量。 */
    processedTableCount?: number
    /** 剩余表数量估算。 */
    remainingTableEstimate?: number
    /** 本次请求采用的分页大小。 */
    pageSize?: number
    /** 是否还有下一批。 */
    hasMore?: boolean
  }
}

/** 读取当前扫描页的有效表名，避免空表名进入批次选择。 */
export function currentScanTableNames(scan?: DatabaseMetadataScanResultLike | null): string[] {
  return (scan?.tables ?? [])
    .map((table) => table.tableName?.trim())
    .filter((tableName): tableName is string => Boolean(tableName))
}

/** 合并当前批次表名并保留用户之前选择的表，避免翻页扫描时丢选择。 */
export function mergeScanTableNames(
  current: string[] | undefined,
  scan?: DatabaseMetadataScanResultLike | null
): string[] {
  const selected = new Set((current ?? []).filter(Boolean))
  for (const tableName of currentScanTableNames(scan)) {
    selected.add(tableName)
  }
  return Array.from(selected)
}

/** 生成人类和 AI 都能读懂的扫描进度摘要。 */
export function scanProgressLabel(scan?: DatabaseMetadataScanResultLike | null): string {
  const processed = scan?.progress?.processedTableCount ?? 0
  const estimated = scan?.estimatedTableCount ?? processed
  const remaining = scan?.progress?.remainingTableEstimate ?? Math.max(0, estimated - processed)
  if (remaining > 0) {
    return `已扫描 ${processed} / ${estimated}，剩余约 ${remaining}`
  }
  return `已扫描 ${processed} / ${estimated}`
}

/** 返回脱敏后的恢复提示；缺少服务端命令时降级到 cursor/pageSize 摘要。 */
export function buildScanResumeSummary(scan?: DatabaseMetadataScanResultLike | null): string {
  const command = redactSensitiveText(scan?.resumeCommand ?? '')
  if (command) {
    return command
  }
  const cursor = scan?.cursor ?? 'DONE'
  const pageSize = scan?.progress?.pageSize ?? 0
  return `cursor=${cursor} pageSize=${pageSize}`
}

function redactSensitiveText(text: string): string {
  return text
    .replace(/jdbc:[^\s]+/gi, '[REDACTED]')
    .replace(/password\s*=\s*[^\s,;]+/gi, 'password=[REDACTED]')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, 'Bearer [REDACTED]')
    .replace(/token\s*[:=]\s*[^\s,;]+/gi, 'token=[REDACTED]')
}
