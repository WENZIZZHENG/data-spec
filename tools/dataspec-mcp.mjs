#!/usr/bin/env node

import { createInterface } from 'node:readline'
import { pathToFileURL } from 'node:url'
import { loadDataSpecConfig } from './dataspec-config.mjs'
import { workflowRecipesResourcePayload } from './dataspec-workflows.mjs'
import {
  createTaskCard,
  renderTaskCardMarkdown
} from './dataspec-task-card.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
const SERVER_NAME = 'dataspec-mcp'
const MCP_VERSION = '0.1.0'
const EVIDENCE_SOURCE_TYPES = ['AI_JOB', 'SQL_CHECK', 'COVERAGE_REPORT', 'AI_BATCH_RUN', 'AI_TASK_RUN']

const READ_ONLY_TOOL_SAFETY = {
  readOnly: true,
  writesProject: false,
  requiresDryRun: false,
  supportsUndo: false,
  requiresIdempotencyKey: false,
  sensitiveInputs: [],
  nextActions: ['读取 capability catalog 后再选择后续工具。']
}

const TOOL_SAFETY = {
  get_session_bootstrap: READ_ONLY_TOOL_SAFETY,
  create_task_card: READ_ONLY_TOOL_SAFETY,
  render_task_card: READ_ONLY_TOOL_SAFETY,
  lint_sql: {
    readOnly: false,
    writesProject: true,
    requiresDryRun: false,
    supportsUndo: true,
    requiresIdempotencyKey: false,
    sensitiveInputs: ['sql'],
    nextActions: ['调用前先读取 capability catalog；完成后可导出 evidence package。']
  },
  get_field_catalog: READ_ONLY_TOOL_SAFETY,
  search_field_catalog: READ_ONLY_TOOL_SAFETY,
  search_fields: READ_ONLY_TOOL_SAFETY,
  suggest_fields: READ_ONLY_TOOL_SAFETY,
  generate_table_ddl: {
    readOnly: false,
    writesProject: true,
    requiresDryRun: false,
    supportsUndo: true,
    requiresIdempotencyKey: false,
    sensitiveInputs: [],
    nextActions: ['生成 DDL 后检查 lintResult 和方言诊断，再交付给用户确认。']
  },
  export_evidence_package: READ_ONLY_TOOL_SAFETY,
  get_ai_task_run: READ_ONLY_TOOL_SAFETY
}

