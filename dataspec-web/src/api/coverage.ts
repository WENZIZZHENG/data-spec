import request from '@/api/request'
import type { DatabaseConnectionReq, FieldCoverageReport, ScanPartialCoverageReq } from '@/types'

export function reportSqlCoverage(projectId: number, sql: string) {
  return request.post<unknown, FieldCoverageReport>('/coverage/sql', {
    projectId,
    sql
  })
}

export function reportDatabaseCoverage(data: DatabaseConnectionReq) {
  return request.post<unknown, FieldCoverageReport>('/coverage/database', data)
}

export function reportScanPartialCoverage(data: ScanPartialCoverageReq) {
  return request.post<unknown, FieldCoverageReport>('/coverage/scan-partial', data)
}
