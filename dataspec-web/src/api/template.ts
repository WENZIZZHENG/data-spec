import request from '@/api/request'
import type { Template, TemplateField, TemplateSaveReq } from '@/types'
import type { TemplateResp } from '@/types'

/** 查询项目下表模板列表；响应会兼容旧模板字段并 additive 带上结构标准。 */
export function listTemplates(projectId: number) {
  return request.get<unknown, Template[]>('/templates', {
    params: { projectId }
  })
}

/** 查询单个表模板详情，用于编辑模板基础信息和表结构标准。 */
export function getTemplate(id: number) {
  return request.get<unknown, TemplateResp>(`/templates/${id}`)
}

/** 创建表模板；`structure` 为表级约束/索引/策略标准，不会直接应用到数据库。 */
export function createTemplate(data: TemplateSaveReq) {
  return request.post<unknown, TemplateResp>('/templates', data)
}

/** 更新表模板和结构标准；后端保持模板字段列表不被该请求移除。 */
export function updateTemplate(id: number, data: TemplateSaveReq) {
  return request.put<unknown, TemplateResp>(`/templates/${id}`, data)
}

/** 查询模板字段列表，供模板维护页和 DDL preview 展示字段边界。 */
export function listTemplateFields(templateId: number) {
  return request.get<unknown, TemplateField[]>(`/templates/${templateId}/fields`)
}
