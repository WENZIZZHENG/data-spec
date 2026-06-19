import request from '@/api/request'
import type { AuthMe } from '@/types'


export function getCurrentAuth() {
  return request.get<unknown, AuthMe>('/auth/me')
}
