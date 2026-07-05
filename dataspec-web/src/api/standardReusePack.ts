import request from '@/api/request'
import type {
  StandardReusePackApplicationInfo,
  StandardReusePackApplyReq,
  StandardReusePackApplyResult,
  StandardReusePackCreateReq,
  StandardReusePackDetail,
  StandardReusePackDriftReport,
  StandardReusePackInfo,
  StandardReusePackPlan
} from '@/types'

export function listStandardReusePacks(projectId: number) {
  return request.get<unknown, StandardReusePackInfo[]>('/standard-reuse-packs', {
    params: { projectId }
  })
}

export function createStandardReusePack(data: StandardReusePackCreateReq) {
  return request.post<unknown, StandardReusePackDetail>('/standard-reuse-packs', data)
}

export function getStandardReusePack(packId: number) {
  return request.get<unknown, StandardReusePackDetail>(`/standard-reuse-packs/${packId}`)
}

export function previewStandardReusePackApply(data: StandardReusePackApplyReq) {
  return request.post<unknown, StandardReusePackPlan>('/standard-reuse-packs/apply/preview', data)
}

export function applyStandardReusePack(data: StandardReusePackApplyReq) {
  return request.post<unknown, StandardReusePackApplyResult>('/standard-reuse-packs/apply', data)
}

export function listStandardReusePackApplications(projectId: number) {
  return request.get<unknown, StandardReusePackApplicationInfo[]>('/standard-reuse-packs/applications', {
    params: { projectId }
  })
}

export function getStandardReusePackDrift(packId: number, projectId: number) {
  return request.get<unknown, StandardReusePackDriftReport>(`/standard-reuse-packs/${packId}/drift`, {
    params: { projectId }
  })
}
