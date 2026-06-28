#!/usr/bin/env node

import { createInterface } from 'node:readline'
import { pathToFileURL } from 'node:url'
import { loadDataSpecConfig } from './dataspec-config.mjs'
import { workflowRecipesResourcePayload } from './dataspec-workflows.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
const SERVER_NAME = 'dataspec-mcp'
const SERVER_VERSION = '0.1.0'

const RESOURCE_DEFS = {
  'field-catalog': {
    name: 'DataSpec Field Catalog',
    description: '当前项目的标准字段目录，供 AI 生成或评审 SQL 时引用。',
    path: '/api/ai-context/field-catalog',
    mimeType: 'application/json'
  },
  'database-rules': {
    name: 'DataSpec Database Rules',
    description: '当前项目的数据库设计规则 Markdown。',
    path: '/api/ai-context/database-rules',
    mimeType: 'text/markdown'
  },
  'rules-yaml': {
    name: 'DataSpec Rules YAML',
    description: '当前项目的规则配置 YAML。',
    path: '/api/ai-context/rules-yaml',
    mimeType: 'text/yaml'
  },
  'workflow-recipes': {
    name: 'DataSpec Workflow Recipes',
    description: 'AI/CLI/MCP 常用 DataSpec 任务计划，包含输入、步骤、失败恢复和下一步建议。',
    mimeType: 'application/json',
    localContent(projectId) {
      return JSON.stringify(workflowRecipesResourcePayload(projectId), null, 2)
    }
  },
  'ai-task-profiles': {
    name: 'DataSpec AI Task Profiles',
    description: '当前项目的 AI 任务模式，说明上下文范围、fixedSql 策略、输出格式和推荐命令。',
    path: '/api/ai-profiles',
    mimeType: 'application/json',
    profileResource: true
  }
}

const PROMPTS = {
  dataspec_create_table: {
    description: '按 DataSpec 标准创建 PostgreSQL 表。',
    arguments: [
      {
        name: 'businessDescription',
        description: '业务表或数据对象描述。',
        required: false
      },
      {
        name: 'projectId',
        description: '可选项目 ID，未提供时使用 MCP Server 启动项目。',
        required: false
      }
    ],
    buildText(args, projectId) {
      return [
        '你是 DataSpec 数据建模助手。请先读取并遵守以下 MCP resources：',
        `- dataspec://project/${projectId}/ai-task-profiles`,
        `- dataspec://project/${projectId}/field-catalog`,
        `- dataspec://project/${projectId}/database-rules`,
        '',
        '先根据 AI task profile 选择 context scope、fixedSql 策略和输出格式；再根据字段目录优先复用标准字段，生成 PostgreSQL DDL。要求表名和列名使用 snake_case，并为表和字段补充 COMMENT ON 语句。',
        args.businessDescription ? `业务描述：${args.businessDescription}` : '业务描述：请根据用户后续输入补全。'
      ].join('\n')
    }
  },
  dataspec_review_sql: {
    description: '按 DataSpec 标准评审 SQL。',
    arguments: [
      {
        name: 'sql',
        description: '待评审 SQL。',
        required: false
      },
      {
        name: 'projectId',
        description: '可选项目 ID，未提供时使用 MCP Server 启动项目。',
        required: false
      }
    ],
    buildText(args, projectId) {
      return [
        '请按 DataSpec 标准评审 SQL。先读取 AI task profile，再读取字段目录和数据库规则，并在需要机器校验时调用 MCP tool `lint_sql`。',
        `AI profile：dataspec://project/${projectId}/ai-task-profiles`,
        `字段目录：dataspec://project/${projectId}/field-catalog`,
        `数据库规则：dataspec://project/${projectId}/database-rules`,
        args.sql ? `\n待评审 SQL：\n\`\`\`sql\n${args.sql}\n\`\`\`` : ''
      ].filter(Boolean).join('\n')
    }
  },
  dataspec_design_fields: {
    description: '把业务需求转换为 DataSpec 字段设计建议。',
    arguments: [
      {
        name: 'businessDescription',
        description: '业务需求或字段场景描述。',
        required: false
      },
      {
        name: 'projectId',
        description: '可选项目 ID，未提供时使用 MCP Server 启动项目。',
        required: false
      }
    ],
    buildText(args, projectId) {
      return [
        '请把业务需求拆成字段设计建议。先读取 AI task profile 和 DataSpec 字段目录，优先复用已有标准字段；缺口字段请说明建议字段名、类型、注释和是否应纳入标准字段库。',
        `AI profile：dataspec://project/${projectId}/ai-task-profiles`,
        `字段目录：dataspec://project/${projectId}/field-catalog`,
        args.businessDescription ? `业务需求：${args.businessDescription}` : '业务需求：请根据用户后续输入补全。'
      ].join('\n')
    }
  }
}