const RESOURCE_DEFS = {
  'version-compatibility': {
    name: 'DataSpec Version Compatibility',
    description: '只读版本兼容握手，建议 AI 在运行版本敏感的 CLI/MCP 工作流前先读取。',
    mimeType: 'application/json',
    globalResource: true,
    versionCompatibilityResource: true
  },
  'capability-catalog': {
    name: 'DataSpec AI Capability Catalog',
    description: '只读自描述能力清单，说明 DataSpec 可供 AI 使用的 API、CLI、MCP 入口、前置检查和 writeRisk。',
    path: '/api/capabilities',
    mimeType: 'application/json',
    capabilityResource: true
  },
  'session-bootstrap': {
    name: 'DataSpec AI Session Bootstrap',
    description: 'AI 新会话第一跳，聚合当前项目、标准版本、可用能力、推荐命令、风险提示和 nextActions。',
    path: '/api/bootstrap/session',
    mimeType: 'application/json',
    bootstrapResource: true
  },
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
  },
  'schema-registry': {
    name: 'DataSpec Schema Registry',
    description: 'AI 可消费输出契约 registry，说明 schemaVersion、稳定字段、废弃字段和兼容策略。',
    path: '/api/contracts',
    mimeType: 'application/json',
    contractResource: true
  },
  'ai-task-runs': {
    name: 'DataSpec AI Task Runs',
    description: '当前项目最近失败或部分失败的 AI task run，包含失败步骤、retryable 状态和恢复命令。',
    mimeType: 'application/json',
    taskRunResource: true
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
        `- dataspec://project/${projectId}/capability-catalog`,
        `- dataspec://project/${projectId}/schema-registry`,
        `- dataspec://project/${projectId}/ai-task-profiles`,
        `- dataspec://project/${projectId}/field-catalog`,
        `- dataspec://project/${projectId}/database-rules`,
        '',
        '先根据 capability catalog 确认可用入口、safety、preflightChecks 和 writeRisk；写入型能力必须先检查 safety.requiresDryRun、safety.requiresIdempotencyKey 和 nextActions；再根据 schema registry 确认稳定字段和兼容策略，根据 AI task profile 选择 context scope、fixedSql 策略和输出格式；再根据字段目录优先复用标准字段，生成 PostgreSQL DDL。要求表名和列名使用 snake_case，并为表和字段补充 COMMENT ON 语句。',
        '交付前请调用 MCP tool `export_evidence_package` 导出 evidence package，作为本次建模依据、输出和下一步建议的只读交接物。',
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
        '请按 DataSpec 标准评审 SQL。先读取 capability catalog、schema registry 和 AI task profile，并检查相关 capability 或 MCP tool 的 safety metadata；写入型动作必须按 safety.nextActions 先 dry-run 或准备 Idempotency-Key；再读取字段目录和数据库规则，并在需要机器校验时调用 MCP tool `lint_sql`。',
        '完成修复或评审交付前，请调用 MCP tool `export_evidence_package` 导出 evidence package，便于用户和下游 AI 复盘。',
        `能力清单：dataspec://project/${projectId}/capability-catalog`,
        `契约 registry：dataspec://project/${projectId}/schema-registry`,
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
        '请把业务需求拆成字段设计建议。先读取 capability catalog、schema registry、AI task profile 和 DataSpec 字段目录，并检查候选写入动作的 safety metadata；优先复用已有标准字段；缺口字段请说明建议字段名、类型、注释和是否应纳入标准字段库。',
        '完成字段设计建议前，请调用 MCP tool `export_evidence_package` 导出 evidence package，记录使用的标准、候选依据和后续动作。',
        `能力清单：dataspec://project/${projectId}/capability-catalog`,
        `契约 registry：dataspec://project/${projectId}/schema-registry`,
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
  const defaultProjectId = parseConfiguredProjectId(config.projectId)
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
 * 解析 MCP Server 启动参数。项目 ID 可由显式参数或 `.dataspec/config.json`
 * 提供；缺省时仍允许 AI 先调用 get_session_bootstrap 获取 SELECT_PROJECT。
 */
export function parseServerArgs(argv, startDir = process.cwd(), env = process.env) {
  const { options } = parseArgs(argv, ['project', 'server', 'dataspec-token', 'profile', 'task-type', 'taskType'])
  const config = loadDataSpecConfig(startDir)
  const profileSelection = resolveProfileSelection({
    profileId: options.profile,
    taskType: options.taskType ?? options['task-type']
  }, config)
  return {
    projectId: parseConfiguredProjectId(options.project ?? config.projectId),
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
        version: MCP_VERSION
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
  const globalResources = Object.entries(RESOURCE_DEFS)
    .filter(([, def]) => def.globalResource)
    .map(([key, def]) => ({
      uri: `dataspec://${key}`,
      name: def.name,
      description: def.description,
      mimeType: def.mimeType
    }))
  if (projectId === undefined) {
    const def = RESOURCE_DEFS['capability-catalog']
    return {
      resources: [
        ...globalResources,
        {
          uri: 'dataspec://capability-catalog',
          name: def.name,
          description: def.description,
          mimeType: def.mimeType
        }
      ]
    }
  }
  return {
    resources: [
      ...globalResources,
      ...Object.entries(RESOURCE_DEFS)
        .filter(([, def]) => !def.globalResource)
        .map(([key, def]) => ({
          uri: resourceUri(projectId, key),
          name: def.name,
          description: def.description,
          mimeType: def.mimeType
        }))
    ]
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
  let structuredContent
  const text = typeof def.localContent === 'function'
    ? def.localContent(projectId)
    : def.versionCompatibilityResource
      ? JSON.stringify(structuredContent = await fetchVersionCompatibilityResource(context), null, 2)
      : def.profileResource
        ? JSON.stringify(await fetchProfileResource(context, projectId), null, 2)
        : def.contractResource
          ? JSON.stringify(await fetchContractResource(context), null, 2)
          : def.capabilityResource
            ? JSON.stringify(structuredContent = await fetchCapabilityResource(context, projectId), null, 2)
            : def.bootstrapResource
              ? JSON.stringify(structuredContent = await fetchSessionBootstrapResource(context, projectId), null, 2)
              : def.taskRunResource
                ? JSON.stringify(structuredContent = await fetchTaskRunResource(context, projectId), null, 2)
                : await fetchAiContextText(context, def.path, projectId)
  const result = {
    contents: [
      {
        uri,
        mimeType: def.mimeType,
        text
      }
    ]
  }
  if (structuredContent !== undefined) {
    result.structuredContent = structuredContent
  }
  return result
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
  const tools = [
      {
        name: 'get_session_bootstrap',
        description: '读取 DataSpec AI 会话启动包，返回当前项目、标准版本、可用能力、推荐命令、风险提示和 nextActions。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            }
          }
        }
      },
      {
        name: 'create_task_card',
        description: '从 DataSpec workflow recipe 创建本地 AI 任务卡；只返回计划和恢复信息，不执行 workflow。',
        inputSchema: {
          type: 'object',
          properties: {
            workflowId: {
              type: 'string',
              description: 'workflow recipe id，例如 create-table、review-pr-sql、reverse-import-standards、export-min-context。'
            },
            goal: {
              type: 'string',
              description: '本次任务目标。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            inputs: {
              type: 'object',
              description: '非敏感任务输入或环境变量占位符；明文 token/password/JDBC URL/Authorization 会被拒绝。'
            }
          },
          required: ['workflowId', 'goal']
        }
      },
      {
        name: 'render_task_card',
        description: '把 DataSpec AI 任务卡渲染成 Markdown；不执行 workflow，不调用远端写接口。',
        inputSchema: {
          type: 'object',
          properties: {
            taskCard: {
              type: 'object',
              description: 'DataSpec AI task card JSON。'
            }
          },
          required: ['taskCard']
        }
      },
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
      },
      {
        name: 'export_evidence_package',
        description: '导出只读 AI 执行证据包，返回 evidence package 结构化 JSON。',
        inputSchema: {
          type: 'object',
          properties: {
            sourceType: {
              type: 'string',
              enum: EVIDENCE_SOURCE_TYPES,
              description: '证据来源类型: AI_JOB、SQL_CHECK、COVERAGE_REPORT、AI_BATCH_RUN、AI_TASK_RUN。'
            },
            sourceId: {
              type: 'integer',
              description: '持久化来源记录 ID。COVERAGE_REPORT 可省略，使用 coverageReport payload。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            sourceTitle: {
              type: 'string',
              description: '可选来源标题，用于 payload 型证据包的人类可读说明。'
            },
            coverageReport: {
              type: 'object',
              description: 'COVERAGE_REPORT 来源需要传入当前覆盖率报告摘要。'
            },
            standardSnapshot: {
              type: 'object',
              description: '可选标准快照摘要。'
            },
            payloadSummary: {
              type: 'object',
              description: '可选脱敏 payload 摘要，后端会再次清洗。'
            }
          },
          required: ['sourceType']
        }
      },
      {
        name: 'get_ai_task_run',
        description: '读取 AI task run 详情，返回失败步骤、partial artifacts、metadata 和恢复命令。',
        inputSchema: {
          type: 'object',
          properties: {
            taskRunId: {
              type: 'integer',
              description: 'AI task run ID。'
            },
            id: {
              type: 'integer',
              description: 'taskRunId 的兼容别名。'
            },
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            }
          },
          anyOf: [
            { required: ['taskRunId'] },
            { required: ['id'] }
          ]
        }
      }
    ]
  return {
    tools: tools.map(withToolSafety)
  }
}

