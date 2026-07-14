import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildTokenEvidenceCandidateApplyReq,
  formatCandidateEvidence,
  isStandardCandidateDecidable,
  standardCandidateSourceLabel,
  standardCandidateStatusLabel,
  standardCandidateStatusTag,
  shouldHandleStandardCandidateListResult,
  shouldHandleTokenEvidenceCandidateApplyResult,
  tokenEvidencePreviewStatusLabel,
  tokenEvidenceResolutionLabel,
  tokenEvidenceSignalLabel
} from '../src/utils/standardCandidateDisplay.ts'
import type { TokenEvidenceCandidatePreviewReq } from '../src/types/index.ts'

test('formats standard candidate status and source labels', () => {
  assert.equal(standardCandidateStatusLabel('PENDING'), '待处理')
  assert.equal(standardCandidateStatusLabel('MERGED'), '已合并')
  assert.equal(standardCandidateStatusTag('ACCEPTED'), 'success')
  assert.equal(standardCandidateStatusTag('POSTPONED'), 'warning')
  assert.equal(standardCandidateSourceLabel('AI_FEEDBACK'), 'AI 反馈')
  assert.equal(standardCandidateSourceLabel('TOKEN_EVIDENCE'), '命名证据')
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

test('formats token evidence preview and signal labels', () => {
  assert.equal(tokenEvidencePreviewStatusLabel('READY'), '可以写入候选')
  assert.equal(tokenEvidencePreviewStatusLabel('NAME_CONFLICT'), '存在同名候选')
  assert.equal(tokenEvidenceSignalLabel('AMBIGUOUS_ABBREVIATION'), '歧义缩写')
  assert.equal(tokenEvidenceResolutionLabel('UNRESOLVED'), '未解析')
})

test('requires a ready preview and explicit confirmation before token evidence apply', () => {
  const previewInput: TokenEvidenceCandidatePreviewReq = {
    projectId: 1,
    candidateName: 'order_amount',
    dataType: 'decimal(18,2)',
    sourceRef: 'field:orders.ord_amt'
  }

  assert.equal(buildTokenEvidenceCandidateApplyReq(previewInput, {
    status: 'READY',
    dryRunToken: 'signed-preview'
  }, false), null)
  assert.equal(buildTokenEvidenceCandidateApplyReq(previewInput, {
    status: 'NAME_CONFLICT',
    dryRunToken: 'signed-preview'
  }, true), null)
  assert.deepEqual(buildTokenEvidenceCandidateApplyReq(previewInput, {
    status: 'READY',
    dryRunToken: 'signed-preview'
  }, true), {
    previewInput,
    dryRunToken: 'signed-preview',
    confirmed: true
  })
})

test('ignores stale token evidence apply results after project or dialog context changes', () => {
  const result = {
    candidate: {
      projectId: 1
    }
  }
  const currentGuard = {
    requestId: 3,
    currentRequestId: 3,
    requestedProjectId: 1,
    currentProjectId: 1,
    dialogVisible: true
  }

  assert.equal(shouldHandleTokenEvidenceCandidateApplyResult(result, currentGuard), true)
  assert.equal(shouldHandleTokenEvidenceCandidateApplyResult(result, {
    ...currentGuard,
    currentRequestId: 4
  }), false)
  assert.equal(shouldHandleTokenEvidenceCandidateApplyResult(result, {
    ...currentGuard,
    currentProjectId: 2
  }), false)
  assert.equal(shouldHandleTokenEvidenceCandidateApplyResult(result, {
    ...currentGuard,
    dialogVisible: false
  }), false)
  assert.equal(shouldHandleTokenEvidenceCandidateApplyResult({
    candidate: { projectId: 2 }
  }, currentGuard), false)
})

test('ignores stale standard candidate list results after project or query changes', () => {
  const current = {
    requestId: 4,
    currentRequestId: 4,
    requestedProjectId: 2,
    currentProjectId: 2,
    requestedQueryKey: '[2,"PENDING","TOKEN_EVIDENCE",null,1,10]',
    currentQueryKey: '[2,"PENDING","TOKEN_EVIDENCE",null,1,10]'
  }

  assert.equal(shouldHandleStandardCandidateListResult(current), true)
  assert.equal(shouldHandleStandardCandidateListResult({ ...current, currentRequestId: 5 }), false)
  assert.equal(shouldHandleStandardCandidateListResult({ ...current, currentProjectId: 3 }), false)
  assert.equal(shouldHandleStandardCandidateListResult({
    ...current,
    currentQueryKey: '[2,"PENDING","MANUAL",null,1,10]'
  }), false)
})
