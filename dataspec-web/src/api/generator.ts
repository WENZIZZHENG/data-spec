import request from '@/api/request'
import type { DdlGenerateResult } from '@/types'

export function previewDdl(projectId: number, templateId: number, tableName: string) {
  return request.get<unknown, DdlGenerateResult>('/generator/ddl/preview', {
    params: { projectId, templateId, tableName }
  })
}
