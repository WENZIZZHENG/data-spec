import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { createMcpHandler, parseServerArgs } from './dataspec-mcp.mjs'
import { supportedWorkflowRecipeIds } from './dataspec-workflows.mjs'

test('initialize advertises resources, prompts, and tools capabilities', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const response = await handler({
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: { protocolVersion: '2025-06-18' }
  })

  assert.equal(response.jsonrpc, '2.0')
  assert.equal(response.id, 1)
  assert.equal(response.result.protocolVersion, '2025-06-18')
  assert.deepEqual(response.result.capabilities, {
    resources: {},
    prompts: {},
    tools: {}
  })
  assert.equal(response.result.serverInfo.name, 'dataspec-mcp')
})

test('resources list and read use configured project', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local/',
    apiToken: 'ds_mcp_token'
  }, async (url, options = {}) => {
    calls.push(url)
    assert.equal(options.headers.Authorization, 'Bearer ds_mcp_token')
    return jsonResponse({ code: 200, data: '{"fields":[]}' })
  })

  const listed = await handler({ jsonrpc: '2.0', id: 2, method: 'resources/list' })
  assert.deepEqual(listed.result.resources.map((resource) => resource.uri), [
    'dataspec://version-compatibility',
    'dataspec://project/7/capability-catalog',
    'dataspec://project/7/session-bootstrap',
    'dataspec://project/7/session-state',
    'dataspec://project/7/field-catalog',
    'dataspec://project/7/database-rules',
    'dataspec://project/7/rules-yaml',
    'dataspec://project/7/workflow-recipes',
    'dataspec://project/7/agent-guidance-pack',
    'dataspec://project/7/ai-task-profiles',
    'dataspec://project/7/schema-registry',
    'dataspec://project/7/ai-task-runs'
  ])

  const read = await handler({
    jsonrpc: '2.0',
    id: 3,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/field-catalog' }
  })

  assert.equal(calls[0], 'http://dataspec.local/api/ai-context/field-catalog?projectId=7')
  assert.equal(read.result.contents[0].uri, 'dataspec://project/7/field-catalog')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(read.result.contents[0].text, '{"fields":[]}')
})

test('agent guidance pack resource and templates are served locally', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const templates = await handler({ jsonrpc: '2.0', id: 201, method: 'resources/templates/list' })
  assert.ok(templates.result.resourceTemplates.some((template) =>
    template.uriTemplate === 'dataspec://project/{projectId}/agent-guidance-pack'))
  assert.ok(templates.result.resourceTemplates.some((template) =>
    template.uriTemplate === 'dataspec://project/{projectId}/session-bootstrap'))

  const projectless = createMcpHandler({ server: 'http://dataspec.local' }, failingFetch)
  const projectlessTemplates = await projectless({ jsonrpc: '2.0', id: 202, method: 'resources/templates/list' })
  assert.ok(projectlessTemplates.result.resourceTemplates.every((template) => template.uriTemplate.includes('{projectId}')))

  const read = await handler({
    jsonrpc: '2.0',
    id: 203,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/agent-guidance-pack' }
  })
  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(payload.kind, 'dataspec-mcp-agent-guidance-pack')
  assert.equal(payload.projectId, 7)
  assert.ok(payload.templates.some((template) => template.id === 'reverse_import_standards'))
  assert.ok(payload.templates.some((template) => template.id === 'answer_field_standard_question'))
  assert.deepEqual(read.result.structuredContent.templates[0].safeDefaults.executeWorkflow, false)
})

test('version compatibility resource is listed and read as structured content', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: 'ds_mcp_token'
  }, async (url, options = {}) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: versionCompatibilityFixture()
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 39,
    method: 'resources/read',
    params: { uri: 'dataspec://version-compatibility' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0].url, 'http://dataspec.local/api/capabilities/version?client=mcp&clientVersion=0.1.0')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_mcp_token')
  assert.equal(read.result.contents[0].uri, 'dataspec://version-compatibility')
  assert.equal(payload.kind, 'dataspec-version-compatibility')
  assert.equal(read.result.structuredContent.compatibility.status, 'COMPATIBLE')
})

test('version compatibility resource failure returns AI-readable DataSpec error', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => {
    throw new Error('ECONNREFUSED token=raw-secret jdbc:postgresql://localhost/db')
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 40,
    method: 'resources/read',
    params: { uri: 'dataspec://version-compatibility' }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'VERSION_COMPATIBILITY_UNAVAILABLE')
  assert.match(response.error.data.dataspecError.suggestedAction, /compat check/)
  assert.doesNotMatch(JSON.stringify(response.error), /raw-secret|jdbc:postgresql:\/\/localhost\/db/)
})

test('version compatibility resource wraps backend errors with stable compatibility code', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => ({
    ok: false,
    status: 500,
    json: async () => ({
      code: 500,
      message: 'server failed token=raw-secret',
      error: {
        code: 'INTERNAL_ERROR',
        category: 'SERVER',
        retryable: true,
        suggestedAction: 'retry token=raw-secret',
        docsRef: 'README.md#验证'
      }
    })
  }))

  const response = await handler({
    jsonrpc: '2.0',
    id: 41,
    method: 'resources/read',
    params: { uri: 'dataspec://version-compatibility' }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'VERSION_COMPATIBILITY_UNAVAILABLE')
  assert.equal(response.error.data.dataspecError.category, 'SERVER')
  assert.equal(response.error.data.dataspecError.httpStatus, 500)
  assert.match(response.error.data.dataspecError.suggestedAction, /compat check/)
  assert.doesNotMatch(JSON.stringify(response.error), /raw-secret/)
})

test('session bootstrap resource is read from backend with structured content', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local/',
    apiToken: 'ds_mcp_token'
  }, async (url, options = {}) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: sessionBootstrapFixture()
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 38,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/session-bootstrap' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0].url, 'http://dataspec.local/api/bootstrap/session?projectId=7&server=http%3A%2F%2Fdataspec.local')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_mcp_token')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(payload.kind, 'dataspec-ai-session-bootstrap')
  assert.equal(read.result.structuredContent.status, 'READY')
})

