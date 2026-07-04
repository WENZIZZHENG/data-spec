import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  conflictFieldEditQuery,
  conflictFieldSummary,
  conflictSeverityTagType,
  conflictTypeLabel,
  filterConflictGroups,
  isNamingRiskType
} from '../src/utils/fieldConflictDisplay.ts'

test('formats conflict type and severity', () => {
  assert.equal(conflictTypeLabel('ALIAS_CONFLICT'), '别名冲突')
  assert.equal(conflictTypeLabel('SEMANTIC_DUPLICATE'), '语义疑似重复')
  assert.equal(conflictTypeLabel('RESERVED_WORD'), 'SQL 保留字')
  assert.equal(conflictTypeLabel('DANGEROUS_SQL_NAME'), 'SQL 危险命名')
  assert.equal(conflictTypeLabel('CASE_COLLISION'), '大小写碰撞')
  assert.equal(conflictTypeLabel('AMBIGUOUS_ALIAS'), 'Alias 歧义')
  assert.equal(conflictSeverityTagType('ERROR'), 'danger')
  assert.equal(conflictSeverityTagType('WARNING'), 'warning')
  assert.equal(conflictSeverityTagType('INFO'), 'info')
  assert.equal(isNamingRiskType('RESERVED_WORD'), true)
  assert.equal(isNamingRiskType('ALIAS_CONFLICT'), false)
})

test('filters conflict groups by severity and type', () => {
  const groups = [
    { severity: 'ERROR' as const, conflictType: 'ALIAS_CONFLICT' as const },
    { severity: 'INFO' as const, conflictType: 'SEMANTIC_DUPLICATE' as const },
    { severity: 'WARNING' as const, conflictType: 'RESERVED_WORD' as const }
  ]
  assert.equal(filterConflictGroups(groups, 'ERROR', 'ALL').length, 1)
  assert.equal(filterConflictGroups(groups, 'ALL', 'SEMANTIC_DUPLICATE').length, 1)
  assert.equal(filterConflictGroups(groups, 'ALL', 'RESERVED_WORD').length, 1)
})

test('builds field edit query and summary', () => {
  assert.deepEqual(conflictFieldEditQuery({ fieldId: 3, name: 'mobile_no' }), {
    keyword: 'mobile_no',
    fieldId: '3'
  })
  assert.equal(conflictFieldSummary([{ name: 'mobile_no' }, { displayName: '联系电话' }]), 'mobile_no、联系电话')
})
