import request from '@/api/request'
import type { ExcelImportPreview, ExcelImportResult } from '@/types'

export function downloadExcelTemplate() {
  return request.get<unknown, Blob>('/import-export/excel/template', {
    responseType: 'blob'
  })
}

export function exportExcel(projectId: number) {
  return request.get<unknown, Blob>('/import-export/excel/export', {
    params: { projectId },
    responseType: 'blob'
  })
}

export function previewExcelImport(projectId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ExcelImportPreview>('/import-export/excel/preview', formData, {
    params: { projectId }
  })
}

export function importExcel(projectId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ExcelImportResult>('/import-export/excel/import', formData, {
    params: { projectId }
  })
}
