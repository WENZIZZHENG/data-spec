import { ref, shallowRef, type Ref } from 'vue'
import type { ErrorDetail } from '@/types'

export interface RequestErrorSummary {
  /** 用户可见的失败摘要，优先来自后端业务错误消息。 */
  message: string
  /** 后端返回的 DataSpec 错误详情，保留给页面展示和调试入口使用。 */
  detail?: ErrorDetail
  /** 单条恢复建议，兼容现有 ErrorDetail 契约。 */
  suggestedAction?: string
  /** 页面可直接展示的恢复动作列表；缺省时由 suggestedAction 派生。 */
  nextActions?: string[]
  /** 与错误相关的项目文档、README 章节或规范引用。 */
  docsRef?: string
  /** 是否适合直接重试；未返回时按可重试处理，保持旧页面行为。 */
  retryable?: boolean
}

export interface RequestState<T> {
  /** 最近一次成功请求的数据；失败不会覆盖旧成功值。 */
  data: Ref<T | null>
  /** 当前请求是否仍在进行中。 */
  loading: Ref<boolean>
  /** 用户可见的错误摘要。 */
  errorMessage: Ref<string>
  /** DataSpec 结构化错误详情。 */
  errorDetail: Ref<ErrorDetail | undefined>
  /** 单条恢复建议，保留给旧页面和 StateBlock 兼容展示。 */
  suggestedAction: Ref<string>
  /** 统一的恢复动作列表，供页面展示 nextActions。 */
  nextActions: Ref<string[]>
  /** 与错误相关的文档引用。 */
  docsRef: Ref<string>
  /** 当前错误是否建议直接重试。 */
  retryable: Ref<boolean>
  /** 最近一次成功刷新时间，ISO 字符串。 */
  lastUpdatedAt: Ref<string>
  /** 执行请求并同步 loading/error/metadata。 */
  run: (request: () => Promise<T>) => Promise<T>
  /** 使用最近一次请求函数重试；没有历史请求时返回 undefined。 */
  retry: () => Promise<T | undefined>
  /** 清空数据、错误和最近请求。 */
  reset: () => void
}

type DataSpecLikeError = Error & {
  dataspecError?: ErrorDetail
  response?: {
    data?: {
      message?: string
      error?: ErrorDetail
    }
  }
}
type ErrorDetailWithNextActions = ErrorDetail & {
  nextActions?: string[]
}

export function useRequestState<T = unknown>(): RequestState<T> {
  const data = shallowRef<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const errorMessage = ref('')
  const errorDetail = ref<ErrorDetail>()
  const suggestedAction = ref('')
  const nextActions = ref<string[]>([])
  const docsRef = ref('')
  const retryable = ref(true)
  const lastUpdatedAt = ref('')
  const lastRequest = shallowRef<(() => Promise<T>) | null>(null)

  async function run(request: () => Promise<T>) {
    lastRequest.value = request
    loading.value = true
    clearError()
    try {
      const result = await request()
      data.value = result
      lastUpdatedAt.value = new Date().toISOString()
      return result
    } catch (error) {
      const summary = normalizeRequestError(error)
      errorMessage.value = summary.message
      errorDetail.value = summary.detail
      suggestedAction.value = summary.suggestedAction ?? ''
      nextActions.value = summary.nextActions ?? []
      docsRef.value = summary.docsRef ?? ''
      retryable.value = summary.retryable ?? true
      throw error
    } finally {
      loading.value = false
    }
  }

  async function retry() {
    if (!lastRequest.value) {
      return undefined
    }
    return run(lastRequest.value)
  }

  function reset() {
    data.value = null
    lastUpdatedAt.value = ''
    lastRequest.value = null
    clearError()
  }

  function clearError() {
    errorMessage.value = ''
    errorDetail.value = undefined
    suggestedAction.value = ''
    nextActions.value = []
    docsRef.value = ''
    retryable.value = true
  }

  return {
    data,
    loading,
    errorMessage,
    errorDetail,
    suggestedAction,
    nextActions,
    docsRef,
    retryable,
    lastUpdatedAt,
    run,
    retry,
    reset
  }
}

export function normalizeRequestError(error: unknown): RequestErrorSummary {
  const dataSpecError = error as DataSpecLikeError
  const detail = dataSpecError.dataspecError ?? dataSpecError.response?.data?.error
  const detailWithNextActions = detail as ErrorDetailWithNextActions | undefined
  const message = dataSpecError.response?.data?.message
    || (error instanceof Error ? error.message : '')
    || '请求失败，请稍后重试'

  return {
    message,
    detail,
    suggestedAction: detail?.suggestedAction,
    nextActions: normalizeNextActions(detail?.suggestedAction, detailWithNextActions?.nextActions),
    docsRef: detail?.docsRef,
    retryable: detail?.retryable
  }
}

function normalizeNextActions(suggestedAction?: string, nextActions: string[] = []) {
  return Array.from(new Set([
    suggestedAction,
    ...nextActions
  ].map((action) => action?.trim()).filter((action): action is string => Boolean(action))))
}
