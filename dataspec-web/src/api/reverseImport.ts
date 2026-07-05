import request from '@/api/request'
import { withIdempotencyKey } from '@/api/idempotency'
import type {
  DatabaseConnectionReq,
  DatabaseImportSourceContext,
  DatabaseConnectionResult,
  DatabaseImportResult,
  DatabaseMetadataBrowser,
  DatabaseMetadataScanReq,
  DatabaseMetadataScanResult,
  DatabaseTableInfo,
  FieldCandidate,
  ReverseImportDecision,
  ReverseImportCompareResult,
  ReverseImportPreview
} from '@/types'

export function previewReverseImport(projectId: number, sql: string) {
  return request.post<unknown, ReverseImportPreview>('/reverse-import/preview', {
    projectId,
    sql
  })
}

export function testDatabaseConnection(data: DatabaseConnectionReq) {
  return request.post<unknown, DatabaseConnectionResult>('/reverse-import/database/test', data)
}

export function listDatabaseTables(data: DatabaseConnectionReq) {
  return request.post<unknown, DatabaseTableInfo[]>('/reverse-import/database/tables', data)
}

export function browseDatabaseMetadata(data: DatabaseConnectionReq) {
  return request.post<unknown, DatabaseMetadataBrowser>('/reverse-import/database/browser', data)
}

/** 分页读取数据库表级 metadata，用于大库分批浏览和 AI 恢复扫描。 */
export function scanDatabaseMetadata(data: DatabaseMetadataScanReq) {
  return request.post<unknown, DatabaseMetadataScanResult>('/reverse-import/database/scan', data)
}

export function previewDatabaseReverseImport(data: DatabaseConnectionReq) {
  return request.post<unknown, ReverseImportPreview>('/reverse-import/database/preview', data)
}

export function compareDatabaseReverseImport(data: DatabaseConnectionReq) {
  return request.post<unknown, ReverseImportCompareResult>('/reverse-import/database/compare', data)
}

export function importDatabaseCandidates(
  projectId: number,
  candidates: FieldCandidate[],
  source?: DatabaseImportSourceContext,
  ignoredCandidates: FieldCandidate[] = [],
  dryRunToken?: string,
  idempotencyKey?: string
) {
  return request.post<unknown, DatabaseImportResult>(
    '/reverse-import/database/import',
    {
      projectId,
      candidates,
      ignoredCandidates,
      dryRunToken,
      ...source
    },
    withIdempotencyKey(idempotencyKey)
  )
}

export function listReverseImportDecisions(projectId: number, batchId?: number, limit = 50) {
  return request.get<unknown, ReverseImportDecision[]>('/reverse-import/decisions', {
    params: { projectId, batchId, limit }
  })
}
