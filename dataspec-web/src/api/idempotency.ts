import type { AxiosRequestConfig } from 'axios'

/**
 * 为高风险确认写入生成客户端幂等 key。
 *
 * key 只标识一次用户确认动作，不复用到新的确认；如果同一次请求网络重试，应复用原 key。
 */
export function createClientIdempotencyKey(operation: string): string {
  const safeOperation = operation.trim() || 'dataspec-write'
  const randomPart = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `${safeOperation}:${randomPart}`
}

/** 把幂等 key 转成 Axios header 配置；空值表示调用方尚未进入确认写入阶段。 */
export function withIdempotencyKey(idempotencyKey?: string): AxiosRequestConfig | undefined {
  if (!idempotencyKey?.trim()) {
    return undefined
  }
  return {
    headers: { 'Idempotency-Key': idempotencyKey }
  }
}
