import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { renderApiPath } from '../src/api/typedPath.ts'

function readSource(path: string) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
}

function assertContains(source: string, expected: string[], label: string) {
  for (const snippet of expected) {
    assert.ok(source.includes(snippet), `${label} missing ${snippet}`)
  }
}

test('renders OpenAPI paths to axios base-url relative paths', () => {
  assert.equal(renderApiPath('/api/ai-batches'), '/ai-batches')
  assert.equal(renderApiPath('/api/ai-batches/{id}', { id: 12 }), '/ai-batches/12')
  assert.equal(renderApiPath('/api/fields/{id}/impact', { id: 7 }), '/fields/7/impact')
  assert.equal(renderApiPath('/api/projects/{projectId}/activities', { projectId: 'a/b c' }), '/projects/a%2Fb%20c/activities')
  assert.throws(() => renderApiPath('/api/ai-batches/{id}'), /缺少路径参数：id/)
})

test('keeps typed client wired to generated OpenAPI paths', () => {
  const client = readSource('src/api/typedClient.ts')

  assertContains(client, [
    "import type { paths } from '@/api/schema'",
    'export type ApiResponse',
    'export { renderApiPath }',
    'export function typedGet',
    'export function typedPost'
  ], 'typed api client')
})

test('migrates high-frequency list/detail APIs to typed helpers', () => {
  const aiBatch = readSource('src/api/aiBatch.ts')
  const aiTaskRun = readSource('src/api/aiTaskRun.ts')

  assertContains(aiBatch, [
    "typedGet('/api/ai-batches'",
    "typedGet('/api/ai-batches/{id}'"
  ], 'ai batch api')
  assertContains(aiTaskRun, [
    "typedGet('/api/ai-task-runs'",
    "typedGet('/api/ai-task-runs/{id}'",
    "typedGet('/api/ai-task-runs/recent-failures'"
  ], 'ai task run api')
})
