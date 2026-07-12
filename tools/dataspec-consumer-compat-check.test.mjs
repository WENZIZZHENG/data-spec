import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH,
  loadConsumerCompatibilitySuite,
  runConsumerCompatibilityCli,
  validateConsumerCompatibilitySuite
} from './dataspec-consumer-compat-check.mjs'

test('bundled consumer compatibility suite validates required adapters', async () => {
  const result = await validateConsumerCompatibilitySuite({ fixturePath: DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH })

  assert.equal(result.ok, true)
  assert.equal(result.status, 'COMPATIBLE')
  assert.match(result.checkedAt, /^\d{4}-\d{2}-\d{2}T/)
  assert.deepEqual(result.diagnostics, [])
  assert.ok(result.summary.adapterCount >= 6)
  assert.ok(result.adapterResults.some((item) => item.adapterId === 'schema-registry'))
  assert.ok(result.adapterResults.some((item) => item.adapterId === 'standard-test-data-package'))
})

test('bundled consumer compatibility suite documents test data golden payload', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const payload = suite.goldenPayloads.find((item) => item.id === 'standard-test-data-package-minimal')

  assert.ok(payload)
  assert.equal(payload.adapterId, 'standard-test-data-package')
  assert.ok(payload.stableFields.includes('kind'))
  assert.ok(payload.stableFields.includes('testDataCases'))
  assert.ok(payload.payload.safety.readOnly)
  assert.equal(payload.payload.safety.containsRealBusinessRows, false)
})

test('validator reports missing required adapter as breaking', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const result = await validateConsumerCompatibilitySuite({
    fixture: {
      ...suite,
      adapters: suite.adapters.filter((item) => item.id !== 'mcp-descriptors')
    }
  })

  assert.equal(result.ok, false)
  assert.equal(result.status, 'BREAKING')
  assert.ok(result.diagnostics.some((item) => item.code === 'REQUIRED_ADAPTER_MISSING' && item.path.includes('mcp-descriptors')))
})

test('validator reports missing stable field while allowing additive fields', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const additive = {
    ...suite,
    goldenPayloads: suite.goldenPayloads.map((item) => item.id === 'standard-test-data-package-minimal'
      ? { ...item, payload: { ...item.payload, futureOptionalField: true } }
      : item)
  }
  const additiveResult = await validateConsumerCompatibilitySuite({ fixture: additive })

  assert.equal(additiveResult.ok, true)

  const breaking = {
    ...suite,
    goldenPayloads: suite.goldenPayloads.map((item) => item.id === 'standard-test-data-package-minimal'
      ? { ...item, payload: { ...item.payload, specHash: undefined } }
      : item)
  }
  const breakingResult = await validateConsumerCompatibilitySuite({ fixture: breaking })

  assert.equal(breakingResult.ok, false)
  assert.ok(breakingResult.diagnostics.some((item) =>
    item.code === 'STABLE_FIELD_MISSING' && item.path.includes('specHash')))
})

test('validator protects documented test data case and safety stable fields', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const breaking = {
    ...suite,
    goldenPayloads: suite.goldenPayloads.map((item) => {
      if (item.id !== 'standard-test-data-package-minimal') {
        return item
      }
      const [firstCase, ...restCases] = item.payload.testDataCases
      const { value: _removedValue, ...caseWithoutValue } = firstCase
      const { readOnly: _removedReadOnly, ...safetyWithoutReadOnly } = item.payload.safety
      return {
        ...item,
        payload: {
          ...item.payload,
          testDataCases: [caseWithoutValue, ...restCases],
          safety: safetyWithoutReadOnly
        }
      }
    })
  }

  const result = await validateConsumerCompatibilitySuite({ fixture: breaking })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'STABLE_FIELD_MISSING' && item.contractPath === 'testDataCases.value'))
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'STABLE_FIELD_MISSING' && item.contractPath === 'safety.readOnly'))
})

