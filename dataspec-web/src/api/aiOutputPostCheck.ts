import request from '@/api/request'

/** AI 输出后置校验支持的内容类型；TEXT 是外部稳定纯文本值，PLAIN_TEXT 仅作为旧输入兼容。 */
export type AiOutputPostCheckContentType = 'SQL' | 'DDL' | 'MARKDOWN' | 'JSON' | 'TEXT' | 'PLAIN_TEXT'

/** 发送给后端和 CLI/MCP 的稳定内容类型集合。 */
export type StableAiOutputPostCheckContentType = Exclude<AiOutputPostCheckContentType, 'PLAIN_TEXT'>

/** AI 输出后置校验状态；WARN/FAIL 都表示不应直接使用产物。 */
export type AiOutputPostCheckStatus = 'PASS' | 'WARN' | 'FAIL' | string

/** AI 输出后置校验请求；content 只用于本次只读检查，不写入项目或业务文件。 */
export interface AiOutputPostCheckReq {
  /** DataSpec 项目 ID。 */
  projectId: number
  /** AI 产物类型。 */
  contentType: AiOutputPostCheckContentType
  /** 待校验的 AI 产物正文；后端会限制大小并脱敏诊断输出。 */
  content: string
  /** 可选标准快照 stableRef，用于识别旧快照引用。 */
  snapshotRef?: string
}

/** 后置校验发现的单个引用问题。 */
export interface AiOutputPostCheckIssue {
  /** 稳定问题码，如 UNKNOWN_STANDARD_REFERENCE。 */
  code?: string
  /** 问题严重级别。 */
  severity?: string
  /** 原始引用或有界脱敏摘录。 */
  inputRef?: string
  /** 当前稳定引用。 */
  stableRef?: string
  /** 当前 canonicalRef 或建议替代引用。 */
  replacementRef?: string
  /** 人类可读问题说明，必须由服务端保持脱敏。 */
  message?: string
  /** true 表示该问题阻止安全使用 AI 产物。 */
  blocking?: boolean
}

/** AI 输出后置校验结果，供前端复制门禁、CLI/MCP 和证据包共用。 */
export interface AiOutputPostCheckResult {
  /** 响应类型标识。 */
  kind?: string
  /** 响应 schema 版本。 */
  schemaVersion?: number
  /** DataSpec 项目 ID。 */
  projectId?: number
  /** PASS/WARN/FAIL 状态。 */
  status?: AiOutputPostCheckStatus
  /** true 表示可继续复制、应用或执行产物。 */
  safeToUse?: boolean
  /** 服务端聚合摘要，字段保持 additive 兼容。 */
  summary?: Record<string, unknown>
  /** 引用问题列表。 */
  issues?: AiOutputPostCheckIssue[]
  /** 已解析引用列表。 */
  resolvedRefs?: Record<string, unknown>[]
  /** 服务端返回的建议修复文本列表。 */
  suggestedFixes?: string[]
  /** 服务端返回的证据链接或证据说明列表。 */
  evidenceLinks?: string[]
  /** 下一步动作建议。 */
  nextActions?: string[]
}

/** 将旧 PLAIN_TEXT 输入收束为外部稳定 TEXT，避免前端、CLI 和 MCP 再暴露分裂协议。 */
export function normalizeAiOutputPostCheckContentType(
  contentType: AiOutputPostCheckContentType
): StableAiOutputPostCheckContentType {
  return contentType === 'PLAIN_TEXT' ? 'TEXT' : contentType
}

/** 对 AI 产物执行只读后置校验。 */
export function checkAiOutput(data: AiOutputPostCheckReq) {
  return request.post<unknown, AiOutputPostCheckResult>('/ai-output/check', {
    ...data,
    contentType: normalizeAiOutputPostCheckContentType(data.contentType)
  })
}
