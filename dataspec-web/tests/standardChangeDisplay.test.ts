import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  shouldShowStandardChangeConfirm,
  standardChangeAttributeLabel,
  standardChangeChangedAttributes,
  standardChangeConfirmMessage,
  standardChangeRiskTagType,
  standardChangeRiskText
} from '../src/utils/standardChangeDisplay.ts'

test('formats standard change risk and attributes', () => {
  assert.equal(standardChangeRiskTagType('HIGH'), 'danger')
  assert.equal(standardChangeRiskTagType('WARNING'), 'warning')
  assert.equal(standardChangeRiskTagType('INFO'), 'info')
  assert.equal(standardChangeRiskText('HIGH'), '高风险')
  assert.equal(standardChangeAttributeLabel('dataType'), '数据类型')
  assert.equal(standardChangeAttributeLabel('formatUnit'), '单位')
  assert.deepEqual(
    standardChangeChangedAttributes([{ attribute: 'name' }, { attribute: 'validExamplesJson' }, { attribute: 'paramsJson' }]),
    ['字段名', '正例', '规则参数']
  )
})

test('builds preview confirm message and confirmation condition', () => {
  const preview = {
    riskLevel: 'WARNING' as const,
    requiresConfirmation: true,
    summary: '将修改 2 个字段属性，已知影响 1 项',
    changes: [{ attribute: 'name' }, { attribute: 'aliases' }],
    impacts: [{ impactType: 'TEMPLATE' }],
    validationCommands: ['node tools/dataspec-cli.mjs lint a.sql --project 1'],
    currentSnapshot: {
      versioned: true,
      specVersion: 'v1'
    }
  }

  assert.equal(shouldShowStandardChangeConfirm(preview), true)
  assert.match(standardChangeConfirmMessage(preview), /字段名、别名/)
  assert.match(standardChangeConfirmMessage(preview), /当前快照：v1/)
})

test('does not require confirmation without effective changes', () => {
  assert.equal(shouldShowStandardChangeConfirm({ requiresConfirmation: true, changes: [] }), false)
})