test('session state resource summarizes local project memory without backend calls', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-mcp-session-'))
  try {
    await mkdir(path.join(dir, '.dataspec', 'context'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'context', 'cache-metadata.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://token:secret@dataspec.local',
        scope: 'field',
        query: 'password=raw-secret Bearer raw-token jdbc:postgresql://db/app',
        contentHash: '0123456789abcdef',
        generatedAt: '2026-07-08T10:00:00Z',
        standard: { specVersion: 'v1', specHash: 'hash-1', source: 'current' }
      }),
      'utf8'
    )

    const handler = createMcpHandler({
      projectId: 7,
      server: 'http://token:secret@dataspec.local',
      apiToken: 'ds_mcp_secret_token',
      aiProfile: 'sql-fix',
      taskType: 'SQL_FIX',
      rootDir: dir
    }, failingFetch)

    const listed = await handler({ jsonrpc: '2.0', id: 301, method: 'resources/list' })
    assert.ok(listed.result.resources.some((resource) => resource.uri === 'dataspec://project/7/session-state'))

    const read = await handler({
      jsonrpc: '2.0',
      id: 302,
      method: 'resources/read',
      params: { uri: 'dataspec://project/7/session-state' }
    })
    const payload = JSON.parse(read.result.contents[0].text)
    const fullText = JSON.stringify(read.result)

    assert.equal(read.result.contents[0].mimeType, 'application/json')
    assert.equal(payload.kind, 'dataspec-mcp-session-state')
    assert.equal(payload.currentProject.projectId, 7)
    assert.equal(payload.currentProject.status, 'READY')
    assert.equal(payload.currentProject.authMode, 'TOKEN_PRESENT')
    assert.equal(payload.currentProject.profile.profileId, 'sql-fix')
    assert.equal(payload.currentProject.profile.taskType, 'SQL_FIX')
    assert.equal(payload.currentSnapshot.specHash, 'hash-1')
    assert.equal(payload.redactedMemory.contextCache.scope, 'field')
    assert.equal(payload.safeDefaults.sessionStateIsAuthorization, false)
    assert.deepEqual(read.result.structuredContent.currentProject, payload.currentProject)
    assert.doesNotMatch(fullText, /ds_mcp_secret_token|token:secret|raw-secret|raw-token|jdbc:postgresql:\/\/db\/app|Bearer raw-token/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('session state tool supports explicit project override and projectless blocked state', async () => {
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local'
  }, failingFetch)

  const tools = await handler({ jsonrpc: '2.0', id: 303, method: 'tools/list' })
  const descriptor = tools.result.tools.find((tool) => tool.name === 'get_session_state')
  assert.ok(descriptor)
  assert.equal(descriptor.safety.readOnly, true)
  assert.equal(descriptor.safety.writesProject, false)
  assert.deepEqual(descriptor.inputSchema.properties.projectId, {
    type: 'integer',
    description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
  })

  const overridden = await handler({
    jsonrpc: '2.0',
    id: 304,
    method: 'tools/call',
    params: { name: 'get_session_state', arguments: { projectId: 8 } }
  })
  assert.equal(overridden.result.structuredContent.currentProject.projectId, 8)
  assert.equal(JSON.parse(overridden.result.content[0].text).currentProject.projectId, 8)

  const projectless = createMcpHandler({ server: 'http://dataspec.local' }, failingFetch)
  const templates = await projectless({ jsonrpc: '2.0', id: 305, method: 'resources/templates/list' })
  assert.ok(templates.result.resourceTemplates.some((template) =>
    template.uriTemplate === 'dataspec://project/{projectId}/session-state'))

  const blocked = await projectless({
    jsonrpc: '2.0',
    id: 306,
    method: 'tools/call',
    params: { name: 'get_session_state', arguments: {} }
  })
  assert.equal(blocked.result.structuredContent.currentProject.status, 'BLOCKED')
  assert.equal(blocked.result.structuredContent.currentProject.projectId, null)
  assert.ok(blocked.result.structuredContent.nextActions.some((action) => /projectId|config/.test(action.command ?? action.message)))
})

test('session state reads real context cache metadata and blocks stale project snapshots', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-mcp-session-real-cache-'))
  try {
    await mkdir(path.join(dir, '.dataspec', 'context'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'context', 'cache-metadata.json'),
      JSON.stringify({
        kind: 'dataspec-ai-context-cache',
        schemaVersion: 1,
        projectId: 7,
        server: 'http://dataspec.local',
        exportedAt: '2026-07-08T09:00:00Z',
        expiresAt: '2026-07-15T09:00:00Z',
        ttlDays: 7,
        exportOptions: {
          scope: 'field',
          query: '{"token":"raw-json-token","password":"raw-json-password"}',
          status: 'enabled',
          limit: 12
        },
        contentHash: 'cache-hash-1',
        standard: { specVersion: 'v2', specHash: 'hash-real', source: 'remote' },
        sourcePackage: {
          schemaVersion: 1,
          kind: 'dataspec-ai-context-package',
          generatedAt: '2026-07-08T08:59:00Z',
          contextScope: 'field'
        }
      }),
      'utf8'
    )

    const handler = createMcpHandler({
      projectId: 7,
      server: 'http://dataspec.local',
      rootDir: dir
    }, failingFetch)

    const read = await handler({
      jsonrpc: '2.0',
      id: 307,
      method: 'resources/read',
      params: { uri: 'dataspec://project/7/session-state' }
    })
    const payload = JSON.parse(read.result.contents[0].text)
    const fullText = JSON.stringify(read.result)

    assert.equal(payload.redactedMemory.contextCache.scope, 'field')
    assert.match(payload.redactedMemory.contextCache.query, /token/)
    assert.equal(payload.redactedMemory.contextCache.statusFilter, 'enabled')
    assert.equal(payload.redactedMemory.contextCache.limit, 12)
    assert.equal(payload.redactedMemory.contextCache.generatedAt, '2026-07-08T09:00:00Z')
    assert.equal(payload.redactedMemory.contextCache.sourcePackageGeneratedAt, '2026-07-08T08:59:00Z')
    assert.equal(payload.currentSnapshot.specHash, 'hash-real')
    assert.doesNotMatch(fullText, /raw-json-token|raw-json-password/)

    const overridden = await handler({
      jsonrpc: '2.0',
      id: 308,
      method: 'tools/call',
      params: { name: 'get_session_state', arguments: { projectId: 8 } }
    })
    const stale = overridden.result.structuredContent
    assert.equal(stale.currentProject.projectId, 8)
    assert.equal(stale.redactedMemory.contextCache.status, 'PROJECT_MISMATCH')
    assert.equal(stale.currentSnapshot.specHash, null)
    assert.ok(stale.diagnostics.some((diagnostic) => diagnostic.code === 'CONTEXT_CACHE_PROJECT_MISMATCH'))
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('capability catalog resource is read from backend with project diagnostics and structured content', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        kind: 'dataspec-ai-capability-catalog',
        schemaVersion: 1,
        catalogVersion: '2026.06.28',
        projectId: 7,
        capabilities: [{ id: 'lint-sql', writeRisk: 'WRITES_DATASPEC_RECORD' }],
        diagnostics: [{ code: 'CATALOG_READY', status: 'pass' }]
      }
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 34,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/capability-catalog' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0], 'http://dataspec.local/api/capabilities?projectId=7')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(payload.kind, 'dataspec-ai-capability-catalog')
  assert.equal(read.result.structuredContent.capabilities[0].id, 'lint-sql')
})

