import request from '@/api/request'
import type { Field, FieldReq, PageResult } from '@/types'

export function pageFields(projectId: number, current = 1, size = 20) {
  return request.get<unknown, PageResult<Field>>('/fields', {
    params: { projectId, current, size }
  })
}

export function listFields(projectId: number) {
  return request.get<unknown, Field[]>('/fields/all', {
    params: { projectId }
  })
}

export function getField(id: number) {
  return request.get<unknown, Field>(`/fields/${id}`)
}

export function createField(data: FieldReq) {
  return request.post<unknown, Field>('/fields', data)
}

export function updateField(id: number, data: FieldReq) {
  return request.put<unknown, Field>(`/fields/${id}`, data)
}

export function deleteField(id: number) {
  return request.delete<unknown, void>(`/fields/${id}`)
}
