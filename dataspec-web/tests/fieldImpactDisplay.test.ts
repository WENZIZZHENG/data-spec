import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  criticalFieldChanged,
  fieldImpactSummaryText,
  impactSeverityTagType,
  impactTypeLabel,
  warningSummaryText
} from '../src/utils/fieldImpactDisplay.ts'

test('formats impact type and severity', () => {
  assert.equal(impactTypeLabel('TEMPLATE'), '表模板')
  assert.equal(impactTypeLabel('IMPORT_SOURCE'), '导入来源')
  assert.equal(impactTypeLabel('SQL_CHECK'), 'SQL 检查')
  assert.equal(impactTypeLabel('STANDARD_SNAPSHOT'), '标准快照')
  assert.equal(impactSeverityTagType('HIGH'), 'danger')
  assert.equal(impactSeverityTagType('WARNING'), 'warning')
  assert.equal(impactSeverityTagType('INFO'), 'info')
})

test('builds summary and warning text', () => {
  assert.equal(
    fieldImpactSummaryText({
      totalImpactCount: 4,
      templateImpactCount: 1,
      importSourceImpactCount: 1,
      sqlCheckImpactCount: 1,
      snapshotImpactCount: 1
    }),
    '共 4 项影响：模板 1，导入来源 1，SQL 1，快照 1'
  )
  assert.equal(warningSummaryText([{ attribute: 'name' }, { attribute: 'dataType' }]), '字段名、数据类型')
})

test('detects critical field changes', () => {
  assert.equal(criticalFieldChanged({ name: 'user_id' }, { name: 'user_id' }), false)
  assert.equal(criticalFieldChanged({ dataType: 'bigint' }, { dataType: 'varchar' }), true)
  assert.equal(criticalFieldChanged({ sensitive: false }, { sensitive: true }), true)
  assert.equal(criticalFieldChanged({ codeSetId: 1 }, { codeSetId: undefined }), true)
})
