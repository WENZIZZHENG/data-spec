import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildEvidenceRequest,
  buildHandoffEvidenceJson,
  evidenceSourceTypeLabel,
  formatHandoffTime,
  handoffStatusLabel,
  handoffStatusTagType,
  sanitizeHandoffText
} from '../src/utils/handoffEvidenceDisplay.ts'
import type { AiEvidencePackage } from '../src/types/index.ts'

test('builds evidence package request from safe handoff source metadata', () => {
  const request = buildEvidenceRequest(7, {
    sourceType: 'AI_TASK_RUN',
    sourceId: 91,
    sourceTitle: '修复 SQL 交接',
    status: 'FAILED'
  })

  assert.deepEqual(request, {
    projectId: 7,
    sourceType: 'AI_TASK_RUN',
    sourceId: 91,
    sourceTitle: '修复 SQL 交接'
  })
  assert.equal(buildEvidenceRequest(null, { sourceType: 'AI_JOB', sourceId: 11 }), null)
  assert.equal(buildEvidenceRequest(7, { sourceType: 'AI_JOB' }), null)
})

test('formats handoff source labels, status tags, and time fallback', () => {
  assert.equal(evidenceSourceTypeLabel('AI_TASK_RUN'), 'AI task run')
  assert.equal(evidenceSourceTypeLabel('SQL_CHECK'), 'SQL 检查')
  assert.equal(handoffStatusLabel('PARTIAL_FAILED'), '部分失败')
  assert.equal(handoffStatusTagType('FAILED'), 'danger')
  assert.equal(handoffStatusTagType('PARTIAL_FAILED'), 'warning')
  assert.equal(handoffStatusTagType('SUCCESS'), 'success')
  assert.equal(formatHandoffTime('2026-07-05T10:02:03.123Z'), '2026-07-05 10:02:03')
  assert.equal(formatHandoffTime(undefined), '-')
})

test('redacts sensitive handoff evidence text recursively', () => {
  const evidence: AiEvidencePackage = {
    kind: 'dataspec-ai-evidence',
    projectId: 7,
    source: { sourceType: 'AI_TASK_RUN', sourceId: 91, sourceTitle: '任务' },
    inputsSummary: {
      command: 'curl -H "Authorization: Bearer abc.def" http://example',
      jdbcUrl: 'jdbc:postgresql://user:secret@localhost:5432/app',
      nested: {
        password: 'should-not-leak',
        token: 'abc123'
      }
    },
    diagnostics: [
      { level: 'ERROR', code: 'CHECK_FAILED', message: 'password=raw token=raw' }
    ]
  }

  const json = buildHandoffEvidenceJson(evidence)
  assert.match(json, /Authorization: \*\*\*/)
  assert.match(json, /jdbc:\*\*\*/)
  assert.doesNotMatch(json, /abc\.def/)
  assert.doesNotMatch(json, /should-not-leak/)
  assert.doesNotMatch(json, /abc123/)
  assert.doesNotMatch(json, /password=raw/)
  assert.equal(sanitizeHandoffText('Authorization: Bearer secret-token'), 'Authorization: ***')
})

test('redacts non bearer authorization and database DSN text', () => {
  assert.equal(sanitizeHandoffText('Authorization: Basic abc123'), 'Authorization: ***')
  assert.equal(sanitizeHandoffText('authorization=Digest username="root", response="abc123"'), 'authorization=***')
  assert.equal(sanitizeHandoffText('Authorization: ApiKey abc123'), 'Authorization: ***')
  assert.equal(sanitizeHandoffText('dsn postgresql://user:pass@localhost:5432/app'), 'dsn postgresql://***')
  assert.equal(sanitizeHandoffText('mysql://user:pass@localhost:3306/app'), 'mysql://***')
  assert.equal(sanitizeHandoffText('{"authorization":"Basic abc123","safe":"keep"}'), '{"authorization":"***","safe":"keep"}')
})
