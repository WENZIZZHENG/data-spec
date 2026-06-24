import request from '@/api/request'
import type { ApiTokenCreateReq, ApiTokenCreateResp, ApiTokenInfo } from '@/types'

export function listApiTokens() {
  return request.get<unknown, ApiTokenInfo[]>('/tokens')
}

export function createApiToken(data: ApiTokenCreateReq) {
  return request.post<unknown, ApiTokenCreateResp>('/tokens', data)
}

export function disableApiToken(id: number) {
  return request.patch<unknown, ApiTokenInfo>(`/tokens/${id}/disable`)
}
