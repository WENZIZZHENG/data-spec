import type {
  DatabaseCommentPatchPlan,
  DatabaseCommentPatchPlanItem,
  DatabaseCommentPlanStatus,
  DatabaseCommentRiskLevel
} from '@/types'

type TagType = 'success' | 'warning' | 'danger' | 'info'

export interface CommentPlanTableGroup {
  tableName: string
  items: DatabaseCommentPatchPlanItem[]
}

const SECRET_PATTERNS = [
  /jdbc:[^\s"'`]+/gi,
  /\bhttps?:\/\/[^\s/?#@]+@/gi,
  /\b(?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/[^\s"'`]+/gi,
  /authorization\s*[:=]\s*bearer\s+[^\s,;]+/gi,
  /authorization\s*[:=]\s*(?:basic|apikey|api-key|token|digest)\s+[^\s"',;}&]+/gi,
  /authorization\s*[:=]\s*[^\s"',;}&]+/gi,
  /bearer\s+[A-Za-z0-9._~+/=-]+/gi,
  /(?:password|passwd|pwd|token|api[_-]?key|secret|dsn|connection[_-]?string)\s*[:=]\s*[^\s"',;}&]+/gi
]

/** 将 COMMENT plan 风险等级转成前端展示文案。 */
export function commentPlanRiskLabel(risk?: DatabaseCommentRiskLevel | string): string {
  switch ((risk ?? '').toUpperCase()) {
    case 'SAFE':
      return '安全'
    case 'LOW':
      return '低风险'
    case 'MEDIUM':
      return '需确认'
    case 'HIGH':
      return '高风险'
    default:
      return risk || '未知'
  }
}

/** 将 COMMENT plan 风险等级转成 Element Plus tag 类型。 */
export function commentPlanRiskTagType(risk?: DatabaseCommentRiskLevel | string): TagType {
  switch ((risk ?? '').toUpperCase()) {
    case 'SAFE':
    case 'LOW':
      return 'success'
    case 'MEDIUM':
      return 'warning'
    case 'HIGH':
      return 'danger'
    default:
      return 'info'
  }
}

/** 将 COMMENT plan 状态转成用户可读文案。 */
export function commentPlanStatusLabel(status?: DatabaseCommentPlanStatus | string): string {
  switch ((status ?? '').toUpperCase()) {
    case 'NO_OP':
      return '无需变更'
    case 'MISSING':
      return '缺少注释'
    case 'CHANGED':
      return '注释变化'
    case 'UNSUPPORTED':
      return '不支持'
    default:
      return status || '未知'
  }
}

/** 按表分组 COMMENT plan items，保持后端返回顺序，便于反向导入页折叠展示。 */
export function buildCommentPlanTableGroups(plan?: DatabaseCommentPatchPlan | null): CommentPlanTableGroup[] {
  const groups = new Map<string, DatabaseCommentPatchPlanItem[]>()
  for (const item of plan?.items ?? []) {
    const tableName = item.tableName || '未命名表'
    groups.set(tableName, [...(groups.get(tableName) ?? []), item])
  }
  return [...groups.entries()].map(([tableName, items]) => ({ tableName, items }))
}

/** 构建可复制给 AI 或人工评审的 COMMENT plan 文本，复制前再次做前端侧脱敏。 */
export function buildCommentPlanCopyPayload(plan?: DatabaseCommentPatchPlan | null): string {
  if (!plan) {
    return ''
  }
  const summary = plan.summary ?? {}
  const lines = [
    'DataSpec COMMENT patch plan',
    `kind=${plan.kind ?? '-'}`,
    `database=${plan.databaseType ?? '-'} / ${plan.databaseName ?? '-'} / ${plan.schemaName ?? '-'}`,
    `risk=${plan.riskLevel ?? '-'}`,
    `metadataFingerprint=${(plan.metadataFingerprint ?? '-').slice(0, 12)}`,
    `planHash=${(plan.planHash ?? '-').slice(0, 12)}`,
    `summary: tables=${summary.tableCount ?? 0}, columns=${summary.columnCount ?? 0}, executable=${summary.executableChangeCount ?? 0}, unsupported=${summary.unsupportedCount ?? 0}, blocked=${summary.blockedCount ?? 0}`
  ]
  for (const item of plan.items ?? []) {
    lines.push(`- ${item.tableName ?? '-'}.${item.columnName ?? '(table)'} status=${item.status ?? '-'} risk=${item.riskLevel ?? '-'}`)
    if (item.standardFieldName) {
      lines.push(`  standard=${item.standardFieldName}`)
    }
    if (item.commentDiff) {
      lines.push(`  diff=${item.commentDiff}`)
    }
    if (item.dryRunSql) {
      lines.push(`  dryRunSql=${item.dryRunSql}`)
    }
    if (item.blockedReasons?.length) {
      lines.push(`  blocked=${item.blockedReasons.join('; ')}`)
    }
  }
  if (plan.dryRunSql) {
    lines.push('dryRunSql:')
    lines.push(plan.dryRunSql)
  }
  if (plan.rollbackHint) {
    lines.push(`rollbackHint=${plan.rollbackHint}`)
  }
  for (const action of plan.nextActions ?? []) {
    lines.push(`next=${action}`)
  }
  return redactCommentPlanText(lines.join('\n'))
}

function redactCommentPlanText(value: string): string {
  return SECRET_PATTERNS.reduce((text, pattern) => text.replace(pattern, '[REDACTED]'), value)
}
