import request from '@/api/request'
import type { DatabaseConnectionPreset, DatabaseConnectionPresetReq } from '@/types'

export function listDatabaseConnectionPresets(projectId: number) {
  return request.get<unknown, DatabaseConnectionPreset[]>('/database-connection-presets', {
    params: { projectId }
  })
}

export function getDatabaseConnectionPreset(id: number) {
  return request.get<unknown, DatabaseConnectionPreset>(`/database-connection-presets/${id}`)
}

export function createDatabaseConnectionPreset(data: DatabaseConnectionPresetReq) {
  return request.post<unknown, DatabaseConnectionPreset>('/database-connection-presets', data)
}

export function updateDatabaseConnectionPreset(id: number, data: DatabaseConnectionPresetReq) {
  return request.put<unknown, DatabaseConnectionPreset>(`/database-connection-presets/${id}`, data)
}

export function deleteDatabaseConnectionPreset(id: number) {
  return request.delete<unknown, void>(`/database-connection-presets/${id}`)
}
