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
    'dataspec://project/7/rules-yaml'
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
    return jsonResponse({ code: 200, data: '{"fields":[{"name":"created_at"}]}' })
  })

  const response = await handler({
    jsonrpc: '2.0',
    id: 7,
    method: 'tools/call',
    params: { name: 'get_field_catalog', arguments: {} }
  })

  assert.equal(response.result.structuredContent.fields[0].name, 'created_at')
  assert.equal(JSON.parse(response.result.content[0].text).fields[0].name, 'created_at')
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
