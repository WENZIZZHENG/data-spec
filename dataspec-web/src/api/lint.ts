import request from '@/api/request'
import type { LintRequest, LintResult, PageResult, RecordDetail, SqlCheckRecord, SqlLintDebugResult } from '@/types'

export function lintSql(data: LintRequest) {
  return request.post<unknown, LintResult>('/lint', data)
}

export function debugLintSql(data: LintRequest) {
  return request.post<unknown, SqlLintDebugResult>('/lint/debug', data)
}

export function listAvailableLintRules() {
  return request.get<unknown, Array<Record<string, string>>>('/lint/rules')
}

export function listLintRecords(projectId: number, current = 1, size = 10) {
  return request.get<unknown, PageResult<SqlCheckRecord>>('/lint/records', {
    params: { projectId, current, size }
  })
}

export function getLintRecord(id: number) {
  return request.get<unknown, RecordDetail>(`/lint/records/${id}`)
}
