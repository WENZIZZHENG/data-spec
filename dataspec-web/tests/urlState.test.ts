import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildRouteUrl,
  copyRouteUrl,
  firstQueryValue,
  mergeRouteQuery,
  readEnumQuery,
  readPositiveIntQuery,
  readStringQuery,
  sanitizeQuery
} from '../src/utils/urlState.ts'

test('reads first query value and trims strings', () => {
  assert.equal(firstQueryValue(['  users  ', 'orders']), 'users')
  assert.equal(readStringQuery({ keyword: '  mobile  ' }, 'keyword'), 'mobile')
  assert.equal(readStringQuery({ keyword: null }, 'keyword'), '')
})

test('parses positive integer query safely', () => {
  assert.equal(readPositiveIntQuery({ projectId: '7' }, 'projectId'), 7)
  assert.equal(readPositiveIntQuery({ projectId: '0' }, 'projectId'), null)
  assert.equal(readPositiveIntQuery({ projectId: '-1' }, 'projectId'), null)
  assert.equal(readPositiveIntQuery({ projectId: '7.5' }, 'projectId'), null)
})

test('reads enum query from allowed values only', () => {
  const allowed = ['ALL', 'NEW', 'MATCHED'] as const
  assert.equal(readEnumQuery({ status: 'NEW' }, 'status', allowed), 'NEW')
  assert.equal(readEnumQuery({ status: 'BAD' }, 'status', allowed), null)
})

test('sanitizes query and removes sensitive or oversized values', () => {
  const query = sanitizeQuery({
    projectId: '7',
    keyword: 'user',
    password: 'secret',
    DATASPEC_TOKEN: 'token',
    Authorization: 'Bearer abc',
    jdbcUrl: 'jdbc:postgresql://localhost/app',
    originalSql: 'create table x(id int)',
    payload: JSON.stringify({ a: 1 }),
    table: 'orders',
    huge: 'x'.repeat(300)
  })

  assert.deepEqual(query, {
    projectId: '7',
    keyword: 'user',
    table: 'orders'
  })
})

test('merges route query and removes empty patched values', () => {
  assert.deepEqual(
    mergeRouteQuery(
      { projectId: '7', keyword: 'user', recordId: '9', token: 'bad' },
      { keyword: 'order', recordId: null, page: 2 }
    ),
    { projectId: '7', keyword: 'order', page: '2' }
  )
})

test('builds sanitized absolute route url', () => {
  const url = buildRouteUrl('https://dataspec.local', '/sql-lint', {
    projectId: '7',
    recordId: '12',
    sql: 'select * from users',
    token: 'secret'
  })

  assert.equal(url, 'https://dataspec.local/sql-lint?projectId=7&recordId=12')
})

test('copies sanitized route url to clipboard', async () => {
  const writes: string[] = []
  const copied = await copyRouteUrl(
    { path: '/fields', query: { projectId: '7', keyword: 'mobile', password: 'secret' } },
    { writeText: async (value: string) => { writes.push(value) } },
    'https://dataspec.local'
  )

  assert.equal(copied, 'https://dataspec.local/fields?projectId=7&keyword=mobile')
  assert.deepEqual(writes, [copied])
})
