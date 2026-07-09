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
  'generate-ddl',
  'synthetic-examples-generate',
  'contract-import-preview',
  'schema-plan'
]

const REQUIRED_MCP_TOOLS = [
  'get_session_bootstrap',
  'get_session_state',
  'lint_sql',
  'get_field_catalog',
  'search_field_catalog',
  'search_fields',
  'suggest_fields',
  'generate_table_ddl',
  'get_ai_task_run',
  'export_evidence_package'
]

const REQUIRED_MCP_RESOURCES = [
  'dataspec://version-compatibility',
  'capability-catalog',
  'session-bootstrap',
  'session-state',
  'field-catalog',
  'workflow-recipes',
  'agent-guidance-pack',
  'ai-task-profiles',
  'schema-registry'
]

const REQUIRED_MCP_RESOURCE_TEMPLATES = [
  'dataspec://project/{projectId}/session-bootstrap',
  'dataspec://project/{projectId}/session-state',
  'dataspec://project/{projectId}/capability-catalog',
  'dataspec://project/{projectId}/schema-registry',
  'dataspec://project/{projectId}/field-catalog',
  'dataspec://project/{projectId}/workflow-recipes',
  'dataspec://project/{projectId}/ai-task-profiles',
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
 * @param {{fixturePath?: string, fixture?: object}} options validation options.
 * @returns {Promise<object>} machine-readable validation result with diagnostics.
 */
export async function validateContractFixtures(options = {}) {
  const fixturePath = options.fixturePath ?? DEFAULT_FIXTURE_PATH
  const fixture = options.fixture ?? await loadContractFixtures(fixturePath)
  const diagnostics = []

  validateRoot(fixture, diagnostics)
  validateCliCommands(fixture.cliCommands, diagnostics)
  await validateMcpContracts(fixture, diagnostics)
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
    requireObject(command.successExample, `${basePath}.successExample`, diagnostics)
    requireObject(command.failureExample, `${basePath}.failureExample`, diagnostics)
  })
}

async function validateMcpContracts(fixture, diagnostics) {
  if (!Array.isArray(fixture?.mcpTools) || !Array.isArray(fixture?.mcpResources) || !Array.isArray(fixture?.mcpResourceTemplates) || !Array.isArray(fixture?.mcpPrompts)) {
    return
  }
  const descriptors = await listLocalMcpDescriptors()
  validateMcpTools(fixture.mcpTools, descriptors.tools, diagnostics)
  validateMcpResources(fixture.mcpResources, descriptors.resources, diagnostics)
  validateMcpResourceTemplates(fixture.mcpResourceTemplates, descriptors.resourceTemplates, diagnostics)
  validateMcpPrompts(fixture.mcpPrompts, descriptors.prompts, diagnostics)
}

async function listLocalMcpDescriptors() {
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
    requireNonEmptyArray(tool.recommendedNextActions, `${basePath}.recommendedNextActions`, diagnostics)

    const liveTool = liveByName.get(tool.name)
    if (!liveTool) {
      diagnostics.push(diagnosticOf('UNKNOWN_MCP_TOOL', `${basePath}.name`, `MCP tools/list 中不存在 ${tool.name}。`))
      return
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
