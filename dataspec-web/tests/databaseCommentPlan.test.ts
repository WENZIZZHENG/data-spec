import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildCommentPlanCopyPayload,
  buildCommentPlanTableGroups,
  commentPlanRiskLabel,
  commentPlanRiskTagType,
  commentPlanStatusLabel
} from '../src/utils/databaseCommentPlan.ts'

const commentPlan = {
  kind: 'dataspec-database-comment-patch-plan',
  databaseType: 'POSTGRESQL',
  databaseName: 'demo',
  schemaName: 'public',
  metadataFingerprint: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
  planHash: '123456abcdef7890123456abcdef7890123456abcdef7890123456abcdef7890',
  riskLevel: 'MEDIUM',
  summary: {
    tableCount: 1,
    columnCount: 2,
    executableChangeCount: 1,
    unsupportedCount: 1,
    blockedCount: 1
  },
  dialectSupport: {
    databaseType: 'POSTGRESQL',
    tableCommentSqlSupported: true,
    columnCommentSqlSupported: true
  },
  safety: {
    readOnly: true,
    writesSourceDatabase: false,
    writesProject: false,
    safeForAiCopy: true
  },
  items: [
    {
      objectType: 'TABLE',
      tableName: 'user_order',
      status: 'CHANGED',
      currentComment: '旧订单表',
      targetComment: '用户订单',
      commentDiff: '旧订单表 -> 用户订单',
      riskLevel: 'LOW',
      dryRunSql: 'COMMENT ON TABLE "public"."user_order" IS \'用户订单\';'
    },
    {
      objectType: 'COLUMN',
      tableName: 'user_order',
      columnName: 'buyer_mobile',
      standardFieldName: 'mobile_no',
      status: 'UNSUPPORTED',
      currentComment: '',
      targetComment: '买家手机号',
      commentDiff: '缺少注释 -> 买家手机号',
      riskLevel: 'MEDIUM',
      blockedReasons: ['MySQL 列注释需要完整列定义 password=secret jdbc:postgresql://db.internal/app https://user:pass@example.com/path']
    }
  ],
  dryRunSql: 'COMMENT ON TABLE "public"."user_order" IS \'用户订单\';',
  rollbackHint: '恢复旧注释；Authorization: Bearer raw-token Authorization: Basic raw-basic',
  nextActions: ['先审阅 dry-run SQL，再交给迁移工具。']
}

test('formats comment plan risk and status labels for display', () => {
  assert.equal(commentPlanRiskLabel('SAFE'), '安全')
  assert.equal(commentPlanRiskLabel('MEDIUM'), '需确认')
  assert.equal(commentPlanRiskTagType('SAFE'), 'success')
  assert.equal(commentPlanRiskTagType('HIGH'), 'danger')
  assert.equal(commentPlanStatusLabel('NO_OP'), '无需变更')
  assert.equal(commentPlanStatusLabel('UNSUPPORTED'), '不支持')
})

test('groups comment plan items by table for reverse import page display', () => {
  const groups = buildCommentPlanTableGroups(commentPlan)

  assert.equal(groups.length, 1)
  assert.equal(groups[0].tableName, 'user_order')
  assert.equal(groups[0].items.length, 2)
  assert.equal(groups[0].items[0].objectType, 'TABLE')
  assert.equal(groups[0].items[1].columnName, 'buyer_mobile')
})

test('builds copyable comment plan payload without leaking secrets', () => {
  const payload = buildCommentPlanCopyPayload(commentPlan)

  assert.match(payload, /dataspec-database-comment-patch-plan/)
  assert.match(payload, /metadataFingerprint=abcdef123456/)
  assert.match(payload, /dryRunSql/)
  assert.match(payload, /buyer_mobile/)
  assert.match(payload, /先审阅 dry-run SQL/)
  assert.doesNotMatch(payload, /password=secret/)
  assert.doesNotMatch(payload, /jdbc:postgresql/)
  assert.doesNotMatch(payload, /user:pass@example/)
  assert.doesNotMatch(payload, /raw-token/)
  assert.doesNotMatch(payload, /raw-basic/)
  assert.doesNotMatch(payload, /Authorization/)
})
