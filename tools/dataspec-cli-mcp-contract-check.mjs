#!/usr/bin/env node

import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createMcpHandler } from './dataspec-mcp.mjs'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const TOOLS_DIR = path.dirname(SCRIPT_PATH)
export const DEFAULT_FIXTURE_PATH = path.join(TOOLS_DIR, 'fixtures', 'cli-mcp-contracts.json')

const CHECK_KIND = 'dataspec.cli-mcp-contract-fixtures.check'
const CHECK_SCHEMA_VERSION = 1
const FIXTURE_KIND = 'dataspec-cli-mcp-contract-fixtures'

const REQUIRED_CLI_COMMANDS = [
  'doctor',
  'compat-check',
  'capability-list',
  'capability-show',
  'capability-check',
  'contract-list',
  'contract-show',
  'contract-check',
  'lint',
  'fixed-sql-patch',
  'index-refs',
  'code-patch-plan',
  'install-hook',
  'export-context',
  'context-budget-plan',
  'context-quality-check',
  'search-fields',
  'ref-resolve',
  'ai-output-check',
  'review-pr',
  'generate-ddl',
  'field-knowledge-list',
  'field-knowledge-show',
  'field-semantics-list',
  'field-semantics-show',
  'metric-definitions-list',
  'metric-definitions-show',
  'synthetic-examples-generate',
  'test-data-generate',
  'consumer-compat-check',
  'contract-import-preview',
  'schema-plan',
  'comment-plan-preview'
]

const REQUIRED_MCP_TOOLS = [
  'get_session_bootstrap',
  'get_session_state',
  'lint_sql',
  'get_field_catalog',
  'get_field_knowledge_cards',
  'get_field_semantics',
  'get_metric_definitions',
  'get_enum_lifecycle',
  'search_field_catalog',
  'search_fields',
  'resolve_standard_refs',
  'check_ai_output',
  'suggest_fields',
  'generate_table_ddl',
  'generate_test_data_package',
  'check_consumer_compatibility',
  'get_ai_task_run',
  'export_evidence_package'
]

const REQUIRED_MCP_RESOURCES = [
  'dataspec://version-compatibility',
  'capability-catalog',
  'session-bootstrap',
  'session-state',
  'field-catalog',
  'field-knowledge-cards',
  'field-semantics',
  'metric-definitions',
  'workflow-recipes',
  'agent-guidance-pack',
  'ai-task-profiles',
  'consumer-compatibility-suite',
  'schema-registry'
]

const REQUIRED_MCP_RESOURCE_TEMPLATES = [
  'dataspec://project/{projectId}/session-bootstrap',
  'dataspec://project/{projectId}/session-state',
  'dataspec://project/{projectId}/capability-catalog',
  'dataspec://project/{projectId}/schema-registry',
  'dataspec://project/{projectId}/field-catalog',
  'dataspec://project/{projectId}/field-knowledge-cards',
  'dataspec://project/{projectId}/field-semantics',
  'dataspec://project/{projectId}/metric-definitions',
  'dataspec://project/{projectId}/workflow-recipes',
  'dataspec://project/{projectId}/ai-task-profiles',
  'dataspec://project/{projectId}/consumer-compatibility-suite',
  'dataspec://project/{projectId}/agent-guidance-pack'
]

const REQUIRED_FIRST_CLASS_MCP_PROMPTS = [
  'create_table_with_dataspec',
  'review_sql_with_dataspec',
  'reverse_import_standards',
  'answer_field_standard_question'
]

const REQUIRED_MCP_PROMPTS = [
  ...REQUIRED_FIRST_CLASS_MCP_PROMPTS,
  'dataspec_create_table',
  'dataspec_review_sql',
  'dataspec_design_fields'
]

const REQUIRED_SAFETY_FIELDS = [
  'readOnly',
  'writesProject',
  'requiresDryRun',
  'requiresIdempotencyKey',
  'sensitiveInputs',
  'nextActions'
]

const TEST_DATA_PACKAGE_SAFETY_REQUIREMENTS = {
  readOnly: true,
  writesProject: false,
  writesBusinessRepo: false,
  containsRealBusinessRows: false,
  externalNetworkUsed: false,
  externalLlmUsed: false
}

const CONSUMER_COMPAT_SAFETY_REQUIREMENTS = {
  readOnly: true,
  writesProject: false,
  requiresServer: false,
  externalNetworkUsed: false,
  externalLlmUsed: false
}

const STANDARD_QUERY_DSL_FILTERS = [
  'category',
  'tag',
  'status',
  'sensitive',
  'sourceBatchId',
  'stableRef',
  'canonicalRef',
  'hasExample',
  'updatedSince'
]

const REVIEW_FINDING_FIELDS = [
  'schemaVersion',
  'source',
  'findingKey',
  'code',
  'severity',
  'subject',
  'location',
  'trigger',
  'expected',
  'observed',
  'evidenceRefs',
  'confidence',
  'suggestedFix',
  'autoFixSafe',
  'waiver'
]
const REVIEW_FINDING_REQUIRED_FIELDS = ['code']
const REVIEW_FINDING_SUBJECT_FIELDS = ['projectId', 'kind', 'name', 'tableName', 'columnName', 'stableRef']
const REVIEW_FINDING_LOCATION_FIELDS = [
  'path', 'line', 'column', 'lineEnd', 'columnEnd', 'sourceStart', 'sourceEnd', 'locationKind'
]
const REVIEW_FINDING_WAIVER_FIELDS = ['waived', 'waiverId', 'reason']
const REVIEW_FINDING_TEXT_BOUNDS = {
  findingKey: 128,
  code: 128,
  trigger: 1000,
  expected: 1000,
  observed: 1000,
  suggestedFix: 1000,
  evidenceRef: 500,
  'subject.kind': 64,
  'subject.name': 256,
  'subject.tableName': 256,
  'subject.columnName': 256,
  'subject.stableRef': 256,
  'location.path': 512,
  'location.locationKind': 64,
  'waiver.reason': 500
}

const FINDING_EVIDENCE_ISSUE_CODES = [
  'MISSING_FINDING_EVIDENCE_REFERENCE',
  'CROSS_PROJECT_FINDING_EVIDENCE_REFERENCE',
  'UNVERIFIABLE_FINDING_EVIDENCE_REFERENCE'
]

const REVIEW_DELIVERY_OUTPUT_FIELDS = [
  'kind',
  'schemaVersion',
  'commitSha',
  'reviewCommentUrl',
  'inlineCommentUrls[]',
  'findings[]',
  'sqlCheckRecordIds[]',
  'postCheck',
  'evidencePackages[]',
  'reviewCommentAction',
  'summary',
  'inline',
  'files[]'
]

/**
 * Load the CLI/MCP contract fixture JSON from disk.
 *
 * @param {string} fixturePath absolute or relative fixture path.
 * @returns {Promise<object>} parsed fixture object.
 */
