import type {
  StandardCandidate,
  TokenEvidenceCandidateApplyResult,
  TokenEvidenceCandidateApplyReq,
  TokenEvidenceCandidatePreview,
  TokenEvidenceCandidatePreviewReq,
  TokenEvidenceCandidatePreviewStatus,
  TokenEvidenceCandidateSignalType
} from '@/types'

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
    AI_FEEDBACK: 'AI 反馈',
    TOKEN_EVIDENCE: '命名证据'
  }
  return labels[sourceType ?? ''] ?? (sourceType || '-')
}

/** 返回命名证据预览状态的用户可见标签。 */
export function tokenEvidencePreviewStatusLabel(status?: TokenEvidenceCandidatePreviewStatus): string {
  const labels: Record<TokenEvidenceCandidatePreviewStatus, string> = {
    READY: '可以写入候选',
    NO_ACTIONABLE_SIGNAL: '没有可处理的命名信号',
    STANDARD_EXISTS: '标准字段已存在',
    EXACT_DUPLICATE: '相同来源候选已存在',
    NAME_CONFLICT: '存在同名候选'
  }
  return status ? labels[status] : '等待预览'
}

/** 返回命名证据信号的业务标签。 */
export function tokenEvidenceSignalLabel(signalType?: TokenEvidenceCandidateSignalType): string {
  const labels: Record<TokenEvidenceCandidateSignalType, string> = {
    UNKNOWN_TERM: '未知业务词',
    AMBIGUOUS_ABBREVIATION: '歧义缩写',
    DISABLED_NAMING: '禁用命名'
  }
  return signalType ? labels[signalType] : '-'
}

/** 返回 token 解析状态的用户可见标签。 */
export function tokenEvidenceResolutionLabel(status?: string): string {
  const labels: Record<string, string> = {
    RESOLVED: '已解析',
    AMBIGUOUS: '有歧义',
    DISABLED: '已禁用',
    UNRESOLVED: '未解析'
  }
  return labels[status ?? ''] ?? (status || '-')
}

/** 只有 READY 预览、有效 token 和显式确认同时满足时才构造 apply 请求。 */
export function buildTokenEvidenceCandidateApplyReq(
  previewInput: TokenEvidenceCandidatePreviewReq,
  preview: TokenEvidenceCandidatePreview | null | undefined,
  confirmed: boolean
): TokenEvidenceCandidateApplyReq | null {
  if (!confirmed || preview?.status !== 'READY' || !preview.dryRunToken) {
    return null
  }
  return {
    previewInput,
    dryRunToken: preview.dryRunToken,
    confirmed: true
  }
}

/** 命名证据 apply 响应的页面上下文，用于拒绝项目切换或弹窗重开后的迟到结果。 */
export interface TokenEvidenceCandidateApplyGuard {
  /** 当前 apply 请求代次。 */
  requestId: number
  /** 组件仍认可的最新 apply 请求代次。 */
  currentRequestId: number
  /** 发起 apply 时绑定的项目 ID。 */
  requestedProjectId: number
  /** 页面当前选中的项目 ID。 */
  currentProjectId?: number | null
  /** 原弹窗是否仍处于打开状态。 */
  dialogVisible: boolean
}

/** 标准候选列表响应写回前必须匹配的请求代际和项目查询快照。 */
export interface StandardCandidateListResultContext {
  /** 发起请求时的递增代际。 */
  requestId: number
  /** 当前最后一次候选列表请求代际。 */
  currentRequestId: number
  /** 发起请求时的项目 ID。 */
  requestedProjectId: number
  /** 响应返回时的当前项目 ID。 */
  currentProjectId?: number | null
  /** 发起请求时由项目、筛选、关键词和分页组成的稳定查询快照。 */
  requestedQueryKey: string
  /** 响应返回时重新计算的当前查询快照。 */
  currentQueryKey: string
}

/** 仅允许仍属于当前弹窗和当前项目的 apply 结果触发成功提示与列表刷新。 */
export function shouldHandleTokenEvidenceCandidateApplyResult(
  result: TokenEvidenceCandidateApplyResult,
  guard: TokenEvidenceCandidateApplyGuard
): boolean {
  return guard.dialogVisible
    && guard.requestId === guard.currentRequestId
    && guard.requestedProjectId === guard.currentProjectId
    && result.candidate?.projectId === guard.requestedProjectId
}

/** 判断候选列表响应是否仍属于当前项目和筛选上下文，防止迟到请求覆盖新页面状态。 */
export function shouldHandleStandardCandidateListResult(
  context: StandardCandidateListResultContext
): boolean {
  return context.requestId === context.currentRequestId
    && context.requestedProjectId === context.currentProjectId
    && context.requestedQueryKey === context.currentQueryKey
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
