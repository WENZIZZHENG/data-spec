import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  formatCandidateEvidence,
  isStandardCandidateDecidable,
  standardCandidateSourceLabel,
  standardCandidateStatusLabel,
  standardCandidateStatusTag
} from '../src/utils/standardCandidateDisplay.ts'

test('formats standard candidate status and source labels', () => {
  assert.equal(standardCandidateStatusLabel('PENDING'), '待处理')
  assert.equal(standardCandidateStatusLabel('MERGED'), '已合并')
  assert.equal(standardCandidateStatusTag('ACCEPTED'), 'success')
  assert.equal(standardCandidateStatusTag('POSTPONED'), 'warning')
  assert.equal(standardCandidateSourceLabel('AI_FEEDBACK'), 'AI 反馈')
})

test('detects decidable candidate statuses', () => {
  assert.equal(isStandardCandidateDecidable({ status: 'PENDING' }), true)
  assert.equal(isStandardCandidateDecidable({ status: 'POSTPONED' }), true)
  assert.equal(isStandardCandidateDecidable({ status: 'ACCEPTED' }), false)
})

test('formats candidate evidence preview', () => {
  assert.equal(formatCandidateEvidence(undefined), '-')
  assert.equal(formatCandidateEvidence('abc'), 'abc')
  assert.equal(formatCandidateEvidence('x'.repeat(130)).endsWith('...'), true)
})
