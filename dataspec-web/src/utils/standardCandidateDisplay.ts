import type { StandardCandidate } from '@/types'

export type CandidateTagType = 'success' | 'warning' | 'danger' | 'info'

export function standardCandidateStatusLabel(status?: string): string {
  const labels: Record<string, string> = {
    PENDING: '待处理',
    ACCEPTED: '已采纳',
    MERGED: '已合并',
    IGNORED: '已忽略',
    POSTPONED: '已延后'
  }
  return labels[status ?? ''] ?? (status || '-')
}

export function standardCandidateStatusTag(status?: string): CandidateTagType {
  if (status === 'ACCEPTED' || status === 'MERGED') {
    return 'success'
  }
  if (status === 'IGNORED') {
    return 'info'
  }
  if (status === 'POSTPONED') {
    return 'warning'
  }
  return 'danger'
}

export function standardCandidateSourceLabel(sourceType?: string): string {
  const labels: Record<string, string> = {
    MANUAL: '手动',
    COVERAGE: '覆盖率',
    REVERSE_IMPORT: '反向导入',
    AI_FEEDBACK: 'AI 反馈'
  }
  return labels[sourceType ?? ''] ?? (sourceType || '-')
}

export function isStandardCandidateDecidable(candidate: StandardCandidate): boolean {
  return candidate.status === 'PENDING' || candidate.status === 'POSTPONED'
}

export function formatCandidateEvidence(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.length > 120 ? `${value.slice(0, 120)}...` : value
}
