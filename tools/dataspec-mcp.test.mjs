import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { createMcpHandler, parseServerArgs } from './dataspec-mcp.mjs'

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
    'dataspec://project/7/field-catalog',
    'dataspec://project/7/database-rules',
    'dataspec://project/7/rules-yaml',
    'dataspec://project/7/workflow-recipes'
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
    'export-min-context'
  ])
  assert.ok(payload.recipes[0].steps.some((step) => step.command.includes('export-context')))
})

test('prompts list and get return DataSpec workflow guidance', async () => {
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local' }, failingFetch)

  const listed = await handler({ jsonrpc: '2.0', id: 4, method: 'prompts/list' })
  assert.deepEqual(listed.result.prompts.map((prompt) => prompt.name), [
    'dataspec_create_table',
    'dataspec_review_sql',
    'dataspec_design_fields'
  ])

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

test('parseServerArgs requires project id and normalizes server url', () => {
  assert.deepEqual(parseServerArgs(['--project', '7', '--server', 'http://dataspec.local/']), {
    projectId: 7,
    server: 'http://dataspec.local',
    apiToken: undefined
  })
  assert.throws(() => parseServerArgs(['--server', 'http://dataspec.local']), /需要提供 --project/)
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

function jsonResponse(payload) {
  return {
    ok: true,
    status: 200,
    json: async () => payload
  }
}

async function failingFetch() {
  throw new Error('fetch should not be called')
}
