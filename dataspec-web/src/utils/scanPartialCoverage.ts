import type { ScanPartialCoverageReq } from '@/types'

const SCAN_PARTIAL_COVERAGE_PREFIX = 'dataspec:scan-partial-coverage:'

function storage(): Storage | null {
  return typeof window === 'undefined' ? null : window.sessionStorage
}

/** 暂存 schema-only scan partial 覆盖率请求；返回的短 ID 可放入 URL，不包含连接凭据。 */
export function saveScanPartialCoveragePayload(payload: ScanPartialCoverageReq): string {
  const id = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
  storage()?.setItem(`${SCAN_PARTIAL_COVERAGE_PREFIX}${id}`, JSON.stringify(payload))
  return id
}

/** 读取反向导入页交接的 schema-only partial coverage 请求；读取失败时返回 null。 */
export function readScanPartialCoveragePayload(id: string): ScanPartialCoverageReq | null {
  const raw = storage()?.getItem(`${SCAN_PARTIAL_COVERAGE_PREFIX}${id}`)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as ScanPartialCoverageReq
  } catch {
    return null
  }
}
