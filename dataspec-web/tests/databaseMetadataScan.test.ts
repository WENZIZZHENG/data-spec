import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildScanEvidenceSummary,
  buildScanFailureSummary,
  buildScanResumeSummary,
  buildSourcePressureHintText,
  buildMetadataCacheSummary,
  currentScanTableNames,
  currentSuccessfulScanTableNames,
  mergeScanTableNames,
  metadataCacheStatusLabel,
  selectSuccessfulPartialTableNames,
  scanJobStatusLabel,
  scanProgressLabel
} from '../src/utils/databaseMetadataScan.ts'

const scanResult = {
  scanId: 'scan-demo',
  scanJobId: 'scan-job-demo',
  status: 'PARTIAL',
  estimatedTableCount: 120,
  cursor: '40',
  resumeCursor: '40',
  cancelToken: 'cancel-demo',
  pageSize: 40,
  tables: [
    { schemaName: 'public', tableName: 'user_order', comment: '用户订单' },
    { schemaName: 'public', tableName: 'payment_bill', comment: '支付账单' }
  ],
  progress: {
    processedTableCount: 40,
    remainingTableEstimate: 80,
    pageSize: 40,
    hasMore: true
  },
  sourcePressureHint: {
    level: 'WARNING',
    message: '请求 pageSize=500，已限制为 40，dsn=postgres://secret@localhost/demo Authorization=Bearer abc',
    suggestedPageSize: 40,
    safeNextActions: ['降低 pageSize 后使用 resumeCursor=40 继续']
  },
  partialResult: {
    successfulTableNames: ['user_order'],
    failedTableNames: ['payment_bill'],
    skippedTableNames: ['audit_log'],
    completeForPreview: true,
    completeForCoverage: true,
    complete: false
  },
  failureSummary: {
    failedTableCount: 1,
    retryable: true,
    failedTables: [
      {
        schemaName: 'public',
        tableName: 'payment_bill',
        category: 'PERMISSION_DENIED',
        retryable: true,
        message: 'permission denied password=secret Authorization: Bearer abc jdbc:postgresql://localhost/demo'
      }
    ],
    safeNextActions: ['降低 pageSize 后重试']
  },
  evidence: {
    scanJobId: 'scan-job-demo',
    status: 'PARTIAL',
    processedTableCount: 40,
    failedTableCount: 1,
    schemaScope: 'public',
    tableScope: ['user_order', 'payment_bill'],
    metadataFingerprint: 'abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789',
    schemaOnly: true,
    noSourceWrites: true,
    noStandardWrites: true,
    safeForAiCopy: true,
    nextActions: ['继续 resumeCursor=40 Authorization: Bearer abc']
  },
  resumeCommand: '继续 scanJobId=scan-job-demo resumeCursor=40 pageSize=40 password=secret jdbc:postgresql://localhost/demo',
  metadataCache: {
    metadataFingerprint: '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
    cacheHit: true,
    stale: false,
    refreshMode: 'AUTO',
    lastSeenAt: '2026-07-06T10:00:00Z',
    expiresAt: '2026-07-07T10:00:00Z',
    sourceDatabaseVersion: 'PostgreSQL 16 password=secret',
    changeSummary: {
      changed: true,
      addedColumnCount: 1,
      removedColumnCount: 0,
      changedColumnCount: 2
    }
  }
}

test('reads current scan table names and ignores nameless rows', () => {
  assert.deepEqual(currentScanTableNames(scanResult), ['user_order', 'payment_bill'])
  assert.deepEqual(currentScanTableNames({ tables: [{ tableName: '' }, { tableName: 'audit_log' }] }), ['audit_log'])
})

test('reads only successful partial tables for preview and coverage', () => {
  assert.deepEqual(currentSuccessfulScanTableNames(scanResult), ['user_order'])
  assert.deepEqual(currentSuccessfulScanTableNames({ tables: [{ tableName: 'fallback_table' }] }), ['fallback_table'])
})

test('selects only successful partial tables after a scan boundary exists', () => {
  assert.deepEqual(
    selectSuccessfulPartialTableNames(['user_order', 'payment_bill'], scanResult),
    ['user_order']
  )
  assert.deepEqual(
    selectSuccessfulPartialTableNames(['user_order'], {
      status: 'CANCELLED',
      partialResult: {
        successfulTableNames: [],
        failedTableNames: ['user_order'],
        skippedTableNames: [],
        completeForPreview: false,
        completeForCoverage: false,
        complete: false
      }
    }),
    []
  )
  assert.deepEqual(
    selectSuccessfulPartialTableNames(['fallback_table'], { tables: [{ tableName: 'fallback_table' }] }),
    ['fallback_table']
  )
  assert.deepEqual(
    selectSuccessfulPartialTableNames(
      ['legacy_success', 'payment_bill'],
      {
        status: 'CANCELLED',
        partialResult: {
          successfulTableNames: [],
          failedTableNames: ['payment_bill'],
          skippedTableNames: [],
          completeForPreview: false,
          completeForCoverage: false,
          complete: false
        }
      },
      ['legacy_success']
    ),
    ['legacy_success']
  )
})

test('merges scan table names without dropping previous selections', () => {
  assert.deepEqual(
    mergeScanTableNames(['legacy_table', 'user_order'], scanResult),
    ['legacy_table', 'user_order']
  )
})

test('formats scan job status and source pressure without credentials', () => {
  assert.equal(scanJobStatusLabel(scanResult), '部分完成')

  const pressure = buildSourcePressureHintText(scanResult)

  assert.match(pressure, /限制为 40/)
  assert.doesNotMatch(pressure, /dsn=postgres/)
  assert.doesNotMatch(pressure, /Authorization=/)
})

test('formats scan progress for large database batches', () => {
  assert.equal(scanProgressLabel(scanResult), '已扫描 40 / 120，剩余约 80')
  assert.equal(
    scanProgressLabel({ ...scanResult, progress: { processedTableCount: 120, remainingTableEstimate: 0, pageSize: 40, hasMore: false } }),
    '已扫描 120 / 120'
  )
})

test('builds safe resume summary without credentials', () => {
  const summary = buildScanResumeSummary(scanResult)

  assert.match(summary, /scanJobId=scan-job-demo/)
  assert.match(summary, /resumeCursor=40/)
  assert.match(summary, /pageSize=40/)
  assert.doesNotMatch(summary, /password=secret/)
  assert.doesNotMatch(summary, /jdbc:postgresql/)
})

test('formats failure summary and evidence without credentials', () => {
  const failure = buildScanFailureSummary(scanResult)
  const evidence = buildScanEvidenceSummary(scanResult)

  assert.match(failure, /payment_bill/)
  assert.match(failure, /PERMISSION_DENIED/)
  assert.doesNotMatch(failure, /password=secret/)
  assert.doesNotMatch(failure, /Authorization/)
  assert.doesNotMatch(failure, /jdbc:postgresql/)
  assert.match(evidence, /scan-job-demo/)
  assert.match(evidence, /failed=1/)
  assert.match(evidence, /safeForAiCopy=true/)
  assert.doesNotMatch(evidence, /Authorization/)
})

test('formats metadata cache status and summary without credentials', () => {
  assert.equal(metadataCacheStatusLabel(scanResult.metadataCache), '缓存命中')

  const summary = buildMetadataCacheSummary(scanResult.metadataCache)

  assert.match(summary, /缓存命中/)
  assert.match(summary, /0123456789ab/)
  assert.match(summary, /新增 1/)
  assert.match(summary, /变更 2/)
  assert.doesNotMatch(summary, /password=secret/)
})
