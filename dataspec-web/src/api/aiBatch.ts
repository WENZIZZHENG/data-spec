import request from '@/api/request'
import { typedGet } from '@/api/typedClient'

export function listAiBatches(projectId: number, current = 1, size = 10) {
  return typedGet('/api/ai-batches', {
    params: { projectId, current, size }
  })
}

export function getAiBatchDetail(id: number) {
  return typedGet('/api/ai-batches/{id}', {
    path: { id }
  })
}

export function downloadAiBatchPackage(id: number) {
  return request.get<unknown, Blob>(`/ai-batches/${id}/download`, {
    responseType: 'blob'
  })
}