test('global capability catalog resource omits project query', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        kind: 'dataspec-ai-capability-catalog',
        schemaVersion: 1,
        catalogVersion: '2026.06.28',
        projectId: null,
        capabilities: [],
        diagnostics: [{ code: 'MISSING_PROJECT', status: 'warn' }]
      }
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 35,
    method: 'resources/read',
    params: { uri: 'dataspec://capability-catalog' }
  })

  assert.equal(calls[0], 'http://dataspec.local/api/capabilities')
  assert.equal(read.result.structuredContent.diagnostics[0].code, 'MISSING_PROJECT')
})

test('capability catalog resource failure returns AI-readable DataSpec error', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => {
    throw new Error('ECONNREFUSED')
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 36,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/capability-catalog' }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'DATASPEC_SERVER_UNAVAILABLE')
  assert.match(response.error.data.dataspecError.suggestedAction, /doctor/)
})

test('workflow recipes resource is served locally without external service', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const read = await handler({
    jsonrpc: '2.0',
    id: 31,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/workflow-recipes' }
  })

  const content = read.result.contents[0]
  const payload = JSON.parse(content.text)
  assert.equal(content.mimeType, 'application/json')
  assert.equal(payload.kind, 'dataspec-workflow-recipes')
  assert.deepEqual(payload.recipes.map((recipe) => recipe.id), [
    'create-table',
    'review-pr-sql',
    'reverse-import-standards',
    'export-min-context',
    'standard-evidence-review',
    'standard-maintenance'
  ])
  assert.ok(payload.recipes[0].steps.some((step) => step.command.includes('export-context')))
  const evidenceRecipe = payload.recipes.find((recipe) => recipe.id === 'standard-evidence-review')
  assert.ok(evidenceRecipe.steps.some((step) => step.command.includes('GET /api/standard-evidence')))
  assert.equal(evidenceRecipe.sideEffectPolicy, 'plan-only')
  const maintenanceRecipe = payload.recipes.find((recipe) => recipe.id === 'standard-maintenance')
  assert.ok(maintenanceRecipe.steps.some((step) => step.command.includes('/api/standard-maintenance/workflows/plan')))
  assert.equal(maintenanceRecipe.sideEffectPolicy, 'plan-only')
})

test('ai task profiles resource is read from backend with configured profile', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    aiProfile: 'sql-fix'
  }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        selectedProfileId: 'sql-fix',
        profiles: [{ profileId: 'sql-fix', taskType: 'SQL_FIX' }],
        diagnostics: [{ code: 'PROFILE_READY', status: 'pass' }]
      }
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 32,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/ai-task-profiles' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0], 'http://dataspec.local/api/ai-profiles?projectId=7&profile=sql-fix')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(payload.selectedProfileId, 'sql-fix')
  assert.equal(payload.profiles[0].taskType, 'SQL_FIX')
})

test('schema registry resource is read from backend without project query', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        kind: 'dataspec-schema-registry',
        schemaVersion: 1,
        registryVersion: '2026.06.28',
        contracts: [{ contractId: 'field', schemaVersion: '1.0', stableFields: ['name'] }]
      }
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 33,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/schema-registry' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0], 'http://dataspec.local/api/contracts')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(payload.kind, 'dataspec-schema-registry')
  assert.equal(payload.contracts[0].contractId, 'field')
})

test('ai task runs resource returns recent failures with structured content', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: 'ds_mcp_token'
  }, async (url, options = {}) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: [taskRunListItemFixture()]
    })
  })

  const read = await handler({
    jsonrpc: '2.0',
    id: 37,
    method: 'resources/read',
    params: { uri: 'dataspec://project/7/ai-task-runs' }
  })

  const payload = JSON.parse(read.result.contents[0].text)
  assert.equal(calls[0].url, 'http://dataspec.local/api/ai-task-runs/recent-failures?projectId=7&limit=10')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_mcp_token')
  assert.equal(read.result.contents[0].mimeType, 'application/json')
  assert.equal(payload.kind, 'dataspec-ai-task-runs')
  assert.equal(payload.items[0].failedStep, 'lint-items')
  assert.equal(read.result.structuredContent.items[0].resumeCommand, 'node tools/dataspec-cli.mjs task show 91 --project 7 --format json')
})

