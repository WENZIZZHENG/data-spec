#!/usr/bin/env node

import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { createInterface } from 'node:readline'
import { pathToFileURL } from 'node:url'
import { loadDataSpecConfig } from './dataspec-config.mjs'
import {
  supportedWorkflowRecipeIds,
  workflowRecipesResourcePayload
} from './dataspec-workflows.mjs'
import {
  createTaskCard,
  renderTaskCardMarkdown
} from './dataspec-task-card.mjs'
import {
  loadConsumerCompatibilitySuite,
  validateConsumerCompatibilitySuite
} from './dataspec-consumer-compat-check.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
const SERVER_NAME = 'dataspec-mcp'
const MCP_VERSION = '0.1.0'
const EVIDENCE_SOURCE_TYPES = ['AI_JOB', 'SQL_CHECK', 'COVERAGE_REPORT', 'AI_BATCH_RUN', 'AI_TASK_RUN']
const AI_OUTPUT_POST_CHECK_CONTENT_TYPES = ['SQL', 'DDL', 'MARKDOWN', 'JSON', 'TEXT']
const TEST_DATA_MAX_FIELDS = 100
const TEST_DATA_MAX_CASES_PER_FIELD = 3
const TEST_DATA_MAX_SEED_ROWS = 50
const REVIEW_FINDINGS_MAX_ITEMS = 100
const REVIEW_FINDING_MAX_EVIDENCE_REFS = 20
const REVIEW_FINDING_FIELDS = new Set([
  'schemaVersion', 'source', 'findingKey', 'code', 'severity', 'subject', 'location', 'trigger',
  'expected', 'observed', 'evidenceRefs', 'confidence', 'suggestedFix', 'autoFixSafe', 'waiver'
])
const REVIEW_FINDING_SUBJECT_FIELDS = new Set(['projectId', 'kind', 'name', 'tableName', 'columnName', 'stableRef'])
const REVIEW_FINDING_LOCATION_FIELDS = new Set([
  'path', 'line', 'column', 'lineEnd', 'columnEnd', 'sourceStart', 'sourceEnd', 'locationKind'
])
const REVIEW_FINDING_WAIVER_FIELDS = new Set(['waived', 'waiverId', 'reason'])
const REVIEW_FINDING_INPUT_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  description: '外部 AI 提交的共享 Review Finding；服务端会重写 source、projectId 和 findingKey，并验证 evidenceRefs。',
  properties: {
    schemaVersion: { type: 'integer', minimum: 1, description: 'Finding schema 版本；省略时使用当前版本。' },
    source: { type: 'string', enum: ['SQL_LINT', 'AI_OUTPUT_POSTCHECK', 'EXTERNAL_AI'], description: '调用方声明的来源；外部输入会被服务端规范化为 EXTERNAL_AI。' },
    findingKey: { type: 'string', maxLength: 128, description: '调用方去重键；服务端会根据规范化字段重新计算。' },
    code: { type: 'string', minLength: 1, maxLength: 128, description: '稳定规则或问题码。' },
    severity: { type: 'string', enum: ['ERROR', 'WARNING', 'SUGGESTION', 'INFO'], description: '共享问题级别；ERROR/high-confidence/autoFixSafe 必须有当前项目可验证证据。' },
    subject: {
      type: 'object',
      additionalProperties: false,
      description: 'Finding 指向的项目级业务对象摘要，不得包含业务数据行。',
      properties: {
        projectId: { type: 'integer', minimum: 1, description: '调用方项目 ID；服务端会重写为请求项目。' },
        kind: { type: 'string', maxLength: 64, description: '对象类型，如 SQL_COLUMN、STANDARD_REFERENCE 或 AI_OUTPUT。' },
        name: { type: 'string', maxLength: 256, description: '脱敏对象名称。' },
        tableName: { type: 'string', maxLength: 256, description: 'SQL 表名；非 SQL finding 省略。' },
        columnName: { type: 'string', maxLength: 256, description: 'SQL 字段名；非字段 finding 省略。' },
        stableRef: { type: 'string', maxLength: 256, description: '标准对象 stableRef；没有稳定引用时省略。' }
      }
    },
    location: {
      type: 'object',
      additionalProperties: false,
      description: '可选业务仓库相对路径和源码范围；未知位置不应伪造。',
      properties: {
        path: { type: 'string', maxLength: 512, description: '业务仓库相对路径。' },
        line: { type: 'integer', minimum: 1, description: '1-based 起始行。' },
        column: { type: 'integer', minimum: 1, description: '1-based 起始列。' },
        lineEnd: { type: 'integer', minimum: 1, description: '1-based 结束行。' },
        columnEnd: { type: 'integer', minimum: 1, description: '1-based 结束列，不含结束位置。' },
        sourceStart: { type: 'integer', minimum: 0, description: '0-based 起始字符偏移。' },
        sourceEnd: { type: 'integer', minimum: 0, description: '0-based 结束字符偏移，不含结束位置。' },
        locationKind: { type: 'string', maxLength: 64, description: '定位类型，如 table、column 或 comment_column。' }
      }
    },
    trigger: { type: 'string', maxLength: 1000, description: '触发规则或判定条件；输出前脱敏。' },
    expected: { type: 'string', maxLength: 1000, description: '期望状态或约束；输出前脱敏。' },
    observed: { type: 'string', maxLength: 1000, description: '实际观察摘要；不得包含完整业务数据行或凭据。' },
    evidenceRefs: {
      type: 'array',
      maxItems: REVIEW_FINDING_MAX_EVIDENCE_REFS,
      description: '当前项目可解析的 canonical evidence refs；最多 20 条。',
      items: { type: 'string', minLength: 1, maxLength: 500, description: '单条 evidence ref。' }
    },
    confidence: { type: 'integer', minimum: 0, maximum: 100, description: '置信度 0-100；80 及以上必须有可验证证据。' },
    suggestedFix: { type: 'string', maxLength: 1000, description: '脱敏修复建议；不表示允许自动执行。' },
    autoFixSafe: { type: 'boolean', description: '外部 AI 声明仍须 evidence gating，不会自动应用修复。' },
    waiver: {
      type: 'object',
      additionalProperties: false,
      description: '调用方豁免摘要；外部输入不会被当作 DataSpec 项目豁免。',
      properties: {
        waived: { type: 'boolean', description: '调用方声明的豁免状态。' },
        waiverId: { type: 'integer', minimum: 1, description: 'DataSpec 规则豁免 ID；没有持久化豁免时省略。' },
        reason: { type: 'string', maxLength: 500, description: '脱敏豁免原因。' }
      }
    }
  },
  required: ['code']
}

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
  get_session_state: READ_ONLY_TOOL_SAFETY,
  create_task_card: READ_ONLY_TOOL_SAFETY,
  render_task_card: READ_ONLY_TOOL_SAFETY,
  get_field_knowledge_cards: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['query'],
    nextActions: ['字段知识卡是只读聚合视图；遇到 unit/source-of-truth/metric 风险时先读取详情再生成 SQL、DDL 或测试。']
  },
  get_field_semantics: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['query'],
    nextActions: ['字段语义规则只提供 guidance，不代表可执行计算或生产 SQL 修改。']
  },
  get_metric_definitions: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['query'],
    nextActions: ['指标 example SQL 仅作说明，不应直接执行。']
  },
  get_enum_lifecycle: READ_ONLY_TOOL_SAFETY,
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
  search_fields: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['query', 'standardQuery', 'filters'],
    nextActions: ['Standard Query DSL 只读且 project-scoped；如 ignoredFilters 非空，先收窄或改写查询。']
  },
  resolve_standard_refs: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['refs'],
    nextActions: ['在采纳 AI 输出前先解析 stableRef/canonicalRef，遇到 UNKNOWN 或 AMBIGUOUS 时停止并让用户确认。']
  },
  check_ai_output: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['content', 'findings'],
    nextActions: ['PASS 后再复制或执行 AI 产物；WARN/FAIL 时先查看 blocking refs、findings、replacement refs 和 nextActions。']
  },
  suggest_fields: READ_ONLY_TOOL_SAFETY,
  get_table_standards: {
    ...READ_ONLY_TOOL_SAFETY,
    nextActions: ['先读取 table standards，再决定是否调用 generate_table_ddl；缺失或不安全结构标准时停止并让用户确认。']
  },
  generate_test_data_package: {
    ...READ_ONLY_TOOL_SAFETY,
    writesBusinessRepo: false,
    containsRealBusinessRows: false,
    externalNetworkUsed: false,
    externalLlmUsed: false,
    sensitiveInputs: ['fieldNames', 'objectScenario'],
    nextActions: ['仅把结果作为测试、mock 或 seed 草稿；复制到业务仓库前必须人工复核 safety、coverageReport 和 seedProfiles。']
  },
  check_consumer_compatibility: {
    ...READ_ONLY_TOOL_SAFETY,
    requiresServer: false,
    externalNetworkUsed: false,
    externalLlmUsed: false,
    nextActions: ['若 status=BREAKING，先按 diagnostics[].path 修复 fixture、descriptor、schema 或迁移说明。']
  },
  generate_table_ddl: {
    readOnly: false,
    writesProject: true,
    requiresDryRun: false,
    supportsUndo: true,
    requiresIdempotencyKey: false,
    sensitiveInputs: [],
    nextActions: ['生成 DDL 后检查 lintResult 和方言诊断，再交付给用户确认。']
  },
  export_evidence_package: {
    ...READ_ONLY_TOOL_SAFETY,
    sensitiveInputs: ['postCheckSummary', 'postCheckReceipt', 'findings'],
    nextActions: ['外部 findings 仅在 post-check PASS、receipt 匹配且 evidence refs 复验通过后导出。']
  },
  get_ai_task_run: READ_ONLY_TOOL_SAFETY
}

