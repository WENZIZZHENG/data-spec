export type AiJobStatus = 'SUCCESS' | 'FAILED' | string

export function aiJobTypeLabel(type?: string): string {
  const labels: Record<string, string> = {
    CREATE_TABLE_PROMPT: '建表 Prompt',
    FIX_SQL_PROMPT: '修 SQL Prompt',
    SQL_LINT_FIX: 'SQL 检查修正',
    DDL_PREVIEW: 'DDL 预览'
  }
  return type ? labels[type] ?? type : '未知'
}

export function aiJobStatusTagType(status?: AiJobStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

export function buildReplayJson(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value ?? {}, null, 2)
}

export function formatAiJobTime(value?: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}
