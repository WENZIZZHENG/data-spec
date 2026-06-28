import request from '@/api/request'
import type {
  StarterKitApplyReq,
  StarterKitApplyResult,
  StarterKitDefinition,
  StarterKitInstallationInfo
} from '@/types'

export function listStarterKits() {
  return request.get<unknown, StarterKitDefinition[]>('/starter-kits')
}

export function applyStarterKit(data: StarterKitApplyReq) {
  return request.post<unknown, StarterKitApplyResult>('/starter-kits/apply', data)
}

export function listStarterKitInstallations(projectId: number) {
  return request.get<unknown, StarterKitInstallationInfo[]>('/starter-kits/installations', {
    params: { projectId }
  })
}
