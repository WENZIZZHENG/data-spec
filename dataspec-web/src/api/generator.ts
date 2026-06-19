import request from '@/api/request'
import type { DdlGenerateResult } from '@/types'

export function previewDdl(projectId: number, templateId: number, tableName: string) {
  return request.get<unknown, DdlGenerateResult>('/generator/ddl/preview', {
    params: { projectId, templateId, tableName }
  })
}

export function previewDataDictionaryHtml(projectId: number) {
  return request.get<unknown, string>('/generator/html/preview', {
    params: { projectId }
  })
}

export function downloadDataDictionaryHtml(projectId: number) {
  return request.get<unknown, Blob>('/generator/html/download', {
    params: { projectId },
    responseType: 'blob'
  })
}

export function previewDataDictionaryErd(projectId: number) {
  return request.get<unknown, string>('/generator/erd/preview', {
    params: { projectId }
  })
}

export function downloadDataDictionaryErd(projectId: number) {
  return request.get<unknown, Blob>('/generator/erd/download', {
    params: { projectId },
    responseType: 'blob'
  })
}