class JsonRpcError extends Error {
  constructor(code, message, data) {
    super(message)
    this.code = code
    this.data = data
  }
}

/**
 * 创建可测试的 MCP handler。第一版只做本地 stdio 协议适配，所有业务能力通过 DataSpec HTTP API 完成。
 */
export function createMcpHandler(config, fetchFn = globalThis.fetch) {
  if (!fetchFn) {
    throw new Error('当前 Node 版本不支持 fetch，请使用 Node.js 18+')
  }
  const server = normalizeServer(config.server)
  const defaultProjectId = parseProjectId(config.projectId)
  const apiToken = normalizeApiToken(config.apiToken)
  const defaultProfileSelection = resolveProfileSelection(config)

  return async function handleMessage(message) {
    const id = message?.id
    try {
      if (!message || message.jsonrpc !== '2.0' || typeof message.method !== 'string') {
        throw new JsonRpcError(-32600, '无效 JSON-RPC 请求')
      }
      if (id === undefined && message.method.startsWith('notifications/')) {
        return undefined
      }
      const result = await dispatch(message.method, message.params ?? {}, {
        server,
        defaultProjectId,
        defaultProfileSelection,
        apiToken,
        fetchFn
      })
      return { jsonrpc: '2.0', id, result }
    } catch (error) {
      return toErrorResponse(id ?? null, error)
    }
  }
}

/**
 * 解析 MCP Server 启动参数。项目 ID 是 resource URI 可确定化的基础，
 * 可由显式参数提供，也可由业务仓库 `.dataspec/config.json` 提供。
 */
export function parseServerArgs(argv, startDir = process.cwd(), env = process.env) {
  const { options } = parseArgs(argv, ['project', 'server', 'dataspec-token', 'profile', 'task-type', 'taskType'])
  const config = loadDataSpecConfig(startDir)
  const profileSelection = resolveProfileSelection({
    profileId: options.profile,
    taskType: options.taskType ?? options['task-type']
  }, config)
  return {
    projectId: parseProjectId(options.project ?? config.projectId),
    server: normalizeServer(options.server ?? config.server),
    apiToken: normalizeApiToken(options['dataspec-token'] ?? env.DATASPEC_TOKEN ?? config.apiToken),
    ...profileSelection
  }
}

async function dispatch(method, params, context) {
  if (method === 'initialize') {
    return {
      protocolVersion: params.protocolVersion ?? '2025-06-18',
      capabilities: {
        resources: {},
        prompts: {},
        tools: {}
      },
      serverInfo: {
        name: SERVER_NAME,
        version: SERVER_VERSION
      }
    }
  }
  if (method === 'ping') {
    return {}
  }
  if (method === 'resources/list') {
    return listResources(context.defaultProjectId)
  }
  if (method === 'resources/read') {
    return await readResource(params, context)
  }
  if (method === 'prompts/list') {
    return listPrompts()
  }
  if (method === 'prompts/get') {
    return getPrompt(params, context.defaultProjectId)
  }
  if (method === 'tools/list') {
    return listTools()
  }
  if (method === 'tools/call') {
    return await callTool(params, context)
  }
  throw new JsonRpcError(-32601, `不支持的方法: ${method}`)
}

function listResources(projectId) {
  return {
    resources: Object.entries(RESOURCE_DEFS).map(([key, def]) => ({
      uri: resourceUri(projectId, key),
      name: def.name,
      description: def.description,
      mimeType: def.mimeType
    }))
  }
}

async function readResource(params, context) {
  const uri = params?.uri
  if (!uri) {
    throw new JsonRpcError(-32602, 'resources/read 需要 uri')
  }
  const { projectId, resourceKey } = parseResourceUri(uri)
  const def = RESOURCE_DEFS[resourceKey]
  if (!def) {
    throw new JsonRpcError(-32602, `未知 resource: ${uri}`)
  }
  const text = typeof def.localContent === 'function'
    ? def.localContent(projectId)
    : def.profileResource
      ? JSON.stringify(await fetchProfileResource(context, projectId), null, 2)
      : await fetchAiContextText(context, def.path, projectId)
  return {
    contents: [
      {
        uri,
        mimeType: def.mimeType,
        text
      }
    ]
  }
}

