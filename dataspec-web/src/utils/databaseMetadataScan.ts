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
  /** 当前扫描页关联的 metadata cache 证据。 */
  metadataCache?: DatabaseMetadataCacheInfoLike
}

/** metadata cache 结构变化摘要的最小前端形状。 */
export interface DatabaseMetadataChangeSummaryLike {
  /** true 表示刷新后相对旧缓存存在结构变化。 */
  changed?: boolean
  /** 新增字段数量。 */
  addedColumnCount?: number
  /** 删除字段数量。 */
  removedColumnCount?: number
  /** 字段属性变化数量。 */
  changedColumnCount?: number
}

/** 前端展示缓存状态所需的最小 metadata cache 证据。 */
export interface DatabaseMetadataCacheInfoLike {
  /** 聚合结构 fingerprint，供 AI 判断是否重跑下游分析。 */
  metadataFingerprint?: string
  /** true 表示结果来自新鲜缓存。 */
  cacheHit?: boolean
  /** true 表示缓存过期或缺失后重新读取源库。 */
  stale?: boolean
  /** 实际缓存策略。 */
  refreshMode?: string
  /** 最近读取源库 metadata 的时间。 */
  lastSeenAt?: string
  /** 当前缓存过期时间。 */
  expiresAt?: string
  /** 源数据库版本摘要。 */
  sourceDatabaseVersion?: string
  /** 结构变化摘要。 */
  changeSummary?: DatabaseMetadataChangeSummaryLike
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

/** 将缓存状态压缩为可扫读的中文标签。 */
export function metadataCacheStatusLabel(cache?: DatabaseMetadataCacheInfoLike | null): string {
  if (!cache) {
    return '无缓存'
  }
  if (cache.refreshMode === 'BYPASS') {
    return '绕过缓存'
  }
  if (cache.cacheHit) {
    return '缓存命中'
  }
  if (cache.refreshMode === 'REFRESH') {
    return '已刷新'
  }
  if (cache.stale) {
    return '缓存已过期'
  }
  return '缓存未命中'
}

/** 构建不含凭据的 cache 摘要，供扫描面板、浏览器和覆盖率报告复用。 */
export function buildMetadataCacheSummary(cache?: DatabaseMetadataCacheInfoLike | null): string {
  if (!cache) {
    return 'metadata cache：暂无'
  }
  const parts = [
    metadataCacheStatusLabel(cache),
    cache.refreshMode ? `mode=${cache.refreshMode}` : '',
    cache.metadataFingerprint ? `fingerprint=${cache.metadataFingerprint.slice(0, 12)}` : '',
    cache.lastSeenAt ? `lastSeenAt=${cache.lastSeenAt}` : '',
    cache.expiresAt ? `expiresAt=${cache.expiresAt}` : ''
  ].filter(Boolean)
  const summary = cache.changeSummary
  if (summary?.changed) {
    parts.push(`新增 ${summary.addedColumnCount ?? 0}`)
    parts.push(`删除 ${summary.removedColumnCount ?? 0}`)
    parts.push(`变更 ${summary.changedColumnCount ?? 0}`)
  }
  if (cache.sourceDatabaseVersion) {
    parts.push(`source=${cache.sourceDatabaseVersion}`)
  }
  return redactSensitiveText(parts.join(' | '))
}

function redactSensitiveText(text: string): string {
  return text
    .replace(/jdbc:[^\s]+/gi, '[REDACTED]')
    .replace(/password\s*=\s*[^\s,;]+/gi, 'password=[REDACTED]')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, 'Bearer [REDACTED]')
    .replace(/token\s*[:=]\s*[^\s,;]+/gi, 'token=[REDACTED]')
}
