import request from '@/api/request'
import type { StandardSnapshotCreateReq, StandardSnapshotInfo } from '@/types'

export function getCurrentStandardSnapshot(projectId: number) {
  return request.get<unknown, StandardSnapshotInfo>(`/projects/${projectId}/standard-snapshots/current`)
}

export function listStandardSnapshots(projectId: number) {
  return request.get<unknown, StandardSnapshotInfo[]>(`/projects/${projectId}/standard-snapshots`)
}

export function createStandardSnapshot(projectId: number, data: StandardSnapshotCreateReq) {
  return request.post<unknown, StandardSnapshotInfo>(`/projects/${projectId}/standard-snapshots`, data)
}