export async function loadContractFixtures(fixturePath = DEFAULT_FIXTURE_PATH) {
  return JSON.parse(await readFile(fixturePath, 'utf8'))
}

/**
 * Validate contract fixtures against local structural rules and MCP descriptors.
 *
 * @param {{fixturePath?: string, fixture?: object, mcpDescriptors?: object}} options validation options.
 * @returns {Promise<object>} machine-readable validation result with diagnostics.
 */
export async function validateContractFixtures(options = {}) {
  const fixturePath = options.fixturePath ?? DEFAULT_FIXTURE_PATH
  const fixture = options.fixture ?? await loadContractFixtures(fixturePath)
  const diagnostics = []

  validateRoot(fixture, diagnostics)
  validateCliCommands(fixture.cliCommands, diagnostics)
  await validateMcpContracts(fixture, diagnostics, options.mcpDescriptors)
  scanSecrets(fixture, diagnostics)

  return {
    kind: CHECK_KIND,
    schemaVersion: CHECK_SCHEMA_VERSION,
    ok: diagnostics.length === 0,
    fixtureKind: fixture?.kind ?? null,
    fixturePath: options.fixture ? null : fixturePath,
    summary: {
      cliCommands: Array.isArray(fixture?.cliCommands) ? fixture.cliCommands.length : 0,
      mcpTools: Array.isArray(fixture?.mcpTools) ? fixture.mcpTools.length : 0,
      mcpResources: Array.isArray(fixture?.mcpResources) ? fixture.mcpResources.length : 0,
      mcpResourceTemplates: Array.isArray(fixture?.mcpResourceTemplates) ? fixture.mcpResourceTemplates.length : 0,
      mcpPrompts: Array.isArray(fixture?.mcpPrompts) ? fixture.mcpPrompts.length : 0,
      diagnostics: diagnostics.length
    },
    diagnostics,
    nextActions: diagnostics.length === 0
      ? ['CLI/MCP contract fixtures 可用于本地验收。']
      : ['根据 diagnostics[].path 更新 fixture、MCP descriptor 或测试后重试。']
  }
}

/**
 * Run the fixture checker as a small local CLI.
 *
 * @param {string[]} argv command-line arguments.
 * @param {{writeOut: Function, writeErr: Function}} io output adapter.
 * @returns {Promise<number>} process-style exit code.
 */
export async function runContractCheckCli(argv = process.argv.slice(2), io = processIo()) {
  try {
    const options = parseArgs(argv)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }
    const result = await validateContractFixtures({ fixturePath: options.fixturePath })
    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    } else {
      io.writeOut(formatText(result))
    }
    return result.ok ? 0 : 1
  } catch (error) {
    const diagnostic = {
      kind: CHECK_KIND,
      schemaVersion: CHECK_SCHEMA_VERSION,
      ok: false,
      diagnostics: [diagnosticOf('CHECK_FAILED', 'cli', error?.message ?? '契约 fixture 检查失败')]
    }
    io.writeErr(`${JSON.stringify(diagnostic, null, 2)}\n`)
    return 2
  }
}

function validateRoot(fixture, diagnostics) {
  if (!fixture || typeof fixture !== 'object' || Array.isArray(fixture)) {
    diagnostics.push(diagnosticOf('INVALID_FIXTURE', '$', 'fixture 必须是 JSON object。'))
    return
  }
  if (fixture.kind !== FIXTURE_KIND) {
    diagnostics.push(diagnosticOf('INVALID_KIND', 'kind', `kind 必须是 ${FIXTURE_KIND}。`))
  }
  if (typeof fixture.schemaVersion !== 'number') {
    diagnostics.push(diagnosticOf('INVALID_SCHEMA_VERSION', 'schemaVersion', 'schemaVersion 必须是 number。'))
  }
  for (const key of ['cliCommands', 'mcpTools', 'mcpResources', 'mcpResourceTemplates', 'mcpPrompts']) {
    if (!Array.isArray(fixture[key])) {
      diagnostics.push(diagnosticOf('INVALID_COLLECTION', key, `${key} 必须是数组。`))
    }
  }
}

function validateCliCommands(commands, diagnostics) {
  if (!Array.isArray(commands)) {
    return
  }
  const byId = mapBy(commands, 'id')
  for (const id of REQUIRED_CLI_COMMANDS) {
    if (!byId.has(id)) {
      diagnostics.push(diagnosticOf('MISSING_REQUIRED_CLI_COMMAND', `cliCommands.${id}`, `缺少 CLI command fixture: ${id}`))
    }
  }
  commands.forEach((command, index) => {
    const basePath = `cliCommands[${index}]`
    requireString(command.id, `${basePath}.id`, diagnostics)
    requireString(command.command, `${basePath}.command`, diagnostics)
    requireString(command.description, `${basePath}.description`, diagnostics)
    requireArray(command.requiredOptions, `${basePath}.requiredOptions`, diagnostics)
    if (command.oneOfRequiredOptions !== undefined) {
      requireNonEmptyArray(command.oneOfRequiredOptions, `${basePath}.oneOfRequiredOptions`, diagnostics)
    }
    requireArray(command.optionalOptions, `${basePath}.optionalOptions`, diagnostics)
    requireNonEmptyArray(command.outputShape, `${basePath}.outputShape`, diagnostics)
    requireNonEmptyArray(command.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)
    if (!command.exitCodes || typeof command.exitCodes !== 'object' || Array.isArray(command.exitCodes)) {
      diagnostics.push(diagnosticOf('INVALID_EXIT_CODES', `${basePath}.exitCodes`, 'exitCodes 必须是 object。'))
    }
    validateSafety(command.safety, `${basePath}.safety`, diagnostics)
    validateSpecialSafetyContract(command.id, command.safety, `${basePath}.safety`, diagnostics)
    requireObject(command.successExample, `${basePath}.successExample`, diagnostics)
    requireObject(command.failureExample, `${basePath}.failureExample`, diagnostics)
    if (command.id === 'search-fields') {
      validateStandardQueryDslFixture(command, basePath, diagnostics, {
        requiredInputs: ['query', 'dsl', 'dsl-file', 'stdin'],
        requiredOutputShape: ['normalizedQuery', 'normalizedQuery.queryTokens[]', 'normalizedQuery.queryTokens[].resolutionStatus', 'querySummary', 'appliedFilters[]', 'ignoredFilters[]', 'nextQueryHints[]'],
        requiredSensitiveInputs: ['query', 'dsl', 'dsl-file', 'stdin']
      })
    }
    if (command.id === 'ai-output-check') {
      validateFindingFixture(command, basePath, diagnostics, {
        inputCollections: [command.optionalOptions],
        outputPrefix: ''
      })
    }
    if (command.id === 'review-pr') {
      validateReviewDeliveryFixture(command, basePath, diagnostics)
    }
  })
}

