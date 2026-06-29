import request from '@/api/request'
import type {
  BusinessGlossary,
  BusinessGlossaryConflictReport,
  BusinessGlossaryReq,
  PageResult
} from '@/types'

export interface BusinessGlossaryListParams {
  projectId: number
  keyword?: string
  status?: string
  current?: number
  size?: number
}

export function listBusinessGlossary(params: BusinessGlossaryListParams) {
  return request.get<unknown, PageResult<BusinessGlossary>>('/glossary', {
    params
  })
}

export function listAllBusinessGlossary(projectId: number, status?: string) {
  return request.get<unknown, BusinessGlossary[]>('/glossary/all', {
    params: { projectId, status }
  })
}

export function getBusinessGlossaryConflicts(projectId: number) {
  return request.get<unknown, BusinessGlossaryConflictReport>('/glossary/conflicts', {
    params: { projectId }
  })
}

export function createBusinessGlossary(data: BusinessGlossaryReq) {
  return request.post<unknown, BusinessGlossary>('/glossary', data)
}

export function updateBusinessGlossary(id: number, data: BusinessGlossaryReq) {
  return request.put<unknown, BusinessGlossary>(`/glossary/${id}`, data)
}

export function deleteBusinessGlossary(id: number) {
  return request.delete<unknown, void>(`/glossary/${id}`)
}
