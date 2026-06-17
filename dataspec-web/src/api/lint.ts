import request from '@/api/request'
import type { LintRequest, LintResult } from '@/types'

export function lintSql(data: LintRequest) {
  return request.post<unknown, LintResult>('/lint', data)
}

export function listAvailableLintRules() {
  return request.get<unknown, Array<Record<string, string>>>('/lint/rules')
}