function withToolSafety(tool) {
  return {
    ...tool,
    safety: TOOL_SAFETY[tool.name] ?? READ_ONLY_TOOL_SAFETY
  }
}

async function callTool(params, context) {
  const name = params?.name
  const args = params?.arguments ?? {}
  if (name === 'get_session_bootstrap') {
    return await callGetSessionBootstrap(args, context)
  }
  if (name === 'create_task_card') {
    return callCreateTaskCard(args, context)
  }
  if (name === 'render_task_card') {
    return callRenderTaskCard(args)
  }
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
  if (name === 'export_evidence_package') {
    return await callExportEvidencePackage(args, context)
  }
  if (name === 'get_ai_task_run') {
    return await callGetAiTaskRun(args, context)
  }
  throw new JsonRpcError(-32602, `未知 tool: ${name}`)
}

async function callGetSessionBootstrap(args, context) {
  const projectId = optionalBootstrapProjectId(args.projectId, context.defaultProjectId)
  const result = await fetchSessionBootstrapResource(context, projectId)
  return toolJsonResult(result)
}

function callCreateTaskCard(args, context) {
  try {
    assertTaskCardMcpInputsSafe({
      goal: args.goal,
      inputs: args.inputs
    })
    const projectId = optionalBootstrapProjectId(args.projectId, context.defaultProjectId)
    const card = createTaskCard({
      workflowId: args.workflowId,
      projectId,
      goal: args.goal,
      inputs: args.inputs && typeof args.inputs === 'object' ? args.inputs : {}
    })
    return toolJsonResult(card)
  } catch (error) {
    throw taskCardRpcError(error)
  }
}

