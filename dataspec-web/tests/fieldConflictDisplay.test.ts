import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  conflictFieldEditQuery,
  conflictFieldSummary,
  conflictSeverityTagType,
  conflictTypeLabel,
  filterConflictGroups
} from '../src/utils/fieldConflictDisplay.ts'

test('formats conflict type and severity', () => {
  assert.equal(conflictTypeLabel('ALIAS_CONFLICT'), '别名冲突')
  assert.equal(conflictTypeLabel('SEMANTIC_DUPLICATE'), '语义疑似重复')
  assert.equal(conflictSeverityTagType('ERROR'), 'danger')
  assert.equal(conflictSeverityTagType('WARNING'), 'warning')
  assert.equal(conflictSeverityTagType('INFO'), 'info')
})

test('filters conflict groups by severity and type', () => {
  const groups = [
    { severity: 'ERROR' as const, conflictType: 'ALIAS_CONFLICT' as const },
    { severity: 'INFO' as const, conflictType: 'SEMANTIC_DUPLICATE' as const }
  ]
  assert.equal(filterConflictGroups(groups, 'ERROR', 'ALL').length, 1)
  assert.equal(filterConflictGroups(groups, 'ALL', 'SEMANTIC_DUPLICATE').length, 1)
})

test('builds field edit query and summary', () => {
  assert.deepEqual(conflictFieldEditQuery({ fieldId: 3, name: 'mobile_no' }), {
    keyword: 'mobile_no',
    fieldId: '3'
  })
  assert.equal(conflictFieldSummary([{ name: 'mobile_no' }, { displayName: '联系电话' }]), 'mobile_no、联系电话')
})