function listPrompts() {
  return {
    prompts: Object.entries(PROMPTS).map(([name, prompt]) => ({
      name,
      description: prompt.description,
      arguments: prompt.arguments
    }))
  }
}

function getPrompt(params, defaultProjectId) {
  const name = params?.name
  const prompt = PROMPTS[name]
  if (!prompt) {
    throw new JsonRpcError(-32602, `未知 prompt: ${name}`)
  }
  const args = params.arguments ?? {}
  const projectId = optionalProjectId(args.projectId, defaultProjectId)
  return {
    description: prompt.description,
    messages: [
      {
        role: 'user',
        content: {
          type: 'text',
          text: prompt.buildText(args, projectId)
        }
      }
    ]
  }
}

function listTools() {
  return {
    tools: [
      {
        name: 'lint_sql',
        description: '调用 DataSpec lint 校验 SQL，返回结构化问题列表。',
        inputSchema: {
          type: 'object',
          properties: {
            sql: {
              type: 'string',
              description: '待校验 SQL。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            profileId: {
              type: 'string',
              description: '可选 AI profile id，未提供时使用 MCP Server 启动配置。'
            },
            taskType: {
              type: 'string',
              description: '可选 AI task type，未提供时使用 MCP Server 启动配置。'
            }
          },
          required: ['sql']
        }
      },
      {
        name: 'get_field_catalog',
        description: '读取 DataSpec 标准字段目录，可按 scope/query/status/limit 裁剪。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            scope: {
              type: 'string',
              description: '可选裁剪范围: all、field、domain、tag、table、changed。'
            },
            query: {
              type: 'string',
              description: '可选检索关键词。'
            },
            status: {
              type: 'string',
              description: '可选字段状态，如 enabled、disabled、deprecated。'
            },
            limit: {
              type: 'integer',
              description: '可选返回字段上限。'
            },
            profileId: {
              type: 'string',
              description: '可选 AI profile id，用于服务端默认裁剪范围。'
            },
            taskType: {
              type: 'string',
              description: '可选 AI task type，用于服务端默认裁剪范围。'
            }
          }
        }
      },
      {
        name: 'search_field_catalog',
        description: '按当前任务关键词检索较小的 DataSpec 标准字段目录。',
        inputSchema: {
          type: 'object',
          properties: {
            query: {
              type: 'string',
              description: '当前 SQL、表名、字段名或业务需求关键词。'
            },
            scope: {
              type: 'string',
              description: '可选裁剪范围，默认 field。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            status: {
              type: 'string',
              description: '可选字段状态。'
            },
            limit: {
              type: 'integer',
              description: '可选返回字段上限，默认 20。'
            },
            profileId: {
              type: 'string',
              description: '可选 AI profile id，用于服务端默认裁剪范围。'
            },
            taskType: {
              type: 'string',
              description: '可选 AI task type，用于服务端默认裁剪范围。'
            }
          },
          required: ['query']
        }
      },
      {
        name: 'search_fields',
        description: '通过 DataSpec 后端检索项目字段标准，返回命中原因、推荐使用范围和下一步建议。',
        inputSchema: {
          type: 'object',
          properties: {
            query: {
              type: 'string',
              description: '自然语言、字段名或业务需求关键词；也可仅传结构化过滤条件。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            category: {
              type: 'string',
              description: '可选字段分类过滤。'
            },
            tag: {
              type: 'string',
              description: '可选字段标签过滤。'
            },
            status: {
              type: 'string',
              description: '可选字段状态，如 enabled、disabled、deprecated。'
            },
            sensitive: {
              type: 'boolean',
              description: '可选敏感字段过滤。'
            },
            sourceBatchId: {
              type: 'integer',
              description: '可选反向导入批次 ID 过滤。'
            },
            limit: {
              type: 'integer',
              description: '可选返回数量，默认 20。'
            }
          }
        }
      },
      {
        name: 'suggest_fields',
        description: '根据业务描述推荐 DataSpec 标准字段候选。',
        inputSchema: {
          type: 'object',
          properties: {
            query: {
              type: 'string',
              description: '业务字段描述，如“用户手机号”。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            limit: {
              type: 'integer',
              description: '可选返回数量，默认 5。'
            }
          },
          required: ['query']
        }
      },
      {
        name: 'generate_table_ddl',
        description: '根据 DataSpec 表模板生成 PostgreSQL DDL，并返回 lint 自检结果。',
        inputSchema: {
          type: 'object',
          properties: {
            templateId: {
              type: 'integer',
              description: '表模板 ID。'
            },
            tableName: {
              type: 'string',
              description: '生成 DDL 使用的 snake_case 表名。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            }
          },
          required: ['templateId', 'tableName']
        }
      }
    ]
  }
}

