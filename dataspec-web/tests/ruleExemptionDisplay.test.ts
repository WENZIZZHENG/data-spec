import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  isRuleExemptionExpired,
  normalizeRuleExemptionPayload,
  ruleExemptionScopeLabel,
  ruleExemptionStatusLabel,
  ruleExemptionStatusTagType
} from '../src/utils/ruleExemptionDisplay.ts'

test('formats rule exemption scope', () => {
  assert.equal(ruleExemptionScopeLabel({ tableName: 'UserOrder', columnName: 'userId' }), 'UserOrder.userId')
  assert.equal(ruleExemptionScopeLabel({ tableName: 'legacy_table' }), 'legacy_table.*')
  assert.equal(ruleExemptionScopeLabel({ columnName: 'legacy_id' }), '*.legacy_id')
})

test('formats rule exemption status', () => {
  const now = new Date('2026-06-27T00:00:00Z')
  assert.equal(ruleExemptionStatusLabel({ enabled: false }, now), '已禁用')
  assert.equal(ruleExemptionStatusTagType({ enabled: false }, now), 'info')
  assert.equal(isRuleExemptionExpired({ enabled: true, expiresAt: '2026-06-26T00:00:00Z' }, now), true)
  assert.equal(ruleExemptionStatusLabel({ enabled: true, expiresAt: '2026-06-26T00:00:00Z' }, now), '已过期')
  assert.equal(ruleExemptionStatusTagType({ enabled: true, expiresAt: '2026-06-28T00:00:00Z' }, now), 'success')
})

test('normalizes payload before submit', () => {
  assert.deepEqual(
    normalizeRuleExemptionPayload({
      projectId: 1,
      ruleCode: ' table_naming_snake_case ',
      tableName: ' ',
      columnName: ' userId ',
      reason: ' 历史字段兼容 '
    }),
    {
      projectId: 1,
      ruleCode: 'table_naming_snake_case',
      tableName: null,
      columnName: 'userId',
      reason: '历史字段兼容'
    }
  )
})
