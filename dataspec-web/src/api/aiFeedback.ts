import request from '@/api/request'
import type { AiFeedbackReport } from '@/types'

export function getAiFeedbackReport(projectId: number) {
  return request.get<unknown, AiFeedbackReport>('/ai-feedback/report', {
    params: { projectId }
  })
}
