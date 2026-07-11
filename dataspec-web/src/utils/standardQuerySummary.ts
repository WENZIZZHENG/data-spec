import type {
  AiContextScope,
  AiContextScopeParams
} from './aiContextScope'
import type {
  StandardQueryAppliedFilter,
  StandardQueryIgnoredFilter,
  StandardQueryRequest,
  StandardQuerySummary
} from '../types'

/** 可展示的 Standard Query summary 分段，文本必须来自本地枚举或服务端脱敏字段。 */
export interface StandardQuerySummaryItem {
  /** 分段稳定 key，用于测试和 v-for。 */
  key: string
  /** 分段显示标签。 */
  label: string
  /** 分段显示值；不得包含 raw secret。 */
  value: string
}

/** 将服务端返回的 DSL summary 转为页面可展示的短摘要。 */
export function standardQuerySummaryItems(
  summary?: StandardQuerySummary | null,
  appliedFilters: StandardQueryAppliedFilter[] = [],
  ignoredFilters: StandardQueryIgnoredFilter[] = []
): StandardQuerySummaryItem[] {
  if (!summary) {
    return []
  }
  const items: StandardQuerySummaryItem[] = [
    { key: 'target', label: 'target', value: summary.target || 'FIELD' }
  ]
  if (summary.text) {
    items.push({ key: 'text', label: 'text', value: summary.text })
  }
  if (typeof summary.resultCount === 'number' || typeof summary.returnedCount === 'number') {
    items.push({
      key: 'count',
      label: '命中',
      value: `${summary.returnedCount ?? 0}/${summary.resultCount ?? 0}`
    })
  }
  if (summary.truncated) {
    items.push({ key: 'truncated', label: '截断', value: '是' })
  }
  if (appliedFilters.length > 0) {
    items.push({ key: 'applied', label: '已应用过滤', value: String(appliedFilters.length) })
  }
  if (ignoredFilters.length > 0) {
    items.push({ key: 'ignored', label: '已忽略过滤', value: String(ignoredFilters.length) })
  }
  return items
}

/** 将 summary 分段合成为 Element Plus 轻量展示文案。 */
export function standardQuerySummaryText(
  summary?: StandardQuerySummary | null,
  appliedFilters: StandardQueryAppliedFilter[] = [],
  ignoredFilters: StandardQueryIgnoredFilter[] = []
) {
  return standardQuerySummaryItems(summary, appliedFilters, ignoredFilters)
    .map((item) => `${item.label}: ${item.value}`)
    .join('；')
}

/**
 * 根据 AI Context 现有 scope 控件构造 FIELD Standard Query 草案。
 *
 * 该 helper 只给前端 summary 和后续 typed wiring 使用，不改变现有导出接口的 GET 参数语义。
 */
export function buildFieldStandardQueryFromAiContextScope(
  projectId: number,
  params: AiContextScopeParams
): StandardQueryRequest | null {
  const filters: NonNullable<StandardQueryRequest['filters']> = []
  const text = normalizeText(params.query)
  const status = normalizeText(params.status)
  const limit = normalizePositiveInt(params.limit)

  if (status) {
    filters.push({ field: 'status', op: 'eq', value: status })
  }
  if (isTagScope(params.scope) && text) {
    filters.push({ field: 'tag', op: 'contains', value: text })
  }

  const request: StandardQueryRequest = {
    projectId,
    target: 'FIELD',
    ...(isTextScope(params.scope) && text ? { text } : {}),
    ...(filters.length ? { filters } : {}),
    ...(limit ? { limit } : {}),
    explain: true,
    strict: false
  }

  return request.text || request.filters?.length || request.limit ? request : null
}

function isTextScope(scope?: AiContextScope) {
  return !scope || scope === 'all' || scope === 'field' || scope === 'table' || scope === 'changed'
}

function isTagScope(scope?: AiContextScope) {
  return scope === 'tag'
}

function normalizeText(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

function normalizePositiveInt(value?: number) {
  return typeof value === 'number' && Number.isFinite(value) && value > 0
    ? Math.floor(value)
    : undefined
}