test('prompts list and get return DataSpec workflow guidance', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const listed = await handler({ jsonrpc: '2.0', id: 4, method: 'prompts/list' })
  assert.deepEqual(listed.result.prompts.map((prompt) => prompt.name), [
    'create_table_with_dataspec',
    'review_sql_with_dataspec',
    'reverse_import_standards',
    'answer_field_standard_question',
    'dataspec_create_table',
    'dataspec_review_sql',
    'dataspec_design_fields'
  ])
  const firstClassPrompt = listed.result.prompts.find((prompt) => prompt.name === 'reverse_import_standards')
  assert.equal(firstClassPrompt.dataspecGuidance.templateId, 'reverse_import_standards')
  assert.ok(firstClassPrompt.dataspecGuidance.toolSequence.includes('search_fields'))
  assert.equal(firstClassPrompt.safety.readOnly, true)
  assert.equal(firstClassPrompt.safety.writesProject, false)

  const prompt = await handler({
    jsonrpc: '2.0',
    id: 5,
    method: 'prompts/get',
    params: {
      name: 'dataspec_create_table',
      arguments: { businessDescription: '用户订单表' }
    }
  })

  assert.match(prompt.result.messages[0].content.text, /用户订单表/)
  assert.match(prompt.result.messages[0].content.text, /DataSpec/)
  assert.match(prompt.result.messages[0].content.text, /ai-task-profiles/)
  assert.match(prompt.result.messages[0].content.text, /schema-registry/)
  assert.match(prompt.result.messages[0].content.text, /capability-catalog/)
  assert.match(prompt.result.messages[0].content.text, /safety/)
  assert.match(prompt.result.messages[0].content.text, /requiresDryRun/)
  assert.match(prompt.result.messages[0].content.text, /export_evidence_package/)

  const reversePrompt = await handler({
    jsonrpc: '2.0',
    id: 205,
    method: 'prompts/get',
    params: {
      name: 'reverse_import_standards',
      arguments: { sourceDescription: '从订单库反向导入字段' }
    }
  })
  assert.match(reversePrompt.result.messages[0].content.text, /requiredInputs/)
  assert.match(reversePrompt.result.messages[0].content.text, /toolSequence/)
  assert.match(reversePrompt.result.messages[0].content.text, /stopConditions/)
  assert.match(reversePrompt.result.messages[0].content.text, /evidenceRequirements/)
  assert.match(reversePrompt.result.messages[0].content.text, /从订单库反向导入字段/)
})

test('lint_sql tool returns structured lint result and json text content', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url, options) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: {
        errorCount: 1,
        warningCount: 0,
        suggestionCount: 0,
        issues: [{ ruleCode: 'table_naming_snake_case' }]
      }
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 6,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE UserOrder (userId bigint);', projectId: 8 }
    }
  })

  assert.equal(calls[0].url, 'http://dataspec.local/api/lint')
  assert.deepEqual(JSON.parse(calls[0].options.body), {
    sql: 'CREATE TABLE UserOrder (userId bigint);',
    projectId: 8
  })
  assert.equal(response.result.structuredContent.errorCount, 1)
  assert.equal(JSON.parse(response.result.content[0].text).issues[0].ruleCode, 'table_naming_snake_case')
  assert.equal(response.result.isError, false)
})

test('lint_sql tool forwards configured and explicit profile hints', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    aiProfile: 'sql-fix'
  }, async (url, options) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
    })
  })

  await handler({
    jsonrpc: '2.0',
    id: 61,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE users (id bigint);', taskType: 'PR_REVIEW' }
    }
  })

  assert.deepEqual(JSON.parse(calls[0].options.body), {
    sql: 'CREATE TABLE users (id bigint);',
    projectId: 7,
    taskType: 'PR_REVIEW'
  })
})

test('get_session_bootstrap tool returns structured bootstrap package', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: 'ds_mcp_token'
  }, async (url, options = {}) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: { ...sessionBootstrapFixture(), projectId: 8 }
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 63,
    method: 'tools/call',
    params: {
      name: 'get_session_bootstrap',
      arguments: { projectId: 8 }
    }
  })

  assert.equal(calls[0].url, 'http://dataspec.local/api/bootstrap/session?projectId=8&server=http%3A%2F%2Fdataspec.local')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_mcp_token')
  assert.equal(response.result.structuredContent.kind, 'dataspec-ai-session-bootstrap')
  assert.equal(response.result.structuredContent.projectId, 8)
  assert.equal(JSON.parse(response.result.content[0].text).status, 'READY')
})

test('get_session_bootstrap tool works without configured project', async () => {
  const calls = []
  const handler = createMcpHandler({
    server: 'http://dataspec.local'
  }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        ...sessionBootstrapFixture(),
        status: 'BLOCKED',
        projectId: null,
        nextActions: [{ code: 'SELECT_PROJECT', severity: 'error', message: '请选择项目' }]
      }
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 64,
    method: 'tools/call',
    params: {
      name: 'get_session_bootstrap',
      arguments: {}
    }
  })

  assert.equal(calls[0], 'http://dataspec.local/api/bootstrap/session?server=http%3A%2F%2Fdataspec.local')
  assert.equal(response.result.structuredContent.status, 'BLOCKED')
  assert.equal(response.result.structuredContent.projectId, null)
  assert.equal(response.result.structuredContent.nextActions[0].code, 'SELECT_PROJECT')
})

test('task card tools create and render local cards without backend calls', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const tools = await handler({ jsonrpc: '2.0', id: 65, method: 'tools/list' })
  assert.ok(tools.result.tools.some((tool) => tool.name === 'create_task_card'))
  assert.ok(tools.result.tools.some((tool) => tool.name === 'render_task_card'))
  const taskCardTool = tools.result.tools.find((tool) => tool.name === 'create_task_card')
  const workflowIdDescription = taskCardTool.inputSchema.properties.workflowId.description
  for (const workflowId of supportedWorkflowRecipeIds()) {
    assert.match(workflowIdDescription, new RegExp(workflowId))
  }
  const lintTool = tools.result.tools.find((tool) => tool.name === 'lint_sql')
  assert.equal(lintTool.safety.readOnly, false)
  assert.equal(lintTool.safety.writesProject, true)
  assert.equal(lintTool.safety.requiresIdempotencyKey, false)

  const created = await handler({
    jsonrpc: '2.0',
    id: 66,
    method: 'tools/call',
    params: {
      name: 'create_task_card',
      arguments: {
        workflowId: 'create-table',
        goal: '创建订单表',
        projectId: 7,
        inputs: { businessDescription: '订单表' }
      }
    }
  })

  const rendered = await handler({
    jsonrpc: '2.0',
    id: 67,
    method: 'tools/call',
    params: {
      name: 'render_task_card',
      arguments: { taskCard: created.result.structuredContent }
    }
  })

  assert.equal(created.result.structuredContent.kind, 'dataspec-ai-task-card')
  assert.equal(created.result.structuredContent.workflowId, 'create-table')
  assert.match(created.result.content[0].text, /dataspec-ai-task-card/)
  assert.match(rendered.result.content[0].text, /DataSpec AI Task Card/)
  assert.match(rendered.result.content[0].text, /创建订单表/)
})

