import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { expect, test, type Page, type Route } from '@playwright/test'

const project = {
  id: 101,
  name: 'E2E 演示项目',
  description: '浏览器级验收项目',
  dbType: 'postgresql',
  updatedAt: '2026-07-06 23:59:00'
}

const fixedSql = `CREATE TABLE user_order (
  id bigint NOT NULL,
  buyer_mobile varchar(32) COMMENT '买家手机号'
);`

const fields = [
  {
    id: 201,
    projectId: project.id,
    name: 'buyer_mobile',
    displayName: '买家手机号',
    dataType: 'varchar(32)',
    aliases: 'mobile, phone',
    category: '交易',
    tags: '订单,用户',
    status: 'enabled',
    nullable: false,
    sensitive: true,
    comment: '买家下单时使用的手机号',
    formatType: 'phone',
    formatPattern: '^1\\d{10}$'
  },
  {
    id: 202,
    projectId: project.id,
    name: 'order_amount',
    displayName: '订单金额',
    dataType: 'decimal(18,2)',
    category: '交易',
    tags: '订单,金额',
    status: 'enabled',
    nullable: false,
    sensitive: false,
    comment: '订单应付金额',
    formatType: 'decimal',
    formatUnit: 'CNY'
  }
]

test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status === testInfo.expectedStatus) {
    return
  }
  const contextDir = path.join(testInfo.outputDir, 'failure-context')
  await mkdir(contextDir, { recursive: true })
  await writeFile(path.join(contextDir, 'current-url.txt'), page.url(), 'utf8')
})

test('核心浏览器工作流可以渲染关键结果并保留失败证据', async ({ page }) => {
  const unhandledApiRequests = await installApiMocks(page)

  await test.step('创建并选择项目', async () => {
    await page.goto('/projects')
    await expect(page.getByRole('heading', { name: '项目列表' })).toBeVisible()

    await page.getByRole('button', { name: /新建项目/ }).click()
    await page.getByPlaceholder('请输入项目名称').fill(project.name)
    await page.getByPlaceholder('请输入项目描述').fill(project.description)
    await page.getByRole('button', { name: '保存' }).click()

    const projectRow = page.getByRole('row').filter({ hasText: project.name })
    await expect(projectRow).toContainText(project.name)
    await expect(projectRow).toContainText('当前')
  })

  await test.step('SQL 校验展示 fixedSql 并打开检查记录详情', async () => {
    await page.goto('/sql-lint?demo=lint')
    await expect(page.getByRole('heading', { name: 'SQL 校验' })).toBeVisible()

    await page.getByRole('button', { name: /执行校验/ }).click()
    await expect(page.getByText('修正 SQL').first()).toBeVisible()
    await expect(page.getByText('buyer_mobile').first()).toBeVisible()

    await page.getByRole('button', { name: /最近检查记录\s+1 条/ }).click()
    await page.getByRole('button', { name: '查看详情' }).click()
    const recordDialog = page.getByRole('dialog', { name: '检查记录详情' })
    await expect(recordDialog).toBeVisible()
    await expect(recordDialog.getByText('历史 Context 导出命令')).toBeVisible()
    await expect(recordDialog.getByText('字段需补充 COMMENT')).toBeVisible()
    await page.keyboard.press('Escape')
  })

  await test.step('数据库直连元数据浏览和预览渲染字段候选', async () => {
    await page.goto('/reverse-import')
    await expect(page.getByRole('heading', { name: '反向导入' })).toBeVisible()
    await page.getByRole('tab', { name: '数据库直连' }).click()

    await page.getByPlaceholder('dataspec_demo').fill('sales_demo')
    await page.getByPlaceholder('public / database').fill('public')
    await page.locator('.db-form .el-form-item').filter({ hasText: '用户名' }).locator('input').fill('readonly_user')

    await page.getByRole('button', { name: /加载表/ }).click()
    const tableChoice = page.locator('label').filter({ hasText: 'public.user_order' })
    await expect(tableChoice).toBeVisible()
    await tableChoice.click()
    await expect(page.getByRole('button', { name: /浏览元数据/ })).toBeEnabled()

    await page.getByRole('button', { name: /浏览元数据/ }).click()
    await expect(page.getByRole('heading', { name: '元数据浏览' })).toBeVisible()
    await expect(page.getByText('public.user_order.buyer_mobile')).toBeVisible()

    await page.getByRole('button', { name: /生成预览/ }).click()
    await expect(page.getByRole('tab', { name: '字段候选' })).toBeVisible()
    await page.getByRole('button', { name: /user_order\s+1 个字段/ }).click()
    await expect(page.getByRole('row', { name: /public\.user_order\.buyer_mobile.*买家手机号/ })).toBeVisible()
  })

  await test.step('字段库筛选展示命中原因', async () => {
    await page.goto('/fields')
    await expect(page.getByRole('heading', { name: '标准字段库' })).toBeVisible()

    await page.getByPlaceholder('搜索字段名、显示名、别名、分类、注释或替代说明').fill('手机号')
    await expect(page.getByText('buyer_mobile')).toBeVisible()
    await expect(page.getByText(/命中原因/)).toBeVisible()
    await expect(page.getByText('order_amount')).toHaveCount(0)
  })

  await test.step('AI Context 预览可读取三类上下文', async () => {
    await page.goto('/ai-export')
    await expect(page.getByRole('heading', { name: 'AI Context' })).toBeVisible()
    await expect(page.getByRole('tab', { name: 'DATABASE_RULES.md' })).toBeVisible()
    await expect(page.getByText('字段命名必须使用 snake_case')).toBeVisible()

    await page.getByRole('tab', { name: 'field-catalog.json' }).click()
    await expect(page.getByText('"buyer_mobile"')).toBeVisible()

    await page.getByRole('tab', { name: 'rules.yaml' }).click()
    await expect(page.getByText('required_columns')).toBeVisible()
  })

  expect(unhandledApiRequests).toEqual([])
})

