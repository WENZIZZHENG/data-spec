import request from '@/api/request'
import type { AiBatchRunDetail, AiBatchRunListItem, PageResult } from '@/types'

export function listAiBatches(projectId: number, current = 1, size = 10) {
  return request.get<unknown, PageResult<AiBatchRunListItem>>('/ai-batches', {
    params: { projectId, current, size }
  })
}

export function getAiBatchDetail(id: number) {
  return request.get<unknown, AiBatchRunDetail>(`/ai-batches/${id}`)
}

export function downloadAiBatchPackage(id: number) {
  return request.get<unknown, Blob>(`/ai-batches/${id}/download`, {
    responseType: 'blob'
  })
}