async function validateMcpContracts(fixture, diagnostics, suppliedDescriptors) {
  if (!Array.isArray(fixture?.mcpTools) || !Array.isArray(fixture?.mcpResources) || !Array.isArray(fixture?.mcpResourceTemplates) || !Array.isArray(fixture?.mcpPrompts)) {
    return
  }
  const descriptors = suppliedDescriptors ?? await loadLocalMcpDescriptors()
  validateMcpTools(fixture.mcpTools, descriptors.tools, diagnostics)
  validateMcpResources(fixture.mcpResources, descriptors.resources, diagnostics)
  validateMcpResourceTemplates(fixture.mcpResourceTemplates, descriptors.resourceTemplates, diagnostics)
  validateMcpPrompts(fixture.mcpPrompts, descriptors.prompts, diagnostics)
}

/**
 * 读取本地 MCP tools/resources/prompts 的 live descriptors，不调用 DataSpec 后端。
 *
 * @returns {Promise<object>} MCP descriptor 集合。
 */
export async function loadLocalMcpDescriptors() {
  const handler = createMcpHandler({
    projectId: 7,
    server: 'http://dataspec.local'
  }, async () => {
    throw new Error('contract fixture check must not call DataSpec backend')
  })
  const tools = await handler({ jsonrpc: '2.0', id: 1, method: 'tools/list' })
  const resources = await handler({ jsonrpc: '2.0', id: 2, method: 'resources/list' })
  const resourceTemplates = await handler({ jsonrpc: '2.0', id: 3, method: 'resources/templates/list' })
  const prompts = await handler({ jsonrpc: '2.0', id: 4, method: 'prompts/list' })
  return {
    tools: tools.result.tools,
    resources: resources.result.resources,
    resourceTemplates: resourceTemplates.result.resourceTemplates,
    prompts: prompts.result.prompts
  }
}

function validateMcpTools(fixtures, liveTools, diagnostics) {
  const fixtureByName = mapBy(fixtures, 'name')
  const liveByName = mapBy(liveTools, 'name')
  for (const name of REQUIRED_MCP_TOOLS) {
    if (!fixtureByName.has(name)) {
      diagnostics.push(diagnosticOf('MISSING_REQUIRED_MCP_TOOL', `mcpTools.${name}`, `缺少 MCP tool fixture: ${name}`))
    }
  }
  fixtures.forEach((tool, index) => {
    const basePath = `mcpTools[${index}]`
    requireString(tool.name, `${basePath}.name`, diagnostics)
    requireString(tool.description, `${basePath}.description`, diagnostics)
    requireArray(tool.inputProperties, `${basePath}.inputProperties`, diagnostics)
    requireNonEmptyArray(tool.outputShape, `${basePath}.outputShape`, diagnostics)
    requireObject(tool.successExample, `${basePath}.successExample`, diagnostics)
    requireObject(tool.failureExample, `${basePath}.failureExample`, diagnostics)
    validateSafety(tool.safety, `${basePath}.safety`, diagnostics)
    validateSpecialSafetyContract(tool.name, tool.safety, `${basePath}.safety`, diagnostics)
    requireNonEmptyArray(tool.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)
    if (tool.name === 'search_fields') {
      validateStandardQueryDslFixture(tool, basePath, diagnostics, {
        requiredInputs: ['standardQuery'],
        requiredOutputShape: ['structuredContent.normalizedQuery', 'structuredContent.normalizedQuery.queryTokens[]', 'structuredContent.normalizedQuery.queryTokens[].resolutionStatus', 'structuredContent.querySummary', 'structuredContent.appliedFilters[]', 'structuredContent.ignoredFilters[]', 'structuredContent.nextQueryHints[]'],
        requiredSensitiveInputs: ['query', 'standardQuery', 'filters']
      })
    }
    if (tool.name === 'check_ai_output') {
      validateFindingFixture(tool, basePath, diagnostics, {
        inputCollections: [tool.inputProperties],
        outputPrefix: 'structuredContent.'
      })
    }
    if (tool.name === 'export_evidence_package') {
      validateEvidencePackageFindingFixture(tool, basePath, diagnostics)
    }

    const liveTool = liveByName.get(tool.name)
    if (!liveTool) {
      diagnostics.push(diagnosticOf('UNKNOWN_MCP_TOOL', `${basePath}.name`, `MCP tools/list 中不存在 ${tool.name}。`))
      return
    }
    if (liveTool.inputSchema?.properties?.findings) {
      validateLiveFindingSchema(
        liveTool.inputSchema.properties.findings,
        `${basePath}.live.inputSchema.properties.findings`,
        diagnostics
      )
    }
    compareStringSets(
      Object.keys(liveTool.inputSchema?.properties ?? {}),
      tool.inputProperties ?? [],
      `${basePath}.inputProperties`,
      'MCP_TOOL_INPUT_PROPERTIES_MISMATCH',
      diagnostics
    )
    compareStringSets(
      liveTool.inputSchema?.required ?? [],
      tool.requiredInputs ?? [],
      `${basePath}.requiredInputs`,
      'MCP_TOOL_REQUIRED_INPUTS_MISMATCH',
      diagnostics
    )
    compareSafety(liveTool.safety, tool.safety, `${basePath}.safety`, diagnostics)
  })
}