async function installApiMocks(page: Page) {
  const state = {
    projects: [] as Array<typeof project>,
    lintRecordCreated: false
  }
  const unhandledApiRequests: string[] = []

  await page.route('**/*', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const method = request.method()
    const pathname = url.pathname

    if (!pathname.startsWith('/api/')) {
      await route.continue()
      return
    }

    if (method === 'GET' && pathname === '/api/projects') {
      await ok(route, state.projects)
      return
    }
    if (method === 'POST' && pathname === '/api/projects') {
      state.projects = [{ ...project, ...readJsonBody(request.postData()) }]
      await ok(route, state.projects[0])
      return
    }
    if (method === 'GET' && pathname === '/api/starter-kits') {
      await ok(route, [])
      return
    }
    if (method === 'GET' && pathname === '/api/starter-kits/installations') {
      await ok(route, [])
      return
    }
    if (method === 'GET' && pathname === '/api/ai-profiles') {
      await ok(route, { profiles: [], defaultProfileId: '', selectedProfileId: '' })
      return
    }
    if (method === 'POST' && pathname === '/api/lint') {
      state.lintRecordCreated = true
      await ok(route, lintResult())
      return
    }
    if (method === 'GET' && pathname === '/api/lint/records') {
      await ok(route, {
        records: state.lintRecordCreated ? [lintRecord()] : [],
        total: state.lintRecordCreated ? 1 : 0,
        current: 1,
        size: 10,
        pages: state.lintRecordCreated ? 1 : 0
      })
      return
    }
    if (method === 'GET' && pathname === '/api/lint/records/701') {
      await ok(route, lintRecordDetail())
      return
    }
    if (method === 'GET' && pathname === '/api/database-connection-presets') {
      await ok(route, [])
      return
    }
    if (method === 'POST' && pathname === '/api/reverse-import/database/tables') {
      await ok(route, [
        { schemaName: 'public', tableName: 'user_order', comment: '订单表' }
      ])
      return
    }
    if (method === 'POST' && pathname === '/api/reverse-import/database/browser') {
      await ok(route, metadataBrowser())
      return
    }
    if (method === 'POST' && pathname === '/api/reverse-import/database/preview') {
      await ok(route, reverseImportPreview())
      return
    }
    if (method === 'GET' && pathname === '/api/fields/all') {
      await ok(route, fields)
      return
    }
    if (method === 'GET' && pathname === '/api/fields/search') {
      await ok(route, fieldSearchResult())
      return
    }
    if (method === 'GET' && pathname === '/api/fields/groups') {
      await ok(route, { totalFieldCount: fields.length, groups: [] })
      return
    }
    if (method === 'GET' && pathname === '/api/domains') {
      await ok(route, [])
      return
    }
    if (method === 'GET' && pathname === `/api/projects/${project.id}/standard-snapshots`) {
      await ok(route, [])
      return
    }
    if (method === 'GET' && pathname === '/api/ai-context/database-rules') {
      await ok(route, '# DATABASE_RULES.md\n字段命名必须使用 snake_case，敏感字段不得进入日志。')
      return
    }
    if (method === 'GET' && pathname === '/api/ai-context/field-catalog') {
      await ok(route, JSON.stringify({ fields: [{ name: 'buyer_mobile', displayName: '买家手机号' }] }, null, 2))
      return
    }
    if (method === 'GET' && pathname === '/api/ai-context/rules-yaml') {
      await ok(route, 'naming:\n  required_columns:\n    - id\n    - created_at\n')
      return
    }

    unhandledApiRequests.push(`${method} ${pathname}`)
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      body: JSON.stringify({ code: 404, message: `E2E fixture 未覆盖 ${method} ${pathname}` })
    })
  })

  return unhandledApiRequests
}

async function ok(route: Route, data: unknown) {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data })
  })
}

function readJsonBody(body: string | null) {
  if (!body) {
    return {}
  }
  try {
    return JSON.parse(body) as Record<string, unknown>
  } catch {
    return {}
  }
}

function lintRecord() {
  return {
    id: 701,
    projectId: project.id,
    originalSql: 'CREATE TABLE UserOrder (buyer_mobile varchar(32));',
    fixedSql,
    errorCount: 1,
    warningCount: 0,
    suggestionCount: 1,
    createdAt: '2026-07-06 23:59:01'
  }
}

