import request from '@/api/request'
import type { AiJobRecordDetail, AiJobRecordListItem, PageResult } from '@/types'

export function listAiJobs(projectId: number, current = 1, size = 10, jobType?: string) {
  return request.get<unknown, PageResult<AiJobRecordListItem>>('/ai-jobs', {
    params: { projectId, current, size, jobType: jobType || undefined }
  })
}

export function getAiJobDetail(id: number) {
  return request.get<unknown, AiJobRecordDetail>(`/ai-jobs/${id}`)
}
