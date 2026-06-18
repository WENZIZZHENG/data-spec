import request from '@/api/request'
import type { Template, TemplateField } from '@/types'

export function listTemplates(projectId: number) {
  return request.get<unknown, Template[]>('/templates', {
    params: { projectId }
  })
}

export function listTemplateFields(templateId: number) {
  return request.get<unknown, TemplateField[]>(`/templates/${templateId}/fields`)
}
