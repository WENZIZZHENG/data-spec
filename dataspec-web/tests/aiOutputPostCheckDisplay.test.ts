import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiOutputPostCheckSummary,
  aiOutputPostCheckTagType,
  blockingPostCheckRefs,
  buildAiOutputPostCheckCommand,
  buildPostCheckInputFromPayload,
  buildSnapshotRef,
  replacementPostCheckRefs
} from '../src/utils/aiOutputPostCheckDisplay.ts'

test('formats post-check status and summary', () => {
  assert.equal(aiOutputPostCheckTagType('PASS'), 'success')
  assert.equal(aiOutputPostCheckTagType('WARN'), 'warning')
  assert.equal(aiOutputPostCheckTagType('FAIL'), 'danger')
  assert.equal(aiOutputPostCheckSummary({
    status: 'WARN',
    safeToUse: false,
    summary: { totalRefCount: 2 },
    issues: [{ code: 'STALE_STANDARD_REFERENCE' }]
  }), 'WARN / 问题 1 / 已解析 2')
})

test('extracts blocking and replacement refs', () => {
  const result = {
    status: 'FAIL',
    safeToUse: false,
    issues: [
      { code: 'UNKNOWN_STANDARD_REFERENCE', severity: 'FAIL', inputRef: 'old_phone' },
      { code: 'STALE_STANDARD_REFERENCE', severity: 'WARN', inputRef: 'old_phone', replacementRef: 'field:1:20' }
    ],
    suggestedFixes: ['改用 replacementRef field:1:20 后再复核。'],
    evidenceLinks: ['dataspec://evidence/field:1:20']
  }

  assert.deepEqual(blockingPostCheckRefs(result), ['old_phone'])
  assert.deepEqual(replacementPostCheckRefs(result), ['field:1:20'])
})

test('builds content input from common ai output payloads', () => {
  assert.deepEqual(buildPostCheckInputFromPayload({ ddl: 'CREATE TABLE user_account (id bigint);' }), {
    contentType: 'DDL',
    content: 'CREATE TABLE user_account (id bigint);'
  })
  assert.deepEqual(buildPostCheckInputFromPayload({ fixedSql: 'select user_id from users;' }), {
    contentType: 'SQL',
    content: 'select user_id from users;'
  })
  assert.equal(buildPostCheckInputFromPayload({ message: 'hello' }).contentType, 'JSON')
})

test('builds snapshot refs and CLI command without local paths', () => {
  assert.equal(buildSnapshotRef(7, 12, 'v1'), 'snapshot:7:12')
  assert.equal(buildSnapshotRef(7, undefined, 'v1'), 'snapshot:7:v1')
  assert.equal(buildSnapshotRef(null, 12, 'v1'), undefined)
  assert.equal(
    buildAiOutputPostCheckCommand({ projectId: 7, contentType: 'SQL', snapshotRef: 'snapshot:7:v1' }),
    'node tools/dataspec-cli.mjs ai-output check --project 7 --type SQL --file <generated-output> --snapshot-ref snapshot:7:v1 --format json'
  )
  assert.equal(
    buildAiOutputPostCheckCommand({ projectId: 7, contentType: 'PLAIN_TEXT' }),
    'node tools/dataspec-cli.mjs ai-output check --project 7 --type TEXT --file <generated-output> --format json'
  )
})
