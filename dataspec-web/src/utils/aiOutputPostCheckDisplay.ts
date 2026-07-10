import type {
  AiOutputPostCheckContentType,
  AiOutputPostCheckIssue,
  AiOutputPostCheckResult
} from '@/api/aiOutputPostCheck'

/** 前端可提交给 post-check 的正文和内容类型。 */
export interface AiOutputPostCheckInput {
  /** 内容类型。 */
  contentType: AiOutputPostCheckContentType
  /** 待校验正文。 */
  content: string
}

/** Element Plus tag 类型，用于表达 post-check 状态。 */
export function aiOutputPostCheckTagType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'PASS') {
    return 'success'
  }
  if (status === 'WARN') {
    return 'warning'
  }
  if (status === 'FAIL') {
    return 'danger'
  }
  return 'info'
}

/** 用于列表和详情页的短状态摘要。 */
export function aiOutputPostCheckSummary(result?: AiOutputPostCheckResult | null): string {
  if (!result) {
    return '未校验'
  }
  const issueCount = result.issues?.length ?? numberFromSummary(result.summary, 'issueCount') ?? 0
  const resolvedCount = result.resolvedRefs?.length ?? numberFromSummary(result.summary, 'totalRefCount') ?? 0
  return `${result.status ?? 'UNKNOWN'} / 问题 ${issueCount} / 已解析 ${resolvedCount}`
}

/** 提取阻断性引用，优先使用服务端显式 blocking 标记。 */
export function blockingPostCheckRefs(result?: AiOutputPostCheckResult | null): string[] {
  return uniqueRefs((result?.issues ?? [])
    .filter((issue) => Boolean(issue.blocking) || issue.severity === 'FAIL' || isBlockingCode(issue.code))
    .map((issue) => issue.inputRef ?? issue.stableRef))
}

/** 提取替代引用，供用户判断该用哪个 canonicalRef。 */
export function replacementPostCheckRefs(result?: AiOutputPostCheckResult | null): string[] {
  return uniqueRefs((result?.issues ?? []).map((issue) => issue.replacementRef))
}

/** 根据 AI job 输出 payload 选择最适合 post-check 的正文。 */
export function buildPostCheckInputFromPayload(payload: unknown): AiOutputPostCheckInput {
  if (typeof payload === 'string') {
    return { contentType: inferContentTypeFromText(payload), content: payload }
  }
  if (isRecord(payload)) {
    const ddl = stringField(payload, 'ddl')
    if (ddl) {
      return { contentType: 'DDL', content: ddl }
    }
    const fixedSql = stringField(payload, 'fixedSql')
    if (fixedSql) {
      return { contentType: 'SQL', content: fixedSql }
    }
    const sql = stringField(payload, 'sql')
    if (sql) {
      return { contentType: 'SQL', content: sql }
    }
  }
  return { contentType: 'JSON', content: JSON.stringify(payload ?? {}, null, 2) }
}

/** 构造可复制的 CLI post-check 命令；filePath 使用占位符时不会包含本机敏感路径。 */
export function buildAiOutputPostCheckCommand(params: {
  projectId?: number | null
  contentType?: AiOutputPostCheckContentType
  filePath?: string
  snapshotRef?: string
}): string {
  const project = params.projectId ?? '<projectId>'
  const type = normalizeCommandContentType(params.contentType ?? 'SQL')
  const filePath = params.filePath ?? '<generated-output>'
  const snapshot = params.snapshotRef ? ` --snapshot-ref ${params.snapshotRef}` : ''
  return `node tools/dataspec-cli.mjs ai-output check --project ${project} --type ${type} --file ${filePath}${snapshot} --format json`
}

/** 用项目和 AI job 快照信息推导 snapshot stableRef。 */
export function buildSnapshotRef(projectId?: number | null, snapshotId?: number, snapshotVersion?: string): string | undefined {
  if (!projectId) {
    return undefined
  }
  if (typeof snapshotId === 'number' && Number.isFinite(snapshotId) && snapshotId > 0) {
    return `snapshot:${projectId}:${snapshotId}`
  }
  if (snapshotVersion && snapshotVersion.trim()) {
    return `snapshot:${projectId}:${snapshotVersion.trim()}`
  }
  return undefined
}

function numberFromSummary(summary: Record<string, unknown> | undefined, key: string): number | undefined {
  const value = summary?.[key]
  return typeof value === 'number' ? value : undefined
}

function isBlockingCode(code?: string): boolean {
  return Boolean(code && /UNKNOWN|INVALID|INCOMPATIBLE|UNSAFE/i.test(code))
}

function uniqueRefs(values: Array<string | undefined>): string[] {
  return [...new Set(values.map((value) => value?.trim()).filter((value): value is string => Boolean(value)))]
}

function normalizeCommandContentType(contentType: AiOutputPostCheckContentType): Exclude<AiOutputPostCheckContentType, 'PLAIN_TEXT'> {
  return contentType === 'PLAIN_TEXT' ? 'TEXT' : contentType
}

function inferContentTypeFromText(text: string): AiOutputPostCheckContentType {
  const trimmed = text.trim()
  if (/^create\s+(table|index)|^alter\s+table|^drop\s+/i.test(trimmed)) {
    return 'DDL'
  }
  if (/^(select|insert|update|delete|with)\b/i.test(trimmed)) {
    return 'SQL'
  }
  if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
    return 'JSON'
  }
  if (/^#|\n#{1,6}\s/.test(trimmed)) {
    return 'MARKDOWN'
  }
  return 'TEXT'
}

function stringField(value: Record<string, unknown>, key: string): string | undefined {
  const field = value[key]
  return typeof field === 'string' && field.trim() ? field : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
