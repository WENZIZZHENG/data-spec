import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import {
  buildFieldStandardQueryFromAiContextScope,
  standardQuerySummaryItems,
  standardQuerySummaryText
} from '../src/utils/standardQuerySummary.ts'

function readSource(path: string) {
  return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
}

function assertContains(source: string, expected: string[], label: string) {
  for (const snippet of expected) {
    assert.ok(source.includes(snippet), `${label} missing ${snippet}`)
  }
}

test('keeps Standard Query API wrapper on read-only search endpoint', () => {
  const api = readSource('src/api/standardQuery.ts')

  assertContains(api, [
    "request.post<unknown, StandardQueryResult>('/standard-query/search', data)",
    'export function searchStandardQuery(data: StandardQueryRequest)'
  ], 'standard query api')
})

test('summarizes service-redacted Standard Query response metadata', () => {
  const items = standardQuerySummaryItems(
    {
      target: 'FIELD',
      text: 'token=[REDACTED]',
      resultCount: 8,
      returnedCount: 5,
      truncated: true
    },
    [{ field: 'category', op: 'eq', redactedValue: 'money', description: '分类 = money' }],
    [{ field: 'owner', op: 'eq', redactedValue: '[REDACTED]', reason: '不支持字段 owner' }]
  )

  assert.deepEqual(items.map((item) => item.key), [
    'target',
    'text',
    'count',
    'truncated',
    'applied',
    'ignored'
  ])
  assert.equal(
    standardQuerySummaryText(
      { target: 'FIELD', resultCount: 8, returnedCount: 5, truncated: false },
      [{ field: 'status', op: 'eq', redactedValue: 'enabled' }]
    ),
    'target: FIELD；命中: 5/8；已应用过滤: 1'
  )
})

test('builds FIELD Standard Query draft from existing AI Context scope controls', () => {
  assert.deepEqual(
    buildFieldStandardQueryFromAiContextScope(7, {
      scope: 'field',
      query: ' 订单金额 ',
      status: ' enabled ',
      limit: 20.8
    }),
    {
      projectId: 7,
      target: 'FIELD',
      text: '订单金额',
      filters: [{ field: 'status', op: 'eq', value: 'enabled' }],
      limit: 20,
      explain: true,
      strict: false
    }
  )

  assert.deepEqual(
    buildFieldStandardQueryFromAiContextScope(7, { scope: 'tag', query: 'finance' })?.filters,
    [{ field: 'tag', op: 'contains', value: 'finance' }]
  )
  assert.equal(buildFieldStandardQueryFromAiContextScope(7, { scope: 'all' }), null)
})

test('keeps public Standard Query types documented for frontend callers', () => {
  const types = readSource('src/types/index.ts')

  assertContains(types, [
    'export interface StandardQueryRequest',
    'Standard Query DSL 只读请求',
    'export interface StandardQueryResult',
    'Standard Query DSL 只读查询结果',
    'export interface StandardQuerySummary',
    'Standard Query DSL 执行摘要',
    'querySummary?: StandardQuerySummary',
    'dslAppliedFilters?: StandardQueryAppliedFilter[]',
    'dslIgnoredFilters?: StandardQueryIgnoredFilter[]'
  ], 'standard query types')
})
