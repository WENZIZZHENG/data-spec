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

test('builds scoped context filename', () => {
  assert.equal(aiContextScopeFilename({}), 'dataspec-ai-context.zip')
  assert.equal(aiContextScopeFilename({ scope: 'domain' }), 'dataspec-ai-context-domain.zip')
})