test('validator compares local contract sources instead of only fixture self shape', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const result = await validateConsumerCompatibilitySuite({
    fixture: suite,
    actualSources: {
      schemaRegistryText: 'standard-test-data-package only',
      mcpSourceText: 'generate_test_data_package only',
      cliSourceText: 'test-data generate only',
      aiContractsText: 'ai-context-package',
      cliMcpFixture: {
        cliCommands: [],
        mcpTools: [],
        mcpResources: [],
        mcpPrompts: []
      }
    }
  })

  assert.equal(result.ok, false)
  assert.equal(result.status, 'BREAKING')
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'ACTUAL_CONTRACT_MISSING' && item.adapterId === 'schema-registry'))
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'ACTUAL_DESCRIPTOR_MISSING' && item.adapterId === 'mcp-descriptors'))
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'ACTUAL_FIXTURE_ENTRY_MISSING' && item.adapterId === 'cli-mcp-contract-fixtures'))
})

test('validator reports schema registry stable field drift for nested test data package fields', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const result = await validateConsumerCompatibilitySuite({
    fixture: suite,
    actualSources: {
      schemaRegistryText: `
        contract(
          "standard-test-data-package",
          "标准测试数据包",
          "schema drift fixture",
          List.of(),
          List.of(),
          objectSchema("DataSpec Standard Test Data Package", List.of("kind", "schemaVersion", "projectId", "specHash"), orderedMap(
            "kind", describedStringProp("kind"),
            "schemaVersion", describedIntegerProp("version"),
            "projectId", describedIntegerProp("project"),
            "specHash", describedStringProp("hash"),
            "generationParams", objectSchema("Test Data Generation Params", List.of(), orderedMap(
              "fieldNames", arrayOf(describedStringProp("field")),
              "objectScenario", describedStringProp("scenario"),
              "maxFields", describedIntegerProp("max"),
              "casesPerField", describedIntegerProp("cases"),
              "seedRowCount", describedIntegerProp("rows"),
              "dialect", describedStringProp("dialect")
            )),
            "sourceSummary", objectSchema("Test Data Source Summary", List.of(), orderedMap(
              "standardFieldCount", describedIntegerProp("total"),
              "selectedFieldCount", describedIntegerProp("selected"),
              "enumValueCount", describedIntegerProp("enum"),
              "fallbackUsed", describedBooleanProp("fallback"),
              "selectedFields", arrayOf(describedStringProp("field")),
              "sourceKinds", arrayOf(describedStringProp("source"))
            )),
            "testDataCases", arrayOf(objectSchema("Standard Test Data Case", List.of("caseId", "fieldName", "caseType", "expectedValidity"), orderedMap(
              "caseId", describedStringProp("case"),
              "fieldName", describedStringProp("field"),
              "caseType", describedStringProp("type"),
              "expectedValidity", describedBooleanProp("valid")
            ))),
            "seedProfiles", arrayOf(objectSchema("Standard Test Data Seed Profile", List.of("profileId", "format"), orderedMap(
              "profileId", describedStringProp("profile"),
              "format", describedStringProp("format"),
              "dialect", describedStringProp("dialect"),
              "content", describedStringProp("content"),
              "fieldNames", arrayOf(describedStringProp("field")),
              "sourceCaseIds", arrayOf(describedStringProp("case")),
              "executable", describedBooleanProp("executable"),
              "requiresReview", describedBooleanProp("review")
            ))),
            "payloadId", describedStringProp("wrong nesting"),
            "coverageReport", objectSchema("Standard Test Data Coverage Report", List.of(), orderedMap(
              "selectedFieldCount", describedIntegerProp("selected"),
              "coveredFieldCount", describedIntegerProp("covered"),
              "caseCount", describedIntegerProp("cases"),
              "coverageLevel", describedStringProp("level")
            )),
            "safety", objectSchema("Test Data Safety Metadata", List.of(), orderedMap(
              "containsRealBusinessRows", describedBooleanProp("real")
            )),
            "nextActions", arrayOf(describedStringProp("next"))
          ))
        )
        "consumer-compatibility-suite"
        "consumer-compatibility-adapter-result"
        "consumer-compatibility-breaking-rule"
      `,
      mcpSourceText: 'generate_test_data_package check_consumer_compatibility consumer-compatibility-suite schema-registry readOnly externalLlmUsed',
      cliSourceText: 'test-data generate consumer-compat check status diagnostics nextActions',
      aiContractsText: 'ai-context-manifest ai-context-field-catalog ai-context-field-semantics ai-context-field-knowledge-cards ai-context-table-standards .dataspec/manifest.json',
      cliMcpFixture: {
        cliCommands: [{ id: 'test-data-generate' }, { id: 'consumer-compat-check' }],
        mcpTools: [{ name: 'generate_test_data_package' }, { name: 'check_consumer_compatibility' }],
        mcpResources: [{ uri: 'dataspec://project/<projectId>/consumer-compatibility-suite' }],
        mcpPrompts: []
      }
    }
  })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((item) =>
    item.code === 'ACTUAL_SCHEMA_FIELD_MISSING' &&
    item.adapterId === 'schema-registry' &&
    item.contractPath === 'standard-test-data-package.mockPayloads.payloadId'))
})

