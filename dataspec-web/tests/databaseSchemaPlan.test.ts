import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildSchemaPlanAiSummary,
  schemaChangeActionLabel,
  schemaRiskLabel,
  schemaRiskTagType
} from '../src/utils/databaseSchemaPlan.ts'

const plan = {
  kind: 'dataspec-database-schema-change-plan',
  databaseType: 'POSTGRESQL',
  databaseName: 'demo',
  schemaName: 'public',
  currentSchemaHash: 'a'.repeat(64),
  targetSpecHash: 'b'.repeat(64),
  riskLevel: 'BLOCKED',
  summary: {
    tableCount: 1,
    columnCount: 3,
    changeCount: 2,
    blockedCount: 1
  },
  changeSet: [
    {
      tableName: 'user_order',
      columnName: 'phone',
      action: 'ALTER_COMMENT',
      property: 'comment',
      riskLevel: 'LOW',
      targetValue: '手机号'
    },
    {
      tableName: 'user_order',
      columnName: 'legacy_token',
      action: 'DROP_CANDIDATE',
      property: 'column',
      riskLevel: 'HIGH',
      currentValue: 'varchar(100)',
      blockedReasons: ['需要人工确认 password=secret jdbc:postgresql://demo']
    }
  ],
  migrationSql: 'COMMENT ON COLUMN "user_order"."phone" IS \'手机号\';',
  manualChecks: ['确认 legacy_token 是否仍被业务引用'],
  blockedReasons: ['legacy_token 需要人工确认'],
  nextActions: ['高风险或阻塞项需要人工确认后再交给迁移工具。']
}

test('formats schema plan risk and actions for display', () => {
  assert.equal(schemaRiskLabel('BLOCKED'), '阻塞')
  assert.equal(schemaRiskTagType('BLOCKED'), 'danger')
  assert.equal(schemaRiskTagType('LOW'), 'success')
  assert.equal(schemaChangeActionLabel('ALTER_COMMENT'), '注释修正')
  assert.equal(schemaChangeActionLabel('DROP_CANDIDATE'), '删除候选')
})

test('builds AI summary without leaking secret-like values', () => {
  const summary = buildSchemaPlanAiSummary(plan)

  assert.match(summary, /DataSpec schema change plan/)
  assert.match(summary, /risk=BLOCKED/)
  assert.match(summary, /legacy_token/)
  assert.match(summary, /currentSchemaHash=aaaaaaaaaaaa/)
  assert.doesNotMatch(summary, /password=secret/)
  assert.doesNotMatch(summary, /jdbc:postgresql/)
})
