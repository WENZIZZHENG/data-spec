import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import * as reusePackDisplay from '../src/utils/standardReusePackDisplay.ts'
import type { StandardReusePackPlan } from '../src/types/index.ts'

const {
  buildStandardReusePackApplyPayload,
  buildStandardReusePackCreatePayload,
  hasBlockingReusePackItems,
  reusePackActionTagType,
  summarizeReusePackCounts
} = reusePackDisplay

function readSource(relativePath: string) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function assertContains(source: string, snippets: string[], context: string) {
  for (const snippet of snippets) {
    assert.ok(source.includes(snippet), `${context} should include ${snippet}`)
  }
}

test('builds standard reuse pack create and apply payloads', () => {
  assert.deepEqual(
    buildStandardReusePackCreatePayload(1, ' shared_core ', ' 通用交易标准 ', ' 2026.07 ', ' 共享字段 '),
    {
      projectId: 1,
      packKey: 'shared_core',
      packName: '通用交易标准',
      basePackVersion: '2026.07',
      description: '共享字段'
    }
  )
  assert.deepEqual(buildStandardReusePackApplyPayload(10, 2), {
    packId: 10,
    targetProjectId: 2,
    overwrite: false
  })
})

test('formats reuse pack actions and detects blocking items', () => {
  const plan: StandardReusePackPlan = {
    counts: {
      created: 2,
      skipped: 1,
      overridden: 1,
      drifted: 1,
      blocked: 0,
      warnings: 0
    },
    items: [
      { assetType: 'field', key: 'order_no', action: 'CREATE', reason: '目标项目缺失' },
      { assetType: 'field', key: 'user_id', action: 'DRIFTED', reason: '目标项目有局部覆盖' }
    ]
  }

  assert.equal(reusePackActionTagType('CREATE'), 'success')
  assert.equal(reusePackActionTagType('SKIP'), 'info')
  assert.equal(reusePackActionTagType('DRIFTED'), 'warning')
  assert.equal(reusePackActionTagType('BLOCKED'), 'danger')
  assert.deepEqual(summarizeReusePackCounts(plan), [
    { key: 'created', label: '创建', value: 2 },
    { key: 'skipped', label: '跳过', value: 1 },
    { key: 'overridden', label: '覆盖项', value: 1 },
    { key: 'drifted', label: '漂移项', value: 1 },
    { key: 'blocked', label: '阻塞', value: 0 },
    { key: 'warnings', label: '警告', value: 0 }
  ])
  assert.equal(hasBlockingReusePackItems(plan), false)
  assert.equal(hasBlockingReusePackItems({ ...plan, items: [{ assetType: 'field', key: '', action: 'BLOCKED' }] }), true)
})

test('builds safety-aware dry-run summary before reuse pack apply', () => {
  const buildAiWriteSafetySummary = (reusePackDisplay as any).buildAiWriteSafetySummary
  assert.equal(typeof buildAiWriteSafetySummary, 'function')

  const summary = buildAiWriteSafetySummary({
    safety: {
      readOnly: false,
      writesProject: true,
      requiresDryRun: true,
      supportsUndo: true,
      requiresIdempotencyKey: true,
      sensitiveInputs: ['databasePassword'],
      nextActions: ['先运行预览应用', '携带 Idempotency-Key 确认应用']
    },
    counts: {
      created: 2,
      skipped: 1,
      drifted: 1,
      blocked: 0,
      warnings: 1
    }
  })

  assert.equal(summary.title, '写入安全 dry-run 摘要')
  assert.equal(summary.requiresDryRun, true)
  assert.equal(summary.requiresIdempotencyKey, true)
  assert.equal(summary.requiresReview, true)
  assert.match(summary.riskText, /需要 dry-run/)
  assert.match(summary.idempotencyText, /Idempotency-Key/)
  assert.deepEqual(summary.counts.map((item) => item.key), ['created', 'skipped', 'drifted', 'blocked', 'warnings'])
  assert.deepEqual(summary.nextActions, ['先运行预览应用', '携带 Idempotency-Key 确认应用'])
  assert.deepEqual(summary.sensitiveInputs, ['databasePassword'])
})

test('keeps standard reuse pack page, api, and types wired', () => {
  const view = readSource('src/views/StandardReusePack.vue')
  const api = readSource('src/api/standardReusePack.ts')
  const types = readSource('src/types/index.ts')
  const router = readSource('src/router/index.ts')
  const app = readSource('src/App.vue')

  assertContains(router, [
    "path: '/standard-reuse-packs'",
    "name: 'StandardReusePack'",
    "component: () => import('@/views/StandardReusePack.vue')",
    "meta: { title: '标准复用包' }"
  ], 'standard reuse pack router')

  assertContains(app, [
    'index="/standard-reuse-packs"',
    '标准复用包'
  ], 'standard reuse pack navigation')

  assertContains(view, [
    "import ProjectRequired from '@/components/ProjectRequired.vue'",
    "import {",
    'createStandardReusePack',
    'listStandardReusePacks',
    'previewStandardReusePackApply',
    'applyStandardReusePack',
    'getStandardReusePackDrift',
    'listStandardReusePackApplications',
    'buildStandardReusePackCreatePayload',
    'buildStandardReusePackApplyPayload',
    'v-if="safetySummary.requiresDryRun"',
    'requiresDryRun: false',
    'standardPackSources',
    '标准复用包',
    '创建复用包',
    '预览应用',
    '确认应用',
    '漂移报告',
    '请先创建并选择项目'
  ], 'StandardReusePack.vue')

  assertContains(api, [
    "request.get<unknown, StandardReusePackInfo[]>('/standard-reuse-packs'",
    "request.post<unknown, StandardReusePackDetail>('/standard-reuse-packs'",
    "request.post<unknown, StandardReusePackPlan>('/standard-reuse-packs/apply/preview'",
    "request.post<unknown, StandardReusePackApplyResult>('/standard-reuse-packs/apply'",
    "request.get<unknown, StandardReusePackApplicationInfo[]>('/standard-reuse-packs/applications'",
    'export function getStandardReusePackDrift'
  ], 'standard reuse pack api')

  assertContains(types, [
    'export interface StandardReusePackCreateReq',
    'export interface StandardReusePackApplyReq',
    'export interface StandardReusePackInfo',
    'export interface StandardReusePackPlan',
    'export interface StandardReusePackApplicationInfo',
    'export interface StandardPackSource'
  ], 'standard reuse pack types')
})
