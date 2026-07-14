import request from '@/api/request'
import type {
  PageResult,
  StandardCandidate,
  StandardCandidateCreateReq,
  StandardCandidateDecisionReq,
  StandardCandidateMergeReq,
  TokenEvidenceCandidateApplyReq,
  TokenEvidenceCandidateApplyResult,
  TokenEvidenceCandidatePreview,
  TokenEvidenceCandidatePreviewReq
} from '@/types'

export interface StandardCandidateListParams {
  projectId: number
  status?: string
  sourceType?: string
  keyword?: string
  current?: number
  size?: number
}

export function listStandardCandidates(params: StandardCandidateListParams) {
  return request.get<unknown, PageResult<StandardCandidate>>('/standard-candidates', {
    params
  })
}

export function createStandardCandidate(data: StandardCandidateCreateReq) {
  return request.post<unknown, StandardCandidate>('/standard-candidates', data)
}

/** 只读预览命名证据候选，不写入标准候选 Inbox。 */
export function previewTokenEvidenceCandidate(data: TokenEvidenceCandidatePreviewReq) {
  return request.post<unknown, TokenEvidenceCandidatePreview>(
    '/standard-candidates/token-evidence/preview',
    data
  )
}

/** 使用匹配的 dry-run token 和显式确认幂等写入 PENDING 候选。 */
export function applyTokenEvidenceCandidate(data: TokenEvidenceCandidateApplyReq) {
  return request.post<unknown, TokenEvidenceCandidateApplyResult>(
    '/standard-candidates/token-evidence/apply',
    data
  )
}

export function acceptStandardCandidate(id: number, data: StandardCandidateDecisionReq) {
  return request.post<unknown, StandardCandidate>(`/standard-candidates/${id}/accept`, data)
}

export function mergeStandardCandidate(id: number, data: StandardCandidateMergeReq) {
  return request.post<unknown, StandardCandidate>(`/standard-candidates/${id}/merge`, data)
}

export function ignoreStandardCandidate(id: number, data: StandardCandidateDecisionReq) {
  return request.post<unknown, StandardCandidate>(`/standard-candidates/${id}/ignore`, data)
}

export function postponeStandardCandidate(id: number, data: StandardCandidateDecisionReq) {
  return request.post<unknown, StandardCandidate>(`/standard-candidates/${id}/postpone`, data)
}