test('task card tools reject unknown workflow and unsafe sensitive inputs', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const unknown = await handler({
    jsonrpc: '2.0',
    id: 68,
    method: 'tools/call',
    params: {
      name: 'create_task_card',
      arguments: { workflowId: 'missing', goal: 'x' }
    }
  })
  const unsafe = await handler({
    jsonrpc: '2.0',
    id: 69,
    method: 'tools/call',
    params: {
      name: 'create_task_card',
      arguments: {
        workflowId: 'review-pr-sql',
        goal: '检查 PR',
        projectId: 7,
        inputs: {
          repo: 'acme/app',
          pr: '12',
          GITHUB_TOKEN: 'ghp_secret_token',
          tokens: [{ value: 'raw_secret_token' }],
          apiKeys: [{ value: 'api_key_one' }],
          note: 'Authorization: Basic raw_auth_secret'
        }
      }
    }
  })
  const placeholder = await handler({
    jsonrpc: '2.0',
    id: 70,
    method: 'tools/call',
    params: {
      name: 'create_task_card',
      arguments: {
        workflowId: 'review-pr-sql',
        goal: '检查 PR',
        projectId: 7,
        inputs: { repo: 'acme/app', pr: '12', GITHUB_TOKEN: '$GITHUB_TOKEN' }
      }
    }
  })

  assert.equal(unknown.error.code, -32000)
  assert.equal(unknown.error.data.dataspecError.code, 'TASK_CARD_INVALID')
  assert.match(unknown.error.message, /未知 workflow recipe: missing/)
  assert.equal(unsafe.error.code, -32000)
  assert.equal(unsafe.error.data.dataspecError.code, 'TASK_CARD_INVALID')
  assert.match(unsafe.error.message, /拒绝接收明文敏感输入/)
  assert.doesNotMatch(JSON.stringify(unsafe.error), /ghp_secret_token|raw_secret_token|api_key_one|raw_auth_secret/)
  assert.equal(placeholder.result.structuredContent.kind, 'dataspec-ai-task-card')
  assert.equal(placeholder.result.structuredContent.inputs.GITHUB_TOKEN, '***')
  assert.doesNotMatch(JSON.stringify(placeholder.result.structuredContent), /ghp_secret_token/)
  assert.match(JSON.stringify(placeholder.result.structuredContent), /\*\*\*/)
})

test('get_field_catalog tool parses json catalog when possible', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    assert.equal(url, 'http://dataspec.local/api/ai-context/field-catalog?projectId=7')
    return jsonResponse({
      code: 200,
      data: '{"projectId":7,"fields":[{"name":"created_at","dataType":"timestamp"}],"futureField":"compatible-addition"}'
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 7,
    method: 'tools/call',
    params: { name: 'get_field_catalog', arguments: {} }
  })

  assert.equal(response.result.structuredContent.projectId, 7)
  assert.equal(response.result.structuredContent.fields[0].name, 'created_at')
  assert.equal(response.result.structuredContent.fields[0].dataType, 'timestamp')
  const textPayload = JSON.parse(response.result.content[0].text)
  assert.equal(textPayload.fields[0].name, 'created_at')
  assert.equal(textPayload.futureField, 'compatible-addition')
})

test('get_field_catalog tool passes scoped options', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    assert.equal(
      url,
      'http://dataspec.local/api/ai-context/field-catalog?projectId=7&scope=domain&query=contact&status=enabled&limit=10'
    )
    return jsonResponse({ code: 200, data: '{"contextScope":{"scope":"domain"},"fields":[]}' })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 12,
    method: 'tools/call',
    params: {
      name: 'get_field_catalog',
      arguments: { scope: 'domain', query: 'contact', status: 'enabled', limit: 10 }
    }
  })

  assert.equal(response.result.structuredContent.contextScope.scope, 'domain')
})

test('get_field_catalog tool forwards configured profile and explicit scope together', async () => {
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    aiProfile: 'minimal-context'
  }, async (url) => {
    assert.equal(
      url,
      'http://dataspec.local/api/ai-context/field-catalog?projectId=7&profileId=minimal-context&scope=field&limit=5'
    )
    return jsonResponse({ code: 200, data: '{"fields":[]}' })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 62,
    method: 'tools/call',
    params: {
      name: 'get_field_catalog',
      arguments: { scope: 'field', limit: 5 }
    }
  })

  assert.deepEqual(response.result.structuredContent.fields, [])
})

test('search_field_catalog tool reads scoped field catalog', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    assert.equal(
      url,
      'http://dataspec.local/api/ai-context/field-catalog?projectId=8&scope=field&query=%E6%89%8B%E6%9C%BA&limit=5'
    )
    return jsonResponse({
      code: 200,
      data: '{"contextScope":{"scope":"field","query":"手机"},"fields":[{"name":"mobile_no"}]}'
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 13,
    method: 'tools/call',
    params: {
      name: 'search_field_catalog',
      arguments: { projectId: 8, query: '手机', limit: 5 }
    }
  })

  assert.equal(response.result.structuredContent.fields[0].name, 'mobile_no')
  assert.equal(JSON.parse(response.result.content[0].text).contextScope.query, '手机')
})