const AGENT_GUIDANCE_TEMPLATES = [
  {
    id: 'create_table_with_dataspec',
    title: 'Create table with DataSpec',
    description: 'Use DataSpec standards to design a PostgreSQL table.',
    requiredInputs: ['businessDescription'],
    safeDefaults: {
      executeWorkflow: false,
      preferExistingFields: true,
      requireEvidencePackage: true
    },
    resourceSequence: ['capability-catalog', 'schema-registry', 'ai-task-profiles', 'field-catalog', 'table-standards', 'database-rules'],
    toolSequence: ['search_fields', 'generate_table_ddl', 'lint_sql', 'export_evidence_package'],
    stopConditions: ['missing project id', 'capability safety metadata unavailable', 'missing or unsafe structure standards for high-risk DDL work', 'DDL contains high-risk unreviewed changes'],
    evidenceRequirements: ['standard fields used', 'DDL preview result', 'lint result', 'next actions'],
    nextActions: ['Read capability safety before using generate_table_ddl or lint_sql.']
  },
  {
    id: 'review_sql_with_dataspec',
    title: 'Review SQL with DataSpec',
    description: 'Review SQL against DataSpec rules and field standards.',
    requiredInputs: ['sql'],
    safeDefaults: {
      executeWorkflow: false,
      fixedSqlMode: 'dry-run',
      requireEvidencePackage: true
    },
    resourceSequence: ['capability-catalog', 'schema-registry', 'ai-task-profiles', 'field-catalog', 'database-rules'],
    toolSequence: ['lint_sql', 'search_fields', 'export_evidence_package'],
    stopConditions: ['SQL text missing', 'lint_sql unavailable', 'write-capable follow-up lacks user confirmation'],
    evidenceRequirements: ['lint result', 'fixedSql diff when present', 'field evidence', 'next actions'],
    nextActions: ['Treat ERROR issues as user-visible findings before suggesting fixedSql.']
  },
  {
    id: 'reverse_import_standards',
    title: 'Reverse import standards',
    description: 'Plan safe reverse import of database metadata into DataSpec standards.',
    requiredInputs: ['sourceDescription'],
    safeDefaults: {
      executeWorkflow: false,
      dryRunOnly: true,
      requireEvidencePackage: true
    },
    resourceSequence: ['capability-catalog', 'schema-registry', 'workflow-recipes', 'ai-task-profiles', 'field-catalog'],
    toolSequence: ['get_session_bootstrap', 'search_fields', 'export_evidence_package'],
    stopConditions: ['source connection details include raw secrets', 'dry-run preview is missing', 'user has not confirmed write action'],
    evidenceRequirements: ['metadata summary', 'candidate mapping rationale', 'dry-run diagnostics', 'next actions'],
    nextActions: ['Use existing reverse-import APIs or UI; do not infer writes from this prompt alone.']
  },
  {
    id: 'answer_field_standard_question',
    title: 'Answer field standard question',
    description: 'Answer read-only questions about standard field naming, status, and evidence.',
    requiredInputs: ['question'],
    safeDefaults: {
      executeWorkflow: false,
      readOnly: true,
      requireEvidencePackage: false
    },
    resourceSequence: ['capability-catalog', 'schema-registry', 'field-catalog'],
    toolSequence: ['search_fields'],
    stopConditions: ['confidence is low', 'no matching standard field', 'question asks for business row data'],
    evidenceRequirements: ['matched fields', 'match reasons', 'confidence or unresolved questions'],
    nextActions: ['When confidence is low, ask for clarification or suggest candidate inbox follow-up.']
  }
]

const RESOURCE_TEMPLATE_KEYS = [
  'session-bootstrap',
  'session-state',
  'capability-catalog',
  'schema-registry',
  'field-catalog',
  'field-knowledge-cards',
  'field-semantics',
  'metric-definitions',
  'table-standards',
  'workflow-recipes',
  'ai-task-profiles',
  'consumer-compatibility-suite',
  'agent-guidance-pack'
]

const READ_ONLY_PROMPT_SAFETY = {
  readOnly: true,
  writesProject: false,
  requiresDryRun: false,
  requiresIdempotencyKey: false,
  sensitiveInputs: [],
  nextActions: []
}

