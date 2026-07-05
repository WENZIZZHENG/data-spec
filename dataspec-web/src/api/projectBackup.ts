import request from '@/api/request'
import { withIdempotencyKey } from '@/api/idempotency'
import type {
  ProjectBackupPackage,
  ProjectRestorePlan,
  ProjectRestoreRecord,
  ProjectRestoreReq,
  ProjectRestoreResult
} from '@/types'

export function exportProjectBackup(projectId: number) {
  return request.get<unknown, ProjectBackupPackage>('/project-backups/export', {
    params: { projectId }
  })
}

export function previewProjectBackupRestore(data: ProjectRestoreReq) {
  return request.post<unknown, ProjectRestorePlan>('/project-backups/restore/preview', data)
}

export function applyProjectBackupRestore(data: ProjectRestoreReq, idempotencyKey?: string) {
  return request.post<unknown, ProjectRestoreResult>(
    '/project-backups/restore/apply',
    data,
    withIdempotencyKey(idempotencyKey)
  )
}

export function listProjectRestoreRecords(projectId: number) {
  return request.get<unknown, ProjectRestoreRecord[]>('/project-backups/restore/records', {
    params: { projectId }
  })
}
