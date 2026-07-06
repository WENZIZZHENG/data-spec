import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildCommandPaletteItems,
  commandMatchesKeyword,
  commandToLocalRecentEntry,
  normalizeRecentCommandEntries,
  readRecentCommandEntries,
  writeRecentCommandEntry
} from '../src/utils/commandPalette.ts'
import type { AiJobRecordListItem, ReverseImportDecision, SqlCheckRecord } from '../src/types/index.ts'

test('builds project-scoped command routes and missing project suggestions', () => {
  const scoped = buildCommandPaletteItems({ projectId: 7 })
  const sql = scoped.find((item) => item.id === 'page.sql-lint')
  const standardQa = scoped.find((item) => item.id === 'page.standard-qa')
  const token = scoped.find((item) => item.id === 'page.tokens')

  assert.equal(sql?.route.path, '/sql-lint')
  assert.equal(sql?.route.query?.projectId, 7)
  assert.equal(standardQa?.route.path, '/standard-qa')
  assert.equal(standardQa?.route.query?.projectId, 7)
  assert.equal(token?.route.path, '/tokens')
  assert.equal(token?.route.query?.projectId, undefined)

  const missingProject = buildCommandPaletteItems({ projectId: null })
  assert.ok(missingProject.some((item) => item.id === 'project.select'))
  assert.ok(missingProject.some((item) => item.id === 'project.create-demo'))
  assert.ok(missingProject.every((item) => item.projectRequired !== true || item.disabled))
})

test('builds recent SQL, reverse import, and AI job resume commands', () => {
  const lintRecords: SqlCheckRecord[] = [
    { id: 11, projectId: 7, sqlText: 'select * from users', createdAt: '2026-07-05T10:00:00Z' } as SqlCheckRecord
  ]
  const reverseDecisions: ReverseImportDecision[] = [
    { batchId: 22, sourceTable: 'orders', confirmReason: '已确认订单字段' } as ReverseImportDecision
  ]
  const aiJobs: AiJobRecordListItem[] = [
    { id: 33, projectId: 7, jobType: 'FIX_SQL_PROMPT', title: '修复 SQL', createdAt: '2026-07-05T10:01:00Z' }
  ]

  const items = buildCommandPaletteItems({ projectId: 7, lintRecords, reverseDecisions, aiJobs })

  assert.deepEqual(items.find((item) => item.id === 'recent.sql.11')?.route, {
    path: '/sql-lint',
    query: { projectId: 7, recordId: 11 }
  })
  assert.deepEqual(items.find((item) => item.id === 'recent.reverse.22')?.route, {
    path: '/reverse-import',
    query: { projectId: 7, sourceBatchId: 22 }
  })
  assert.deepEqual(items.find((item) => item.id === 'recent.ai.33')?.route, {
    path: '/ai-replay',
    query: { projectId: 7, aiJobId: 33, jobType: 'FIX_SQL_PROMPT' }
  })

  const mixedProjectItems = buildCommandPaletteItems({
    projectId: 7,
    lintRecords: [
      { id: 12, projectId: 99, createdAt: '2026-07-05T10:02:00Z' } as SqlCheckRecord,
      { id: 13, projectId: 7, createdAt: '2026-07-05T10:03:00Z' } as SqlCheckRecord
    ],
    reverseDecisions: [
      { batchId: 23, projectId: 99 } as ReverseImportDecision,
      { batchId: 24, projectId: 7 } as ReverseImportDecision
    ],
    aiJobs: [
      { id: 34, projectId: 99, jobType: 'FIX_SQL_PROMPT' } as AiJobRecordListItem,
      { id: 35, projectId: 7, jobType: 'DDL_PREVIEW' } as AiJobRecordListItem
    ]
  })

  assert.equal(mixedProjectItems.some((item) => item.id === 'recent.sql.12'), false)
  assert.equal(mixedProjectItems.some((item) => item.id === 'recent.reverse.23'), false)
  assert.equal(mixedProjectItems.some((item) => item.id === 'recent.ai.34'), false)
  assert.ok(mixedProjectItems.some((item) => item.id === 'recent.sql.13'))
  assert.ok(mixedProjectItems.some((item) => item.id === 'recent.reverse.24'))
  assert.ok(mixedProjectItems.some((item) => item.id === 'recent.ai.35'))
})

