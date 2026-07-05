import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  blockingMergeRisks,
  buildFieldMergeApplyPayload,
  buildFieldMergePreviewPayload,
  fieldMergeRiskTagType,
  submitFieldMergeApply
} from '../src/utils/standardFieldMerge.ts'
import type { StandardFieldMergePreview } from '../src/types/index.ts'

test('builds standard field merge preview and apply payloads', () => {
  assert.deepEqual(buildFieldMergePreviewPayload(1, 10, 20), {
    projectId: 1,
    targetFieldId: 10,
    sourceFieldId: 20
  })
  assert.deepEqual(buildFieldMergeApplyPayload(1, 10, 20, '  统一使用 mobile_no  '), {
    projectId: 1,
    targetFieldId: 10,
    sourceFieldId: 20,
    reason: '统一使用 mobile_no'
  })
})

test('formats merge risks and blocks apply for blank reason without calling api', async () => {
  const preview: StandardFieldMergePreview = {
    risks: [
      { code: 'ALIAS_OWNER_CONFLICT', severity: 'ERROR', blocking: true },
      { code: 'NULLABILITY_MISMATCH', severity: 'WARNING', blocking: false }
    ]
  }
  let calls = 0

  assert.equal(fieldMergeRiskTagType('ERROR'), 'danger')
  assert.equal(fieldMergeRiskTagType('WARNING'), 'warning')
  assert.equal(blockingMergeRisks(preview).length, 1)

  const result = await submitFieldMergeApply(preview, ' ', async () => {
    calls += 1
    return { applied: true }
  })

  assert.equal(result.submitted, false)
  assert.match(result.error ?? '', /合并原因/)
  assert.equal(calls, 0)
})

test('submits merge apply when reason is present and preview has no blocking risks', async () => {
  const preview: StandardFieldMergePreview = {
    projectId: 1,
    target: { id: 10 },
    source: { id: 20 },
    risks: []
  }
  let calls = 0

  const result = await submitFieldMergeApply(preview, '统一字段', async (payload) => {
    calls += 1
    assert.equal(payload.reason, '统一字段')
    return { applied: true }
  })

  assert.equal(calls, 1)
  assert.equal(result.submitted, true)
  assert.equal(result.result?.applied, true)
})

test('rejects stale merge preview when current selection changed', async () => {
  const preview: StandardFieldMergePreview = {
    projectId: 1,
    target: { id: 10 },
    source: { id: 20 },
    risks: []
  }
  let calls = 0

  const result = await submitFieldMergeApply(preview, '统一字段', async () => {
    calls += 1
    return { applied: true }
  }, {
    projectId: 1,
    targetFieldId: 30,
    sourceFieldId: 20
  })

  assert.equal(result.submitted, false)
  assert.match(result.error ?? '', /重新生成合并预览/)
  assert.equal(calls, 0)
})
