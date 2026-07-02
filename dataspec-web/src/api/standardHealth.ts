import request from '@/api/request'
import type {
  StandardHealthPlan,
  StandardHealthSnapshotCreateReq,
  StandardHealthSnapshotView,
  StandardHealthTrend
} from '@/types'

export function createStandardHealthSnapshot(data: StandardHealthSnapshotCreateReq) {
  return request.post<unknown, StandardHealthSnapshotView>('/standard-health/snapshots', data)
}

export function getStandardHealthTrend(projectId: number, limit = 30) {
  return request.get<unknown, StandardHealthTrend>('/standard-health/trend', {
    params: { projectId, limit }
  })
}

export function getStandardHealthPlan(projectId: number) {
  return request.get<unknown, StandardHealthPlan>('/standard-health/plan', {
    params: { projectId }
  })
}
