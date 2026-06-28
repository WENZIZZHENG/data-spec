import request from '@/api/request'
import type {
  PageResult,
  StandardCandidate,
  StandardCandidateCreateReq,
  StandardCandidateDecisionReq,
  StandardCandidateMergeReq
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
