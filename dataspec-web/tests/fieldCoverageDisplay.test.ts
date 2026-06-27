import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  coverageStatusLabel,
  filterCoverageFields,
  formatCoverageRate
} from '../src/utils/fieldCoverageDisplay.ts'

test('formats field coverage status and rate', () => {
  assert.equal(coverageStatusLabel('STANDARD_MATCH'), '标准命中')
  assert.equal(coverageStatusLabel('POSSIBLE_DUPLICATE'), '疑似重复')
  assert.equal(formatCoverageRate(66.666), '66.7%')
  assert.equal(formatCoverageRate(undefined), '0.0%')
})

test('filters coverage fields by table and status', () => {
  const tables = [
    {
      tableName: 'users',
      fields: [
        { tableName: 'users', columnName: 'id', status: 'STANDARD_MATCH' },
        { tableName: 'users', columnName: 'nickname', status: 'UNMANAGED' }
      ]
    },
    {
      tableName: 'orders',
      fields: [
        { tableName: 'orders', columnName: 'phone', status: 'ALIAS_MATCH' }
      ]
    }
  ]

  assert.deepEqual(
    filterCoverageFields(tables, 'users', 'ALL').map((field) => field.columnName),
    ['id', 'nickname']
  )
  assert.deepEqual(
    filterCoverageFields(tables, 'ALL', 'ALIAS_MATCH').map((field) => field.columnName),
    ['phone']
  )
})