function validateLiveFindingSchema(findingsSchema, basePath, diagnostics) {
  if (findingsSchema?.type !== 'array') {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.type`, 'live findings schema type 必须是 array。'))
  }
  if (findingsSchema?.maxItems !== 100) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.maxItems`, 'live findings maxItems 必须是 100。'))
  }
  const itemSchema = findingsSchema?.items
  if (!itemSchema || itemSchema.type !== 'object') {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.items`, 'live findings.items 必须是 object schema。'))
    return
  }
  if (itemSchema.additionalProperties !== false) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.items.additionalProperties`, 'live findings.items 必须拒绝未知字段。'))
  }
  compareStringSets(
    REVIEW_FINDING_FIELDS,
    Object.keys(itemSchema.properties ?? {}),
    `${basePath}.items.properties`,
    'MCP_REVIEW_FINDING_SCHEMA_MISMATCH',
    diagnostics
  )
  compareStringSets(
    REVIEW_FINDING_REQUIRED_FIELDS,
    itemSchema.required ?? [],
    `${basePath}.items.required`,
    'MCP_REVIEW_FINDING_SCHEMA_MISMATCH',
    diagnostics
  )
  if (itemSchema.properties?.schemaVersion?.minimum !== 1) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.items.properties.schemaVersion.minimum`, 'schemaVersion minimum 必须是 1。'))
  }

  validateLiveNestedFindingObject(
    itemSchema.properties?.subject,
    REVIEW_FINDING_SUBJECT_FIELDS,
    `${basePath}.items.properties.subject`,
    diagnostics
  )
  validateLiveNestedFindingObject(
    itemSchema.properties?.location,
    REVIEW_FINDING_LOCATION_FIELDS,
    `${basePath}.items.properties.location`,
    diagnostics
  )
  validateLiveNestedFindingObject(
    itemSchema.properties?.waiver,
    REVIEW_FINDING_WAIVER_FIELDS,
    `${basePath}.items.properties.waiver`,
    diagnostics
  )

  const evidenceRefs = itemSchema.properties?.evidenceRefs
  if (evidenceRefs?.type !== 'array' || evidenceRefs.maxItems !== 20) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.items.properties.evidenceRefs`, 'evidenceRefs 必须是 maxItems=20 的 array。'))
  }
  if (evidenceRefs?.items?.minLength !== 1) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.items.properties.evidenceRefs.items.minLength`, 'evidenceRef minLength 必须是 1。'))
  }

  for (const [field, maxLength] of Object.entries(REVIEW_FINDING_TEXT_BOUNDS)) {
    const schema = liveFindingTextSchema(itemSchema, field)
    if (schema?.maxLength !== maxLength) {
      diagnostics.push(diagnosticOf(
        'MCP_REVIEW_FINDING_SCHEMA_MISMATCH',
        `${basePath}.items.${field}.maxLength`,
        `live Finding ${field} maxLength 必须是 ${maxLength}。`
      ))
    }
  }
}

function validateLiveNestedFindingObject(schema, expectedFields, basePath, diagnostics) {
  if (!schema || schema.type !== 'object') {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', basePath, 'live nested Finding 字段必须是 object schema。'))
    return
  }
  if (schema.additionalProperties !== false) {
    diagnostics.push(diagnosticOf('MCP_REVIEW_FINDING_SCHEMA_MISMATCH', `${basePath}.additionalProperties`, 'live nested Finding schema 必须拒绝未知字段。'))
  }
  compareStringSets(
    expectedFields,
    Object.keys(schema.properties ?? {}),
    `${basePath}.properties`,
    'MCP_REVIEW_FINDING_SCHEMA_MISMATCH',
    diagnostics
  )
  compareStringSets(
    [],
    schema.required ?? [],
    `${basePath}.required`,
    'MCP_REVIEW_FINDING_SCHEMA_MISMATCH',
    diagnostics
  )
}

function liveFindingTextSchema(itemSchema, field) {
  if (field === 'evidenceRef') {
    return itemSchema.properties?.evidenceRefs?.items
  }
  return field.split('.').reduce((schema, part, index, parts) => {
    if (!schema) return undefined
    return index === parts.length - 1
      ? schema.properties?.[part]
      : schema.properties?.[part]
  }, itemSchema)
}

function validateMcpResources(fixtures, liveResources, diagnostics) {
  const fixtureUris = new Set(fixtures.map((resource) => resource.uri ?? resource.key).filter(Boolean))
  const liveByUri = mapBy(liveResources, 'uri')
  for (const uri of REQUIRED_MCP_RESOURCES) {
    const present = uri.startsWith('dataspec://')
      ? fixtureUris.has(uri)
      : [...fixtureUris].some((item) => String(item).includes(uri))
    if (!present) {
      diagnostics.push(diagnosticOf('MISSING_REQUIRED_MCP_RESOURCE', `mcpResources.${uri}`, `缺少 MCP resource fixture: ${uri}`))
    }
  }
  fixtures.forEach((resource, index) => {
    const basePath = `mcpResources[${index}]`
    requireString(resource.uri, `${basePath}.uri`, diagnostics)
    requireString(resource.description, `${basePath}.description`, diagnostics)
    requireNonEmptyArray(resource.outputShape, `${basePath}.outputShape`, diagnostics)
    requireObject(resource.successExample, `${basePath}.successExample`, diagnostics)
    validateSafety(resource.safety, `${basePath}.safety`, diagnostics)
    validateSpecialSafetyContract(resource.uri, resource.safety, `${basePath}.safety`, diagnostics)
    requireNonEmptyArray(resource.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)
    const liveResource = liveByUri.get(normalizeFixtureResourceUri(resource.uri))
    if (!liveResource) {
      diagnostics.push(diagnosticOf('UNKNOWN_MCP_RESOURCE', `${basePath}.uri`, `MCP resources/list 中不存在 ${resource.uri}。`))
      return
    }
    if (resource.name !== undefined && liveResource.name !== resource.name) {
      diagnostics.push(diagnosticOf('MCP_RESOURCE_NAME_MISMATCH', `${basePath}.name`, 'fixture 与 MCP resources/list 的 name 不一致。'))
    }
    if (liveResource.description !== resource.description) {
      diagnostics.push(diagnosticOf('MCP_RESOURCE_DESCRIPTION_MISMATCH', `${basePath}.description`, 'fixture 与 MCP resources/list 的 description 不一致。'))
    }
    if (resource.mimeType !== undefined && liveResource.mimeType !== resource.mimeType) {
      diagnostics.push(diagnosticOf('MCP_RESOURCE_MIME_TYPE_MISMATCH', `${basePath}.mimeType`, 'fixture 与 MCP resources/list 的 mimeType 不一致。'))
    }
  })
}

function validateMcpResourceTemplates(fixtures, liveResourceTemplates, diagnostics) {
  const fixtureByUriTemplate = mapBy(fixtures, 'uriTemplate')
  const liveByUriTemplate = mapBy(liveResourceTemplates, 'uriTemplate')
  for (const uriTemplate of REQUIRED_MCP_RESOURCE_TEMPLATES) {
    if (!fixtureByUriTemplate.has(uriTemplate)) {
      diagnostics.push(diagnosticOf('MISSING_REQUIRED_MCP_RESOURCE_TEMPLATE', `mcpResourceTemplates.${uriTemplate}`, `缺少 MCP resource template fixture: ${uriTemplate}`))
    }
  }
  fixtures.forEach((template, index) => {
    const basePath = `mcpResourceTemplates[${index}]`
    requireString(template.uriTemplate, `${basePath}.uriTemplate`, diagnostics)
    requireString(template.name, `${basePath}.name`, diagnostics)
    requireString(template.description, `${basePath}.description`, diagnostics)
    requireString(template.mimeType, `${basePath}.mimeType`, diagnostics)
    requireNonEmptyArray(template.outputShape, `${basePath}.outputShape`, diagnostics)
    requireObject(template.successExample, `${basePath}.successExample`, diagnostics)
    validateSafety(template.safety, `${basePath}.safety`, diagnostics)
    validateSpecialSafetyContract(template.uriTemplate, template.safety, `${basePath}.safety`, diagnostics)
    requireNonEmptyArray(template.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)
    const liveTemplate = liveByUriTemplate.get(template.uriTemplate)
    if (!liveTemplate) {
      diagnostics.push(diagnosticOf('UNKNOWN_MCP_RESOURCE_TEMPLATE', `${basePath}.uriTemplate`, `MCP resources/templates/list 中不存在 ${template.uriTemplate}。`))
      return
    }
    if (liveTemplate.description !== template.description) {
      diagnostics.push(diagnosticOf('MCP_RESOURCE_TEMPLATE_DESCRIPTION_MISMATCH', `${basePath}.description`, 'fixture 与 MCP resources/templates/list 的 description 不一致。'))
    }
    if (liveTemplate.mimeType !== template.mimeType) {
      diagnostics.push(diagnosticOf('MCP_RESOURCE_TEMPLATE_MIME_TYPE_MISMATCH', `${basePath}.mimeType`, 'fixture 与 MCP resources/templates/list 的 mimeType 不一致。'))
    }
  })
}

function validateMcpPrompts(fixtures, livePrompts, diagnostics) {
  const fixtureByName = mapBy(fixtures, 'name')
  const liveByName = mapBy(livePrompts, 'name')
  for (const name of REQUIRED_MCP_PROMPTS) {
    if (!fixtureByName.has(name)) {
      diagnostics.push(diagnosticOf('MISSING_REQUIRED_MCP_PROMPT', `mcpPrompts.${name}`, `缺少 MCP prompt fixture: ${name}`))
    }
  }
  fixtures.forEach((prompt, index) => {
    const basePath = `mcpPrompts[${index}]`
    requireString(prompt.name, `${basePath}.name`, diagnostics)
    requireString(prompt.description, `${basePath}.description`, diagnostics)
    requireArray(prompt.arguments, `${basePath}.arguments`, diagnostics)
    requireNonEmptyArray(prompt.outputShape, `${basePath}.outputShape`, diagnostics)
    requireObject(prompt.successExample, `${basePath}.successExample`, diagnostics)
    validateSafety(prompt.safety, `${basePath}.safety`, diagnostics)
    requireNonEmptyArray(prompt.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)
    const livePrompt = liveByName.get(prompt.name)
    if (!livePrompt) {
      diagnostics.push(diagnosticOf('UNKNOWN_MCP_PROMPT', `${basePath}.name`, `MCP prompts/list 中不存在 ${prompt.name}。`))
      return
    }
    if (livePrompt.description !== prompt.description) {
      diagnostics.push(diagnosticOf('MCP_PROMPT_DESCRIPTION_MISMATCH', `${basePath}.description`, 'fixture 与 MCP prompts/list 的 description 不一致。'))
    }
    validateMcpPromptArguments(prompt, livePrompt, basePath, diagnostics)
    if (REQUIRED_FIRST_CLASS_MCP_PROMPTS.includes(prompt.name)) {
      validateMcpPromptGuidance(prompt, livePrompt, basePath, diagnostics)
      validateMcpPromptSafety(prompt, livePrompt, basePath, diagnostics)
    }
  })
}

function validateMcpPromptArguments(prompt, livePrompt, basePath, diagnostics) {
  const fixtureArguments = normalizePromptArguments(prompt.arguments ?? [], `${basePath}.arguments`, diagnostics)
  const liveArguments = normalizePromptArguments(livePrompt.arguments ?? [], `${basePath}.liveArguments`, diagnostics)
  compareStringSets(
    liveArguments.map((argument) => argument.name),
    fixtureArguments.map((argument) => argument.name),
    `${basePath}.arguments`,
    'MCP_PROMPT_ARGUMENTS_MISMATCH',
    diagnostics
  )
  const liveByName = mapBy(liveArguments, 'name')
  for (const argument of fixtureArguments) {
    const liveArgument = liveByName.get(argument.name)
    if (!liveArgument) {
      continue
    }
    if (argument.description !== undefined && liveArgument.description !== argument.description) {
      diagnostics.push(diagnosticOf('MCP_PROMPT_ARGUMENT_DESCRIPTION_MISMATCH', `${basePath}.arguments.${argument.name}.description`, 'fixture 与 MCP prompts/list 的 argument description 不一致。'))
    }
    if (argument.required !== undefined && Boolean(liveArgument.required) !== Boolean(argument.required)) {
      diagnostics.push(diagnosticOf('MCP_PROMPT_ARGUMENT_REQUIRED_MISMATCH', `${basePath}.arguments.${argument.name}.required`, 'fixture 与 MCP prompts/list 的 argument required 不一致。'))
    }
  }
}

function normalizePromptArguments(argumentsValue, pathName, diagnostics) {
  return argumentsValue.map((argument, index) => {
    if (typeof argument === 'string') {
      return { name: argument }
    }
    if (argument && typeof argument === 'object' && !Array.isArray(argument) && typeof argument.name === 'string') {
      return {
        name: argument.name,
        description: typeof argument.description === 'string' ? argument.description : undefined,
        required: typeof argument.required === 'boolean' ? argument.required : undefined
      }
    }
    diagnostics.push(diagnosticOf('INVALID_PROMPT_ARGUMENT', `${pathName}[${index}]`, 'prompt argument 必须是 string 或包含 name 的 object。'))
    return { name: '' }
  })
}

function validateMcpPromptGuidance(prompt, livePrompt, basePath, diagnostics) {
  const guidance = livePrompt.dataspecGuidance
  if (!guidance || typeof guidance !== 'object') {
    diagnostics.push(diagnosticOf('MCP_PROMPT_GUIDANCE_MISSING', `${basePath}.dataspecGuidance`, '一等化 MCP prompt descriptor 缺少 dataspecGuidance。'))
    return
  }
  const fixtureGuidance = prompt.dataspecGuidance
  if (!fixtureGuidance || typeof fixtureGuidance !== 'object' || Array.isArray(fixtureGuidance)) {
    diagnostics.push(diagnosticOf('MCP_PROMPT_FIXTURE_GUIDANCE_MISSING', `${basePath}.dataspecGuidance`, '一等化 MCP prompt fixture 必须保存 dataspecGuidance 契约。'))
    return
  }
  if (!(prompt.outputShape ?? []).includes('dataspecGuidance')) {
    diagnostics.push(diagnosticOf('MCP_PROMPT_OUTPUT_SHAPE_MISSING_GUIDANCE', `${basePath}.outputShape`, '一等化 MCP prompt fixture 必须声明 dataspecGuidance 输出。'))
  }
  compareJsonValue(guidance.templateId, fixtureGuidance.templateId, `${basePath}.dataspecGuidance.templateId`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.requiredInputs ?? [], fixtureGuidance.requiredInputs ?? [], `${basePath}.dataspecGuidance.requiredInputs`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.safeDefaults ?? {}, fixtureGuidance.safeDefaults ?? {}, `${basePath}.dataspecGuidance.safeDefaults`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.resourceSequence ?? [], fixtureGuidance.resourceSequence ?? [], `${basePath}.dataspecGuidance.resourceSequence`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.toolSequence ?? [], fixtureGuidance.toolSequence ?? [], `${basePath}.dataspecGuidance.toolSequence`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.stopConditions ?? [], fixtureGuidance.stopConditions ?? [], `${basePath}.dataspecGuidance.stopConditions`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.evidenceRequirements ?? [], fixtureGuidance.evidenceRequirements ?? [], `${basePath}.dataspecGuidance.evidenceRequirements`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.nextActions ?? [], fixtureGuidance.nextActions ?? [], `${basePath}.dataspecGuidance.nextActions`, 'MCP_PROMPT_GUIDANCE_MISMATCH', diagnostics)
  compareJsonValue(guidance.nextActions ?? [], prompt.recommendedNextActions ?? [], `${basePath}.recommendedNextActions`, 'MCP_PROMPT_RECOMMENDED_NEXT_ACTIONS_MISMATCH', diagnostics)
}

function validateMcpPromptSafety(prompt, livePrompt, basePath, diagnostics) {
  if (!livePrompt.safety || typeof livePrompt.safety !== 'object' || Array.isArray(livePrompt.safety)) {
    diagnostics.push(diagnosticOf('MCP_PROMPT_SAFETY_MISSING', `${basePath}.safety`, '一等化 MCP prompt descriptor 缺少 safety metadata。'))
    return
  }
  compareSafety(livePrompt.safety, prompt.safety, `${basePath}.safety`, diagnostics, 'MCP_PROMPT_SAFETY_MISMATCH')
}

function validateSafety(safety, pathName, diagnostics) {
  if (!safety || typeof safety !== 'object' || Array.isArray(safety)) {
    diagnostics.push(diagnosticOf('INVALID_SAFETY', pathName, 'safety 必须是 object。'))
    return
  }
  for (const field of REQUIRED_SAFETY_FIELDS) {
    if (!(field in safety)) {
      diagnostics.push(diagnosticOf('MISSING_SAFETY_FIELD', `${pathName}.${field}`, `缺少 safety.${field}。`))
    }
  }
  for (const field of ['readOnly', 'writesProject', 'requiresDryRun', 'requiresIdempotencyKey']) {
    if (field in safety && typeof safety[field] !== 'boolean') {
      diagnostics.push(diagnosticOf('INVALID_SAFETY_FIELD', `${pathName}.${field}`, `${field} 必须是 boolean。`))
    }
  }
  if ('sensitiveInputs' in safety) {
    requireArray(safety.sensitiveInputs, `${pathName}.sensitiveInputs`, diagnostics)
  }
  if ('nextActions' in safety) {
    requireArray(safety.nextActions, `${pathName}.nextActions`, diagnostics)
  }
}

function validateSpecialSafetyContract(contractId, safety, pathName, diagnostics) {
  if (!safety || typeof safety !== 'object' || Array.isArray(safety)) {
    return
  }
  if (contractId === 'test-data-generate' || contractId === 'generate_test_data_package') {
    validateSafetyRequirements(
      TEST_DATA_PACKAGE_SAFETY_REQUIREMENTS,
      safety,
      pathName,
      'TEST_DATA_FIXTURE_SAFETY_MISMATCH',
      diagnostics
    )
  }
  if (
    contractId === 'consumer-compat-check' ||
    contractId === 'check_consumer_compatibility' ||
    contractId === 'dataspec://project/<projectId>/consumer-compatibility-suite' ||
    contractId === 'dataspec://project/{projectId}/consumer-compatibility-suite'
  ) {
    validateSafetyRequirements(
      CONSUMER_COMPAT_SAFETY_REQUIREMENTS,
      safety,
      pathName,
      'CONSUMER_COMPAT_FIXTURE_SAFETY_MISMATCH',
      diagnostics
    )
  }
}

function validateSafetyRequirements(requirements, safety, pathName, code, diagnostics) {
  for (const [field, expected] of Object.entries(requirements)) {
    if (safety[field] !== expected) {
      diagnostics.push(diagnosticOf(code, `${pathName}.${field}`, `safety.${field} 必须是 ${expected}。`))
    }
  }
}

function validateStandardQueryDslFixture(entry, basePath, diagnostics, options) {
  const dsl = entry.standardQueryDsl
  if (!dsl || typeof dsl !== 'object' || Array.isArray(dsl)) {
    diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_FIXTURE_MISSING', `${basePath}.standardQueryDsl`, 'Standard Query DSL fixture 必须声明 standardQueryDsl 元数据。'))
    return
  }
  if (dsl.version !== 1) {
    diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_VERSION_MISMATCH', `${basePath}.standardQueryDsl.version`, 'Standard Query DSL fixture version 必须是 1。'))
  }
  if (dsl.target !== 'FIELD') {
    diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_TARGET_MISMATCH', `${basePath}.standardQueryDsl.target`, 'Standard Query DSL v1 target 必须是 FIELD。'))
  }
  if (dsl.readOnly !== true || dsl.secretSafeDiagnostics !== true) {
    diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_SAFETY_MISMATCH', `${basePath}.standardQueryDsl`, 'Standard Query DSL fixture 必须声明 readOnly 和 secretSafeDiagnostics。'))
  }
  compareStringSets(
    STANDARD_QUERY_DSL_FILTERS,
    dsl.supportedFilters ?? [],
    `${basePath}.standardQueryDsl.supportedFilters`,
    'STANDARD_QUERY_DSL_FILTERS_MISMATCH',
    diagnostics
  )
  for (const input of options.requiredInputs) {
    const inputCollections = [
      entry.inputProperties ?? [],
      entry.optionalOptions ?? [],
      entry.oneOfRequiredOptions ?? []
    ].flat().map(String)
    if (!inputCollections.includes(input)) {
      diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_INPUT_MISSING', `${basePath}.standardQueryDsl`, `缺少 DSL 输入声明: ${input}`))
    }
  }
  for (const shape of options.requiredOutputShape) {
    if (!(entry.outputShape ?? []).includes(shape)) {
      diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_OUTPUT_SHAPE_MISSING', `${basePath}.outputShape`, `缺少 DSL 输出字段: ${shape}`))
    }
  }
  for (const input of options.requiredSensitiveInputs) {
    if (!(entry.safety?.sensitiveInputs ?? []).includes(input)) {
      diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_SENSITIVE_INPUT_MISSING', `${basePath}.safety.sensitiveInputs`, `缺少 DSL 敏感输入声明: ${input}`))
    }
  }
  if (entry.safety?.readOnly !== true || entry.safety?.writesProject !== false) {
    diagnostics.push(diagnosticOf('STANDARD_QUERY_DSL_SAFETY_MISMATCH', `${basePath}.safety`, 'Standard Query DSL 必须保持只读且不写项目。'))
  }
}

function validateFindingFixture(entry, basePath, diagnostics, options) {
  const findingShape = entry.findingShape
  if (!findingShape || typeof findingShape !== 'object' || Array.isArray(findingShape)) {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_SHAPE_MISSING', `${basePath}.findingShape`, '共享 Finding fixture 必须声明 findingShape 元数据。'))
    return
  }
  const inputs = options.inputCollections.filter(Array.isArray).flat().map(String)
  if (!inputs.includes('findings')) {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_INPUT_MISSING', basePath, '缺少 findings 输入声明。'))
  }
  compareStringSets(
    REVIEW_FINDING_FIELDS,
    findingShape.fields ?? [],
    `${basePath}.findingShape.fields`,
    'REVIEW_FINDING_FIELDS_MISMATCH',
    diagnostics
  )
  compareStringSets(
    REVIEW_FINDING_REQUIRED_FIELDS,
    findingShape.requiredFields ?? [],
    `${basePath}.findingShape.requiredFields`,
    'REVIEW_FINDING_REQUIRED_FIELDS_MISMATCH',
    diagnostics
  )
  compareStringSets(
    REVIEW_FINDING_SUBJECT_FIELDS,
    findingShape.subjectFields ?? [],
    `${basePath}.findingShape.subjectFields`,
    'REVIEW_FINDING_SUBJECT_FIELDS_MISMATCH',
    diagnostics
  )
  compareStringSets(
    REVIEW_FINDING_LOCATION_FIELDS,
    findingShape.locationFields ?? [],
    `${basePath}.findingShape.locationFields`,
    'REVIEW_FINDING_LOCATION_FIELDS_MISMATCH',
    diagnostics
  )
  compareStringSets(
    REVIEW_FINDING_WAIVER_FIELDS,
    findingShape.waiverFields ?? [],
    `${basePath}.findingShape.waiverFields`,
    'REVIEW_FINDING_WAIVER_FIELDS_MISMATCH',
    diagnostics
  )
  validateFindingTextBounds(findingShape.textBounds, `${basePath}.findingShape.textBounds`, diagnostics)
  compareStringSets(
    FINDING_EVIDENCE_ISSUE_CODES,
    entry.findingEvidenceIssueCodes ?? [],
    `${basePath}.findingEvidenceIssueCodes`,
    'REVIEW_FINDING_EVIDENCE_CODES_MISMATCH',
    diagnostics
  )
  if (findingShape.maxItems !== 100 || findingShape.maxEvidenceRefs !== 20) {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_BOUNDS_MISMATCH', `${basePath}.findingShape`, 'Finding fixture 必须声明 maxItems=100 且 maxEvidenceRefs=20。'))
  }
  const gating = findingShape.evidenceGating
  const highImpact = gating?.highImpact ?? []
  for (const trigger of ['severity=ERROR', 'confidence>=80', 'autoFixSafe=true']) {
    if (!highImpact.includes(trigger)) {
      diagnostics.push(diagnosticOf('REVIEW_FINDING_GATING_MISSING', `${basePath}.findingShape.evidenceGating`, `缺少高影响 evidence gating: ${trigger}`))
    }
  }
  if (gating?.projectScoped !== true || gating?.invalidHighImpactStatus !== 'FAIL' || gating?.invalidLowImpactStatus !== 'WARN') {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_GATING_MISMATCH', `${basePath}.findingShape.evidenceGating`, 'Finding evidence gating 必须声明 projectScoped、FAIL/WARN 分级语义。'))
  }
  for (const shape of [
    `${options.outputPrefix}findings[]`,
    `${options.outputPrefix}findings[].findingKey`,
    `${options.outputPrefix}findings[].evidenceRefs[]`,
    `${options.outputPrefix}findings[].waiver`,
    `${options.outputPrefix}verificationReceipt`
  ]) {
    if (!(entry.outputShape ?? []).includes(shape)) {
      diagnostics.push(diagnosticOf('REVIEW_FINDING_OUTPUT_SHAPE_MISSING', `${basePath}.outputShape`, `缺少 Finding 输出字段: ${shape}`))
    }
  }
  if (!(entry.safety?.sensitiveInputs ?? []).includes('findings')) {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_SENSITIVE_INPUT_MISSING', `${basePath}.safety.sensitiveInputs`, 'findings 必须声明为敏感输入。'))
  }
}

function validateFindingTextBounds(actual, pathName, diagnostics) {
  if (!actual || typeof actual !== 'object' || Array.isArray(actual)) {
    diagnostics.push(diagnosticOf('REVIEW_FINDING_TEXT_BOUNDS_MISSING', pathName, 'Finding fixture 必须声明逐字段 textBounds。'))
    return
  }
  compareStringSets(
    Object.keys(REVIEW_FINDING_TEXT_BOUNDS),
    Object.keys(actual),
    pathName,
    'REVIEW_FINDING_TEXT_BOUND_FIELDS_MISMATCH',
    diagnostics
  )
  for (const [field, expected] of Object.entries(REVIEW_FINDING_TEXT_BOUNDS)) {
    if (actual[field] !== expected) {
      diagnostics.push(diagnosticOf(
        'REVIEW_FINDING_TEXT_BOUND_MISMATCH',
        `${pathName}.${field}`,
        `Finding 文本上限应为 ${expected}。`
      ))
    }
  }
}

function validateReviewDeliveryFixture(entry, basePath, diagnostics) {
  for (const shape of REVIEW_DELIVERY_OUTPUT_FIELDS) {
    if (!(entry.outputShape ?? []).includes(shape)) {
      diagnostics.push(diagnosticOf('REVIEW_DELIVERY_OUTPUT_SHAPE_MISSING', `${basePath}.outputShape`, `缺少 review-pr delivery 字段: ${shape}`))
    }
  }
  if (entry.deliveryEvidencePolicy?.githubMetadataInEvidenceRefs !== false ||
      entry.deliveryEvidencePolicy?.missingUrlValue !== null ||
      entry.deliveryEvidencePolicy?.legacyIssueFallback !== true ||
      entry.deliveryEvidencePolicy?.requiresPrBlobMatch !== true ||
      entry.deliveryEvidencePolicy?.rechecksHeadBeforePublish !== true) {
    diagnostics.push(diagnosticOf('REVIEW_DELIVERY_EVIDENCE_POLICY_MISMATCH', `${basePath}.deliveryEvidencePolicy`, 'review-pr fixture 必须声明 PR blob/head 绑定、GitHub metadata 与 finding evidence 分离、URL 可空和 legacy issue fallback。'))
  }
}

function validateEvidencePackageFindingFixture(entry, basePath, diagnostics) {
  for (const input of ['postCheckSummary', 'postCheckReceipt', 'findings']) {
    if (!(entry.inputProperties ?? []).includes(input)) {
      diagnostics.push(diagnosticOf('EVIDENCE_PACKAGE_FINDING_INPUT_MISSING', `${basePath}.inputProperties`, `缺少 Evidence Package 输入: ${input}`))
    }
  }
  for (const shape of ['structuredContent.postCheckSummary', 'structuredContent.findings[]', 'structuredContent.findings[].evidenceRefs[]']) {
    if (!(entry.outputShape ?? []).includes(shape)) {
      diagnostics.push(diagnosticOf('EVIDENCE_PACKAGE_FINDING_OUTPUT_MISSING', `${basePath}.outputShape`, `缺少 Evidence Package Finding 输出: ${shape}`))
    }
  }
  if (entry.findingAcceptance?.requiresPassingPostCheck !== true ||
      entry.findingAcceptance?.requiresVerificationReceipt !== true ||
      entry.findingAcceptance?.revalidatesEvidenceRefs !== true) {
    diagnostics.push(diagnosticOf('EVIDENCE_PACKAGE_FINDING_GATING_MISMATCH', `${basePath}.findingAcceptance`, 'Evidence Package fixture 必须声明 post-check、receipt 与 evidence 复验门禁。'))
  }
}

function compareSafety(liveSafety, fixtureSafety, pathName, diagnostics, code = 'MCP_TOOL_SAFETY_MISMATCH') {
  if (!liveSafety || !fixtureSafety) {
    return
  }
  for (const field of ['readOnly', 'writesProject', 'requiresDryRun', 'requiresIdempotencyKey']) {
    if (liveSafety[field] !== fixtureSafety[field]) {
      diagnostics.push(diagnosticOf(code, `${pathName}.${field}`, `fixture 与 MCP descriptor 的 ${field} 不一致。`))
    }
  }
  compareStringSets(
    liveSafety.sensitiveInputs ?? [],
    fixtureSafety.sensitiveInputs ?? [],
    `${pathName}.sensitiveInputs`,
    code,
    diagnostics
  )
  compareStringSets(
    liveSafety.nextActions ?? [],
    fixtureSafety.nextActions ?? [],
    `${pathName}.nextActions`,
    code,
    diagnostics
  )
}

function compareJsonValue(expected, actual, pathName, code, diagnostics) {
  if (JSON.stringify(expected) !== JSON.stringify(actual)) {
    diagnostics.push(diagnosticOf(code, pathName, 'fixture 与 MCP descriptor 的结构化值不一致。'))
  }
}

function compareStringSets(expected, actual, pathName, code, diagnostics) {
  const expectedSet = new Set(expected.map(String).sort())
  const actualSet = new Set(actual.map(String).sort())
  const missing = [...expectedSet].filter((item) => !actualSet.has(item))
  const extra = [...actualSet].filter((item) => !expectedSet.has(item))
  if (missing.length || extra.length) {
    diagnostics.push(diagnosticOf(code, pathName, `集合不一致，missing=${missing.join(',') || '-'} extra=${extra.join(',') || '-'}`))
  }
}

function scanSecrets(value, diagnostics, pathName = '$') {
  if (typeof value === 'string') {
    if (looksLikeRawSecret(value)) {
      diagnostics.push(diagnosticOf('SECRET_LIKE_VALUE', pathName, 'fixture 示例包含疑似 raw secret 或连接串。'))
    }
    return
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => scanSecrets(item, diagnostics, `${pathName}[${index}]`))
    return
  }
  if (value && typeof value === 'object') {
    for (const [key, item] of Object.entries(value)) {
      scanSecrets(item, diagnostics, `${pathName}.${key}`)
    }
  }
}

function looksLikeRawSecret(value) {
  return [
    /jdbc:[^\s"',;}&]+/i,
    /\bhttps?:\/\/(?!\[REDACTED\]@|<)[^\s/?#@]+@/i,
    /\b(?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/(?!\[REDACTED\]|<)[^\s"',;}&]+/i,
    /(authorization\s*[:=]\s*bearer\s+)(?!<|\[REDACTED\]|\*\*\*)[^\s,;]+/i,
    /(authorization\s*[:=]\s*(?:basic|apikey|api-key|token|digest)\s+)(?!<|\[REDACTED\]|\*\*\*)[^\s"',;}&]+/i,
    /(authorization\s*[:=]\s*)(?!<|\[REDACTED\]|\*\*\*|bearer\s+|basic\s+|apikey\s+|api-key\s+|token\s+|digest\s+)[^\s"',;}&]+/i,
    /--dataspec-token\s+(?!<|\[REDACTED\]|\$\{|\*\*\*)[^\s,;]+/i,
    /(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|dsn)\s*[:=]\s*(?!<|\[REDACTED\]|\*\*\*)[^\s"',;}&]+/i
  ].some((pattern) => pattern.test(value))
}

function parseArgs(argv) {
  const options = {
    fixturePath: DEFAULT_FIXTURE_PATH,
    format: 'text',
    help: false
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--help' || arg === '-h') {
      options.help = true
    } else if (arg === '--fixture') {
      options.fixturePath = requireValue(argv, ++index, arg)
    } else if (arg === '--format') {
      options.format = requireValue(argv, ++index, arg)
      if (!['text', 'json'].includes(options.format)) {
        throw new Error('--format 仅支持 text 或 json')
      }
    } else {
      throw new Error(`未知参数: ${arg}`)
    }
  }
  return options
}

function requireValue(argv, index, option) {
  const value = argv[index]
  if (!isOptionValue(value)) {
    throw new Error(`${option} 需要参数值`)
  }
  return value
}

function isOptionValue(value) {
  return typeof value === 'string' && value.length > 0 && !value.startsWith('-')
}

function requireString(value, pathName, diagnostics) {
  if (typeof value !== 'string' || !value.trim()) {
    diagnostics.push(diagnosticOf('INVALID_STRING', pathName, `${pathName} 必须是非空 string。`))
  }
}

function requireArray(value, pathName, diagnostics) {
  if (!Array.isArray(value)) {
    diagnostics.push(diagnosticOf('INVALID_ARRAY', pathName, `${pathName} 必须是 array。`))
  }
}

function requireNonEmptyArray(value, pathName, diagnostics) {
  if (!Array.isArray(value) || value.length === 0) {
    diagnostics.push(diagnosticOf('INVALID_NON_EMPTY_ARRAY', pathName, `${pathName} 必须是非空 array。`))
  }
}

function requireObject(value, pathName, diagnostics) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    diagnostics.push(diagnosticOf('INVALID_OBJECT', pathName, `${pathName} 必须是 object。`))
  }
}

function normalizeFixtureResourceUri(uri) {
  return String(uri).replace('<projectId>', '7')
}

function mapBy(items, key) {
  return new Map((items ?? []).filter((item) => item?.[key]).map((item) => [item[key], item]))
}

function diagnosticOf(code, pathName, message) {
  return {
    code,
    path: pathName,
    message,
    suggestedAction: '更新 CLI/MCP contract fixture、对应 descriptor 或测试断言后重试。'
  }
}

function formatText(result) {
  if (result.ok) {
    return `CLI/MCP contract fixtures: pass (${result.summary.cliCommands} CLI, ${result.summary.mcpTools} MCP tools)\n`
  }
  const lines = [
    `CLI/MCP contract fixtures: fail (${result.diagnostics.length} diagnostics)`
  ]
  for (const diagnostic of result.diagnostics) {
    lines.push(`- ${diagnostic.code} ${diagnostic.path}: ${diagnostic.message}`)
  }
  return `${lines.join('\n')}\n`
}

function helpText() {
  return `Usage: node tools/dataspec-cli-mcp-contract-check.mjs [--fixture <path>] [--format text|json]\n`
}

function processIo() {
  return {
    writeOut(text) {
      process.stdout.write(text)
    },
    writeErr(text) {
      process.stderr.write(text)
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  runContractCheckCli().then((code) => {
    process.exitCode = code
  })
}
