import request from '@/api/request'
import type { StandardQueryRequest, StandardQueryResult } from '@/types'

/**
 * 执行 Standard Query DSL 只读查询。
 *
 * v1 仅用于 FIELD 标准对象检索；请求中的 text/filter value 由服务端按敏感输入处理，
 * 前端只消费服务端返回的脱敏 summary、filters 和字段命中项。
 */
export function searchStandardQuery(data: StandardQueryRequest) {
  return request.post<unknown, StandardQueryResult>('/standard-query/search', data)
}