function callRenderTaskCard(args) {
  try {
    const markdown = renderTaskCardMarkdown(args.taskCard)
    return {
      content: [
        {
          type: 'text',
          text: markdown
        }
      ],
      structuredContent: { markdown },
      isError: false
    }
  } catch (error) {
    throw taskCardRpcError(error)
  }
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

async function callExportEvidencePackage(args, context) {
  const req = buildEvidencePackageRequest(args, context.defaultProjectId)
  const response = await context.fetchFn(`${context.server}/api/evidence-packages`, {
    method: 'POST',
    headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(req)
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(result)
}

async function callGetAiTaskRun(args, context) {
  const taskRunId = parsePositiveInteger(args.taskRunId ?? args.id, 'taskRunId')
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  try {
    const params = new URLSearchParams()
    params.set('projectId', String(projectId))
    const response = await context.fetchFn(`${context.server}/api/ai-task-runs/${encodeURIComponent(taskRunId)}?${params.toString()}`, {
      headers: dataSpecHeaders(context.apiToken)
    })
    const result = await readDataSpecJson(response)
    return toolJsonResult(result)
  } catch (error) {
    throw normalizeTaskRunRpcError(error)
  }
}

function buildEvidencePackageRequest(args, defaultProjectId) {
  const sourceType = normalizeEvidenceSourceType(args.sourceType ?? args['source-type'])
  if (!sourceType) {
    throw new JsonRpcError(-32602, `export_evidence_package 需要 sourceType，支持: ${EVIDENCE_SOURCE_TYPES.join(', ')}`)
  }
  if (!EVIDENCE_SOURCE_TYPES.includes(sourceType)) {
    throw new JsonRpcError(-32602, `不支持的 evidence sourceType: ${sourceType}，支持: ${EVIDENCE_SOURCE_TYPES.join(', ')}`)
  }
  const sourceIdInput = args.sourceId ?? args['source-id']
  if (sourceType !== 'COVERAGE_REPORT' && (sourceIdInput === undefined || sourceIdInput === null || sourceIdInput === '')) {
    throw new JsonRpcError(-32602, `${sourceType} 需要 sourceId`)
  }
  const req = {
    projectId: optionalProjectId(args.projectId, defaultProjectId),
    sourceType
  }
  if (sourceIdInput !== undefined && sourceIdInput !== null && sourceIdInput !== '') {
    req.sourceId = parsePositiveInteger(sourceIdInput, 'sourceId')
  }
  appendOptionalObject(req, 'coverageReport', args.coverageReport)
  appendOptionalObject(req, 'standardSnapshot', args.standardSnapshot)
  appendOptionalObject(req, 'payloadSummary', args.payloadSummary)
  appendOptionalTextProperty(req, 'sourceTitle', args.sourceTitle ?? args['source-title'])
  return req
}

function normalizeEvidenceSourceType(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return String(value).trim().replaceAll('-', '_').toUpperCase()
}

function appendOptionalObject(target, key, value) {
  if (value !== undefined && value !== null) {
    if (typeof value !== 'object' || Array.isArray(value)) {
      throw new JsonRpcError(-32602, `${key} 必须是 object`)
    }
    target[key] = value
  }
}

function appendOptionalTextProperty(target, key, value) {
  const normalized = normalizeOptionalText(value)
  if (normalized) {
    target[key] = normalized
  }
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

async function fetchContractResource(context) {
  const response = await context.fetchFn(`${context.server}/api/contracts`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchCapabilityResource(context, projectId) {
  try {
    const params = new URLSearchParams()
    appendOptionalParam(params, 'projectId', projectId)
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await context.fetchFn(`${context.server}/api/capabilities${suffix}`, {
      headers: dataSpecHeaders(context.apiToken)
    })
    return await readDataSpecJson(response)
  } catch (error) {
    if (error instanceof JsonRpcError) {
      throw error
    }
    throw new JsonRpcError(-32000, `读取 capability catalog 失败: ${error?.message ?? 'DataSpec 服务不可用'}`, {
      dataspecError: {
        code: 'DATASPEC_SERVER_UNAVAILABLE',
        category: 'NETWORK',
        retryable: true,
        suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置。',
        docsRef: 'README.md#cli',
        httpStatus: null
      }
    })
  }
}

async function fetchVersionCompatibilityResource(context) {
  try {
    const params = new URLSearchParams()
    params.set('client', 'mcp')
    params.set('clientVersion', MCP_VERSION)
    const response = await context.fetchFn(`${context.server}/api/capabilities/version?${params.toString()}`, {
      headers: dataSpecHeaders(context.apiToken)
    })
    return await readDataSpecJson(response)
  } catch (error) {
    throw versionCompatibilityRpcError(error)
  }
}

function versionCompatibilityRpcError(error) {
  const upstreamDiagnostic = error instanceof JsonRpcError && error.data?.dataspecError
    ? sanitizeSecretValue(error.data.dataspecError)
    : {}
  const httpStatus = Number.isInteger(upstreamDiagnostic.httpStatus) ? upstreamDiagnostic.httpStatus : null
  const retryable = typeof upstreamDiagnostic.retryable === 'boolean' ? upstreamDiagnostic.retryable : true
  return new JsonRpcError(-32000, `读取版本兼容握手失败: ${sanitizeSecretText(error?.message ?? 'DataSpec 服务不可用')}`, {
    dataspecError: {
      code: 'VERSION_COMPATIBILITY_UNAVAILABLE',
      category: upstreamDiagnostic.category ?? 'NETWORK',
      retryable,
      suggestedAction: '先运行 dataspec compat check --format json 或 dataspec doctor --format json 检查服务、server URL、token 和版本兼容状态。',
      docsRef: 'README.md#版本兼容握手',
      httpStatus
    }
  })
}

async function fetchSessionBootstrapResource(context, projectId) {
  try {
    const params = new URLSearchParams()
    appendOptionalParam(params, 'projectId', projectId)
    params.set('server', context.server)
    const response = await context.fetchFn(`${context.server}/api/bootstrap/session?${params.toString()}`, {
      headers: dataSpecHeaders(context.apiToken)
    })
    return await readDataSpecJson(response)
  } catch (error) {
    if (error instanceof JsonRpcError) {
      throw error
    }
    throw new JsonRpcError(-32000, `读取 session bootstrap 失败: ${error?.message ?? 'DataSpec 服务不可用'}`, {
      dataspecError: {
        code: 'DATASPEC_SERVER_UNAVAILABLE',
        category: 'NETWORK',
        retryable: true,
        suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置；然后重试 session bootstrap。',
        docsRef: 'README.md#ai-会话启动包',
        httpStatus: null
      }
    })
  }
}

async function fetchTaskRunResource(context, projectId) {
  try {
    const params = new URLSearchParams()
    params.set('projectId', String(projectId))
    params.set('limit', '10')
    const response = await context.fetchFn(`${context.server}/api/ai-task-runs/recent-failures?${params.toString()}`, {
      headers: dataSpecHeaders(context.apiToken)
    })
    const items = await readDataSpecJson(response)
    return {
      kind: 'dataspec-ai-task-runs',
      schemaVersion: 1,
      projectId,
      scope: 'recent-failures',
      items
    }
  } catch (error) {
    throw normalizeTaskRunRpcError(error)
  }
}

function normalizeTaskRunRpcError(error) {
  if (error instanceof JsonRpcError) {
    return error
  }
  return new JsonRpcError(-32000, `读取 AI task run 失败: ${error?.message ?? 'DataSpec 服务不可用'}`, {
    dataspecError: {
      code: 'DATASPEC_SERVER_UNAVAILABLE',
      category: 'NETWORK',
      retryable: true,
      suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置；然后重试 task run 查询。',
      docsRef: 'README.md#cli',
      httpStatus: null
    }
  })
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
  const message = sanitizeSecretText(payload?.message || `DataSpec 请求失败，HTTP ${httpStatus}`)
  return new JsonRpcError(-32000, message, {
    dataspecError: sanitizeSecretValue(normalizeDataSpecDiagnostic(payload?.error, httpStatus, message))
  })
}

function taskCardRpcError(error) {
  return new JsonRpcError(-32000, error?.message ?? 'DataSpec task card 请求无效', {
    dataspecError: {
      code: 'TASK_CARD_INVALID',
      category: 'TASK_CARD',
      retryable: true,
      suggestedAction: '检查 workflowId、goal、inputs 或 taskCard JSON 后重试；不要传入 token/password/JDBC URL 明文。',
      docsRef: 'README.md#ai-任务卡',
      httpStatus: null
    }
  })
}

function normalizeDataSpecDiagnostic(error, httpStatus, message) {
  if (error && typeof error === 'object') {
    const diagnostic = {
      code: String(error.code ?? 'DATASPEC_ERROR'),
      category: String(error.category ?? 'DATASPEC'),
      retryable: Boolean(error.retryable),
      suggestedAction: String(error.suggestedAction ?? '查看 DataSpec 响应 message 并按提示修正请求。'),
      docsRef: String(error.docsRef ?? 'README.md#验证'),
      httpStatus
    }
    appendDiagnosticExtras(diagnostic, error)
    return diagnostic
  }
  return fallbackDataSpecDiagnostic(httpStatus, message)
}

function appendDiagnosticExtras(diagnostic, error) {
  if (Array.isArray(error.missing)) {
    diagnostic.missing = error.missing.map((item) => String(item))
  }
  if (typeof error.operation === 'string' && error.operation.trim()) {
    diagnostic.operation = error.operation
  }
  if (typeof error.capabilityId === 'string' && error.capabilityId.trim()) {
    diagnostic.capabilityId = error.capabilityId
  }
  if (error.safety && typeof error.safety === 'object' && !Array.isArray(error.safety)) {
    diagnostic.safety = error.safety
  }
  if (Array.isArray(error.nextActions)) {
    diagnostic.nextActions = error.nextActions.map((item) => String(item))
  }
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

function assertTaskCardMcpInputsSafe(payload) {
  const unsafePaths = findUnsafeTaskCardInputPaths(payload)
  if (unsafePaths.length > 0) {
    throw new Error(`create_task_card 拒绝接收明文敏感输入: ${unsafePaths.join(', ')}。请改用环境变量占位符或省略该输入，让任务卡进入 BLOCKED。`)
  }
}

function findUnsafeTaskCardInputPaths(value, pathLabel = 'arguments', key = '') {
  if (isSensitiveTaskCardKey(key) && value !== undefined && value !== null && value !== '') {
    if (typeof value === 'string' && isSafeSecretPlaceholder(String(value).trim())) {
      return []
    }
    return [pathLabel]
  }
  if (typeof value === 'string') {
    return isUnsafeTaskCardSecretValue(key, value) ? [pathLabel] : []
  }
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => findUnsafeTaskCardInputPaths(item, `${pathLabel}[${index}]`, key))
  }
  if (value && typeof value === 'object') {
    return Object.entries(value).flatMap(([itemKey, itemValue]) => (
      findUnsafeTaskCardInputPaths(itemValue, `${pathLabel}.${itemKey}`, itemKey)
    ))
  }
  return []
}

function isUnsafeTaskCardSecretValue(key, value) {
  const text = String(value).trim()
  if (text === '' || isSafeSecretPlaceholder(text)) {
    return false
  }
  return isSensitiveTaskCardKey(key) || containsTaskCardSecretPattern(text)
}

function isSafeSecretPlaceholder(value) {
  return /^\$[A-Za-z_][A-Za-z0-9_]*$/.test(value) ||
    /^\$\{[A-Za-z_][A-Za-z0-9_]*}$/.test(value) ||
    /^%[A-Za-z_][A-Za-z0-9_]*%$/.test(value) ||
    /^(?:\*\*\*|\[REDACTED]|<[^<>]+>)$/i.test(value)
}

function containsTaskCardSecretPattern(value) {
  return /jdbc:[^\s"'<>]+/i.test(value) ||
    /authorization\s*[:=]\s*(?:"[^"]*"|'[^']*'|[^\r\n,;]+)/i.test(value) ||
    /authorization\s*[:=]\s*bearer\s+[^\s,;]+/i.test(value) ||
    /\bbearer\s+[A-Za-z0-9._~+/-]+=*/i.test(value) ||
    /\b(?:passwords?|passwds?|pwds?|tokens?|api[_-]?tokens?|dataspec[_-]?tokens?|api[_-]?keys?|secrets?|client[_-]?secrets?|access[_-]?tokens?|refresh[_-]?tokens?|plain[_-]?tokens?|token[_-]?hash(?:es)?|jdbc[_-]?urls?|connection[_-]?strings?|dsns?)\b\s*[:=]\s*(?:"[^"]*"|'[^']*'|[^\s"',;}&]+)/i.test(value)
}

function isSensitiveTaskCardKey(key) {
  const normalized = String(key ?? '').replace(/[^A-Za-z0-9]/g, '').toLowerCase()
  return [
    'password',
    'passwords',
    'passwd',
    'passwds',
    'pwd',
    'pwds',
    'token',
    'tokens',
    'githubtoken',
    'githubtokens',
    'apitoken',
    'apitokens',
    'dataspectoken',
    'dataspectokens',
    'apikey',
    'apikeys',
    'authorization',
    'secret',
    'secrets',
    'clientsecret',
    'clientsecrets',
    'accesstoken',
    'accesstokens',
    'refreshtoken',
    'refreshtokens',
    'plaintoken',
    'plaintokens',
    'tokenhash',
    'tokenhashes',
    'jdbcurl',
    'jdbcurls',
    'connectionstring',
    'connectionstrings',
    'dsn',
    'dsns'
  ].includes(normalized)
}

function parseResourceUri(uri) {
  if (uri === 'dataspec://version-compatibility') {
    return {
      projectId: undefined,
      resourceKey: 'version-compatibility'
    }
  }
  if (uri === 'dataspec://capability-catalog') {
    return {
      projectId: undefined,
      resourceKey: 'capability-catalog'
    }
  }
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
  return parseProjectId(value === undefined || value === null || value === '' ? fallback : value)
}

function optionalBootstrapProjectId(value, fallback) {
  if ((value === undefined || value === null || value === '') && (fallback === undefined || fallback === null || fallback === '')) {
    return undefined
  }
  return parseProjectId(value === undefined || value === null || value === '' ? fallback : value)
}

function parseConfiguredProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
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
      message: sanitizeSecretText(error.message)
    }
  }
  if (error instanceof JsonRpcError && error.data !== undefined) {
    body.error.data = sanitizeSecretValue(error.data)
  }
  return body
}

function sanitizeSecretValue(value) {
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeSecretValue(item))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key,
      isSensitiveTaskCardKey(key) ? '***' : sanitizeSecretValue(item)
    ]))
  }
  return value
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
  }
  return String(value)
    .replace(/jdbc:[^\s"',;}&]+/gi, 'jdbc:[REDACTED]')
    .replace(/\b((?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/)[^\s"',;}&]+/gi, '$1[REDACTED]')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1[REDACTED]')
    .replace(/(authorization\s*[:=]\s*)(?!\s*['"]?bearer\s+)(['"]?)[^,;}&\r\n]+\2/gi, '$1$2[REDACTED]$2')
    .replace(/(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1[REDACTED]')
    .replace(/((?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)\s*[:=]\s*)[^\s"',;}&]+/gi, '$1[REDACTED]')
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