async function callTool(params, context) {
  const name = params?.name
  const args = params?.arguments ?? {}
  if (name === 'lint_sql') {
    return await callLintSql(args, context)
  }
  if (name === 'get_field_catalog') {
    return await callGetFieldCatalog(args, context)
  }
  if (name === 'search_field_catalog') {
    return await callSearchFieldCatalog(args, context)
  }
  if (name === 'search_fields') {
    return await callSearchFields(args, context)
  }
  if (name === 'suggest_fields') {
    return await callSuggestFields(args, context)
  }
  if (name === 'generate_table_ddl') {
    return await callGenerateTableDdl(args, context)
  }
  throw new JsonRpcError(-32602, `未知 tool: ${name}`)
}

async function callLintSql(args, context) {
  const sql = args.sql
  if (typeof sql !== 'string' || sql.trim() === '') {
    throw new JsonRpcError(-32602, 'lint_sql 需要非空 sql')
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const response = await context.fetchFn(`${context.server}/api/lint`, {
    method: 'POST',
    headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ sql, projectId, ...resolveProfileSelection(args, context.defaultProfileSelection) })
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(result)
}

async function callGetFieldCatalog(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const text = await fetchAiContextText(context, RESOURCE_DEFS['field-catalog'].path, projectId, scopedCatalogParams(args, context))
  const structured = parseJsonOrFallback(text)
  return toolJsonResult(structured)
}

async function callSearchFieldCatalog(args, context) {
  const query = args.query
  if (typeof query !== 'string' || query.trim() === '') {
    throw new JsonRpcError(-32602, 'search_field_catalog 需要非空 query')
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const scopedArgs = {
    ...args,
    scope: args.scope ?? 'field',
    limit: args.limit ?? 20
  }
  const text = await fetchAiContextText(context, RESOURCE_DEFS['field-catalog'].path, projectId, scopedCatalogParams(scopedArgs, context))
  return toolJsonResult(parseJsonOrFallback(text))
}

async function callSearchFields(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'query', args.query)
  appendOptionalParam(params, 'category', args.category)
  appendOptionalParam(params, 'tag', args.tag)
  appendOptionalParam(params, 'status', args.status)
  appendOptionalParam(params, 'sourceBatchId', args.sourceBatchId)
  if (args.sensitive !== undefined && args.sensitive !== null && args.sensitive !== '') {
    params.set('sensitive', String(parseOptionalBoolean(args.sensitive, 'sensitive')))
  }
  params.set('limit', String(optionalLimit(args.limit, 20)))
  const response = await context.fetchFn(`${context.server}/api/fields/search?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(result)
}

async function callSuggestFields(args, context) {
  const query = args.query
  if (typeof query !== 'string' || query.trim() === '') {
    throw new JsonRpcError(-32602, 'suggest_fields 需要非空 query')
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const limit = optionalLimit(args.limit, 5)
  const url = `${context.server}/api/fields/suggest?projectId=${encodeURIComponent(projectId)}&query=${encodeURIComponent(query)}&limit=${encodeURIComponent(limit)}`
  const response = await context.fetchFn(url, { headers: dataSpecHeaders(context.apiToken) })
  const result = await readDataSpecJson(response)
  return toolJsonResult(result)
}

async function callGenerateTableDdl(args, context) {
  const templateId = parsePositiveInteger(args.templateId, 'templateId')
  const tableName = args.tableName
  if (typeof tableName !== 'string' || tableName.trim() === '') {
    throw new JsonRpcError(-32602, 'generate_table_ddl 需要非空 tableName')
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const url = `${context.server}/api/generator/ddl/preview?projectId=${encodeURIComponent(projectId)}&templateId=${encodeURIComponent(templateId)}&tableName=${encodeURIComponent(tableName)}`
  const response = await context.fetchFn(url, { headers: dataSpecHeaders(context.apiToken) })
  const result = await readDataSpecJson(response)
  return toolJsonResult(result)
}

async function fetchAiContextText(context, path, projectId, extraParams = {}) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'profileId', extraParams.profileId)
  appendOptionalParam(params, 'taskType', extraParams.taskType)
  appendOptionalParam(params, 'scope', extraParams.scope)
  appendOptionalParam(params, 'query', extraParams.query)
  appendOptionalParam(params, 'status', extraParams.status)
  appendOptionalParam(params, 'limit', extraParams.limit)
  const url = `${context.server}${path}?${params.toString()}`
  const response = await context.fetchFn(url, { headers: dataSpecHeaders(context.apiToken) })
  return await readDataSpecJson(response)
}

async function fetchProfileResource(context, projectId) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'profile', context.defaultProfileSelection.profileId ?? context.defaultProfileSelection.taskType)
  const response = await context.fetchFn(`${context.server}/api/ai-profiles?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

function scopedCatalogParams(args, context) {
  return {
    ...resolveProfileSelection(args, context.defaultProfileSelection),
    scope: normalizeOptionalText(args.scope),
    query: normalizeOptionalText(args.query),
    status: normalizeOptionalText(args.status),
    limit: args.limit === undefined || args.limit === null || args.limit === ''
      ? undefined
      : optionalLimit(args.limit, 20)
  }
}

function appendOptionalParam(params, key, value) {
  if (value !== undefined && value !== null && String(value).trim() !== '') {
    params.set(key, String(value).trim())
  }
}

function normalizeOptionalText(value) {
  if (value === undefined || value === null) {
    return undefined
  }
  const normalized = String(value).trim()
  return normalized === '' ? undefined : normalized
}

function resolveProfileSelection(input = {}, fallback = {}) {
  const explicitProfile = normalizeOptionalText(input.profileId ?? input.aiProfile)
  const explicitTaskType = normalizeOptionalText(input.taskType)
  if (explicitProfile || explicitTaskType) {
    return profileSelection(explicitProfile, explicitTaskType)
  }
  return profileSelection(
    normalizeOptionalText(fallback.profileId ?? fallback.aiProfile),
    normalizeOptionalText(fallback.taskType)
  )
}

function profileSelection(profileId, taskType) {
  const selection = {}
  if (profileId) {
    selection.profileId = profileId
  }
  if (taskType) {
    selection.taskType = taskType
  }
  return selection
}

async function readDataSpecJson(response) {
  const payload = await readResponseJson(response)
  if (!response.ok) {
    throw toDataSpecRpcError(payload, response.status)
  }
  if (payload?.code && payload.code !== 200) {
    throw toDataSpecRpcError(payload, response.status)
  }
  return payload?.data ?? payload
}

async function readResponseJson(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

function toDataSpecRpcError(payload, httpStatus) {
  const message = payload?.message || `DataSpec 请求失败，HTTP ${httpStatus}`
  return new JsonRpcError(-32000, message, {
    dataspecError: normalizeDataSpecDiagnostic(payload?.error, httpStatus, message)
  })
}

function normalizeDataSpecDiagnostic(error, httpStatus, message) {
  if (error && typeof error === 'object') {
    return {
      code: String(error.code ?? 'DATASPEC_ERROR'),
      category: String(error.category ?? 'DATASPEC'),
      retryable: Boolean(error.retryable),
      suggestedAction: String(error.suggestedAction ?? '查看 DataSpec 响应 message 并按提示修正请求。'),
      docsRef: String(error.docsRef ?? 'README.md#验证'),
      httpStatus
    }
  }
  return fallbackDataSpecDiagnostic(httpStatus, message)
}

function fallbackDataSpecDiagnostic(httpStatus, message) {
  if (httpStatus === 401) {
    return {
      code: 'AUTH_TOKEN_MISSING_OR_INVALID',
      category: 'AUTH',
      retryable: true,
      suggestedAction: '提供有效的 API Token；CLI/MCP 可设置 DATASPEC_TOKEN 或 --dataspec-token。',
      docsRef: 'README.md#安全基线',
      httpStatus
    }
  }
  if (httpStatus === 403) {
    return {
      code: 'PROJECT_ACCESS_DENIED',
      category: 'AUTH',
      retryable: false,
      suggestedAction: '切换到 token 授权的项目，或使用具备该项目权限的 API Token 后重试。',
      docsRef: 'README.md#安全基线',
      httpStatus
    }
  }
  return {
    code: httpStatus >= 500 ? 'INTERNAL_ERROR' : 'DATASPEC_REQUEST_FAILED',
    category: httpStatus >= 500 ? 'SERVER' : 'DATASPEC',
    retryable: httpStatus >= 500,
    suggestedAction: message || '查看 DataSpec 响应 message 并按提示修正请求。',
    docsRef: 'README.md#验证',
    httpStatus
  }
}

function toolJsonResult(structuredContent) {
  return {
    content: [
      {
        type: 'text',
        text: JSON.stringify(structuredContent, null, 2)
      }
    ],
    structuredContent,
    isError: false
  }
}

function parseJsonOrFallback(text) {
  try {
    return JSON.parse(text)
  } catch {
    return { text }
  }
}

function parseResourceUri(uri) {
  const match = /^dataspec:\/\/project\/(\d+)\/([a-z-]+)$/.exec(uri)
  if (!match) {
    throw new JsonRpcError(-32602, `无效 resource uri: ${uri}`)
  }
  return {
    projectId: parseProjectId(match[1]),
    resourceKey: match[2]
  }
}

function resourceUri(projectId, key) {
  return `dataspec://project/${projectId}/${key}`
}

function optionalProjectId(value, fallback) {
  if (value === undefined || value === null || value === '') {
    return fallback
  }
  return parseProjectId(value)
}

function optionalLimit(value, fallback) {
  if (value === undefined || value === null || value === '') {
    return fallback
  }
  return parsePositiveInteger(value, 'limit')
}

function parseOptionalBoolean(value, label) {
  if (typeof value === 'boolean') {
    return value
  }
  const normalized = String(value).trim().toLowerCase()
  if (['true', '1', 'yes'].includes(normalized)) {
    return true
  }
  if (['false', '0', 'no'].includes(normalized)) {
    return false
  }
  throw new JsonRpcError(-32602, `无效 ${label}: ${value}`)
}

function parseProjectId(value) {
  if (value === undefined || value === null || value === '') {
    throw new JsonRpcError(-32602, '需要提供 --project <id> 或 .dataspec/config.json 的 projectId')
  }
  return parsePositiveInteger(value, 'project id')
}

function parsePositiveInteger(value, label) {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new JsonRpcError(-32602, `无效 ${label}: ${value}`)
  }
  return parsed
}

function normalizeServer(server = DEFAULT_SERVER) {
  const normalized = String(server || DEFAULT_SERVER).replace(/\/+$/, '')
  return normalized || DEFAULT_SERVER
}

function normalizeApiToken(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return String(value).trim() || undefined
}

function dataSpecHeaders(apiToken, headers = {}) {
  if (!apiToken) {
    return headers
  }
  return {
    ...headers,
    Authorization: `Bearer ${apiToken}`
  }
}

function parseArgs(args, allowedOptions) {
  const options = {}
  const allowedOptionSet = new Set(allowedOptions)
  for (let i = 0; i < args.length; i += 1) {
    const token = args[i]
    if (!token.startsWith('--')) {
      throw new Error(`未知参数: ${token}`)
    }
    const name = token.slice(2)
    if (!allowedOptionSet.has(name)) {
      throw new Error(`未知参数: ${token}`)
    }
    const value = args[i + 1]
    if (!value || value.startsWith('--')) {
      throw new Error(`缺少参数值: ${token}`)
    }
    options[name] = value
    i += 1
  }
  return { options }
}

function toErrorResponse(id, error) {
  const code = error instanceof JsonRpcError ? error.code : -32000
  const body = {
    jsonrpc: '2.0',
    id,
    error: {
      code,
      message: error.message
    }
  }
  if (error instanceof JsonRpcError && error.data !== undefined) {
    body.error.data = error.data
  }
  return body
}

async function runStdioServer(argv) {
  const config = parseServerArgs(argv)
  const handler = createMcpHandler(config)
  const input = createInterface({
    input: process.stdin,
    crlfDelay: Infinity
  })

  for await (const line of input) {
    if (!line.trim()) {
      continue
    }
    try {
      const response = await handler(JSON.parse(line))
      if (response) {
        process.stdout.write(`${JSON.stringify(response)}\n`)
      }
    } catch (error) {
      process.stdout.write(`${JSON.stringify(toErrorResponse(null, error))}\n`)
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  runStdioServer(process.argv.slice(2)).catch((error) => {
    process.stderr.write(`错误: ${error.message}\n`)
    process.exitCode = 2
  })
}
