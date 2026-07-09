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
  /** 新版采集作业 ID；兼容旧 scanId。 */
  scanJobId?: string
  /** 作业状态：RUNNING/PARTIAL/COMPLETED/CANCELLED/FAILED。 */
  status?: string
  /** 当前连接可见表数量估算。 */
  estimatedTableCount?: number
  /** 下一批 cursor；为空表示无后续批次。 */
  cursor?: string | null
  /** 新版恢复 cursor；为空表示无后续批次。 */
  resumeCursor?: string | null
  /** 新版取消令牌，只用于显式取消动作。 */
  cancelToken?: string
  /** 本次请求实际采用的 pageSize。 */
  pageSize?: number
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
  /** 源库压力提示；不得包含连接凭据。 */
  sourcePressureHint?: DatabaseMetadataScanSourcePressureHintLike
  /** 当前页 schema-only 部分结果。 */
  partialResult?: DatabaseMetadataScanPartialResultLike
  /** 当前页失败摘要。 */
  failureSummary?: DatabaseMetadataScanFailureSummaryLike
  /** 可复制给 AI 的只读证据摘要。 */
  evidence?: DatabaseMetadataScanEvidenceLike
  /** 当前扫描页关联的 metadata cache 证据。 */
  metadataCache?: DatabaseMetadataCacheInfoLike
  /** 兼容旧响应的取消标识；取消后的 partialResult 为空时也必须形成选择边界。 */
  cancelled?: boolean
}

/** 源库压力提示的最小前端形状。 */
export interface DatabaseMetadataScanSourcePressureHintLike {
  /** 压力等级。 */
  level?: string
  /** 脱敏提示文本。 */
  message?: string
  /** 建议下一批 pageSize。 */
  suggestedPageSize?: number
  /** 安全下一步。 */
  safeNextActions?: string[]
}

/** 当前页可用于预览/覆盖率的成功表边界。 */
export interface DatabaseMetadataScanPartialResultLike {
  /** 成功表名。 */
  successfulTableNames?: string[]
  /** 失败表名。 */
  failedTableNames?: string[]
  /** 跳过表名。 */
  skippedTableNames?: string[]
  /** 是否足以生成预览。 */
  completeForPreview?: boolean
  /** 是否足以生成覆盖率。 */
  completeForCoverage?: boolean
  /** 是否完整完成。 */
  complete?: boolean
}

/** 单表失败摘要的最小前端形状。 */
export interface DatabaseMetadataScanFailureItemLike {
  schemaName?: string
  tableName?: string
  category?: string
  retryable?: boolean
  message?: string
}

/** 当前页失败摘要。 */
export interface DatabaseMetadataScanFailureSummaryLike {
  failedTableCount?: number
  retryable?: boolean
  failedTables?: DatabaseMetadataScanFailureItemLike[]
  failureCategories?: string[]
  safeNextActions?: string[]
}

