import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildCandidateKey,
  filterDatabaseTables,
  groupFieldCandidatesByTable,
  mergeSelectedTableNames,
  pickSelectedCandidates,
  selectAllTableNames
} from '../src/utils/reverseImportSelection.ts'

test('filters database tables by schema, table name, and comment', () => {
  const tables = [
    { schemaName: 'public', tableName: 'user_order', comment: '用户订单' },
    { schemaName: 'audit', tableName: 'event_log', comment: '事件日志' },
    { schemaName: 'public', tableName: 'payment_bill', comment: '支付账单' }
  ]

  assert.deepEqual(
    filterDatabaseTables(tables, 'order').map((table) => table.tableName),
    ['user_order']
  )
  assert.deepEqual(
    filterDatabaseTables(tables, '事件').map((table) => table.tableName),
    ['event_log']
  )
  assert.deepEqual(
    filterDatabaseTables(tables, '').map((table) => table.tableName),
    ['user_order', 'event_log', 'payment_bill']
  )
})

test('selects visible table names and ignores nameless rows', () => {
  const tables = [
    { schemaName: 'public', tableName: 'users' },
    { schemaName: 'public', tableName: '' },
    { schemaName: 'audit', tableName: 'event_log' }
  ]

  assert.deepEqual(selectAllTableNames(tables), ['users', 'event_log'])
})

test('merges selected table names without dropping hidden selections', () => {
  const visibleTables = [
    { schemaName: 'public', tableName: 'users' },
    { schemaName: 'public', tableName: 'orders' }
  ]

  assert.deepEqual(
    mergeSelectedTableNames(['event_log', 'users'], visibleTables),
    ['event_log', 'users', 'orders']
  )
})

test('builds candidate keys and groups candidates by table', () => {
  const candidates = [
    { tableName: 'users', columnName: 'id', dataType: 'bigint' },
    { tableName: 'users', columnName: 'mobile_no', dataType: 'varchar' },
    { tableName: 'orders', columnName: 'order_no', dataType: 'varchar' }
  ]

  assert.equal(buildCandidateKey(candidates[1]), 'users.mobile_no')
  assert.deepEqual(groupFieldCandidatesByTable(candidates), [
    { tableName: 'users', candidates: [candidates[0], candidates[1]] },
    { tableName: 'orders', candidates: [candidates[2]] }
  ])
})

test('picks only selected field candidates for import', () => {
  const candidates = [
    { tableName: 'users', columnName: 'id' },
    { tableName: 'users', columnName: 'mobile_no' },
    { tableName: 'orders', columnName: 'order_no' }
  ]

  assert.deepEqual(
    pickSelectedCandidates(candidates, new Set(['users.mobile_no', 'orders.order_no'])),
    [candidates[1], candidates[2]]
  )
})
