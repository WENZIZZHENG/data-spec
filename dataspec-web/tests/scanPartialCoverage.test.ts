import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  readScanPartialCoveragePayload,
  saveScanPartialCoveragePayload
} from '../src/utils/scanPartialCoverage.ts'

class MemoryStorage {
  private readonly items = new Map<string, string>()

  getItem(key: string) {
    return this.items.get(key) ?? null
  }

  setItem(key: string, value: string) {
    this.items.set(key, value)
  }
}

test('stores scan partial coverage payload in session storage without putting schema payload in URL id', () => {
  const sessionStorage = new MemoryStorage()
  globalThis.window = { sessionStorage } as Window & typeof globalThis

  const payload = {
    projectId: 1,
    partialResult: {
      successfulTableNames: ['user_order'],
      failedTableNames: ['payment_bill'],
      skippedTableNames: [],
      completeForPreview: true,
      completeForCoverage: true,
      complete: false
    },
    failureSummary: {
      failedTableCount: 1,
      retryable: true,
      safeNextActions: ['继续分页扫描']
    },
    scanStatus: 'PARTIAL'
  }

  const id = saveScanPartialCoveragePayload(payload)

  assert.doesNotMatch(id, /user_order|payment_bill|password|jdbc:/)
  assert.deepEqual(readScanPartialCoveragePayload(id), payload)
})

test('returns null when scan partial coverage payload is unavailable or invalid', () => {
  const sessionStorage = new MemoryStorage()
  globalThis.window = { sessionStorage } as Window & typeof globalThis

  sessionStorage.setItem('dataspec:scan-partial-coverage:broken', '{')

  assert.equal(readScanPartialCoveragePayload('missing'), null)
  assert.equal(readScanPartialCoveragePayload('broken'), null)
})