test('filters commands by title, keywords, and group label', () => {
  const item = buildCommandPaletteItems({ projectId: 7 })
    .find((candidate) => candidate.id === 'page.reverse-import')

  assert.ok(item)
  assert.equal(commandMatchesKeyword(item!, '反向'), true)
  assert.equal(commandMatchesKeyword(item!, 'database'), true)
  assert.equal(commandMatchesKeyword(item!, '不存在的入口'), false)
})

test('builds local recent command id without repeated local prefix', () => {
  const remoteRecent = buildCommandPaletteItems({
    projectId: 7,
    lintRecords: [{ id: 11, projectId: 7, createdAt: '2026-07-05T10:00:00Z' } as SqlCheckRecord]
  }).find((item) => item.id === 'recent.sql.11')

  assert.ok(remoteRecent)
  assert.equal(commandToLocalRecentEntry(remoteRecent!).id, 'local.recent.sql.11')
  assert.equal(commandToLocalRecentEntry({
    ...remoteRecent!,
    id: 'local.recent.sql.11'
  }).id, 'local.recent.sql.11')
})

test('normalizes and persists safe recent command entries', () => {
  const writes: string[] = []
  const storage = {
    value: '',
    getItem: () => storage.value,
    setItem: (_key: string, value: string) => {
      storage.value = value
      writes.push(value)
    }
  }

  writeRecentCommandEntry(storage, {
    id: 'local-1',
    title: 'SQL 校验',
    route: {
      path: '/sql-lint',
      query: {
        projectId: 7,
        recordId: 11,
        token: 'should-not-persist',
        sql: 'select password from user'
      }
    },
    usedAt: '2026-07-05T10:00:00Z'
  })

  const entries = readRecentCommandEntries(storage)
  assert.equal(writes.length, 1)
  assert.equal(entries.length, 1)
  assert.deepEqual(entries[0].route.query, { projectId: '7', recordId: '11' })

  const blockedStorage = {
    getItem: () => {
      throw new Error('storage unavailable')
    },
    setItem: () => {
      throw new Error('storage unavailable')
    }
  }
  const fallbackEntries = writeRecentCommandEntry(blockedStorage, {
    id: 'local-2',
    title: '覆盖率报告',
    route: { path: '/field-coverage', query: { projectId: 7 } },
    usedAt: '2026-07-05T10:01:00Z'
  })
  assert.equal(readRecentCommandEntries(blockedStorage).length, 0)
  assert.equal(fallbackEntries.length, 1)
  assert.equal(fallbackEntries[0].route.path, '/field-coverage')

  const refreshed = normalizeRecentCommandEntries([
    {
      id: 'local.page.sql-lint',
      title: '最新 SQL 校验',
      route: { path: '/sql-lint', query: { projectId: 7, recordId: 12 } },
      usedAt: '2026-07-05T10:03:00Z'
    },
    {
      id: 'local.page.sql-lint',
      title: '旧 SQL 校验',
      route: { path: '/sql-lint', query: { projectId: 7, recordId: 11 } },
      usedAt: '2026-07-05T10:00:00Z'
    }
  ])
  assert.equal(refreshed.length, 1)
  assert.equal(refreshed[0].title, '最新 SQL 校验')
  assert.deepEqual(refreshed[0].route.query, { projectId: '7', recordId: '12' })

  const many = normalizeRecentCommandEntries(Array.from({ length: 12 }, (_, index) => ({
    id: `item-${index}`,
    title: `入口 ${index}`,
    route: { path: '/dashboard', query: { projectId: index + 1 } },
    usedAt: `2026-07-05T10:${String(index).padStart(2, '0')}:00Z`
  })))
  assert.equal(many.length, 8)
  assert.equal(many[0].id, 'item-11')
})