const FIRST_CLASS_PROMPT_SAFETY = {
  create_table_with_dataspec: {
    ...READ_ONLY_PROMPT_SAFETY,
    nextActions: ['Read capability safety before using generate_table_ddl or lint_sql.']
  },
  review_sql_with_dataspec: {
    ...READ_ONLY_PROMPT_SAFETY,
    sensitiveInputs: ['sql'],
    nextActions: ['Treat ERROR issues as user-visible findings before suggesting fixedSql.']
  },
  reverse_import_standards: {
    ...READ_ONLY_PROMPT_SAFETY,
    nextActions: ['Use existing reverse-import APIs or UI; do not infer writes from this prompt alone.']
  },
  answer_field_standard_question: {
    ...READ_ONLY_PROMPT_SAFETY,
    nextActions: ['When confidence is low, ask for clarification or suggest candidate inbox follow-up.']
  }
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
  'session-state': {
    name: 'DataSpec MCP Session State',
    description: 'MCP 当前项目记忆，只读聚合本地配置、上下文缓存、最近任务入口、安全默认值和下一步建议。',
    mimeType: 'application/json',
    localContent(projectId, context) {
      return buildMcpSessionState(projectId, context)
    }
  },
  'field-catalog': {
    name: 'DataSpec Field Catalog',
    description: '当前项目的标准字段目录，供 AI 生成或评审 SQL 时引用。',
    path: '/api/ai-context/field-catalog',
    mimeType: 'application/json'
  },
  'field-knowledge-cards': {
    name: 'DataSpec Field Knowledge Cards',
    description: '只读字段知识卡列表，聚合字段语义、枚举生命周期、命名翻译、使用示例和指标口径摘要。',
    path: '/api/field-knowledge-cards',
    mimeType: 'application/json',
    jsonResource: true
  },
  'field-semantics': {
    name: 'DataSpec Field Semantic Rules',
    description: '只读字段语义规则，说明派生、单位换算、聚合、时间粒度和 source-of-truth guidance。',
    path: '/api/field-semantics',
    mimeType: 'application/json',
    jsonResource: true
  },
  'metric-definitions': {
    name: 'DataSpec Metric Definitions',
    description: '只读指标口径映射，说明 metricKey、标准字段引用、过滤、聚合、时间粒度和 example SQL guidance。',
    path: '/api/metric-definitions',
    mimeType: 'application/json',
    jsonResource: true
  },
  'table-standards': {
    name: 'DataSpec Table Standards',
    description: '只读业务对象、关系、表模板和结构标准上下文，供 AI 在生成 DDL 前检查。',
    mimeType: 'application/json',
    tableStandardsResource: true
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
  'agent-guidance-pack': {
    name: 'DataSpec MCP Agent Guidance Pack',
    description: 'MCP agent 任务引导包，说明常用任务的输入、安全默认值、资源顺序、工具顺序、停止条件和证据要求。',
    mimeType: 'application/json',
    localContent(projectId) {
      return agentGuidancePackPayload(projectId)
    }
  },
  'ai-task-profiles': {
    name: 'DataSpec AI Task Profiles',
    description: '当前项目的 AI 任务模式，说明上下文范围、fixedSql 策略、输出格式和推荐命令。',
    path: '/api/ai-profiles',
    mimeType: 'application/json',
    profileResource: true
  },
  'consumer-compatibility-suite': {
    name: 'DataSpec Consumer Compatibility Suite',
    description: '本地只读消费端兼容套件 fixture，覆盖 DataSpec 自有 API、CLI、MCP、AI Context 和 Schema Registry 契约。',
    mimeType: 'application/json',
    async localContent() {
      return sanitizeSecretValue(await loadConsumerCompatibilitySuite())
    },
    safety: TOOL_SAFETY.check_consumer_compatibility
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

function agentGuidancePackPayload(projectId) {
  return {
    kind: 'dataspec-mcp-agent-guidance-pack',
    schemaVersion: 1,
    projectId,
    compatibilityPolicy: 'Additive templates and optional fields are compatible; removing or renaming template ids, required inputs, safe defaults, tool sequence, stop conditions, or evidence requirements requires fixture and spec updates.',
    templates: AGENT_GUIDANCE_TEMPLATES.map((template) => materializeGuidanceTemplate(template, projectId)),
    nextActions: ['Choose a guidance template, read the listed resources in order, inspect tool safety metadata, then call tools only when inputs and stop conditions are satisfied.']
  }
}

async function buildMcpSessionState(projectId, context) {
  const diagnostics = []
  const [configMemory, contextCache] = await Promise.all([
    readLocalConfigMemory(context.rootDir),
    readContextCacheMemory(context.rootDir, projectId)
  ])
  diagnostics.push(...configMemory.diagnostics, ...contextCache.diagnostics)
  if (projectId === undefined) {
    diagnostics.push({
      code: 'PROJECT_ID_MISSING',
      severity: 'WARN',
      message: '当前 MCP Server 未配置项目 ID，session-state 只能返回 BLOCKED 状态和后续选择项目建议。'
    })
  }
  diagnostics.push({
    code: 'SESSION_STATE_NOT_AUTHORIZATION',
    severity: 'INFO',
    message: 'session-state 只是本地只读快照，不代表后端权限判定；写入型后续动作仍需检查 capability safety、dry-run 和用户确认。'
  })

  const profile = {
    profileId: context.defaultProfileSelection.profileId ?? null,
    taskType: context.defaultProfileSelection.taskType ?? null
  }
  const currentProject = {
    projectId: projectId ?? null,
    status: projectId === undefined ? 'BLOCKED' : 'READY',
    server: sanitizeSecretText(context.server),
    authMode: context.apiToken ? 'TOKEN_PRESENT' : 'TOKEN_MISSING',
    profile
  }
  const currentSnapshot = contextCache.currentSnapshot
  const state = {
    kind: 'dataspec-mcp-session-state',
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    currentProject,
    currentSnapshot,
    lastTaskResult: {
      source: 'ai-task-runs-resource',
      status: projectId === undefined ? 'PROJECT_REQUIRED' : 'AVAILABLE',
      resourceUri: projectId === undefined ? null : resourceUri(projectId, 'ai-task-runs'),
      tool: 'get_ai_task_run',
      summary: projectId === undefined
        ? '选择项目后可读取最近失败或部分失败的 AI task run。'
        : '如需恢复上一轮任务，先读 ai-task-runs resource，再按 taskRunId 调用 get_ai_task_run。'
    },
    toolCursor: {
      recommendedFirstResources: projectId === undefined
        ? ['dataspec://project/{projectId}/session-state', 'dataspec://project/{projectId}/session-bootstrap']
        : [resourceUri(projectId, 'session-state'), resourceUri(projectId, 'session-bootstrap'), resourceUri(projectId, 'capability-catalog')],
      recommendedNextTools: ['get_session_bootstrap', 'get_field_catalog', 'search_fields', 'export_evidence_package']
    },
    safeDefaults: {
      readOnly: true,
      executeWorkflow: false,
      dryRunOnly: true,
      requireCapabilitySafety: true,
      sessionStateIsAuthorization: false,
      writesProject: false,
      containsRealBusinessRows: false
    },
    redactedMemory: {
      config: {
        status: configMemory.status,
        hasConfig: configMemory.status === 'PRESENT',
        location: configMemory.status === 'PRESENT' ? '.dataspec/config.json' : null,
        projectIdPresent: configMemory.projectIdPresent,
        server: configMemory.server,
        apiTokenPresent: Boolean(context.apiToken) || configMemory.apiTokenPresent,
        defaultPathsCount: configMemory.defaultPathsCount,
        securityProfile: configMemory.securityProfile
      },
      contextCache: contextCache.memory
    },
    diagnostics,
    nextActions: projectId === undefined
      ? [
          { command: 'get_session_state(projectId)', message: '重新调用时提供 projectId 参数，或在 .dataspec/config.json 配置 projectId。' },
          { command: 'get_session_bootstrap', message: '选择项目后调用 get_session_bootstrap 获取后端能力启动包。' }
        ]
      : [
          { command: 'get_session_bootstrap', message: '读取后端会话启动包，确认项目能力、版本兼容和下一步建议。' },
          { command: 'get_field_catalog', message: '需要字段标准时读取裁剪后的字段目录。' },
          { command: 'export_evidence_package', message: '交付前导出证据包，便于用户和下游 AI 复盘。' }
        ]
  }
  return sanitizeSecretValue(state)
}

async function readLocalConfigMemory(rootDir) {
  const diagnostics = []
  const configPath = path.join(rootDir, '.dataspec', 'config.json')
  try {
    const rawConfig = JSON.parse(await readFile(configPath, 'utf8'))
    return {
      status: 'PRESENT',
      projectIdPresent: rawConfig.projectId !== undefined && rawConfig.projectId !== null && rawConfig.projectId !== '',
      server: sanitizeSecretText(rawConfig.server ?? null),
      apiTokenPresent: rawConfig.apiToken !== undefined && rawConfig.apiToken !== null && rawConfig.apiToken !== '',
      defaultPathsCount: Array.isArray(rawConfig.defaultPaths) ? rawConfig.defaultPaths.length : 0,
      securityProfile: summarizeSecurityProfile(rawConfig.securityProfile),
      diagnostics
    }
  } catch (error) {
    if (error?.code === 'ENOENT' || error?.code === 'ENOTDIR') {
      diagnostics.push({
        code: 'CONFIG_MISSING',
        severity: 'INFO',
        message: '未找到 .dataspec/config.json；已仅使用 MCP 启动参数。'
      })
      return {
        status: 'MISSING',
        projectIdPresent: false,
        server: null,
        apiTokenPresent: false,
        defaultPathsCount: 0,
        securityProfile: { present: false },
        diagnostics
      }
    }
    diagnostics.push({
      code: 'CONFIG_INVALID',
      severity: 'WARN',
      message: `无法读取 .dataspec/config.json: ${sanitizeSecretText(error?.message ?? 'unknown error')}`
    })
    return {
      status: 'INVALID',
      projectIdPresent: false,
      server: null,
      apiTokenPresent: false,
      defaultPathsCount: 0,
      securityProfile: { present: false },
      diagnostics
    }
  }
}

function summarizeSecurityProfile(profile) {
  if (!profile || typeof profile !== 'object' || Array.isArray(profile)) {
    return { present: false }
  }
  return {
    present: true,
    redactionStrictness: summarizeSecurityProfilePolicy(profile.redactionStrictness),
    sensitiveFieldPolicy: summarizeSecurityProfilePolicy(profile.sensitiveFieldPolicy),
    samplePolicy: summarizeSecurityProfilePolicy(profile.samplePolicy),
    credentialPolicy: summarizeSecurityProfilePolicy(profile.credentialPolicy),
    allowedAiToolsCount: countStringArray(profile.allowedAiTools),
    neverExportPatternsCount: countStringArray(profile.neverExportPatterns),
    localOnlyPathsCount: countStringArray(profile.localOnlyPaths)
  }
}

function summarizeSecurityProfilePolicy(value) {
  return typeof value === 'string' && value.trim() ? sanitizeSecretText(value.trim()) : null
}

function countStringArray(value) {
  return Array.isArray(value) ? value.filter((item) => typeof item === 'string' && item.trim()).length : 0
}

async function readContextCacheMemory(rootDir, projectId) {
  const diagnostics = []
  const metadataPath = path.join(rootDir, '.dataspec', 'context', 'cache-metadata.json')
  try {
    const metadata = JSON.parse(await readFile(metadataPath, 'utf8'))
    const metadataProjectId = parseMetadataProjectId(metadata.projectId)
    if (projectId !== undefined && metadataProjectId !== null && metadataProjectId !== projectId) {
      diagnostics.push({
        code: 'CONTEXT_CACHE_PROJECT_MISMATCH',
        severity: 'WARN',
        message: `context cache 属于项目 ${metadataProjectId}，当前 session projectId 为 ${projectId}；已忽略该缓存快照。`
      })
      return {
        currentSnapshot: emptySessionSnapshot(),
        memory: {
          status: 'PROJECT_MISMATCH',
          projectId: metadataProjectId,
          scope: null,
          query: null,
          statusFilter: null,
          limit: null,
          server: sanitizeSecretText(metadata.server ?? null),
          contentHash: sanitizeSecretText(metadata.contentHash ?? null),
          generatedAt: sanitizeSecretText(metadata.exportedAt ?? metadata.generatedAt ?? null),
          expiresAt: sanitizeSecretText(metadata.expiresAt ?? null),
          ttlDays: metadata.ttlDays ?? null,
          standard: null,
          sourcePackageGeneratedAt: sanitizeSecretText(metadata.sourcePackage?.generatedAt ?? null)
        },
        diagnostics
      }
    }
    const exportOptions = metadata.exportOptions && typeof metadata.exportOptions === 'object'
      ? metadata.exportOptions
      : {}
    return {
      currentSnapshot: sessionSnapshotFromMetadata(metadata),
      memory: {
        status: 'PRESENT',
        projectId: metadataProjectId,
        scope: sanitizeSecretText(exportOptions.scope ?? metadata.scope ?? null),
        query: sanitizeSecretText(exportOptions.query ?? metadata.query ?? null),
        statusFilter: sanitizeSecretText(exportOptions.status ?? metadata.status ?? null),
        limit: exportOptions.limit ?? metadata.limit ?? null,
        server: sanitizeSecretText(metadata.server ?? null),
        contentHash: sanitizeSecretText(metadata.contentHash ?? null),
        generatedAt: sanitizeSecretText(metadata.exportedAt ?? metadata.generatedAt ?? null),
        expiresAt: sanitizeSecretText(metadata.expiresAt ?? null),
        ttlDays: metadata.ttlDays ?? null,
        standard: sanitizeSecretValue(metadata.standard ?? null),
        sourcePackageGeneratedAt: sanitizeSecretText(metadata.sourcePackage?.generatedAt ?? null)
      },
      diagnostics
    }
  } catch (error) {
    if (error?.code === 'ENOENT' || error?.code === 'ENOTDIR') {
      diagnostics.push({
        code: 'CONTEXT_CACHE_MISSING',
        severity: 'INFO',
        message: '未找到 .dataspec/context/cache-metadata.json；如需更准确的标准快照，请先导出或刷新 AI context。'
      })
      return {
        currentSnapshot: emptySessionSnapshot(),
        memory: {
          status: 'MISSING',
          scope: null,
          query: null,
          server: null,
          contentHash: null,
          generatedAt: null,
          standard: null
        },
        diagnostics
      }
    }
    diagnostics.push({
      code: 'CONTEXT_CACHE_INVALID',
      severity: 'WARN',
      message: `无法读取 context cache metadata: ${sanitizeSecretText(error?.message ?? 'unknown error')}`
    })
    return {
      currentSnapshot: emptySessionSnapshot(),
      memory: {
        status: 'INVALID',
        scope: null,
        query: null,
        server: null,
        contentHash: null,
        generatedAt: null,
        standard: null
      },
      diagnostics
    }
  }
}

function sessionSnapshotFromMetadata(metadata) {
  return {
    specVersion: sanitizeSecretText(metadata?.standard?.specVersion ?? null),
    specHash: sanitizeSecretText(metadata?.standard?.specHash ?? null),
    source: sanitizeSecretText(metadata?.standard?.source ?? null),
    contentHash: sanitizeSecretText(metadata?.contentHash ?? null),
    generatedAt: sanitizeSecretText(metadata?.exportedAt ?? metadata?.generatedAt ?? metadata?.sourcePackage?.generatedAt ?? null)
  }
}

function parseMetadataProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function emptySessionSnapshot() {
  return {
    specVersion: null,
    specHash: null,
    source: null,
    contentHash: null,
    generatedAt: null
  }
}

function materializeGuidanceTemplate(template, projectId) {
  return {
    ...template,
    resourceUris: template.resourceSequence.map((resourceKey) => resourceUri(projectId ?? '{projectId}', resourceKey))
  }
}

function guidancePrompt(templateId, args, extraText) {
  const template = AGENT_GUIDANCE_TEMPLATES.find((item) => item.id === templateId)
  return {
    description: template.description,
    arguments: args,
    safety: FIRST_CLASS_PROMPT_SAFETY[templateId],
    dataspecGuidance: {
      templateId,
      requiredInputs: template.requiredInputs,
      safeDefaults: template.safeDefaults,
      resourceSequence: template.resourceSequence,
      toolSequence: template.toolSequence,
      stopConditions: template.stopConditions,
      evidenceRequirements: template.evidenceRequirements,
      nextActions: template.nextActions
    },
    buildText(promptArgs, projectId) {
      const guidance = materializeGuidanceTemplate(template, projectId)
      return [
        `请按 DataSpec MCP agent guidance template \`${template.id}\` 执行。`,
        '',
        `title: ${template.title}`,
        `description: ${template.description}`,
        `requiredInputs: ${JSON.stringify(template.requiredInputs)}`,
        `safeDefaults: ${JSON.stringify(template.safeDefaults)}`,
        `resourceSequence: ${JSON.stringify(guidance.resourceUris)}`,
        `toolSequence: ${JSON.stringify(template.toolSequence)}`,
        `stopConditions: ${JSON.stringify(template.stopConditions)}`,
        `evidenceRequirements: ${JSON.stringify(template.evidenceRequirements)}`,
        `nextActions: ${JSON.stringify(template.nextActions)}`,
        '',
        '执行约束：先读取 capability catalog 和 schema registry；调用写入型或可能持久化结果的工具前必须检查 safety metadata；本 prompt 不执行工作流、不连接源数据库、不授权写入。',
        extraText(promptArgs)
      ].join('\n')
    }
  }
}

const PROMPTS = {
  create_table_with_dataspec: guidancePrompt('create_table_with_dataspec', [
    { name: 'businessDescription', description: '业务表或数据对象描述。', required: false },
    { name: 'projectId', description: '可选项目 ID，未提供时使用 MCP Server 启动项目。', required: false }
  ], (args) => args.businessDescription ? `业务描述：${args.businessDescription}` : '业务描述：请根据用户后续输入补全。'),
  review_sql_with_dataspec: guidancePrompt('review_sql_with_dataspec', [
    { name: 'sql', description: '待评审 SQL。', required: false },
    { name: 'projectId', description: '可选项目 ID，未提供时使用 MCP Server 启动项目。', required: false }
  ], (args) => args.sql ? `待评审 SQL：\n\`\`\`sql\n${args.sql}\n\`\`\`` : '待评审 SQL：请根据用户后续输入补全。'),
  reverse_import_standards: guidancePrompt('reverse_import_standards', [
    { name: 'sourceDescription', description: '源库、schema 或本次反向导入目标的脱敏描述。', required: false },
    { name: 'projectId', description: '可选项目 ID，未提供时使用 MCP Server 启动项目。', required: false }
  ], (args) => args.sourceDescription ? `源数据描述：${args.sourceDescription}` : '源数据描述：请仅提供脱敏 schema/表范围，不要提供 raw secret。'),
  answer_field_standard_question: guidancePrompt('answer_field_standard_question', [
    { name: 'question', description: '关于字段标准、命名、状态、敏感性或证据的问题。', required: false },
    { name: 'projectId', description: '可选项目 ID，未提供时使用 MCP Server 启动项目。', required: false }
  ], (args) => args.question ? `用户问题：${args.question}` : '用户问题：请根据用户后续输入补全。'),
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
  const rootDir = path.resolve(config.rootDir ?? process.cwd())

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
        rootDir,
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
  if (method === 'resources/templates/list') {
    return listResourceTemplates()
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

function listResourceTemplates() {
  return {
    resourceTemplates: RESOURCE_TEMPLATE_KEYS.map((key) => {
      const def = RESOURCE_DEFS[key]
      return {
        uriTemplate: `dataspec://project/{projectId}/${key}`,
        name: def.name,
        description: def.description,
        mimeType: def.mimeType
      }
    })
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
  const localContent = typeof def.localContent === 'function' ? await def.localContent(projectId, context) : undefined
  const text = localContent !== undefined
    ? typeof localContent === 'string'
      ? localContent
      : JSON.stringify(structuredContent = localContent, null, 2)
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
                : def.tableStandardsResource
                  ? JSON.stringify(structuredContent = await fetchTableStandardsResource(context, { projectId }), null, 2)
                  : def.jsonResource
                    ? JSON.stringify(structuredContent = await fetchProjectJsonResource(context, def.path, { projectId }), null, 2)
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
      arguments: prompt.arguments,
      ...(prompt.safety ? { safety: prompt.safety } : {}),
      ...(prompt.dataspecGuidance ? { dataspecGuidance: prompt.dataspecGuidance } : {})
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
        name: 'get_session_state',
        description: '读取 DataSpec MCP 当前项目记忆，返回只读本地会话状态、上下文缓存摘要、安全默认值和下一步建议。',
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
              description: `workflow recipe id，例如 ${supportedWorkflowRecipeIds().join('、')}。`
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
        name: 'get_field_knowledge_cards',
        description: '读取字段知识卡列表或单字段详情，聚合字段语义、枚举生命周期、命名翻译、使用示例和指标口径摘要。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            fieldId: {
              type: 'integer',
              description: '可选字段 ID。仅提供 fieldId 时默认读取详情；传 detail=false 时按列表过滤。'
            },
            detail: {
              type: 'boolean',
              description: '是否读取单字段详情；默认在仅提供 fieldId 时为 true。'
            },
            query: {
              type: 'string',
              description: '可选检索关键词。'
            },
            status: {
              type: 'string',
              description: '可选字段状态过滤。'
            },
            limit: {
              type: 'integer',
              description: '可选返回上限，默认 20。'
            }
          }
        }
      },
      {
        name: 'get_field_semantics',
        description: '读取字段语义规则列表或单条详情，说明派生、单位换算、聚合、时间粒度和 source-of-truth guidance。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '列表查询的项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            id: {
              type: 'integer',
              description: '可选语义规则 ID；提供时读取详情，不能同时传列表过滤。'
            },
            fieldId: {
              type: 'integer',
              description: '可选字段 ID 过滤。'
            },
            ruleType: {
              type: 'string',
              description: '可选语义规则类型过滤。'
            },
            query: {
              type: 'string',
              description: '可选检索关键词。'
            },
            limit: {
              type: 'integer',
              description: '可选返回上限，默认由服务端限制。'
            }
          }
        }
      },
      {
        name: 'get_metric_definitions',
        description: '读取指标口径列表或单条详情，说明 metricKey、字段引用、过滤、聚合、时间粒度和 example SQL guidance。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '列表查询的项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            id: {
              type: 'integer',
              description: '可选指标口径 ID；提供时读取详情，不能同时传列表过滤。'
            },
            query: {
              type: 'string',
              description: '可选检索关键词。'
            },
            status: {
              type: 'string',
              description: '可选状态过滤。'
            },
            fieldId: {
              type: 'integer',
              description: '可选标准字段 ID 过滤。'
            },
            metricKey: {
              type: 'string',
              description: '可选 metricKey 精确过滤。'
            },
            limit: {
              type: 'integer',
              description: '可选返回上限，默认由服务端限制。'
            }
          }
        }
      },
      {
        name: 'get_enum_lifecycle',
        description: '读取枚举字典值及生命周期 metadata，包括 status、alias、replacement、有效期和 AI mapping hints。',
        inputSchema: {
          type: 'object',
          properties: {
            enumId: {
              type: 'integer',
              description: '枚举字典 ID。'
            }
          },
          required: ['enumId']
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
            standardQuery: {
              type: 'object',
              description: '可选 Standard Query DSL JSON AST；v1 target=FIELD，query/filter 值均视为敏感输入并要求服务端脱敏摘要。',
              properties: {
                target: {
                  type: 'string',
                  enum: ['FIELD'],
                  description: '查询目标；v1 仅支持 FIELD。'
                },
                text: {
                  type: 'string',
                  description: '自然语言或字段名检索文本；视为敏感输入。'
                },
                filters: {
                  type: 'array',
                  description: 'allowlist 过滤条件；支持 category/tag/status/sensitive/sourceBatchId/stableRef/canonicalRef/hasExample/updatedSince。',
                  items: {
                    type: 'object',
                    properties: {
                      field: { type: 'string', description: '过滤字段名。' },
                      op: { type: 'string', description: '操作符，如 eq、contains、gte。' },
                      value: { description: '过滤值；视为敏感输入。' }
                    }
                  }
                },
                limit: {
                  type: 'integer',
                  description: '返回上限。'
                },
                explain: {
                  type: 'boolean',
                  description: '是否请求解释摘要。'
                },
                strict: {
                  type: 'boolean',
                  description: 'true 时不支持过滤条件会失败；false 时进入 ignoredFilters。'
                }
              }
            },
            limit: {
              type: 'integer',
              description: '可选返回数量，默认 20。'
            }
          }
        }
      },
      {
        name: 'resolve_standard_refs',
        description: 'Resolve project-scoped DataSpec standard references; this tool is read-only and does not write standards, business files, or databases.',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            refType: {
              type: 'string',
              enum: ['FIELD', 'ENUM', 'RULE', 'SNAPSHOT'],
              description: '引用对象类型：FIELD 字段、ENUM 枚举代码集、RULE 标准规则、SNAPSHOT 标准快照。'
            },
            refs: {
              type: 'array',
              description: '待解析引用列表，可包含 stableRef、当前名称、别名或历史名称；不得包含 token/password/JDBC URL/Authorization 等明文秘密。',
              items: {
                type: 'string',
                description: '单个待解析引用。'
              },
              minItems: 1
            }
          },
          required: ['refType', 'refs']
        }
      },
      {
        name: 'check_ai_output',
        description: 'Run deterministic DataSpec post-check on AI output; this tool is read-only and does not write standards, business files, or databases.',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            contentType: {
              type: 'string',
              enum: AI_OUTPUT_POST_CHECK_CONTENT_TYPES,
              description: 'AI 产物类型，用于选择确定性引用提取规则；TEXT 是稳定纯文本值，兼容旧 PLAIN_TEXT 输入并按 TEXT 发送。'
            },
            content: {
              type: 'string',
              maxLength: 20000,
              description: '待校验 AI 产物正文；工具只读传给后端 post-check，返回前会保留结构化结果并依赖后端/本地脱敏保护秘密。'
            },
            snapshotRef: {
              type: 'string',
              description: '可选标准快照 stableRef，用于判断输出是否引用旧快照或当前标准。'
            },
            findings: {
              type: 'array',
              maxItems: REVIEW_FINDINGS_MAX_ITEMS,
              description: '可选结构化 findings；MCP 在请求前校验共享字段和安全边界，服务端再做 project-scoped evidence gating。',
              items: REVIEW_FINDING_INPUT_SCHEMA
            }
          },
          required: ['contentType', 'content']
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
        name: 'get_table_standards',
        description: '读取项目表结构标准上下文；只返回业务对象、模板、关系、安全 metadata 和 nextActions，不生成 DDL、不连接数据库、不写项目状态。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            templateId: {
              type: 'integer',
              description: '可选表模板 ID，用于裁剪 table standards。'
            },
            businessObject: {
              type: 'string',
              description: '可选业务对象 key，用于裁剪 table standards。'
            }
          }
        }
      },
      {
        name: 'generate_test_data_package',
        description: '生成标准驱动测试数据包；只读调用 DataSpec 后端 API，不写项目标准、业务仓库或源数据库，不调用外部 LLM。',
        inputSchema: {
          type: 'object',
          properties: {
            projectId: {
              type: 'integer',
              description: '可选项目 ID，未提供时使用 MCP Server 启动项目。'
            },
            fieldNames: {
              type: 'array',
              description: '可选字段名筛选；为空时服务端按项目标准和 maxFields 选择。不得包含 token/password/JDBC URL/Authorization 明文。',
              items: {
                type: 'string',
                description: '标准字段名。'
              }
            },
            objectScenario: {
              type: 'string',
              description: '可选轻量对象场景，如 order、user、audit；仅影响 fallback 命名和 seed 草稿表名。'
            },
            maxFields: {
              type: 'integer',
              description: `最大字段数，MCP 本地安全上限为 ${TEST_DATA_MAX_FIELDS}。`
            },
            casesPerField: {
              type: 'integer',
              description: `每字段最多用例数，MCP 本地安全上限为 ${TEST_DATA_MAX_CASES_PER_FIELD}。`
            },
            seedRowCount: {
              type: 'integer',
              description: `mock/CSV/SQL seed 草稿行数，MCP 本地安全上限为 ${TEST_DATA_MAX_SEED_ROWS}。`
            },
            dialect: {
              type: 'string',
              description: 'SQL seed 草稿方言提示；仅作为说明，不代表可直接执行。'
            }
          }
        }
      },
      {
        name: 'check_consumer_compatibility',
        description: '本地只读运行 DataSpec consumer compatibility suite；不要求 DataSpec server，不调用外部网络或 LLM。',
        inputSchema: {
          type: 'object',
          properties: {
            fixturePath: {
              type: 'string',
              description: '可选本地 compatibility suite fixture 路径；默认使用 tools/fixtures/consumer-compatibility-suite.json。'
            }
          }
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
            },
            postCheckSummary: {
              type: 'object',
              description: '可选 AI output post-check 摘要；携带外部 findings 时必须声明 status=PASS 且 safeToUse=true，不得包含 raw AI output。'
            },
            postCheckReceipt: {
              type: 'string',
              maxLength: 4096,
              description: '可选 post-check 进程内 HMAC receipt；携带外部 findings 时必须匹配当前项目和完整规范化 findings，服务重启后失效。'
            },
            findings: {
              type: 'array',
              maxItems: REVIEW_FINDINGS_MAX_ITEMS,
              description: '可选已通过 post-check 的结构化 findings；MCP 先校验共享字段，后端再复验 project-scoped evidence refs。',
              items: REVIEW_FINDING_INPUT_SCHEMA
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
  if (name === 'get_session_state') {
    return await callGetSessionState(args, context)
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
  if (name === 'get_field_knowledge_cards') {
    return await callGetFieldKnowledgeCards(args, context)
  }
  if (name === 'get_field_semantics') {
    return await callGetFieldSemantics(args, context)
  }
  if (name === 'get_metric_definitions') {
    return await callGetMetricDefinitions(args, context)
  }
  if (name === 'get_enum_lifecycle') {
    return await callGetEnumLifecycle(args, context)
  }
  if (name === 'search_field_catalog') {
    return await callSearchFieldCatalog(args, context)
  }
  if (name === 'search_fields') {
    return await callSearchFields(args, context)
  }
  if (name === 'resolve_standard_refs') {
    return await callResolveStandardRefs(args, context)
  }
  if (name === 'check_ai_output') {
    return await callCheckAiOutput(args, context)
  }
  if (name === 'suggest_fields') {
    return await callSuggestFields(args, context)
  }
  if (name === 'get_table_standards') {
    return await callGetTableStandards(args, context)
  }
  if (name === 'generate_test_data_package') {
    return await callGenerateTestDataPackage(args, context)
  }
  if (name === 'check_consumer_compatibility') {
    return await callCheckConsumerCompatibility(args)
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

async function callGetSessionState(args, context) {
  const projectId = optionalSessionProjectId(args.projectId, context.defaultProjectId)
  return toolJsonResult(await buildMcpSessionState(projectId, context))
}

function optionalSessionProjectId(value, fallback) {
  if (value !== undefined && value !== null && value !== '') {
    return parseProjectId(value)
  }
  return fallback
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

async function callGetFieldKnowledgeCards(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const fieldId = optionalPositiveInteger(args.fieldId, 'fieldId')
  const hasListFilters = args.query !== undefined || args.status !== undefined || args.limit !== undefined
  const detail = args.detail === true || (fieldId !== undefined && !hasListFilters && args.detail !== false)
  if (detail && hasListFilters) {
    throw new JsonRpcError(-32602, 'get_field_knowledge_cards 读取详情时不要同时传 query、status 或 limit；如需按 fieldId 过滤列表，请传 detail=false')
  }
  const result = detail
    ? await fetchFieldKnowledgeCardResource(context, { projectId, fieldId })
    : await fetchFieldKnowledgeCardsResource(context, {
        projectId,
        fieldId,
        query: normalizeOptionalText(args.query),
        status: normalizeOptionalText(args.status),
        limit: args.limit === undefined || args.limit === null || args.limit === ''
          ? 20
          : optionalLimit(args.limit, 20)
      })
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callGetFieldSemantics(args, context) {
  const id = optionalPositiveInteger(args.id, 'id')
  if (id !== undefined) {
    assertNoDetailFilterMix('get_field_semantics', args, ['fieldId', 'ruleType', 'query', 'limit'])
    return toolJsonResult(sanitizeSecretValue(await fetchFieldSemanticRuleResource(context, id)))
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const result = await fetchFieldSemanticsResource(context, {
    projectId,
    fieldId: optionalPositiveInteger(args.fieldId, 'fieldId'),
    ruleType: normalizeOptionalText(args.ruleType),
    query: normalizeOptionalText(args.query),
    limit: optionalLimit(args.limit, undefined)
  })
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callGetMetricDefinitions(args, context) {
  const id = optionalPositiveInteger(args.id, 'id')
  if (id !== undefined) {
    assertNoDetailFilterMix('get_metric_definitions', args, ['query', 'status', 'fieldId', 'metricKey', 'limit'])
    return toolJsonResult(sanitizeSecretValue(await fetchMetricDefinitionResource(context, id)))
  }
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const result = await fetchMetricDefinitionsResource(context, {
    projectId,
    query: normalizeOptionalText(args.query),
    status: normalizeOptionalText(args.status),
    fieldId: optionalPositiveInteger(args.fieldId, 'fieldId'),
    metricKey: normalizeOptionalText(args.metricKey),
    limit: optionalLimit(args.limit, undefined)
  })
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callGetEnumLifecycle(args, context) {
  const enumId = parsePositiveInteger(args.enumId, 'enumId')
  const response = await context.fetchFn(`${context.server}/api/enums/${encodeURIComponent(enumId)}/values`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(sanitizeSecretValue({
    kind: 'dataspec-enum-lifecycle',
    schemaVersion: 1,
    enumId,
    values: Array.isArray(result) ? result : result?.values ?? result
  }))
}

function assertNoDetailFilterMix(toolName, args, filterNames) {
  const usedFilters = filterNames.filter((name) => args[name] !== undefined && args[name] !== null && args[name] !== '')
  if (usedFilters.length > 0) {
    throw new JsonRpcError(-32602, `${toolName} 使用 id 读取详情时不要同时传列表过滤: ${usedFilters.join(', ')}`)
  }
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
  if (args.standardQuery !== undefined && args.standardQuery !== null) {
    const response = await context.fetchFn(`${context.server}/api/standard-query/search`, {
      method: 'POST',
      headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ projectId, ...args.standardQuery })
    })
    const result = await readDataSpecJson(response)
    return toolJsonResult(sanitizeSecretValue(result))
  }
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
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callResolveStandardRefs(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const refType = stringArg(args.refType, 'resolve_standard_refs 需要 refType')
  const refs = Array.isArray(args.refs)
    ? args.refs.map((item) => String(item).trim()).filter(Boolean)
    : []
  if (refs.length === 0) {
    throw new JsonRpcError(-32602, 'resolve_standard_refs 需要至少一个 refs[] 引用')
  }
  const response = await context.fetchFn(`${context.server}/api/standard-references/resolve`, {
    method: 'POST',
    headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ projectId, refType, refs })
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callCheckAiOutput(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const contentType = normalizeAiOutputPostCheckContentType(stringArg(args.contentType, 'check_ai_output 需要 contentType'))
  const content = stringArg(args.content, 'check_ai_output 需要非空 content')
  const findings = optionalReviewFindings(args.findings)
  const body = {
    projectId,
    contentType,
    content,
    ...(findings === undefined ? {} : { findings })
  }
  if (args.snapshotRef !== undefined && args.snapshotRef !== null && String(args.snapshotRef).trim() !== '') {
    body.snapshotRef = String(args.snapshotRef).trim()
  }
  const response = await context.fetchFn(`${context.server}/api/ai-output/check`, {
    method: 'POST',
    headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(sanitizeSecretValue(result))
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

async function callGetTableStandards(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const templateId = args.templateId === undefined || args.templateId === null || args.templateId === ''
    ? undefined
    : parsePositiveInteger(args.templateId, 'templateId')
  const businessObject = normalizeOptionalText(args.businessObject)
  if (templateId !== undefined && businessObject !== undefined) {
    throw new JsonRpcError(-32602, 'get_table_standards 需要在 templateId 与 businessObject 间二选一')
  }
  const result = await fetchTableStandardsResource(context, { projectId, templateId, businessObject })
  return toolJsonResult(result)
}

async function callGenerateTestDataPackage(args, context) {
  const projectId = optionalProjectId(args.projectId, context.defaultProjectId)
  const req = removeUndefinedValues({
    projectId,
    fieldNames: optionalStringArray(args.fieldNames, 'fieldNames', TEST_DATA_MAX_FIELDS),
    objectScenario: normalizeOptionalText(args.objectScenario),
    maxFields: optionalBoundedPositiveInteger(args.maxFields, 'maxFields', TEST_DATA_MAX_FIELDS),
    casesPerField: optionalBoundedPositiveInteger(args.casesPerField, 'casesPerField', TEST_DATA_MAX_CASES_PER_FIELD),
    seedRowCount: optionalBoundedPositiveInteger(args.seedRowCount, 'seedRowCount', TEST_DATA_MAX_SEED_ROWS),
    dialect: normalizeOptionalText(args.dialect)
  })
  const response = await context.fetchFn(`${context.server}/api/test-data/package/generate`, {
    method: 'POST',
    headers: dataSpecHeaders(context.apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(req)
  })
  const result = await readDataSpecJson(response)
  return toolJsonResult(sanitizeSecretValue(result))
}

async function callCheckConsumerCompatibility(args) {
  const fixturePath = normalizeOptionalText(args.fixturePath)
  const result = await validateConsumerCompatibilitySuite(
    fixturePath === undefined ? {} : { fixturePath }
  )
  return toolJsonResult(sanitizeSecretValue(result))
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
  return toolJsonResult(sanitizeSecretValue(result))
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
  appendOptionalObject(req, 'postCheckSummary', args.postCheckSummary)
  appendOptionalBoundedTextProperty(req, 'postCheckReceipt', args.postCheckReceipt, 4096)
  const findings = optionalReviewFindings(args.findings)
  if (findings !== undefined) {
    req.findings = findings
  }
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

function appendOptionalBoundedTextProperty(target, key, value, maxCodePoints) {
  const normalized = normalizeOptionalText(value)
  if (!normalized) {
    return
  }
  if ([...normalized].length > maxCodePoints) {
    throw new JsonRpcError(-32602, `${key} 不能超过 ${maxCodePoints} 个 Unicode code point`)
  }
  target[key] = normalized
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

async function fetchTableStandardsResource(context, { projectId, templateId, businessObject }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'templateId', templateId)
  appendOptionalParam(params, 'businessObject', businessObject)
  const response = await context.fetchFn(`${context.server}/api/table-standards?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchProjectJsonResource(context, path, { projectId }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  const response = await context.fetchFn(`${context.server}${path}?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchFieldKnowledgeCardsResource(context, { projectId, fieldId, query, status, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'status', status)
  appendOptionalParam(params, 'limit', limit)
  const response = await context.fetchFn(`${context.server}/api/field-knowledge-cards?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchFieldKnowledgeCardResource(context, { projectId, fieldId }) {
  if (fieldId === undefined) {
    throw new JsonRpcError(-32602, 'get_field_knowledge_cards 读取详情需要 fieldId')
  }
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  const response = await context.fetchFn(`${context.server}/api/field-knowledge-cards/${encodeURIComponent(fieldId)}?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchFieldSemanticsResource(context, { projectId, fieldId, ruleType, query, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'ruleType', ruleType)
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'limit', limit)
  const response = await context.fetchFn(`${context.server}/api/field-semantics?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchFieldSemanticRuleResource(context, id) {
  const response = await context.fetchFn(`${context.server}/api/field-semantics/${encodeURIComponent(id)}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchMetricDefinitionsResource(context, { projectId, query, status, fieldId, metricKey, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'status', status)
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'metricKey', metricKey)
  appendOptionalParam(params, 'limit', limit)
  const response = await context.fetchFn(`${context.server}/api/metric-definitions?${params.toString()}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
  return await readDataSpecJson(response)
}

async function fetchMetricDefinitionResource(context, id) {
  const response = await context.fetchFn(`${context.server}/api/metric-definitions/${encodeURIComponent(id)}`, {
    headers: dataSpecHeaders(context.apiToken)
  })
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

function optionalPositiveInteger(value, label) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return parsePositiveInteger(value, label)
}

function optionalBoundedPositiveInteger(value, label, max) {
  const parsed = optionalPositiveInteger(value, label)
  if (parsed === undefined) {
    return undefined
  }
  if (parsed > max) {
    throw new JsonRpcError(-32602, `${label} 超过安全上限 ${max}: ${value}`)
  }
  return parsed
}

function optionalStringArray(value, label, maxItems) {
  if (value === undefined || value === null) {
    return undefined
  }
  if (!Array.isArray(value)) {
    throw new JsonRpcError(-32602, `${label} 必须是字符串数组`)
  }
  if (value.length > maxItems) {
    throw new JsonRpcError(-32602, `${label} 超过安全上限 ${maxItems}`)
  }
  const result = value.map((item) => String(item).trim()).filter(Boolean)
  return result.length > 0 ? result : undefined
}

/** 校验 MCP 结构化 findings，确保越界或畸形输入不会到达后端。 */
function optionalReviewFindings(value) {
  if (value === undefined || value === null) return undefined
  if (!Array.isArray(value)) {
    throw new JsonRpcError(-32602, 'findings 必须是数组')
  }
  if (value.length > REVIEW_FINDINGS_MAX_ITEMS) {
    throw new JsonRpcError(-32602, `findings 超过安全上限 ${REVIEW_FINDINGS_MAX_ITEMS}`)
  }
  value.forEach((finding, index) => validateReviewFinding(finding, index))
  return value
}

function validateReviewFinding(finding, index) {
  const prefix = `findings[${index}]`
  if (!isPlainObject(finding)) throw new JsonRpcError(-32602, `${prefix} 必须是 object`)
  rejectUnknownReviewFindingFields(finding, prefix, REVIEW_FINDING_FIELDS)
  optionalIntegerInRange(finding.schemaVersion, `${prefix}.schemaVersion`, 1, Number.MAX_SAFE_INTEGER)
  requireBoundedText(finding.code, `${prefix}.code`, 128)
  optionalEnum(finding.source, `${prefix}.source`, ['SQL_LINT', 'AI_OUTPUT_POSTCHECK', 'EXTERNAL_AI'])
  optionalBoundedText(finding.findingKey, `${prefix}.findingKey`, 128)
  optionalEnum(finding.severity, `${prefix}.severity`, ['ERROR', 'WARNING', 'SUGGESTION', 'INFO'])
  for (const field of ['trigger', 'expected', 'observed', 'suggestedFix']) {
    optionalBoundedText(finding[field], `${prefix}.${field}`, 1000)
  }
  optionalIntegerInRange(finding.confidence, `${prefix}.confidence`, 0, 100)
  if (finding.autoFixSafe !== undefined && finding.autoFixSafe !== null && typeof finding.autoFixSafe !== 'boolean') {
    throw new JsonRpcError(-32602, `${prefix}.autoFixSafe 必须是 boolean`)
  }
  validateReviewFindingSubject(finding.subject, `${prefix}.subject`)
  validateReviewFindingLocation(finding.location, `${prefix}.location`)
  validateReviewFindingWaiver(finding.waiver, `${prefix}.waiver`)
  if (finding.evidenceRefs !== undefined && finding.evidenceRefs !== null) {
    if (!Array.isArray(finding.evidenceRefs)) {
      throw new JsonRpcError(-32602, `${prefix}.evidenceRefs 必须是数组`)
    }
    if (finding.evidenceRefs.length > REVIEW_FINDING_MAX_EVIDENCE_REFS) {
      throw new JsonRpcError(-32602, `${prefix}.evidenceRefs 超过安全上限 ${REVIEW_FINDING_MAX_EVIDENCE_REFS}`)
    }
    finding.evidenceRefs.forEach((ref, refIndex) =>
      requireBoundedText(ref, `${prefix}.evidenceRefs[${refIndex}]`, 500))
  }
}

function validateReviewFindingSubject(subject, label) {
  if (subject === undefined || subject === null) return
  if (!isPlainObject(subject)) throw new JsonRpcError(-32602, `${label} 必须是 object`)
  rejectUnknownReviewFindingFields(subject, label, REVIEW_FINDING_SUBJECT_FIELDS)
  optionalIntegerInRange(subject.projectId, `${label}.projectId`, 1, Number.MAX_SAFE_INTEGER)
  optionalBoundedText(subject.kind, `${label}.kind`, 64)
  for (const field of ['name', 'tableName', 'columnName', 'stableRef']) {
    optionalBoundedText(subject[field], `${label}.${field}`, 256)
  }
}

function validateReviewFindingLocation(location, label) {
  if (location === undefined || location === null) return
  if (!isPlainObject(location)) throw new JsonRpcError(-32602, `${label} 必须是 object`)
  rejectUnknownReviewFindingFields(location, label, REVIEW_FINDING_LOCATION_FIELDS)
  optionalBoundedText(location.path, `${label}.path`, 512)
  optionalBoundedText(location.locationKind, `${label}.locationKind`, 64)
  for (const field of ['line', 'column', 'lineEnd', 'columnEnd']) {
    optionalIntegerInRange(location[field], `${label}.${field}`, 1, Number.MAX_SAFE_INTEGER)
  }
  for (const field of ['sourceStart', 'sourceEnd']) {
    optionalIntegerInRange(location[field], `${label}.${field}`, 0, Number.MAX_SAFE_INTEGER)
  }
}

function validateReviewFindingWaiver(waiver, label) {
  if (waiver === undefined || waiver === null) return
  if (!isPlainObject(waiver)) throw new JsonRpcError(-32602, `${label} 必须是 object`)
  rejectUnknownReviewFindingFields(waiver, label, REVIEW_FINDING_WAIVER_FIELDS)
  if (waiver.waived !== undefined && waiver.waived !== null && typeof waiver.waived !== 'boolean') {
    throw new JsonRpcError(-32602, `${label}.waived 必须是 boolean`)
  }
  optionalIntegerInRange(waiver.waiverId, `${label}.waiverId`, 1, Number.MAX_SAFE_INTEGER)
  optionalBoundedText(waiver.reason, `${label}.reason`, 500)
}

function rejectUnknownReviewFindingFields(value, label, allowedFields) {
  const unknown = Object.keys(value).filter((field) => !allowedFields.has(field))
  if (unknown.length > 0) {
    throw new JsonRpcError(-32602, `${label} 包含不支持字段: ${unknown.join(', ')}`)
  }
}

function optionalIntegerInRange(value, label, min, max) {
  if (value === undefined || value === null) return
  if (!Number.isSafeInteger(value) || value < min || value > max) {
    throw new JsonRpcError(-32602, `${label} 必须是 ${min}-${max} 的整数`)
  }
}

function optionalEnum(value, label, allowed) {
  if (value === undefined || value === null) return
  if (typeof value !== 'string' || !allowed.includes(value)) {
    throw new JsonRpcError(-32602, `${label} 必须是 ${allowed.join('|')}`)
  }
}

function requireBoundedText(value, label, maxCodePoints) {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new JsonRpcError(-32602, `${label} 必须是非空字符串`)
  }
  if ([...value].length > maxCodePoints) {
    throw new JsonRpcError(-32602, `${label} 超过安全上限 ${maxCodePoints}`)
  }
}

function optionalBoundedText(value, label, maxCodePoints) {
  if (value === undefined || value === null) return
  requireBoundedText(value, label, maxCodePoints)
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function removeUndefinedValues(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined))
}

function stringArg(value, message) {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new JsonRpcError(-32602, message)
  }
  return value
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

function normalizeAiOutputPostCheckContentType(value) {
  const normalized = String(value ?? '').trim().toUpperCase()
  return normalized === 'PLAIN_TEXT' ? 'TEXT' : normalized
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
    if (!isOptionValue(value)) {
      throw new Error(`缺少参数值: ${token}`)
    }
    options[name] = value
    i += 1
  }
  return { options }
}

function isOptionValue(value) {
  return typeof value === 'string' && value.length > 0 && !value.startsWith('-')
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

const QUERY_TOKEN_EVIDENCE_KINDS = new Set(['WORD', 'ACRONYM', 'NUMBER', 'UNIT', 'HAN'])
const QUERY_TOKEN_EVIDENCE_STATUSES = new Set(['RESOLVED', 'AMBIGUOUS', 'DISABLED', 'UNRESOLVED'])

function sanitizeSecretValue(value) {
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeSecretValue(item))
  }
  if (value && typeof value === 'object') {
    const queryTokenEvidence = isQueryTokenEvidence(value)
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key,
      isSensitiveTaskCardKey(key) && !(queryTokenEvidence && isQueryTokenEvidenceTextKey(key))
        ? '***'
        : sanitizeSecretValue(item)
    ]))
  }
  return value
}

function isQueryTokenEvidence(value) {
  return typeof value.token === 'string' &&
    typeof value.normalizedToken === 'string' &&
    QUERY_TOKEN_EVIDENCE_KINDS.has(value.tokenKind) &&
    QUERY_TOKEN_EVIDENCE_STATUSES.has(value.resolutionStatus)
}

function isQueryTokenEvidenceTextKey(key) {
  // token 在该契约中是命名证据而非凭据；只保留字段结构，字段值仍递归脱敏。
  return key === 'token' || key === 'normalizedToken'
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
  }
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/?#@]+@/gi, '$1')
    .replace(/jdbc:[^\s"',;}&]+/gi, 'jdbc:[REDACTED]')
    .replace(/\b((?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/)[^\s"',;}&]+/gi, '$1[REDACTED]')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1[REDACTED]')
    .replace(/(authorization\s*[:=]\s*)(?!\s*['"]?bearer\s+)(['"]?)[^,;}&\r\n]+\2/gi, '$1$2[REDACTED]$2')
    .replace(/(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1[REDACTED]')
    .replace(/((?:["'])(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)(?:["'])\s*[:=]\s*)(["'])[^"']*\2/gi, '$1$2[REDACTED]$2')
    .replace(/((?:["'])(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)(?:["'])\s*[:=]\s*)[^\s"',;}&]+/gi, '$1[REDACTED]')
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
