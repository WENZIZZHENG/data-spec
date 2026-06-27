import request from '@/api/request'
import type { Domain } from '@/types'

export function listDomains(projectId: number) {
  return request.get<unknown, Domain[]>('/domains', {
    params: { projectId }
  })
}
