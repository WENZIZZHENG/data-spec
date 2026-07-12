import type { Page, Route } from '@playwright/test'

/**
 * 浏览器级测试使用的项目 fixture。
 *
 * 该对象模拟后端项目响应，字段保持非敏感且可被项目选择、字段库和 AI Context 页面共同复用。
 */
export const project = {
  id: 101,
  name: 'E2E 演示项目',
  description: '浏览器级验收项目',
  dbType: 'postgresql',
  updatedAt: '2026-07-06 23:59:00'
}

/**
 * SQL 校验 fixture 返回的修正 SQL。
 *
 * 内容只包含演示表结构和标准字段名，不包含真实业务数据或连接信息。
 */
export const fixedSql = `CREATE TABLE user_order (
  id bigint NOT NULL,
  buyer_mobile varchar(32) COMMENT '买家手机号'
);`

/**
 * 字段库和 AI Context 预览共享的标准字段 fixture。
 *
 * 第一条字段用于检索命中和反向导入候选，第二条字段用于验证过滤后不会误展示。
 */
export const fields = [
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

export interface DataSpecRouteHarness {
  /** 当前测试会话创建并选择的项目 fixture。 */
  project: typeof project
  /** 字段库、检索和 AI Context 共享的字段 fixture。 */
  fields: typeof fields
  /** route harness 未覆盖的 API 请求；用例末尾必须断言为空。 */
  unhandledApiRequests: string[]
}

/**
 * 安装浏览器级测试的 API route harness。
 *
 * Harness 只模拟核心页面读取和 dry-run 请求，不触达真实后端或外部数据库，确保 E2E 用例可重复且不会写入用户环境。
 */
export async function installDataSpecRouteHarness(page: Page): Promise<DataSpecRouteHarness> {
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
    if (method === 'GET' && pathname === '/api/ai-jobs') {
      await ok(route, { records: [], total: 0, current: 1, size: 5, pages: 0 })
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
    if (method === 'POST' && pathname === '/api/reverse-import/database/comment-plan') {
      await ok(route, commentPatchPlan())
      return
    }
    if (method === 'GET' && pathname === '/api/reverse-import/decisions') {
      await ok(route, [])
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
    if (method === 'GET' && pathname === '/api/field-semantics') {
      await ok(route, [])
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

  return { project, fields, unhandledApiRequests }
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

function commentPatchPlan() {
  return {
    kind: 'databaseCommentPatchPlan',
    schemaVersion: 1,
    projectId: project.id,
    databaseType: 'postgresql',
    databaseName: 'sales_demo',
    schemaName: 'public',
    metadataFingerprint: 'e2e-fingerprint-123456',
    planHash: 'e2e-comment-plan-abcdef',
    riskLevel: 'LOW',
    summary: {
      tableCount: 1,
      columnCount: 2,
      itemCount: 2,
      executableChangeCount: 1,
      noOpCount: 1,
      missingCount: 0,
      changedCount: 1,
      unsupportedCount: 0,
      blockedCount: 0
    },
    items: [
      {
        objectType: 'TABLE',
        schemaName: 'public',
        tableName: 'user_order',
        status: 'NO_OP',
        currentComment: '订单表',
        targetComment: '订单表',
        commentDiff: '表 COMMENT 已符合 DataSpec 表标准',
        dialectSupport: 'POSTGRESQL_TABLE_COMMENT',
        riskLevel: 'LOW',
        rollbackHint: '无需回滚',
        evidenceRefs: ['template:user_order']
      },
      {
        objectType: 'COLUMN',
        schemaName: 'public',
        tableName: 'user_order',
        columnName: 'buyer_mobile',
        standardFieldName: 'buyer_mobile',
        status: 'CHANGED',
        currentComment: '手机号',
        targetComment: '买家手机号',
        commentDiff: '需补充标准字段 COMMENT：买家手机号',
        dryRunSql: "COMMENT ON COLUMN public.user_order.buyer_mobile IS '买家手机号';",
        dialectSupport: 'POSTGRESQL_COLUMN_COMMENT',
        riskLevel: 'LOW',
        rollbackHint: "COMMENT ON COLUMN public.user_order.buyer_mobile IS '手机号';",
        evidenceRefs: ['field:buyer_mobile'],
        manualChecks: ['确认手机号字段在业务库中仍表示买家联系方式']
      }
    ],
    dryRunSql: "COMMENT ON COLUMN public.user_order.buyer_mobile IS '买家手机号';",
    dialectSupport: {
      databaseType: 'postgresql',
      tableCommentSqlSupported: true,
      columnCommentSqlSupported: true,
      unsupportedReasons: [],
      notes: ['PostgreSQL COMMENT ON 语句仅作为 dry-run 审阅输出']
    },
    rollbackHint: "COMMENT ON COLUMN public.user_order.buyer_mobile IS '手机号';",
    evidence: {
      schemaScope: 'postgresql/sales_demo/public',
      tableScope: ['public.user_order'],
      metadataFingerprint: 'e2e-fingerprint-123456',
      standardReferences: ['template:user_order', 'field:buyer_mobile'],
      normalizedInputSummary: 'project=101 database=postgresql schema=public tables=public.user_order',
      safetyFlags: ['readOnly', 'schemaOnly', 'noSourceWrites', 'noCredentialsPersisted']
    },
    safety: {
      readOnly: true,
      writesSourceDatabase: false,
      writesProject: false,
      requiresManualApply: true,
      safeForAiCopy: true,
      sensitiveRedaction: true
    },
    nextActions: ['复制 dry-run SQL 前再次人工审阅', '确认 COMMENT SQL 由正式 migration 流程执行']
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
