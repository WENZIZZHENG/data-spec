import type { AiEvidencePackageReq, EvidenceSourceType } from '@/types'

const SECRET_KEY_PATTERN = /(password|passwd|pwd|token|Authorization|authorization|bearer|secret|api[_-]?key|apikey|dsn|connectionstring|connection[_-]?string)/i
const DATABASE_URI_PATTERN = /\b(postgresql|postgres|mysql|mariadb|mongodb|redis|sqlserver|oracle):\/\/[^\s"',}<>]+/gi

export interface HandoffEvidenceSource {
  /** 证据包来源类型，必须沿用现有 evidence package API 支持的枚举。 */
  sourceType: EvidenceSourceType
  /** 证据源 ID；第一版只对已持久化来源生成证据包。 */
  sourceId?: number
  /** 用户可读标题，不包含 SQL 原文、payload 或凭据。 */
  sourceTitle?: string
  /** 来源当前状态，用于把失败或未验证项显式标红。 */
  status?: string
  /** 来源创建时间，展示层只做轻量格式化。 */
  createdAt?: string
  /** 任务或作业类型。 */
  taskType?: string
  /** 下一步建议，展示前会脱敏。 */
  nextAction?: string
  /** 附加摘要，展示前会脱敏。 */
  description?: string
}

/**
 * 构造 evidence package 只读请求。
 *
 * 前端不把 raw SQL、payload、token 或连接串塞进 payloadSummary；证据内容统一交给既有后端生成和脱敏。
 */
export function buildEvidenceRequest(
  projectId: number | null | undefined,
  source: HandoffEvidenceSource | null | undefined
): AiEvidencePackageReq | null {
  if (!projectId || !source?.sourceId) {
    return null
  }
  return {
    projectId,
    sourceType: source.sourceType,
    sourceId: source.sourceId,
    sourceTitle: sanitizeHandoffText(source.sourceTitle ?? evidenceSourceTypeLabel(source.sourceType))
  }
}

export function evidenceSourceTypeLabel(sourceType?: string) {
  const labels: Record<string, string> = {
    AI_TASK_RUN: 'AI task run',
    AI_JOB: 'AI 作业',
    SQL_CHECK: 'SQL 检查',
    AI_BATCH_RUN: 'AI 批量任务',
    COVERAGE_REPORT: '覆盖率报告'
  }
  return sourceType ? labels[sourceType] ?? sourceType : '-'
}

export function handoffStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    PASS: '通过',
    COMPLETED: '完成',
    DONE: '完成',
    RUNNING: '运行中',
    PENDING: '等待中',
    PARTIAL_FAILED: '部分失败',
    FAILED: '失败',
    ERROR: '错误',
    BLOCKED: '阻塞',
    CANCELLED: '已取消'
  }
  return status ? labels[status] ?? status : '未验证'
}

export function handoffStatusTagType(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (['SUCCESS', 'PASS', 'COMPLETED', 'DONE'].includes(status ?? '')) {
    return 'success'
  }
  if (['PARTIAL_FAILED', 'RUNNING', 'PENDING'].includes(status ?? '')) {
    return 'warning'
  }
  if (['FAILED', 'ERROR', 'BLOCKED', 'CANCELLED'].includes(status ?? '')) {
    return 'danger'
  }
  return 'info'
}

export function formatHandoffTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').replace(/\.\d+Z?$/, '').slice(0, 19)
}

/**
 * 生成前端可复制 JSON，复制前再次做防御性脱敏。
 *
 * 后端 evidence package 已有脱敏边界；这里防止未来新增字段或浏览器侧拼接摘要时误带入敏感文本。
 */
export function buildHandoffEvidenceJson(value: unknown): string {
  return JSON.stringify(redactValue(value), null, 2)
}

export function sanitizeHandoffText(value: string) {
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/]*@/gi, '$1')
    .replace(/jdbc:[^\s"',}<>]+/gi, 'jdbc:***')
    .replace(DATABASE_URI_PATTERN, '$1://***')
    .replace(/(Authorization|authorization)(\s*[:=]\s*)(Digest\s+[^;\r\n}]+|(?:Bearer|Basic|ApiKey|API-Key|Token)\s+[^"',;}\]\r\n]+)/gi, '$1$2***')
    .replace(/(Authorization|authorization)(\s*[:=]\s*)[^\s"',;}\]\r\n]+/gi, '$1$2***')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(secretAssignmentPattern(), (_match, prefix, doubleQuoted, singleQuoted) => {
      if (doubleQuoted !== undefined) {
        return `${prefix}"***"`
      }
      if (singleQuoted !== undefined) {
        return `${prefix}'***'`
      }
      return `${prefix}***`
    })
}

function redactValue(value: unknown): unknown {
  if (typeof value === 'string') {
    return sanitizeHandoffText(value)
  }
  if (Array.isArray(value)) {
    return value.map(redactValue)
  }
  if (!value || typeof value !== 'object') {
    return value
  }
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, item]) => [
      key,
      SECRET_KEY_PATTERN.test(key) ? '***' : redactValue(item)
    ])
  )
}

function secretAssignmentPattern(): RegExp {
  return /((?:"|')?\b(?:passwords?|passwds?|pwds?|tokens?|authorizations?|bearers?|api[_-]?tokens?|dataspec[_-]?tokens?|api[_-]?keys?|secrets?|client[_-]?secrets?|access[_-]?tokens?|refresh[_-]?tokens?|plain[_-]?tokens?|token[_-]?hash(?:es)?|jdbc[_-]?urls?|connection[_-]?strings?|dsns?)\b(?:"|')?\s*[:=]\s*)(?:"([^"]*)"|'([^']*)'|[^\s"',;}&]+)/gi
}
