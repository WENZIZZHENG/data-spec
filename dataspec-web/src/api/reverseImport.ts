import request from '@/api/request'
import type {
  DatabaseConnectionReq,
  DatabaseImportSourceContext,
  DatabaseConnectionResult,
  DatabaseImportResult,
  DatabaseTableInfo,
  FieldCandidate,
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

export function previewDatabaseReverseImport(data: DatabaseConnectionReq) {
  return request.post<unknown, ReverseImportPreview>('/reverse-import/database/preview', data)
}

export function compareDatabaseReverseImport(data: DatabaseConnectionReq) {
  return request.post<unknown, ReverseImportCompareResult>('/reverse-import/database/compare', data)
}

export function importDatabaseCandidates(
  projectId: number,
  candidates: FieldCandidate[],
  source?: DatabaseImportSourceContext
) {
  return request.post<unknown, DatabaseImportResult>('/reverse-import/database/import', {
    projectId,
    candidates,
    ...source
  })
}