test('search_fields tool calls backend field search api', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        projectId: 8,
        query: '手机',
        summary: { matchedCount: 1, returnedCount: 1, appliedFilters: { category: 'contact' } },
        items: [{ field: { name: 'mobile_no' }, score: 98, matchReasons: ['别名匹配'] }],
        nextActions: ['优先查看首个高分字段']
      }
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 14,
    method: 'tools/call',
    params: {
      name: 'search_fields',
      arguments: {
        projectId: 8,
        query: '手机',
        category: 'contact',
        tag: 'pii',
        sensitive: true,
        sourceBatchId: 3,
        limit: 5
      }
    }
  })

  const url = new URL(calls[0])
  assert.equal(url.pathname, '/api/fields/search')
  assert.equal(url.searchParams.get('projectId'), '8')
  assert.equal(url.searchParams.get('query'), '手机')
  assert.equal(url.searchParams.get('category'), 'contact')
  assert.equal(url.searchParams.get('tag'), 'pii')
  assert.equal(url.searchParams.get('sensitive'), 'true')
  assert.equal(url.searchParams.get('sourceBatchId'), '3')
  assert.equal(url.searchParams.get('limit'), '5')
  assert.equal(response.result.structuredContent.items[0].field.name, 'mobile_no')
  assert.equal(JSON.parse(response.result.content[0].text).summary.matchedCount, 1)
})

test('suggest_fields tool returns structured suggestions', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: [
        {
          recommendedName: 'mobile_no',
          score: 92,
          matchReason: '显示名匹配',
          existing: true
        }
      ]
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 10,
    method: 'tools/call',
    params: {
      name: 'suggest_fields',
      arguments: { query: '用户手机号', projectId: 8, limit: 3 }
    }
  })

  assert.equal(
    calls[0],
    'http://dataspec.local/api/fields/suggest?projectId=8&query=%E7%94%A8%E6%88%B7%E6%89%8B%E6%9C%BA%E5%8F%B7&limit=3'
  )
  assert.equal(response.result.structuredContent[0].recommendedName, 'mobile_no')
  assert.equal(JSON.parse(response.result.content[0].text)[0].existing, true)
})

test('generate_table_ddl tool returns structured ddl result', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url) => {
    calls.push(url)
    return jsonResponse({
      code: 200,
      data: {
        ddl: 'CREATE TABLE user_order (id bigserial);',
        lintResult: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
      }
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 11,
    method: 'tools/call',
    params: {
      name: 'generate_table_ddl',
      arguments: { templateId: 10, tableName: 'user_order', projectId: 8 }
    }
  })

  assert.equal(
    calls[0],
    'http://dataspec.local/api/generator/ddl/preview?projectId=8&templateId=10&tableName=user_order'
  )
  assert.equal(response.result.structuredContent.ddl, 'CREATE TABLE user_order (id bigserial);')
  assert.equal(JSON.parse(response.result.content[0].text).lintResult.errorCount, 0)
})

test('export_evidence_package tool returns structured package without adding secrets', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: 'ds_mcp_secret_token'
  }, async (url, options) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: evidencePackageFixture()
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 16,
    method: 'tools/call',
    params: {
      name: 'export_evidence_package',
      arguments: {
        sourceType: 'sql-check',
        sourceId: 42,
        sourceTitle: 'SQL 检查 #42'
      }
    }
  })

  assert.equal(calls[0].url, 'http://dataspec.local/api/evidence-packages')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_mcp_secret_token')
  assert.deepEqual(JSON.parse(calls[0].options.body), {
    projectId: 7,
    sourceType: 'SQL_CHECK',
    sourceId: 42,
    sourceTitle: 'SQL 检查 #42'
  })
  assert.equal(response.result.structuredContent.kind, 'dataspec-ai-evidence-package')
  assert.equal(response.result.structuredContent.source.sourceType, 'SQL_CHECK')
  assert.equal(JSON.parse(response.result.content[0].text).nextActions[0], '复核 fixedSql 后再应用补丁。')
  assert.doesNotMatch(response.result.content[0].text, /ds_mcp_secret_token|Authorization|password=secret|jdbc:postgresql/)
})

test('get_ai_task_run tool returns task run detail as structured content', async () => {
  const calls = []
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local'
  }, async (url, options) => {
    calls.push({ url, options })
    return jsonResponse({
      code: 200,
      data: taskRunDetailFixture()
    })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 18,
    method: 'tools/call',
    params: {
      name: 'get_ai_task_run',
      arguments: {
        taskRunId: 91,
        projectId: 7
      }
    }
  })

  assert.equal(calls[0].url, 'http://dataspec.local/api/ai-task-runs/91?projectId=7')
  assert.equal(response.result.structuredContent.id, 91)
  assert.equal(response.result.structuredContent.stepStatus[0].step, 'lint-items')
  assert.equal(JSON.parse(response.result.content[0].text).partialArtifacts[0].type, 'sql-lint-result')
  assert.equal(JSON.parse(response.result.content[0].text).partialArtifacts[0].ref, 'sql-lint-result:good.sql')
  assert.equal(response.result.isError, false)
})

test('export_evidence_package tool accepts coverage payload source', async () => {
  const calls = []
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async (url, options) => {
    calls.push({ url, options })
    return jsonResponse({ code: 200, data: evidencePackageFixture({ sourceType: 'COVERAGE_REPORT', persisted: false }) })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 17,
    method: 'tools/call',
    params: {
      name: 'export_evidence_package',
      arguments: {
        sourceType: 'COVERAGE_REPORT',
        coverageReport: { projectId: 7, summary: { totalFields: 10, coveredFields: 8 } },
        payloadSummary: { note: 'jdbc:mysql://localhost:3306/app?password=secret' }
      }
    }
  })

  const body = JSON.parse(calls[0].options.body)
  assert.equal(body.sourceType, 'COVERAGE_REPORT')
  assert.equal(body.coverageReport.summary.coveredFields, 8)
  assert.equal(response.result.structuredContent.source.persisted, false)
})

test('tool call returns DataSpec diagnostic in json-rpc error data', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => ({
    ok: false,
    status: 403,
    json: async () => ({
      code: 403,
      message: '无权访问项目: 9',
      error: {
        code: 'PROJECT_ACCESS_DENIED',
        category: 'AUTH',
        retryable: false,
        suggestedAction: '切换到 token 授权的项目，或使用具备该项目权限的 API Token 后重试。',
        docsRef: 'README.md#安全基线'
      }
    })
  }))

  const response = await handler({
    jsonrpc: '2.0',
    id: 12,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE users (id bigint);', projectId: 9 }
    }
  })

  assert.equal(response.error.code, -32000)
  assert.match(response.error.message, /无权访问项目/)
  assert.equal(response.error.data.dataspecError.code, 'PROJECT_ACCESS_DENIED')
  assert.equal(response.error.data.dataspecError.category, 'AUTH')
  assert.equal(response.error.data.dataspecError.retryable, false)
  assert.equal(response.error.data.dataspecError.httpStatus, 403)
})

