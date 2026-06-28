import { ref, shallowRef, type Ref } from 'vue'
import type { ErrorDetail } from '@/types'

export interface RequestErrorSummary {
  message: string
  detail?: ErrorDetail
  suggestedAction?: string
  docsRef?: string
  retryable?: boolean
}

export interface RequestState<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  errorMessage: Ref<string>
  errorDetail: Ref<ErrorDetail | undefined>
  suggestedAction: Ref<string>
  docsRef: Ref<string>
  retryable: Ref<boolean>
  lastUpdatedAt: Ref<string>
  run: (request: () => Promise<T>) => Promise<T>
  retry: () => Promise<T | undefined>
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

export function useRequestState<T = unknown>(): RequestState<T> {
  const data = shallowRef<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const errorMessage = ref('')
  const errorDetail = ref<ErrorDetail>()
  const suggestedAction = ref('')
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
    docsRef.value = ''
    retryable.value = true
  }

  return {
    data,
    loading,
    errorMessage,
    errorDetail,
    suggestedAction,
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
  const message = dataSpecError.response?.data?.message
    || (error instanceof Error ? error.message : '')
    || '请求失败，请稍后重试'

  return {
    message,
    detail,
    suggestedAction: detail?.suggestedAction,
    docsRef: detail?.docsRef,
    retryable: detail?.retryable
  }
}
