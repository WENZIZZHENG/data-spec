import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiJobStatusTagType,
  aiJobTypeLabel,
  buildReplayJson,
  formatAiJobTime
} from '../src/utils/aiReplayDisplay.ts'

test('formats ai job type and status', () => {
  assert.equal(aiJobTypeLabel('CREATE_TABLE_PROMPT'), '建表 Prompt')
  assert.equal(aiJobTypeLabel('SQL_LINT_FIX'), 'SQL 检查修正')
  assert.equal(aiJobTypeLabel('UNKNOWN_TYPE'), 'UNKNOWN_TYPE')
  assert.equal(aiJobStatusTagType('SUCCESS'), 'success')
  assert.equal(aiJobStatusTagType('FAILED'), 'danger')
})

test('builds stable replay json text', () => {
  const text = buildReplayJson({
    kind: 'dataspec-ai-replay',
    jobType: 'DDL_PREVIEW',
    projectId: 1
  })

  assert.match(text, /"kind": "dataspec-ai-replay"/)
  assert.match(text, /"jobType": "DDL_PREVIEW"/)
})

test('formats ai job time fallback', () => {
  assert.equal(formatAiJobTime(undefined), '-')
  assert.equal(formatAiJobTime('2026-06-27T10:30:00'), '2026-06-27 10:30:00')
})