test('tool call preserves safety diagnostic fields and redacts secrets', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => ({
    ok: false,
    status: 400,
    json: async () => ({
      code: 400,
      message: '缺少 Idempotency-Key password=raw-secret Authorization: Bearer ds_mcp_secret Authorization: Basic raw_basic_secret jdbc:mysql://host/db dsn=postgres://user:dsn-secret@host/db postgres://user:naked-secret@host/db',
      error: {
        code: 'IDEMPOTENCY_KEY_REQUIRED',
        category: 'SAFETY',
        retryable: true,
        suggestedAction: '传入 idempotencyKey 后重试 token=raw-secret Authorization: Basic raw_basic_secret dsn=postgres://user:dsn-secret@host/db mysql://user:naked-secret@host/db',
        docsRef: 'README.md#ai-写入安全协议',
        missing: ['Idempotency-Key'],
        operation: 'project-restore:apply',
        safety: { readOnly: false, writesProject: true, requiresIdempotencyKey: true },
        nextActions: ['传入 idempotencyKey 后重试']
      }
    })
  }))

  const response = await handler({
    jsonrpc: '2.0',
    id: 131,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE users (id bigint);', projectId: 7 }
    }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'IDEMPOTENCY_KEY_REQUIRED')
  assert.deepEqual(response.error.data.dataspecError.missing, ['Idempotency-Key'])
  assert.equal(response.error.data.dataspecError.safety.requiresIdempotencyKey, true)
  assert.match(response.error.data.dataspecError.nextActions[0], /idempotencyKey/)
  assert.doesNotMatch(JSON.stringify(response.error), /raw-secret|raw_basic_secret|ds_mcp_secret|jdbc:mysql:\/\/host\/db|dsn-secret|naked-secret|dsn=postgres:\/\/user:|postgres:\/\/user:|mysql:\/\/user:/)
})

test('tool call classifies legacy authorization failure without backend diagnostic', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => ({
    ok: false,
    status: 401,
    json: async () => ({
      code: 401,
      message: '缺少 Authorization Bearer token'
    })
  }))

  const response = await handler({
    jsonrpc: '2.0',
    id: 13,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE users (id bigint);', projectId: 7 }
    }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'AUTH_TOKEN_MISSING_OR_INVALID')
  assert.equal(response.error.data.dataspecError.category, 'AUTH')
  assert.equal(response.error.data.dataspecError.retryable, true)
  assert.equal(response.error.data.dataspecError.httpStatus, 401)
})

test('get_ai_task_run tool returns AI-readable backend diagnostic', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, async () => ({
    ok: false,
    status: 403,
    json: async () => ({
      code: 403,
      message: '无权访问项目: 9',
      error: {
        code: 'PROJECT_ACCESS_DENIED',
        category: 'AUTH',
        retryable: false,
        suggestedAction: '切换到 token 授权的项目后重试。',
        docsRef: 'README.md#安全基线'
      }
    })
  }))

  const response = await handler({
    jsonrpc: '2.0',
    id: 19,
    method: 'tools/call',
    params: {
      name: 'get_ai_task_run',
      arguments: { taskRunId: 91, projectId: 9 }
    }
  })

  assert.equal(response.error.code, -32000)
  assert.equal(response.error.data.dataspecError.code, 'PROJECT_ACCESS_DENIED')
  assert.equal(response.error.data.dataspecError.retryable, false)
  assert.equal(response.error.data.dataspecError.httpStatus, 403)
})

test('unknown method returns json rpc method not found error', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const response = await handler({ jsonrpc: '2.0', id: 8, method: 'nope' })

  assert.equal(response.error.code, -32601)
  assert.match(response.error.message, /不支持的方法/)
})

test('invalid tool project id returns invalid params error', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const response = await handler({
    jsonrpc: '2.0',
    id: 9,
    method: 'tools/call',
    params: {
      name: 'lint_sql',
      arguments: { sql: 'CREATE TABLE users (id bigint);', projectId: 'abc' }
    }
  })

  assert.equal(response.error.code, -32602)
  assert.match(response.error.message, /无效 project id/)
})

test('parseServerArgs keeps project optional and normalizes server url', () => {
  assert.deepEqual(parseServerArgs(['--project', '7', '--server', 'http://dataspec.local/']), {
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: undefined
  })
  assert.deepEqual(parseServerArgs(['--server', 'http://dataspec.local']), {
    projectId: undefined,
    server: 'http://dataspec.local',
    apiToken: undefined
  })
})

test('parseServerArgs rejects option-like values', () => {
  for (const option of ['--project', '--server', '--dataspec-token', '--profile', '--task-type', '--taskType']) {
    assert.throws(
      () => parseServerArgs([option, '-h']),
      new RegExp(`缺少参数值: ${option}`)
    )
  }
})

test('parseServerArgs reads local config defaults and keeps explicit overrides', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-mcp-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, server: 'http://dataspec.local/', apiToken: 'ds_config_token' }),
      'utf8'
    )

    assert.deepEqual(parseServerArgs([], dir), {
      projectId: 7,
      server: 'http://dataspec.local',
      apiToken: 'ds_config_token'
    })
    assert.deepEqual(parseServerArgs([
      '--project',
      '8',
      '--server',
      'http://override.local/',
      '--dataspec-token',
      'ds_arg_token'
    ], dir), {
      projectId: 8,
      server: 'http://override.local',
      apiToken: 'ds_arg_token'
    })
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('parseServerArgs reads and overrides local profile defaults', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-mcp-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local',
        aiProfile: 'sql-fix',
        taskType: 'SQL_FIX'
      }),
      'utf8'
    )

    assert.deepEqual(parseServerArgs([], dir), {
      projectId: 7,
      server: 'http://dataspec.local',
      apiToken: undefined,
      profileId: 'sql-fix',
      taskType: 'SQL_FIX'
    })
    assert.deepEqual(parseServerArgs(['--task-type', 'PR_REVIEW'], dir), {
      projectId: 7,
      server: 'http://dataspec.local',
      apiToken: undefined,
      taskType: 'PR_REVIEW'
    })
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

function jsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    json: async () => payload
  }
}

function versionCompatibilityFixture(overrides = {}) {
  return {
    kind: 'dataspec-version-compatibility',
    schemaVersion: 1,
    serverVersion: '0.1.0-SNAPSHOT',
    apiSchemaHash: 'sha256:test',
    minCliVersion: '0.1.0',
    supportedCapabilities: [
      { id: 'version-compatibility', status: 'AVAILABLE', minClientVersion: '0.1.0' }
    ],
    deprecatedFields: [],
    compatibility: {
      status: 'COMPATIBLE',
      clientVersion: '0.1.0',
      compatible: true,
      reasons: ['兼容'],
      nextActions: ['继续执行']
    },
    upgradeHints: ['先运行 dataspec compat check --format json'],
    generatedAt: '2026-07-05T10:00:00',
    ...overrides
  }
}

function sessionBootstrapFixture() {
  return {
    kind: 'dataspec-ai-session-bootstrap',
    schemaVersion: 1,
    generatedAt: '2026-07-04T12:00:00',
    status: 'READY',
    projectId: 7,
    server: 'http://dataspec.local',
    authMode: 'TOKEN_PRESENT',
    specVersion: 'v2026.07.04',
    standardSnapshot: {
      snapshotId: 11,
      projectId: 7,
      specVersion: 'v2026.07.04',
      specHash: 'hash-1',
      versioned: true,
      source: 'current'
    },
    availableCapabilities: [
      {
        id: 'lint-sql',
        title: 'SQL 校验与 fixedSql',
        status: 'AVAILABLE',
        writeRisk: 'WRITES_DATASPEC_RECORD',
        requiresProject: true,
        apiEndpoints: ['POST /api/lint'],
        cliCommands: ['dataspec lint <sql-file> --project 7 --format json'],
        mcpResources: [],
        mcpTools: ['lint_sql'],
        nextActions: ['先读取字段目录和规则']
      }
    ],
    recommendedCommands: [
      'dataspec doctor --project 7 --format json',
      'dataspec export-context --project 7 --cache',
      'dataspec lint <sql-file> --project 7 --format json'
    ],
    knownRisks: ['启动包不会执行 lint、导出 Context、反向导入或生成 DDL。'],
    docsRefs: ['README.md#ai-会话启动包', 'README.md#cli'],
    checks: [
      { name: 'project', status: 'pass', message: '已选择 projectId: 7' },
      { name: 'standard', status: 'pass', message: '当前标准快照可用: v2026.07.04' }
    ],
    nextActions: [
      {
        code: 'RUN_DOCTOR',
        severity: 'info',
        message: '从 doctor 开始确认本地配置和远端契约状态。',
        command: 'dataspec doctor --project 7 --format json',
        docsRef: 'README.md#cli',
        retryable: true
      }
    ]
  }
}

async function failingFetch() {
  throw new Error('fetch should not be called')
}

function evidencePackageFixture(overrides = {}) {
  const sourceType = overrides.sourceType ?? 'SQL_CHECK'
  return {
    kind: 'dataspec-ai-evidence-package',
    schemaVersion: 1,
    packageId: 'ev_42',
    projectId: 7,
    generatedAt: '2026-06-28T10:00:00Z',
    source: {
      sourceType,
      sourceId: sourceType === 'COVERAGE_REPORT' ? null : 42,
      sourceTitle: 'SQL 检查 #42',
      status: 'COMPLETED',
      persisted: overrides.persisted ?? true
    },
    standardSnapshot: {
      snapshotId: 3,
      version: 'v1',
      hash: 'abc123'
    },
    inputsSummary: {
      sqlLength: 128,
      connection: '[REDACTED_JDBC_URL]'
    },
    outputsSummary: {
      errorCount: 0,
      fixedSqlAvailable: true
    },
    validationSummary: {
      status: 'PASSED',
      diagnostics: [{ level: 'INFO', code: 'READY', message: '可交付' }]
    },
    artifacts: [{ name: 'fixedSql', mediaType: 'text/sql', summary: { available: true } }],
    nextActions: ['复核 fixedSql 后再应用补丁。'],
    suggestedCommands: ['dataspec evidence export --source-type SQL_CHECK --source-id 42 --format zip --output evidence.zip']
  }
}

function taskRunListItemFixture(overrides = {}) {
  return {
    id: 91,
    projectId: 7,
    taskType: 'SQL_LINT',
    sourceType: 'AI_BATCH',
    sourceId: 31,
    status: 'FAILED',
    inputHash: 'sha256:test',
    retryable: true,
    failedStep: 'lint-items',
    resumeCommand: 'node tools/dataspec-cli.mjs task show 91 --project 7 --format json',
    nextAction: '查看失败步骤并复制 resumeCommand 重试。',
    operatorName: 'mcp',
    startedAt: '2026-07-04T10:00:00',
    finishedAt: '2026-07-04T10:00:02',
    expiresAt: '2026-07-11T10:00:00',
    createdAt: '2026-07-04T10:00:00',
    ...overrides
  }
}

function taskRunDetailFixture(overrides = {}) {
  return {
    ...taskRunListItemFixture(),
    idempotencyKey: 'retry-91',
    stepStatus: [
      {
        step: 'lint-items',
        status: 'FAILED',
        message: '1 个 SQL 文件检查失败',
        artifactRef: 'sql-lint-result:bad.sql'
      }
    ],
    partialArtifacts: [
      {
        type: 'sql-lint-result',
        name: 'good.sql',
        ref: 'sql-lint-result:good.sql',
        summary: { errorCount: 0 }
      }
    ],
    metadata: {
      profileId: 'sql-fix',
      source: 'mcp'
    },
    updatedAt: '2026-07-04T10:00:02',
    ...overrides
  }
}
