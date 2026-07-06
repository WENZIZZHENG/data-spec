import type { DatabaseSchemaChangeAction, DatabaseSchemaChangePlan, DatabaseSchemaRiskLevel } from '@/types'

type TagType = 'success' | 'warning' | 'danger' | 'info'

const SECRET_PATTERNS = [
  /jdbc:[^\s"'`]+/gi,
  /password\s*=\s*[^,\s;]+/gi,
  /Bearer\s+[A-Za-z0-9._~+/=-]+/gi,
  /token\s*=\s*[^,\s;]+/gi
]

/** 将 schema plan 风险等级转成前端展示文案。 */
export function schemaRiskLabel(risk?: DatabaseSchemaRiskLevel | string): string {
  switch ((risk ?? '').toUpperCase()) {
    case 'SAFE':
      return '安全'
    case 'LOW':
      return '低风险'
    case 'MEDIUM':
      return '需确认'
    case 'HIGH':
      return '高风险'
    case 'BLOCKED':
      return '阻塞'
    default:
      return risk || '未知'
  }
}

/** 将 schema plan 风险等级转成 Element Plus tag 类型。 */
export function schemaRiskTagType(risk?: DatabaseSchemaRiskLevel | string): TagType {
  switch ((risk ?? '').toUpperCase()) {
    case 'SAFE':
    case 'LOW':
      return 'success'
    case 'MEDIUM':
      return 'warning'
    case 'HIGH':
    case 'BLOCKED':
      return 'danger'
    default:
      return 'info'
  }
}

/** 将 schema plan action 转成用户可读文案。 */
export function schemaChangeActionLabel(action?: DatabaseSchemaChangeAction | string): string {
  switch ((action ?? '').toUpperCase()) {
    case 'ALTER_COMMENT':
      return '注释修正'
    case 'ALTER_COLUMN':
      return '结构调整'
    case 'DROP_CANDIDATE':
      return '删除候选'
    default:
      return action || '未知动作'
  }
}

/** 构建适合复制给 AI 的 schema plan 摘要；摘要会脱敏连接串、password 和 token 类文本。 */
export function buildSchemaPlanAiSummary(plan?: DatabaseSchemaChangePlan | null): string {
  if (!plan) {
    return ''
  }
  const summary = plan.summary ?? {}
  const lines = [
    'DataSpec schema change plan',
    `database=${plan.databaseType ?? '-'} / ${plan.databaseName ?? '-'} / ${plan.schemaName ?? '-'}`,
    `risk=${plan.riskLevel ?? '-'}`,
    `currentSchemaHash=${(plan.currentSchemaHash ?? '-').slice(0, 12)}`,
    `targetSpecHash=${(plan.targetSpecHash ?? '-').slice(0, 12)}`,
    `summary: tables=${summary.tableCount ?? 0}, columns=${summary.columnCount ?? 0}, changes=${summary.changeCount ?? 0}, blocked=${summary.blockedCount ?? 0}`
  ]
  for (const item of plan.changeSet ?? []) {
    lines.push(`- ${item.tableName ?? '-'}.${item.columnName ?? '-'} action=${item.action ?? '-'} risk=${item.riskLevel ?? '-'}`)
    if (item.property) {
      lines.push(`  property=${item.property}`)
    }
    if (item.blockedReasons?.length) {
      lines.push(`  blocked=${item.blockedReasons.join('; ')}`)
    }
  }
  for (const action of plan.nextActions ?? []) {
    lines.push(`next=${action}`)
  }
  return redactSchemaPlanText(lines.join('\n'))
}

function redactSchemaPlanText(value: string): string {
  return SECRET_PATTERNS.reduce((text, pattern) => text.replace(pattern, '[REDACTED]'), value)
}