/** 可复制给 AI 的 scan evidence。 */
export interface DatabaseMetadataScanEvidenceLike {
  scanJobId?: string
  status?: string
  processedTableCount?: number
  failedTableCount?: number
  schemaScope?: string
  tableScope?: string[]
  metadataFingerprint?: string
  schemaOnly?: boolean
  noSourceWrites?: boolean
  noStandardWrites?: boolean
  safeForAiCopy?: boolean
  nextActions?: string[]
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

/** 读取成功 partial tables；没有 partialResult 的旧响应回退到当前页 tables。 */
export function currentSuccessfulScanTableNames(scan?: DatabaseMetadataScanResultLike | null): string[] {
  const successful = scan?.partialResult?.successfulTableNames
    ?.map((tableName) => tableName?.trim())
    .filter((tableName): tableName is string => Boolean(tableName)) ?? []
  if (successful.length > 0 || scan?.partialResult) {
    return successful
  }
  return currentScanTableNames(scan)
}

/** 按 scan partial 边界过滤用户选择；已有扫描结果但成功集为空时返回空，避免失败/取消表进入预览或比对。 */
export function selectSuccessfulPartialTableNames(
  selected: string[] | undefined,
  scan?: DatabaseMetadataScanResultLike | null,
  accumulatedSuccessfulTableNames: Iterable<string> = []
): string[] {
  const selectedNames = (selected ?? []).filter(Boolean)
  if (!scan?.partialResult && !scan?.cancelled && scan?.status !== 'CANCELLED') {
    return selectedNames
  }
  const successful = new Set([
    ...Array.from(accumulatedSuccessfulTableNames).filter(Boolean),
    ...currentSuccessfulScanTableNames(scan)
  ])
  return selectedNames.filter((tableName) => successful.has(tableName))
}

/** 合并当前批次表名并保留用户之前选择的表，避免翻页扫描时丢选择。 */
export function mergeScanTableNames(
  current: string[] | undefined,
  scan?: DatabaseMetadataScanResultLike | null
): string[] {
  const selected = new Set((current ?? []).filter(Boolean))
  for (const tableName of currentSuccessfulScanTableNames(scan)) {
    selected.add(tableName)
  }
  return Array.from(selected)
}

/** 将 scan job 状态压缩为页面标签。 */
export function scanJobStatusLabel(scan?: DatabaseMetadataScanResultLike | null): string {
  switch ((scan?.status ?? '').toUpperCase()) {
    case 'RUNNING':
      return '运行中'
    case 'PARTIAL':
      return '部分完成'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已取消'
    case 'FAILED':
      return '失败'
    default:
      return scan?.progress?.hasMore ? '可继续' : '已完成'
  }
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
  const scanJobId = scan?.scanJobId ? `scanJobId=${scan.scanJobId} ` : ''
  const cursor = scan?.resumeCursor ?? scan?.cursor ?? 'DONE'
  const pageSize = scan?.pageSize ?? scan?.progress?.pageSize ?? 0
  return redactSensitiveText(`${scanJobId}resumeCursor=${cursor} pageSize=${pageSize}`)
}

/** 构建源库压力提示摘要，统一脱敏。 */
export function buildSourcePressureHintText(scan?: DatabaseMetadataScanResultLike | null): string {
  const hint = scan?.sourcePressureHint
  if (!hint) {
    return ''
  }
  const actions = hint.safeNextActions?.filter(Boolean).join('；') ?? ''
  return redactSensitiveText([hint.message, actions].filter(Boolean).join(' | '))
}

/** 构建失败摘要，避免失败表被静默忽略。 */
export function buildScanFailureSummary(scan?: DatabaseMetadataScanResultLike | null): string {
  const summary = scan?.failureSummary
  if (!summary || (summary.failedTableCount ?? 0) === 0) {
    return ''
  }
  const examples = (summary.failedTables ?? [])
    .map((item) => [
      item.schemaName ? `${item.schemaName}.` : '',
      item.tableName ?? '-',
      item.category ? ` ${item.category}` : '',
      item.retryable ? ' retryable' : '',
      item.message ? ` ${item.message}` : ''
    ].join(''))
    .join('；')
  const actions = summary.safeNextActions?.join('；') ?? ''
  return redactSensitiveText(`失败 ${summary.failedTableCount} 张表：${examples}${actions ? ` | ${actions}` : ''}`)
}

/** 构建 AI 可读 evidence 摘要，保留安全边界。 */
export function buildScanEvidenceSummary(scan?: DatabaseMetadataScanResultLike | null): string {
  const evidence = scan?.evidence
  if (!evidence) {
    return ''
  }
  const parts = [
    evidence.scanJobId ? `scanJobId=${evidence.scanJobId}` : '',
    evidence.status ? `status=${evidence.status}` : '',
    typeof evidence.processedTableCount === 'number' ? `processed=${evidence.processedTableCount}` : '',
    typeof evidence.failedTableCount === 'number' ? `failed=${evidence.failedTableCount}` : '',
    evidence.metadataFingerprint ? `fingerprint=${evidence.metadataFingerprint.slice(0, 12)}` : '',
    `schemaOnly=${Boolean(evidence.schemaOnly)}`,
    `noSourceWrites=${Boolean(evidence.noSourceWrites)}`,
    `noStandardWrites=${Boolean(evidence.noStandardWrites)}`,
    `safeForAiCopy=${Boolean(evidence.safeForAiCopy)}`,
    ...(evidence.nextActions ?? [])
  ].filter(Boolean)
  return redactSensitiveText(parts.join(' | '))
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
    .replace(/\b(?:postgres|postgresql|mysql):\/\/[^\s,;]+/gi, '[REDACTED]')
    .replace(/\bdsn\s*[:=]\s*[^\s,;]+/gi, 'dsn=[REDACTED]')
    .replace(/Authorization\s*[:=]\s*[^\s,;]+(?:\s+[^\s,;]+)?/gi, '[REDACTED]')
    .replace(/password\s*=\s*[^\s,;]+/gi, 'password=[REDACTED]')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/gi, 'Bearer [REDACTED]')
    .replace(/token\s*[:=]\s*[^\s,;]+/gi, 'token=[REDACTED]')
}
