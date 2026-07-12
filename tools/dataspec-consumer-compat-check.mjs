#!/usr/bin/env node

import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const TOOLS_DIR = path.dirname(SCRIPT_PATH)
const REPO_ROOT = path.dirname(TOOLS_DIR)
export const DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH = path.join(TOOLS_DIR, 'fixtures', 'consumer-compatibility-suite.json')
const DEFAULT_CLI_MCP_FIXTURE_PATH = path.join(TOOLS_DIR, 'fixtures', 'cli-mcp-contracts.json')
const DEFAULT_SCHEMA_REGISTRY_SOURCE_PATH = path.join(REPO_ROOT, 'dataspec-server', 'src', 'main', 'java', 'com', 'dataspec', 'contract', 'service', 'impl', 'SchemaRegistryServiceImpl.java')
const DEFAULT_MCP_SOURCE_PATH = path.join(TOOLS_DIR, 'dataspec-mcp.mjs')
const DEFAULT_CLI_SOURCE_PATH = path.join(TOOLS_DIR, 'dataspec-cli.mjs')
const DEFAULT_AI_CONTRACTS_DOC_PATH = path.join(REPO_ROOT, 'docs', 'ai-contracts.md')

const CHECK_KIND = 'dataspec.consumer-compatibility-suite.check'
const CHECK_SCHEMA_VERSION = 1
const FIXTURE_KIND = 'dataspec-consumer-compatibility-suite'

const REQUIRED_ADAPTERS = [
  'schema-registry',
  'ai-context',
  'cli-json',
  'mcp-descriptors',
  'cli-mcp-contract-fixtures',
  'standard-test-data-package'
]

const STANDARD_TEST_DATA_SCHEMA_TITLE = 'DataSpec Standard Test Data Package'

