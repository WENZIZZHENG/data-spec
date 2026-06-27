import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  fieldQualityEditQuery,
  filterQualityItems,
  issueOptions,
  qualityLevelLabel,
  qualityLevelTagType,
  qualitySeverityTagType
} from '../src/utils/fieldQualityDisplay.ts'
import type { FieldQualityItem } from '../src/types/index.ts'

test('formats quality level and severity labels', () => {
  assert.equal(qualityLevelLabel('GOOD'), '良好')
  assert.equal(qualityLevelLabel('WARNING'), '待完善')
  assert.equal(qualityLevelLabel('POOR'), '低质量')
  assert.equal(qualityLevelTagType('POOR'), 'danger')
  assert.equal(qualitySeverityTagType('ERROR'), 'danger')
})

test('filters quality items by level and issue code', () => {
  const items: FieldQualityItem[] = [
    { name: 'mobile_no', level: 'GOOD', issues: [] },
    { name: 'user_phone', level: 'WARNING', issues: [{ code: 'sensitive_not_marked' }] },
    { name: 'custom_flag', level: 'POOR', issues: [{ code: 'comment_missing' }] }
  ]

  assert.deepEqual(filterQualityItems(items, 'ALL', 'sensitive_not_marked').map((item) => item.name), ['user_phone'])
  assert.deepEqual(filterQualityItems(items, 'POOR', 'ALL').map((item) => item.name), ['custom_flag'])
  assert.deepEqual(issueOptions(items), ['comment_missing', 'sensitive_not_marked'])
})

test('builds field library edit query', () => {
  assert.deepEqual(fieldQualityEditQuery({ fieldId: 42, name: 'mobile_no' }), {
    keyword: 'mobile_no',
    fieldId: '42'
  })
  assert.deepEqual(fieldQualityEditQuery({ displayName: '手机号' }), {
    keyword: '手机号'
  })
})
