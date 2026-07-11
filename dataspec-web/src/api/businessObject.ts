import request from '@/api/request'
import type { BusinessObjectRelationSummary, BusinessObjectStandard, BusinessObjectStandardReq } from '@/types'

/** 查询项目业务对象标准列表；只返回当前项目可见的对象标准。 */
export function listBusinessObjects(projectId: number) {
  return request.get<unknown, BusinessObjectStandard[]>('/business-objects', {
    params: { projectId }
  })
}

/** 查询业务对象标准详情；用于编辑对象字段、关系提示和 AI guidance。 */
export function getBusinessObject(id: number) {
  return request.get<unknown, BusinessObjectStandard>(`/business-objects/${id}`)
}

/** 创建业务对象标准；关系和外键提示只作为结构化 guidance 保存。 */
export function createBusinessObject(data: BusinessObjectStandardReq) {
  return request.post<unknown, BusinessObjectStandard>('/business-objects', data)
}

/** 更新业务对象标准；服务端会校验项目归属和 objectKey/entityName 唯一性。 */
export function updateBusinessObject(id: number, data: BusinessObjectStandardReq) {
  return request.put<unknown, BusinessObjectStandard>(`/business-objects/${id}`, data)
}

/** 删除业务对象标准；不会删除模板字段或改写任何业务库。 */
export function deleteBusinessObject(id: number) {
  return request.delete<unknown, void>(`/business-objects/${id}`)
}

/** 获取项目业务对象、模板和字段的只读关系摘要。 */
export function getBusinessObjectRelationSummary(projectId: number) {
  return request.get<unknown, BusinessObjectRelationSummary>('/business-objects/relation-summary', {
    params: { projectId }
  })
}
