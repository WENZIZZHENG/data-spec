import request from '@/api/request'
import type { ReverseImportPreview } from '@/types'

export function previewReverseImport(projectId: number, sql: string) {
  return request.post<unknown, ReverseImportPreview>('/reverse-import/preview', {
    projectId,
    sql
  })
}
