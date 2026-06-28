import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiBatchStatusLabel,
  aiBatchStatusTagType,
  buildAiBatchJson,
  formatAiBatchTime
} from '../src/utils/aiBatchDisplay.ts'

test('formats ai batch status for table tags', () => {
  assert.equal(aiBatchStatusLabel('SUCCESS'), '成功')
  assert.equal(aiBatchStatusLabel('PARTIAL_FAILED'), '部分失败')
  assert.equal(aiBatchStatusLabel('FAILED'), '失败')
  assert.equal(aiBatchStatusLabel('UNKNOWN'), 'UNKNOWN')
  assert.equal(aiBatchStatusTagType('SUCCESS'), 'success')
  assert.equal(aiBatchStatusTagType('PARTIAL_FAILED'), 'warning')
  assert.equal(aiBatchStatusTagType('FAILED'), 'danger')
})

test('builds stable ai batch json text', () => {
  const text = buildAiBatchJson({
    packageVersion: 'ai-batch-delivery@1',
    batchId: 'server-42',
    batchType: 'SQL_LINT'
  })

  assert.match(text, /"packageVersion": "ai-batch-delivery@1"/)
  assert.match(text, /"batchType": "SQL_LINT"/)
})

test('formats ai batch time fallback', () => {
  assert.equal(formatAiBatchTime(undefined), '-')
  assert.equal(formatAiBatchTime('2026-06-28T10:30:00'), '2026-06-28 10:30:00')
})