const SECRET_PATTERNS = [
  /\bjdbc:[^\s"'<>]+/i,
  /\b(?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/[^\s"'<>]+/i,
  /\bauthorization\s*[:=]\s*bearer\s+[^\s,;]+/i,
  /\bbearer\s+[A-Za-z0-9._~+\-/]+=*/i,
  /\b(?:password|passwd|pwd|token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|dsn)\s*[:=]\s*[^\s"',;}&]+/i,
  /-----BEGIN [A-Z ]*PRIVATE KEY-----/i
]

/**
 * Load the consumer compatibility suite fixture.
 *
 * @param {string} fixturePath path to fixture JSON.
 * @returns {Promise<object>} parsed fixture.
 */
export async function loadConsumerCompatibilitySuite(fixturePath = DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH) {
  return JSON.parse(await readFile(fixturePath, 'utf8'))
}

/**
 * Validate checked-in consumer compatibility fixtures.
 *
 * @param {{fixturePath?: string, fixture?: object}} options validation options.
 * @returns {Promise<object>} compatibility result.
 */
export async function validateConsumerCompatibilitySuite(options = {}) {
  const fixturePath = options.fixturePath ?? DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH
  const fixture = options.fixture ?? await loadConsumerCompatibilitySuite(fixturePath)
  const actualSources = await loadActualSources(options.actualSources ?? {})
  const diagnostics = []

  validateRoot(fixture, diagnostics)
  validateAdapters(fixture, diagnostics)
  validateGoldenPayloads(fixture, diagnostics)
  validateActualSources(fixture, actualSources, diagnostics)
  scanSecrets(fixture, diagnostics)

  const status = diagnostics.length === 0 ? 'COMPATIBLE' : 'BREAKING'
  const adapters = Array.isArray(fixture?.adapters) ? fixture.adapters : []
  const goldenPayloads = Array.isArray(fixture?.goldenPayloads) ? fixture.goldenPayloads : []
  const breakingRules = Array.isArray(fixture?.breakingRules) ? fixture.breakingRules : []

  return {
    kind: CHECK_KIND,
    schemaVersion: CHECK_SCHEMA_VERSION,
    suiteVersion: sanitizeText(fixture?.suiteVersion ?? null),
    checkedAt: new Date().toISOString(),
    minimumSupportedVersion: sanitizeText(fixture?.minimumSupportedVersion ?? null),
    status,
    ok: diagnostics.length === 0,
    fixturePath: options.fixture ? null : fixturePath,
    summary: {
      adapterCount: adapters.length,
      goldenPayloadCount: goldenPayloads.length,
      breakingRuleCount: breakingRules.length,
      diagnostics: diagnostics.length
    },
    goldenPayloads: goldenPayloads.map((item) => ({
      id: sanitizeText(item?.id ?? null),
      adapterId: sanitizeText(item?.adapterId ?? null),
      contractId: sanitizeText(item?.contractId ?? null),
      stableFields: Array.isArray(item?.stableFields) ? item.stableFields.map(sanitizeText) : []
    })),
    breakingRules: breakingRules.map((item) => ({
      ruleId: sanitizeText(item?.ruleId ?? item?.id ?? null),
      adapterId: sanitizeText(item?.adapterId ?? null),
      contractPath: sanitizeText(item?.contractPath ?? '*'),
      category: sanitizeText(item?.category ?? categoryForRule(item?.ruleId ?? item?.id)),
      severity: sanitizeText(item?.severity ?? null),
      description: sanitizeText(item?.description ?? null),
      migrationHint: sanitizeText(item?.migrationHint ?? defaultMigrationHint(item?.ruleId ?? item?.id))
    })),
    adapterResults: buildAdapterResults(fixture, diagnostics),
    diagnostics,
    safety: {
      readOnly: true,
      writesProject: false,
      requiresServer: false,
      externalNetworkUsed: false,
      externalLlmUsed: false
    },
    nextActions: diagnostics.length === 0
      ? ['Consumer compatibility suite 可用于本地验收。']
      : ['根据 diagnostics[].path 更新 fixture、schema、CLI/MCP descriptor 或迁移说明后重试。']
  }
}

async function loadActualSources(overrides = {}) {
  return {
    schemaRegistryText: await sourceValue(overrides, 'schemaRegistryText', () => readFile(DEFAULT_SCHEMA_REGISTRY_SOURCE_PATH, 'utf8')),
    mcpSourceText: await sourceValue(overrides, 'mcpSourceText', () => readFile(DEFAULT_MCP_SOURCE_PATH, 'utf8')),
    cliSourceText: await sourceValue(overrides, 'cliSourceText', () => readFile(DEFAULT_CLI_SOURCE_PATH, 'utf8')),
    aiContractsText: await sourceValue(overrides, 'aiContractsText', () => readFile(DEFAULT_AI_CONTRACTS_DOC_PATH, 'utf8')),
    cliMcpFixture: await sourceValue(overrides, 'cliMcpFixture', async () => JSON.parse(await readFile(DEFAULT_CLI_MCP_FIXTURE_PATH, 'utf8')))
  }
}

async function sourceValue(overrides, key, loader) {
  if (Object.prototype.hasOwnProperty.call(overrides, key)) {
    return overrides[key]
  }
  return await loader()
}

/**
 * Run the consumer compatibility check CLI.
 *
 * @param {string[]} argv command-line arguments.
 * @param {{writeOut: Function, writeErr: Function}} io output adapter.
 * @returns {Promise<number>} process-style exit code.
 */
export async function runConsumerCompatibilityCli(argv = process.argv.slice(2), io = processIo()) {
  try {
    const options = parseArgs(argv)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }
    const result = await validateConsumerCompatibilitySuite({ fixturePath: options.fixturePath })
    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    } else {
      io.writeOut(formatText(result))
    }
    return result.ok ? 0 : 1
  } catch (error) {
    const result = {
      kind: CHECK_KIND,
      schemaVersion: CHECK_SCHEMA_VERSION,
      status: 'ERROR',
      ok: false,
      diagnostics: [diagnosticOf('CHECK_FAILED', 'cli', error?.message ?? '消费端兼容检查失败')]
    }
    io.writeErr(`${JSON.stringify(result, null, 2)}\n`)
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
  if (!Array.isArray(fixture.adapters)) {
    diagnostics.push(diagnosticOf('ADAPTERS_MISSING', 'adapters', 'adapters 必须是数组。'))
  }
  if (!Array.isArray(fixture.goldenPayloads)) {
    diagnostics.push(diagnosticOf('GOLDEN_PAYLOADS_MISSING', 'goldenPayloads', 'goldenPayloads 必须是数组。'))
  }
  if (!Array.isArray(fixture.breakingRules)) {
    diagnostics.push(diagnosticOf('BREAKING_RULES_MISSING', 'breakingRules', 'breakingRules 必须是数组。'))
  }
}

function validateAdapters(fixture, diagnostics) {
  const adapters = Array.isArray(fixture?.adapters) ? fixture.adapters : []
  const byId = new Map(adapters.map((item, index) => [item?.id, { item, index }]))
  for (const adapterId of REQUIRED_ADAPTERS) {
    if (!byId.has(adapterId)) {
      diagnostics.push(diagnosticOf('REQUIRED_ADAPTER_MISSING', `adapters.${adapterId}`, `缺少必选 adapter: ${adapterId}`))
    }
  }
  adapters.forEach((adapter, index) => {
    const base = `adapters[${index}]`
    if (!adapter?.id) {
      diagnostics.push(diagnosticOf('ADAPTER_ID_MISSING', `${base}.id`, 'adapter 需要 id。'))
    }
    if (adapter?.required !== true) {
      diagnostics.push(diagnosticOf('ADAPTER_REQUIRED_MISSING', `${base}.required`, 'DataSpec 自有消费端 adapter 必须 required=true。'))
    }
    if (!Array.isArray(adapter?.stableFields) || adapter.stableFields.length === 0) {
      diagnostics.push(diagnosticOf('ADAPTER_STABLE_FIELDS_MISSING', `${base}.stableFields`, 'adapter 需要 stableFields。'))
    }
    if (adapter?.safety?.readOnly !== true) {
      diagnostics.push(diagnosticOf('ADAPTER_SAFETY_MISSING', `${base}.safety.readOnly`, 'adapter safety.readOnly 必须为 true。'))
    }
  })
}

function validateGoldenPayloads(fixture, diagnostics) {
  const payloads = Array.isArray(fixture?.goldenPayloads) ? fixture.goldenPayloads : []
  payloads.forEach((entry, index) => {
    const base = `goldenPayloads[${index}]`
    if (!entry?.id) {
      diagnostics.push(diagnosticOf('GOLDEN_PAYLOAD_ID_MISSING', `${base}.id`, 'golden payload 需要 id。'))
    }
    if (!entry?.adapterId) {
      diagnostics.push(diagnosticOf('GOLDEN_PAYLOAD_ADAPTER_MISSING', `${base}.adapterId`, 'golden payload 需要 adapterId。'))
    }
    if (!entry?.payload || typeof entry.payload !== 'object') {
      diagnostics.push(diagnosticOf('GOLDEN_PAYLOAD_BODY_MISSING', `${base}.payload`, 'golden payload 需要 object payload。'))
      return
    }
    const stableFields = Array.isArray(entry.stableFields) ? entry.stableFields : []
    if (stableFields.length === 0) {
      diagnostics.push(diagnosticOf('GOLDEN_PAYLOAD_STABLE_FIELDS_MISSING', `${base}.stableFields`, 'golden payload 需要 stableFields。'))
    }
    for (const stableField of stableFields) {
      if (!hasPath(entry.payload, stableField)) {
        diagnostics.push(diagnosticOf('STABLE_FIELD_MISSING', `${base}.payload.${stableField}`, `stable field 缺失: ${stableField}`, {
          adapterId: entry.adapterId,
          contractId: entry.contractId,
          contractPath: stableField,
          ruleId: 'stable-field-required',
          migrationHint: '保留 stable field，或提升 schemaVersion 并补迁移说明。'
        }))
      }
    }
  })
}

function validateActualSources(fixture, sources, diagnostics) {
  const adapters = Array.isArray(fixture?.adapters) ? fixture.adapters : []
  const byId = new Map(adapters.map((adapter) => [adapter?.id, adapter]))
  validateSourceContains(byId.get('schema-registry'), sources.schemaRegistryText, 'schema-registry', diagnostics, [
    'standard-test-data-package',
    'consumer-compatibility-suite',
    'consumer-compatibility-adapter-result',
    'consumer-compatibility-breaking-rule'
  ])
  validateSchemaRegistryTestDataFields(byId.get('schema-registry'), byId.get('standard-test-data-package'), fixture, sources.schemaRegistryText, diagnostics)
  validateSourceContains(byId.get('ai-context'), sources.aiContractsText, 'ai-context', diagnostics, [
    'ai-context-manifest',
    'ai-context-field-catalog',
    'ai-context-field-semantics',
    'ai-context-field-knowledge-cards',
    'ai-context-table-standards',
    '.dataspec/manifest.json'
  ])
  validateSourceContains(byId.get('cli-json'), sources.cliSourceText, 'cli-json', diagnostics, [
    'test-data generate',
    'consumer-compat check',
    'status',
    'diagnostics',
    'nextActions'
  ])
  validateSourceContains(byId.get('mcp-descriptors'), sources.mcpSourceText, 'mcp-descriptors', diagnostics, [
    'generate_test_data_package',
    'check_consumer_compatibility',
    'consumer-compatibility-suite',
    'schema-registry',
    'readOnly',
    'externalLlmUsed'
  ], 'ACTUAL_DESCRIPTOR_MISSING')
  validateCliMcpFixtureSource(byId.get('cli-mcp-contract-fixtures'), sources.cliMcpFixture, diagnostics)
  validateSourceContains(byId.get('standard-test-data-package'), `${sources.schemaRegistryText}\n${sources.cliSourceText}\n${sources.mcpSourceText}`, 'standard-test-data-package', diagnostics, [
    'standard-test-data-package',
    'testDataCases',
    'seedProfiles',
    'mockPayloads',
    'coverageReport',
    'containsRealBusinessRows'
  ])
}

function validateSchemaRegistryTestDataFields(schemaRegistryAdapter, testDataAdapter, fixture, sourceText, diagnostics) {
  if (!schemaRegistryAdapter || !testDataAdapter) {
    return
  }
  const text = typeof sourceText === 'string' ? sourceText : JSON.stringify(sourceText ?? '')
  const contractBlock = extractContractBlock(text, 'standard-test-data-package')
  const schemaBlock = extractNamedObjectSchema(contractBlock, STANDARD_TEST_DATA_SCHEMA_TITLE)
  if (!schemaBlock) {
    diagnostics.push(diagnosticOf('ACTUAL_SCHEMA_FIELD_MISSING', 'actual.schema-registry.standard-test-data-package', 'Schema Registry 缺少 standard-test-data-package JSON Schema。', {
      adapterId: 'schema-registry',
      contractId: 'standard-test-data-package',
      contractPath: 'standard-test-data-package',
      ruleId: 'stable-field-required',
      migrationHint: '恢复 standard-test-data-package schema，或补 schemaVersion 迁移说明。'
    }))
    return
  }

  const stableFields = collectTestDataStableFields(testDataAdapter, fixture)
  for (const stableField of stableFields) {
    if (!hasJavaSchemaPath(schemaBlock, stableField)) {
      const contractPath = `standard-test-data-package.${stableField}`
      diagnostics.push(diagnosticOf('ACTUAL_SCHEMA_FIELD_MISSING', `actual.schema-registry.${contractPath}`, `Schema Registry 的 standard-test-data-package 契约缺少 ${contractPath}。`, {
        adapterId: 'schema-registry',
        contractId: 'standard-test-data-package',
        contractPath,
        ruleId: 'stable-field-required',
        migrationHint: `恢复 ${contractPath} schema 字段，或补 schemaVersion 迁移说明。`
      }))
    }
  }
}

function collectTestDataStableFields(adapter, fixture) {
  const fields = Array.isArray(adapter?.stableFields) ? [...adapter.stableFields] : []
  const payloadFields = (Array.isArray(fixture?.goldenPayloads) ? fixture.goldenPayloads : [])
    .filter((item) => item?.adapterId === 'standard-test-data-package')
    .flatMap((item) => Array.isArray(item?.stableFields) ? item.stableFields : [])
  return [...new Set([...fields, ...payloadFields])]
    .map((item) => typeof item === 'string' ? item.replace(/\[\]/g, '') : '')
    .filter(Boolean)
}

function extractContractBlock(text, contractId) {
  const idIndex = text.indexOf(`"${contractId}"`)
  if (idIndex === -1) {
    return ''
  }
  const contractStart = text.lastIndexOf('contract(', idIndex)
  if (contractStart === -1) {
    return ''
  }
  return extractBalancedCall(text, contractStart)
}

function extractNamedObjectSchema(block, title) {
  if (!block) {
    return ''
  }
  const marker = `objectSchema("${title}"`
  const start = block.indexOf(marker)
  if (start === -1) {
    return ''
  }
  return extractBalancedCall(block, start)
}

function hasJavaSchemaPath(schemaBlock, stablePath) {
  if (!schemaBlock || !stablePath) {
    return false
  }
  let current = schemaBlock
  const segments = stablePath.split('.').map((item) => item.replace(/\[\]/g, '')).filter(Boolean)
  for (let index = 0; index < segments.length; index += 1) {
    const initializer = extractPropertyInitializer(current, segments[index])
    if (!initializer) {
      return false
    }
    if (index === segments.length - 1) {
      return true
    }
    current = extractNestedSchemaBlock(initializer)
    if (!current) {
      return false
    }
  }
  return true
}

function extractPropertyInitializer(block, propertyName) {
  const marker = `"${propertyName}"`
  let searchFrom = 0
  while (searchFrom < block.length) {
    const markerIndex = block.indexOf(marker, searchFrom)
    if (markerIndex === -1) {
      return ''
    }
    const afterMarker = markerIndex + marker.length
    const commaIndex = nextNonWhitespaceIndex(block, afterMarker)
    if (block[commaIndex] !== ',') {
      searchFrom = afterMarker
      continue
    }
    const valueStart = nextNonWhitespaceIndex(block, commaIndex + 1)
    const callName = /^[A-Za-z_][A-Za-z0-9_]*/.exec(block.slice(valueStart))
    if (!callName) {
      return block.slice(valueStart, Math.min(block.length, valueStart + 200))
    }
    const balanced = extractBalancedCall(block, valueStart)
    return balanced || block.slice(valueStart, Math.min(block.length, valueStart + 200))
  }
  return ''
}

function extractNestedSchemaBlock(initializer) {
  if (!initializer) {
    return ''
  }
  if (initializer.startsWith('objectSchema(') || initializer.startsWith('describedObjectSchema(')) {
    return initializer
  }
  const objectIndex = initializer.indexOf('objectSchema(')
  if (objectIndex !== -1) {
    return extractBalancedCall(initializer, objectIndex)
  }
  const describedObjectIndex = initializer.indexOf('describedObjectSchema(')
  if (describedObjectIndex !== -1) {
    return extractBalancedCall(initializer, describedObjectIndex)
  }
  return ''
}

function extractBalancedCall(text, callStart) {
  const parenStart = text.indexOf('(', callStart)
  if (parenStart === -1) {
    return ''
  }
  let depth = 0
  let inString = false
  let escaped = false
  for (let index = parenStart; index < text.length; index += 1) {
    const char = text[index]
    if (inString) {
      if (escaped) {
        escaped = false
      } else if (char === '\\') {
        escaped = true
      } else if (char === '"') {
        inString = false
      }
      continue
    }
    if (char === '"') {
      inString = true
      continue
    }
    if (char === '(') {
      depth += 1
    } else if (char === ')') {
      depth -= 1
      if (depth === 0) {
        return text.slice(callStart, index + 1)
      }
    }
  }
  return ''
}

function nextNonWhitespaceIndex(text, start) {
  let index = start
  while (index < text.length && /\s/.test(text[index])) {
    index += 1
  }
  return index
}

function validateSourceContains(adapter, sourceText, adapterId, diagnostics, markers, code = 'ACTUAL_CONTRACT_MISSING') {
  if (!adapter) {
    return
  }
  const text = typeof sourceText === 'string' ? sourceText : JSON.stringify(sourceText ?? '')
  for (const marker of markers) {
    if (!text.includes(marker)) {
      diagnostics.push(diagnosticOf(code, `actual.${adapterId}.${marker}`, `本地契约源缺少 ${marker}。`, {
        adapterId,
        contractId: firstContractRef(adapter),
        contractPath: marker,
        ruleId: 'stable-field-required',
        migrationHint: `恢复 ${adapterId} 的 ${marker} 契约，或补 schemaVersion 迁移说明。`
      }))
    }
  }
}

function validateCliMcpFixtureSource(adapter, fixture, diagnostics) {
  if (!adapter) {
    return
  }
  const requiredEntries = [
    ['cliCommands', entryMatches('id', 'test-data-generate'), 'test-data-generate'],
    ['cliCommands', entryMatches('id', 'consumer-compat-check'), 'consumer-compat-check'],
    ['mcpTools', entryMatches('name', 'generate_test_data_package'), 'generate_test_data_package'],
    ['mcpTools', entryMatches('name', 'check_consumer_compatibility'), 'check_consumer_compatibility'],
    ['mcpResources', entryContains('consumer-compatibility-suite', ['id', 'uri', 'uriTemplate', 'name']), 'consumer-compatibility-suite']
  ]
  for (const [collection, matcher, expected] of requiredEntries) {
    const items = Array.isArray(fixture?.[collection]) ? fixture[collection] : []
    if (!items.some((item) => matcher(item))) {
      diagnostics.push(diagnosticOf('ACTUAL_FIXTURE_ENTRY_MISSING', `actual.cli-mcp-contract-fixtures.${collection}.${expected}`, `CLI/MCP contract fixture 缺少 ${expected}。`, {
        adapterId: 'cli-mcp-contract-fixtures',
        contractId: 'cli-mcp-contract-fixtures',
        contractPath: `${collection}.${expected}`,
        ruleId: 'stable-field-required',
        migrationHint: `恢复 ${expected} fixture 条目并重新运行 CLI/MCP contract check。`
      }))
    }
  }
}

function entryMatches(key, expected) {
  return (item) => item?.[key] === expected
}

function entryContains(expected, keys) {
  return (item) => keys.some((key) => typeof item?.[key] === 'string' && item[key].includes(expected))
}

function buildAdapterResults(fixture, diagnostics) {
  const adapters = Array.isArray(fixture?.adapters) ? fixture.adapters : []
  const adapterIds = [...new Set([...REQUIRED_ADAPTERS, ...adapters.map((adapter) => adapter?.id).filter(Boolean)])]
  return adapterIds.map((adapterId) => {
    const adapter = adapters.find((item) => item?.id === adapterId) ?? {
      id: adapterId,
      required: true,
      contractRefs: [],
      stableFields: []
    }
    const index = adapters.findIndex((item) => item?.id === adapterId)
    const prefix = `adapters[${index}]`
    const relatedDiagnostics = diagnostics.filter((item) =>
      item.adapterId === adapterId || item.path?.startsWith(prefix) || item.path?.includes(adapterId))
    const missingStableFields = relatedDiagnostics
      .filter((item) => item.code?.includes('MISSING'))
      .map((item) => item.contractPath ?? item.path)
    return {
      adapterId: sanitizeText(adapterId),
      contractId: sanitizeText(firstContractRef(adapter)),
      status: relatedDiagnostics.length === 0 ? 'COMPATIBLE' : 'BREAKING',
      required: adapter?.required === true,
      contractRefs: Array.isArray(adapter?.contractRefs) ? adapter.contractRefs.map(sanitizeText) : [],
      stableFields: Array.isArray(adapter?.stableFields) ? adapter.stableFields.map(sanitizeText) : [],
      checkedStableFields: Array.isArray(adapter?.stableFields) ? adapter.stableFields.map(sanitizeText) : [],
      missingStableFields: missingStableFields.map(sanitizeText),
      typeMismatches: relatedDiagnostics
        .filter((item) => item.code?.includes('TYPE'))
        .map((item) => sanitizeText(item.message)),
      additiveFields: [],
      diagnostics: relatedDiagnostics.map((item) => ({
        code: item.code,
        path: item.path,
        message: item.message,
        ruleId: item.ruleId,
        migrationHint: item.migrationHint
      })),
      migrationHints: [...new Set(relatedDiagnostics.map((item) => item.migrationHint).filter(Boolean))].map(sanitizeText)
    }
  })
}

function scanSecrets(value, diagnostics) {
  walk(value, '$', (pathName, item) => {
    if (typeof item !== 'string') {
      return
    }
    if (SECRET_PATTERNS.some((pattern) => pattern.test(item))) {
      diagnostics.push(diagnosticOf('UNSAFE_FIXTURE_SECRET', pathName, 'fixture 包含敏感片段，已隐藏原值。'))
    }
  })
}

function hasPath(value, stablePath) {
  if (!stablePath || typeof stablePath !== 'string') {
    return false
  }
  const segments = stablePath.split('.')
  let current = value
  for (const segment of segments) {
    const key = segment.replace(/\[\]$/, '')
    if (Array.isArray(current)) {
      if (current.length === 0) {
        return false
      }
      current = current[0]
    }
    if (!current || typeof current !== 'object' || !(key in current) || current[key] === undefined) {
      return false
    }
    current = current[key]
  }
  return true
}

function walk(value, pathName, visitor) {
  visitor(pathName, value)
  if (Array.isArray(value)) {
    value.forEach((item, index) => walk(item, `${pathName}[${index}]`, visitor))
    return
  }
  if (value && typeof value === 'object') {
    Object.entries(value).forEach(([key, item]) => walk(item, `${pathName}.${key}`, visitor))
  }
}

function parseArgs(argv) {
  const options = { fixturePath: DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH, format: 'text', help: false }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--help' || arg === '-h') {
      options.help = true
    } else if (arg === '--fixture') {
      options.fixturePath = argv[++i]
    } else if (arg === '--format') {
      options.format = argv[++i]
    } else {
      throw new Error(`未知参数: ${sanitizeText(arg)}`)
    }
  }
  if (!['text', 'json'].includes(options.format)) {
    throw new Error('consumer-compat check 仅支持 --format text|json')
  }
  return options
}

function formatText(result) {
  const lines = [
    `Consumer compatibility: ${result.status}`,
    `adapters: ${result.summary.adapterCount}, goldenPayloads: ${result.summary.goldenPayloadCount}, diagnostics: ${result.summary.diagnostics}`
  ]
  for (const diagnostic of result.diagnostics) {
    lines.push(`- [${diagnostic.code}] ${diagnostic.path}: ${diagnostic.message}`)
  }
  return `${lines.join('\n')}\n`
}

function helpText() {
  return `Usage: node tools/dataspec-consumer-compat-check.mjs [--fixture <path>] [--format text|json]\n`
}

function diagnosticOf(code, pathName, message, extra = {}) {
  return {
    code,
    path: sanitizeText(pathName),
    message: sanitizeText(message),
    ...Object.fromEntries(Object.entries(extra).map(([key, value]) => [key, sanitizeDiagnosticValue(value)]))
  }
}

function sanitizeDiagnosticValue(value) {
  if (Array.isArray(value)) {
    return value.map(sanitizeDiagnosticValue)
  }
  return sanitizeText(value)
}

function firstContractRef(adapter) {
  return Array.isArray(adapter?.contractRefs) && adapter.contractRefs.length > 0 ? adapter.contractRefs[0] : null
}

function categoryForRule(ruleId) {
  if (ruleId === 'safety-metadata-required') {
    return 'SAFETY_METADATA_CHANGED'
  }
  if (ruleId === 'fixture-secret-free') {
    return 'SEMANTICS_CHANGED'
  }
  return 'REMOVED_FIELD'
}

function defaultMigrationHint(ruleId) {
  if (ruleId === 'safety-metadata-required') {
    return '恢复 read-only 和 secret-safety metadata，或补兼容迁移说明。'
  }
  if (ruleId === 'fixture-secret-free') {
    return '替换为脱敏占位符并重新运行 consumer compatibility check。'
  }
  return '保留 stable field，或提升 schemaVersion 并补迁移说明。'
}

function sanitizeText(value) {
  if (value == null) {
    return value
  }
  let text = String(value)
  for (const pattern of SECRET_PATTERNS) {
    text = text.replace(pattern, '[REDACTED]')
  }
  return text
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
  const code = await runConsumerCompatibilityCli()
  process.exitCode = code
}
