import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildBrowserCandidateKeySet,
  buildMetadataBrowserAiSummary,
  filterMetadataBrowserRows,
  flattenMetadataBrowserRows
} from '../src/utils/databaseMetadataBrowser.ts'

const browser = {
  databaseType: 'POSTGRESQL',
  databaseName: 'demo',
  schemaName: 'public',
  selectedTableNames: ['user_order'],
  summary: {
    tableCount: 1,
    columnCount: 3,
    indexCount: 1,
    candidateCount: 1,
    missingCommentCount: 1,
    changedCount: 1,
    unmanagedCount: 1
  },
  tables: [
    {
      schemaName: 'public',
      tableName: 'user_order',
      comment: '用户订单',
      indexes: [
        { indexName: 'idx_user_order_mobile', columnName: 'mobile_no', nonUnique: true }
      ],
      columns: [
        {
          tableName: 'user_order',
          columnName: 'id',
          dataType: 'bigint',
          comment: '主键',
          matchStatus: 'MATCHED',
          standardFieldName: 'id'
        },
        {
          tableName: 'user_order',
          columnName: 'mobile_no',
          dataType: 'varchar(20)',
          comment: '手机号',
          matchStatus: 'CHANGED',
          standardFieldName: 'mobile_no',
          typeChanged: true
        },
        {
          tableName: 'user_order',
          columnName: 'buyer_name',
          dataType: 'varchar(50)',
          comment: '',
          matchStatus: 'NEW',
          importCandidate: true,
          selectedByDefault: true,
          candidateKey: 'user_order.buyer_name',
          matchReason: '未命中已有标准字段'
        }
      ]
    }
  ],
  aiReadableSummary: '后端摘要'
}

test('flattens metadata browser rows with table index context', () => {
  const rows = flattenMetadataBrowserRows(browser)

  assert.equal(rows.length, 3)
  assert.deepEqual(rows[1].indexNames, ['idx_user_order_mobile'])
  assert.equal(rows[2].importCandidate, true)
  assert.equal(rows[2].candidateKey, 'user_order.buyer_name')
})

test('filters metadata browser rows by schema, table, column, comment, type, index and standard match', () => {
  const rows = flattenMetadataBrowserRows(browser)

  assert.deepEqual(filterMetadataBrowserRows(rows, 'public').map((row) => row.columnName), ['id', 'mobile_no', 'buyer_name'])
  assert.deepEqual(filterMetadataBrowserRows(rows, '用户订单').map((row) => row.columnName), ['id', 'mobile_no', 'buyer_name'])
  assert.deepEqual(filterMetadataBrowserRows(rows, 'buyer').map((row) => row.columnName), ['buyer_name'])
  assert.deepEqual(filterMetadataBrowserRows(rows, '手机号').map((row) => row.columnName), ['mobile_no'])
  assert.deepEqual(filterMetadataBrowserRows(rows, 'varchar(50)').map((row) => row.columnName), ['buyer_name'])
  assert.deepEqual(filterMetadataBrowserRows(rows, 'idx_user_order_mobile').map((row) => row.columnName), ['mobile_no'])
  assert.deepEqual(filterMetadataBrowserRows(rows, 'mobile_no').map((row) => row.columnName), ['mobile_no'])
})

test('builds default selected browser candidate key set', () => {
  assert.deepEqual(
    Array.from(buildBrowserCandidateKeySet(browser)).sort(),
    ['user_order.buyer_name']
  )
})

test('builds AI readable summary from browser payload without secrets', () => {
  const summary = buildMetadataBrowserAiSummary({
    ...browser,
    aiReadableSummary: '数据库 demo user_order buyer_name idx_user_order_mobile password=secret jdbc:postgresql://localhost/demo'
  })

  assert.match(summary, /user_order/)
  assert.match(summary, /buyer_name/)
  assert.match(summary, /idx_user_order_mobile/)
  assert.doesNotMatch(summary, /password=secret/)
  assert.doesNotMatch(summary, /jdbc:postgresql/)
})