function lintResult() {
  return {
    ...lintRecord(),
    issues: [
      {
        severity: 'ERROR',
        tableName: 'UserOrder',
        columnName: 'buyer_mobile',
        line: 1,
        column: 25,
        lineEnd: 1,
        columnEnd: 37,
        ruleCode: 'column_comment_required',
        message: '字段需补充 COMMENT',
        fixStatus: 'APPLIED',
        fixSuggestion: '补充标准字段注释'
      }
    ],
    fixedSqlDiff: "-CREATE TABLE UserOrder (buyer_mobile varchar(32));\n+CREATE TABLE user_order (buyer_mobile varchar(32) COMMENT '买家手机号');",
    fixPolicy: { mode: 'GENERATE', maxRiskLevel: 'LOW', includeExplanations: true },
    fixSummary: { appliedCount: 1, plannedCount: 0, skippedCount: 0 },
    fixChanges: [
      {
        status: 'APPLIED',
        riskLevel: 'LOW',
        ruleCode: 'column_comment_required',
        explain: '使用标准字段注释补齐 COMMENT'
      }
    ],
    fixNextActions: ['检查 fixedSql 后再提交到业务仓库']
  }
}

function lintRecordDetail() {
  return {
    record: lintRecord(),
    issues: lintResult().issues,
    replay: {
      status: 'MATCHED',
      recordedStandard: { version: 'snapshot-1', hash: 'abc123' },
      currentStandard: { version: 'snapshot-1', hash: 'abc123' },
      summary: {
        fieldCount: 2,
        enumCount: 0,
        ruleCount: 3,
        exportCommand: 'dataspec export-context --project 101 --scope minimal'
      },
      nextActions: ['记录可复现，可直接交给 AI 修正 SQL']
    }
  }
}

function metadataBrowser() {
  return {
    databaseType: 'postgresql',
    databaseName: 'sales_demo',
    schemaName: 'public',
    selectedTableNames: ['user_order'],
    aiReadableSummary: 'metadataFingerprint=e2e-fingerprint table user_order buyer_mobile',
    metadataCache: {
      metadataFingerprint: 'e2e-fingerprint-123456',
      cacheHit: false,
      stale: false,
      refreshMode: 'AUTO',
      lastSeenAt: '2026-07-06T23:59:00',
      expiresAt: '2026-07-07T23:59:00'
    },
    summary: {
      tableCount: 1,
      columnCount: 2,
      indexCount: 1,
      candidateCount: 1,
      missingCommentCount: 0,
      changedCount: 0,
      unmanagedCount: 1
    },
    tables: [
      {
        schemaName: 'public',
        tableName: 'user_order',
        comment: '订单表',
        indexes: [{ indexName: 'idx_user_order_mobile', columnName: 'buyer_mobile', nonUnique: true }],
        columns: [
          {
            columnName: 'id',
            dataType: 'bigint',
            comment: '主键',
            standardFieldName: 'id',
            standardDisplayName: '主键',
            matchStatus: 'MATCHED',
            matchReason: '字段名命中'
          },
          {
            columnName: 'buyer_mobile',
            dataType: 'varchar(32)',
            comment: '买家手机号',
            matchStatus: 'NEW',
            matchReason: '标准字段库未纳管',
            importCandidate: true,
            selectedByDefault: true,
            candidateKey: 'user_order.buyer_mobile'
          }
        ]
      }
    ]
  }
}

function reverseImportPreview() {
  return {
    summary: {
      tableCount: 1,
      fieldCount: 2,
      candidateCount: 1,
      missingCommentCount: 0,
      nonStandardCount: 1
    },
    fieldCandidates: [
      {
        tableName: 'user_order',
        columnName: 'buyer_mobile',
        dataType: 'varchar(32)',
        nullable: false,
        comment: '买家手机号',
        suggestedName: 'buyer_mobile',
        displayName: '买家手机号',
        matchReason: '数据库字段尚未纳管，可导入标准字段库'
      }
    ],
    missingComments: [],
    nonStandardFields: [
      {
        tableName: 'user_order',
        columnName: 'buyer_mobile',
        recommendedName: 'buyer_mobile',
        reason: '字段未纳管'
      }
    ],
    tableRows: [
      { tableName: 'user_order', columnName: 'buyer_mobile', dataType: 'varchar(32)', comment: '买家手机号' }
    ]
  }
}

function fieldSearchResult() {
  const field = fields[0]
  return {
    summary: {
      matchedCount: 1,
      returnedCount: 1,
      hints: ['手机号命中显示名和别名']
    },
    items: [
      {
        field,
        score: 0.98,
        matchReasons: ['显示名命中：买家手机号'],
        recommendedUse: '用于订单买家联系方式展示，敏感场景需脱敏',
        evidence: [{ sourceType: 'FIELD', sourceId: field.id, reason: '字段名和显示名匹配' }]
      }
    ],
    nextActions: ['确认是否需要手机号脱敏展示']
  }
}
