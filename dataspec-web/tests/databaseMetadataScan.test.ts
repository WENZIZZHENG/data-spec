import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildScanResumeSummary,
  buildMetadataCacheSummary,
  currentScanTableNames,
  mergeScanTableNames,
  metadataCacheStatusLabel,
  scanProgressLabel
} from '../src/utils/databaseMetadataScan.ts'

const scanResult = {
  scanId: 'scan-demo',
  estimatedTableCount: 120,
  cursor: '40',
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
  resumeCommand: '继续 cursor=40 pageSize=40 password=secret jdbc:postgresql://localhost/demo',
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

test('merges scan table names without dropping previous selections', () => {
  assert.deepEqual(
    mergeScanTableNames(['legacy_table', 'user_order'], scanResult),
    ['legacy_table', 'user_order', 'payment_bill']
  )
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

  assert.match(summary, /cursor=40/)
  assert.match(summary, /pageSize=40/)
  assert.doesNotMatch(summary, /password=secret/)
  assert.doesNotMatch(summary, /jdbc:postgresql/)
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
