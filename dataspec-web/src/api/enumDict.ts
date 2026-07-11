import request from '@/api/request'
import type { EnumDict, EnumDictReq, EnumValue, EnumValueReq } from '@/types'

export function listEnumDicts(projectId: number) {
  return request.get<unknown, EnumDict[]>('/enums', {
    params: { projectId }
  })
}

export function createEnumDict(data: EnumDictReq) {
  return request.post<unknown, EnumDict>('/enums', data)
}

export function updateEnumDict(id: number, data: EnumDictReq) {
  return request.put<unknown, EnumDict>(`/enums/${id}`, data)
}

export function deleteEnumDict(id: number) {
  return request.delete<unknown, void>(`/enums/${id}`)
}

export function listEnumValues(enumId: number) {
  return request.get<unknown, EnumValue[]>(`/enums/${enumId}/values`)
}

export function createEnumValue(enumId: number, data: EnumValueReq) {
  return request.post<unknown, EnumValue>(`/enums/${enumId}/values`, data)
}

export function updateEnumValue(id: number, data: EnumValueReq) {
  return request.put<unknown, EnumValue>(`/enums/values/${id}`, data)
}

export function deleteEnumValue(id: number) {
  return request.delete<unknown, void>(`/enums/values/${id}`)
}
