export type AiBatchStatus = 'SUCCESS' | 'PARTIAL_FAILED' | 'FAILED' | string

export function aiBatchStatusLabel(status?: AiBatchStatus): string {
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    PARTIAL_FAILED: '部分失败',
    FAILED: '失败'
  }
  return status ? labels[status] ?? status : '未知'
}

export function aiBatchStatusTagType(status?: AiBatchStatus): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'PARTIAL_FAILED') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

export function buildAiBatchJson(value: unknown): string {
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value ?? {}, null, 2)
}

export function formatAiBatchTime(value?: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}