test('adapter results and breaking rules expose stable schema fields', async () => {
  const result = await validateConsumerCompatibilitySuite({ fixturePath: DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH })
  const schemaAdapter = result.adapterResults.find((item) => item.adapterId === 'schema-registry')

  assert.ok(schemaAdapter)
  assert.ok(Array.isArray(schemaAdapter.checkedStableFields))
  assert.ok(Array.isArray(schemaAdapter.missingStableFields))
  assert.ok(Array.isArray(schemaAdapter.typeMismatches))
  assert.ok(Array.isArray(schemaAdapter.additiveFields))
  assert.ok(Array.isArray(schemaAdapter.migrationHints))
  assert.equal(result.safety.readOnly, true)
  assert.equal(result.safety.requiresServer, false)
  assert.ok(result.breakingRules.every((item) => item.ruleId && item.migrationHint))
})

test('validator rejects unsafe fixture examples without echoing the secret', async () => {
  const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
  const result = await validateConsumerCompatibilitySuite({
    fixture: {
      ...suite,
      goldenPayloads: [
        ...suite.goldenPayloads,
        {
          id: 'unsafe',
          adapterId: 'cli-json',
          contractId: 'unsafe',
          stableFields: ['kind'],
          payload: { kind: 'unsafe', command: 'Authorization: Bearer raw-secret-token' }
        }
      ]
    }
  })

  const serialized = JSON.stringify(result)
  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((item) => item.code === 'UNSAFE_FIXTURE_SECRET'))
  assert.doesNotMatch(serialized, /raw-secret-token/)
})

test('consumer compatibility CLI returns stable exit codes', async () => {
  const tempDir = await mkdtemp(path.join(tmpdir(), 'dataspec-consumer-compat-'))
  try {
    const suite = await loadConsumerCompatibilitySuite(DEFAULT_CONSUMER_COMPAT_FIXTURE_PATH)
    const fixturePath = path.join(tempDir, 'suite.json')
    await writeFile(fixturePath, JSON.stringify({
      ...suite,
      adapters: suite.adapters.filter((item) => item.id !== 'schema-registry')
    }), 'utf8')
    const io = { stdout: '', stderr: '', writeOut(text) { this.stdout += text }, writeErr(text) { this.stderr += text } }

    const code = await runConsumerCompatibilityCli(['--fixture', fixturePath, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 1)
    assert.equal(output.status, 'BREAKING')
    assert.ok(output.diagnostics.some((item) => item.code === 'REQUIRED_ADAPTER_MISSING'))
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
})

test('consumer compatibility script invocation prints stable JSON', async () => {
  const { code, stdout, stderr } = await runNodeScript(['--format', 'json'])

  assert.equal(code, 0)
  assert.equal(stderr, '')
  const output = JSON.parse(stdout)
  assert.equal(output.kind, 'dataspec.consumer-compatibility-suite.check')
  assert.equal(output.ok, true)
  assert.equal(output.status, 'COMPATIBLE')
  assert.ok(output.adapterResults.some((item) => item.adapterId === 'standard-test-data-package'))
})

function runNodeScript(args) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [fileURLToPath(new URL('./dataspec-consumer-compat-check.mjs', import.meta.url)), ...args], {
      cwd: fileURLToPath(new URL('.', import.meta.url)),
      shell: false,
      stdio: ['ignore', 'pipe', 'pipe']
    })
    const stdoutChunks = []
    const stderrChunks = []
    child.stdout.on('data', (chunk) => stdoutChunks.push(Buffer.from(chunk)))
    child.stderr.on('data', (chunk) => stderrChunks.push(Buffer.from(chunk)))
    child.on('error', reject)
    child.on('close', (code) => resolve({
      code,
      stdout: Buffer.concat(stdoutChunks).toString('utf8'),
      stderr: Buffer.concat(stderrChunks).toString('utf8')
    }))
  })
}
