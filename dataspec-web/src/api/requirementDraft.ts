import request from '@/api/request'
import type { RequirementDraftReq, RequirementDraftResult } from '@/types'

export function createRequirementDraft(data: RequirementDraftReq) {
  return request.post<unknown, RequirementDraftResult>('/requirement-drafts', data)
}
