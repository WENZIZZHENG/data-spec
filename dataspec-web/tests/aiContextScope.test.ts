import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiContextScopeFilename,
  normalizeAiContextScopeParams
} from '../src/utils/aiContextScope.ts'

test('normalizes empty AI context scope to full export params', () => {
  assert.deepEqual(normalizeAiContextScopeParams({ scope: 'all', query: ' ', status: '', limit: null }), {})
})

test('normalizes scoped AI context params', () => {
  assert.deepEqual(
    normalizeAiContextScopeParams({ scope: 'field', query: ' 手机 ', status: ' enabled ', limit: 20.8 }),
    { scope: 'field', query: '手机', status: 'enabled', limit: 20 }
  )
})

test('normalizes table-standard scoped AI context params', () => {
  assert.deepEqual(
    normalizeAiContextScopeParams({ scope: 'business-object', query: ' 订单 ', status: '', limit: null }),
    { scope: 'business-object', query: '订单' }
  )
  assert.deepEqual(
    normalizeAiContextScopeParams({ scope: 'table-template', query: ' order_table ', status: '', limit: 5 }),
    { scope: 'table-template', query: 'order_table', limit: 5 }
  )
})

test('normalizes snapshot AI context params', () => {
  assert.deepEqual(
    normalizeAiContextScopeParams({
      scope: 'all',
      query: '',
      status: '',
      limit: null,
      snapshotId: 42,
      snapshotVersion: ' v-history '
    }),
    { scope: 'all', snapshotId: 42, snapshotVersion: 'v-history' }
  )
})

test('builds scoped context filename', () => {
  assert.equal(aiContextScopeFilename({}), 'dataspec-ai-context.zip')
  assert.equal(aiContextScopeFilename({ scope: 'domain' }), 'dataspec-ai-context-domain.zip')
  assert.equal(aiContextScopeFilename({ scope: 'business-object' }), 'dataspec-ai-context-business-object.zip')
  assert.equal(aiContextScopeFilename({ scope: 'table-template' }), 'dataspec-ai-context-table-template.zip')
  assert.equal(aiContextScopeFilename({ snapshotId: 42 }), 'dataspec-ai-context-snapshot-42.zip')
  assert.equal(
    aiContextScopeFilename({ snapshotVersion: '2026/06 快照' }),
    'dataspec-ai-context-snapshot-2026-06.zip'
  )
})
