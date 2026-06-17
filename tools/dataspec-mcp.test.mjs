import assert from 'node:assert/strict'
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
  const handler = createMcpHandler({ projectId: 7, server: 'http://dataspec.local/' }, async (url) => {
    calls.push(url)
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
    server: 'http://dataspec.local'
  })
  assert.throws(() => parseServerArgs(['--server', 'http://dataspec.local']), /需要提供 --project/)
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
