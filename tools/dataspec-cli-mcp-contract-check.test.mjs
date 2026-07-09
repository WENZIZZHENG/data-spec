import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { runCli } from './dataspec-cli.mjs'
import {
  DEFAULT_FIXTURE_PATH,
  loadContractFixtures,
  runContractCheckCli,
  validateContractFixtures
} from './dataspec-cli-mcp-contract-check.mjs'

test('bundled CLI/MCP contract fixtures validate against local MCP descriptors', async () => {
  const result = await validateContractFixtures({ fixturePath: DEFAULT_FIXTURE_PATH })

  assert.equal(result.ok, true)
  assert.deepEqual(result.diagnostics, [])
  assert.ok(result.summary.cliCommands >= 8)
  assert.ok(result.summary.mcpTools >= 8)
  assert.ok(result.summary.mcpResources >= 6)
  assert.ok(result.summary.mcpPrompts >= 3)
  assert.ok(result.summary.mcpResourceTemplates >= 1)
})

test('bundled fixtures include fixed SQL patch safety contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'fixed-sql-patch')

  assert.ok(command)
  assert.equal(command.command, 'fixed-sql patch --lint-result <json> --target <file.sql> --format json')
  assert.ok(command.outputShape.includes('planHash'))
  assert.ok(command.outputShape.includes('unifiedDiff'))
  assert.ok(command.outputShape.includes('lintOriginalSha256'))
  assert.ok(command.outputShape.includes('dryRunResult.status'))
  assert.ok(command.outputShape.includes('safety.requiresExplicitConfirmation'))
  assert.equal(command.safety.readOnly, false)
  assert.equal(command.safety.requiresDryRun, true)
  assert.equal(command.safety.requiresExplicitConfirmation, true)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.ok(command.recommendedNextActions.some((item) => item.includes('confirm')))
})

test('bundled fixtures include schema plan readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'schema-plan')

  assert.ok(command)
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.requiresDryRun, true)
  assert.ok(command.outputShape.includes('currentSchemaHash'))
  assert.ok(command.outputShape.includes('targetSpecHash'))
  assert.ok(command.outputShape.includes('blockedReasons[]'))
  assert.ok(command.recommendedNextActions.some((item) => item.includes('password-env')))
})

test('bundled fixtures include index-refs readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'index-refs')

  assert.ok(command)
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.ok(command.outputShape.includes('references[]'))
  assert.ok(command.outputShape.includes('references[].fieldName'))
  assert.ok(command.outputShape.includes('references[].snippet'))
  assert.ok(command.outputShape.includes('renameRisk'))
  assert.ok(command.successExample.command.includes('phone=mobile_phone'))
  assert.equal(command.failureExample.diagnostic.code, 'DATASPEC_DEFAULT_PATHS_MISSING')
  assert.ok(command.recommendedNextActions.some((item) => item.includes('defaultPaths') || item.includes('--path')))
})

test('bundled fixtures include code-patch plan readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'code-patch-plan')

  assert.ok(command)
  assert.equal(command.command, 'code-patch plan --field <name> --to-field <new> --format json')
  assert.ok(command.outputShape.includes('candidateEdits[]'))
  assert.ok(command.outputShape.includes('candidateEdits[].id'))
  assert.ok(command.outputShape.includes('candidateEdits[].changeType'))
  assert.ok(command.outputShape.includes('candidateEdits[].reference'))
  assert.ok(command.outputShape.includes('candidateEdits[].riskLevel'))
  assert.ok(command.outputShape.includes('candidateEdits[].confidence'))
  assert.ok(command.outputShape.includes('candidateEdits[].requiresHumanReview'))
  assert.ok(command.outputShape.includes('candidateEdits[].reason'))
  assert.ok(command.outputShape.includes('manualSteps[]'))
  assert.ok(command.outputShape.includes('dryRunResult.willWrite'))
  assert.ok(command.outputShape.includes('safety.readOnly'))
  assert.ok(command.outputShape.includes('safety.writesProject'))
  assert.ok(command.outputShape.includes('safety.requiresDryRun'))
  assert.ok(command.outputShape.includes('safety.externalNetworkUsed'))
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.requiresDryRun, true)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.equal(command.safety.externalNetworkUsed, false)
  assert.equal(command.safety.externalLlmUsed, false)
  assert.equal(command.failureExample.diagnostic.code, 'DATASPEC_DEFAULT_PATHS_MISSING')
  assert.ok(command.recommendedNextActions.some((item) => item.includes('review') || item.includes('人工')))
})

test('bundled fixtures include context-budget plan readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'context-budget-plan')

  assert.ok(command)
  assert.equal(command.command, 'context-budget plan --project <id> --token-budget <n> --format json')
  assert.deepEqual(command.requiredOptions, ['project', 'token-budget'])
  assert.ok(command.optionalOptions.includes('target-table'))
  assert.ok(command.optionalOptions.includes('target-file'))
  assert.ok(command.outputShape.includes('selectedArtifacts[]'))
  assert.ok(command.outputShape.includes('recommendedNextActions[]'))
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.ok(command.recommendedNextActions.some((item) => item.includes('tokenBudget') || item.includes('query')))
})

test('bundled fixtures include context-quality check readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'context-quality-check')

  assert.ok(command)
  assert.equal(command.command, 'context-quality check --context-dir <dir> --format json')
  assert.deepEqual(command.requiredOptions, [])
  assert.deepEqual(command.oneOfRequiredOptions, ['context-dir', 'context-zip', 'budget-plan'])
  assert.ok(command.optionalOptions.includes('context-dir'))
  assert.ok(command.optionalOptions.includes('context-zip'))
  assert.ok(command.optionalOptions.includes('budget-plan'))
  assert.ok(command.outputShape.includes('contextQualityScore'))
  assert.ok(command.outputShape.includes('missingCriticalResources[]'))
  assert.ok(command.outputShape.includes('nextContextActions[]'))
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.match(command.failureExample.diagnostic.message, /context-dir/)
  assert.ok(command.recommendedNextActions.some((item) => item.includes('budget-plan') || item.includes('export-context')))
})

test('bundled fixtures include synthetic examples readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'synthetic-examples-generate')

  assert.ok(command)
  assert.equal(command.command, 'synthetic-examples generate --project <id> --scenario <scenario> --format json')
  assert.deepEqual(command.requiredOptions, ['project', 'scenario'])
  assert.ok(command.optionalOptions.includes('max-cases'))
  assert.ok(command.outputShape.includes('specHash'))
  assert.ok(command.outputShape.includes('goodSql[]'))
  assert.ok(command.outputShape.includes('badSql[]'))
  assert.ok(command.outputShape.includes('fieldSuggestionQuestions[]'))
  assert.ok(command.outputShape.includes('standardQaCases[]'))
  assert.ok(command.outputShape.includes('safety.containsRealBusinessRows'))
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.containsRealBusinessRows, false)
  assert.equal(command.safety.externalLlmUsed, false)
  assert.ok(command.recommendedNextActions.some((item) => item.includes('usage example') || item.includes('Prompt')))
})

test('bundled fixtures include contract import preview readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'contract-import-preview')

  assert.ok(command)
  assert.equal(command.command, 'contract-import preview --project <id> --source-kind <sourceKind> --input <path> --format json')
  assert.deepEqual(command.requiredOptions, ['project', 'source-kind', 'input'])
  assert.ok(command.optionalOptions.includes('max-candidates'))
  assert.ok(command.outputShape.includes('contractHash'))
  assert.ok(command.outputShape.includes('candidateFields[]'))
  assert.ok(command.outputShape.includes('candidateFields[].candidateKey'))
  assert.ok(command.outputShape.includes('candidateFields[].displayName'))
  assert.ok(command.outputShape.includes('candidateFields[].required'))
  assert.ok(command.outputShape.includes('candidateFields[].enumValues[]'))
  assert.ok(command.outputShape.includes('candidateFields[].exampleValues[]'))
  assert.ok(command.outputShape.includes('candidateFields[].schemaVersion'))
  assert.ok(command.outputShape.includes('candidateFields[].confidence'))
  assert.ok(command.outputShape.includes('candidateFields[].inboxPayload'))
  assert.ok(command.outputShape.includes('safety.containsRealBusinessRows'))
  assert.ok(command.outputShape.includes('safety.externalNetworkUsed'))
  assert.equal(command.successExample.output.candidateFields[0].candidateKey, 'openapi:order_id')
  assert.equal(command.successExample.output.candidateFields[0].displayName, '订单ID')
  assert.equal(command.successExample.output.candidateFields[0].required, true)
  assert.deepEqual(command.successExample.output.candidateFields[0].enumValues, [])
  assert.deepEqual(command.successExample.output.candidateFields[0].exampleValues, ['1001'])
  assert.equal(command.successExample.output.candidateFields[0].schemaVersion, 1)
  assert.equal(command.successExample.output.candidateFields[0].confidence, 82)
  assert.equal(command.safety.readOnly, true)
  assert.equal(command.safety.writesProject, false)
  assert.equal(command.safety.containsRealBusinessRows, false)
  assert.equal(command.safety.externalNetworkUsed, false)
  assert.equal(command.safety.externalLlmUsed, false)
  assert.ok(command.recommendedNextActions.some((item) => item.includes('inboxPayload')))
})

test('bundled fixtures include install-hook local write contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'install-hook')

  assert.ok(command)
  assert.equal(command.command, 'install-hook --hook pre-commit --format json')
  assert.deepEqual(command.requiredOptions, [])
  assert.ok(command.optionalOptions.includes('with-vscode'))
  assert.ok(command.outputShape.includes('writtenFiles[]'))
  assert.ok(command.outputShape.includes('skippedFiles[]'))
  assert.ok(command.outputShape.includes('safety.overwritesUnmanagedFiles'))
  assert.equal(command.safety.readOnly, false)
  assert.equal(command.safety.writesProject, true)
  assert.equal(command.safety.overwritesUnmanagedFiles, false)
  assert.equal(command.safety.requiresIdempotencyKey, false)
  assert.equal(command.failureExample.diagnostic.code, 'HOOK_EXISTS_UNMANAGED')
  assert.ok(command.recommendedNextActions.some((item) => item.includes('lint-changed')))
})

test('bundled fixtures include MCP session state readonly contract', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const tool = fixture.mcpTools.find((item) => item.name === 'get_session_state')
  const resource = fixture.mcpResources.find((item) => item.uri === 'dataspec://project/<projectId>/session-state')
  const template = fixture.mcpResourceTemplates.find((item) =>
    item.uriTemplate === 'dataspec://project/{projectId}/session-state')

  assert.ok(tool)
  assert.equal(tool.safety.readOnly, true)
  assert.equal(tool.safety.writesProject, false)
  assert.ok(tool.inputProperties.includes('projectId'))
  assert.ok(tool.outputShape.includes('structuredContent.currentProject'))
  assert.ok(tool.outputShape.includes('structuredContent.redactedMemory'))
  assert.ok(tool.outputShape.includes('structuredContent.safeDefaults.sessionStateIsAuthorization'))
  assert.ok(tool.recommendedNextActions.some((item) => item.includes('get_session_bootstrap')))

  assert.ok(resource)
  assert.equal(resource.name, 'DataSpec MCP Session State')
  assert.equal(resource.mimeType, 'application/json')
  assert.equal(resource.safety.readOnly, true)
  assert.equal(resource.safety.writesProject, false)
  assert.ok(resource.outputShape.includes('currentProject'))
  assert.ok(resource.outputShape.includes('redactedMemory'))

  assert.ok(template)
  assert.equal(template.mimeType, 'application/json')
})

test('fixed SQL patch fixture matches actual dry-run json shape', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'fixed-sql-patch')
  const tempDir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-fixture-'))
  try {
    const originalSql = 'CREATE TABLE UserOrder (id bigint);\n'
    const fixedSql = 'CREATE TABLE user_order (id bigint);\n'
    await writeFile(path.join(tempDir, 'bad.sql'), originalSql, 'utf8')
    await writeFile(path.join(tempDir, 'lint-result.json'), JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')
    const io = createIo()
    io.cwd = () => tempDir

    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'bad.sql',
      '--format',
      'json'
    ], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.kind, 'dataspec.fixed-sql.patch-plan')
    assert.equal(output.dryRunResult.status, 'READY')
    assert.equal(output.safety.requiresExplicitConfirmation, true)
    assert.equal(typeof output.lintOriginalSha256, 'string')
    assert.equal(typeof output.planHash, 'string')
    assert.ok(command.outputShape.every((field) => hasFixtureShapeField(output, field)))
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
})

test('code-patch plan fixture matches actual dry-run json shape', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'code-patch-plan')
  const tempDir = await mkdtemp(path.join(tmpdir(), 'dataspec-code-patch-fixture-'))
  try {
    await mkdir(path.join(tempDir, 'src'), { recursive: true })
    await writeFile(path.join(tempDir, 'src', 'User.java'), 'class User { String phone; }\n', 'utf8')
    const io = createIo()
    io.cwd = () => tempDir

    const code = await runCli([
      'code-patch',
      'plan',
      '--field',
      'phone',
      '--to-field',
      'mobile_phone',
      '--path',
      'src',
      '--format',
      'json'
    ], io)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    for (const shape of command.outputShape) {
      assert.ok(hasFixtureShapeField(output, shape), `missing output shape: ${shape}`)
    }
    assert.equal(output.kind, 'dataspec.code-field.patch-plan')
    assert.equal(output.safety.readOnly, true)
    assert.equal(output.safety.writesProject, false)
    assert.equal(output.dryRunResult.willWrite, false)
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
})

test('fixture checker reports missing MCP tool and safety metadata drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpTools = fixture.mcpTools
    .filter((tool) => tool.name !== 'lint_sql')
    .map((tool) => tool.name === 'search_fields'
      ? { ...tool, safety: { ...tool.safety, readOnly: false } }
      : tool)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MISSING_REQUIRED_MCP_TOOL'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_TOOL_SAFETY_MISMATCH'))
})

test('fixture checker reports MCP resource and prompt descriptor drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpResources = fixture.mcpResources.map((resource) => resource.uri === 'dataspec://version-compatibility'
    ? { ...resource, name: 'Drifted Name', description: 'drifted description', mimeType: 'text/plain' }
    : resource)
  fixture.mcpPrompts = fixture.mcpPrompts.map((prompt) => prompt.name === 'dataspec_create_table'
    ? { ...prompt, description: 'drifted description' }
    : prompt)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_RESOURCE_NAME_MISMATCH'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_RESOURCE_DESCRIPTION_MISMATCH'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_RESOURCE_MIME_TYPE_MISMATCH'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_PROMPT_DESCRIPTION_MISMATCH'))
})

test('fixture checker reports MCP resource template descriptor drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpResourceTemplates = fixture.mcpResourceTemplates.map((template) =>
    template.uriTemplate === 'dataspec://project/{projectId}/agent-guidance-pack'
      ? { ...template, description: 'drifted description' }
      : template)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_RESOURCE_TEMPLATE_DESCRIPTION_MISMATCH'))
})

test('fixture checker reports MCP first-class prompt guidance drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpPrompts = fixture.mcpPrompts.map((prompt) =>
    prompt.name === 'answer_field_standard_question'
      ? { ...prompt, recommendedNextActions: ['drifted next action'] }
      : prompt)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_PROMPT_RECOMMENDED_NEXT_ACTIONS_MISMATCH'))
})

test('fixture checker reports MCP first-class prompt argument and safety drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpPrompts = fixture.mcpPrompts.map((prompt) =>
    prompt.name === 'create_table_with_dataspec'
      ? {
          ...prompt,
          arguments: prompt.arguments.map((argument) =>
            argument.name === 'businessDescription'
              ? { ...argument, required: true }
              : argument),
          safety: { ...prompt.safety, writesProject: true }
        }
      : prompt)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_PROMPT_ARGUMENT_REQUIRED_MISMATCH'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_PROMPT_SAFETY_MISMATCH'))
})

test('fixture checker reports MCP first-class prompt full guidance drift', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpPrompts = fixture.mcpPrompts.map((prompt) =>
    prompt.name === 'review_sql_with_dataspec'
      ? {
          ...prompt,
          dataspecGuidance: {
            ...prompt.dataspecGuidance,
            safeDefaults: { ...prompt.dataspecGuidance.safeDefaults, executeWorkflow: true },
            toolSequence: ['search_fields'],
            stopConditions: ['drifted stop condition'],
            evidenceRequirements: ['drifted evidence requirement']
          }
        }
      : prompt)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) =>
    diagnostic.code === 'MCP_PROMPT_GUIDANCE_MISMATCH' && diagnostic.path.includes('safeDefaults')))
  assert.ok(result.diagnostics.some((diagnostic) =>
    diagnostic.code === 'MCP_PROMPT_GUIDANCE_MISMATCH' && diagnostic.path.includes('toolSequence')))
  assert.ok(result.diagnostics.some((diagnostic) =>
    diagnostic.code === 'MCP_PROMPT_GUIDANCE_MISMATCH' && diagnostic.path.includes('stopConditions')))
  assert.ok(result.diagnostics.some((diagnostic) =>
    diagnostic.code === 'MCP_PROMPT_GUIDANCE_MISMATCH' && diagnostic.path.includes('evidenceRequirements')))
})

test('fixture checker rejects incomplete stable fixture entries', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.cliCommands[0] = {
    ...fixture.cliCommands[0],
    outputShape: []
  }
  delete fixture.cliCommands[0].failureExample
  fixture.mcpResources[0] = {
    ...fixture.mcpResources[0],
    description: '',
    outputShape: []
  }
  delete fixture.mcpResources[0].successExample
  delete fixture.mcpResources[0].safety
  fixture.mcpPrompts[0] = {
    ...fixture.mcpPrompts[0],
    outputShape: []
  }
  delete fixture.mcpPrompts[0].successExample
  delete fixture.mcpPrompts[0].safety

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'cliCommands[0].outputShape'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'cliCommands[0].failureExample'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpResources[0].description'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpResources[0].successExample'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpResources[0].safety'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpPrompts[0].outputShape'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpPrompts[0].successExample'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpPrompts[0].safety'))
})

test('fixture checker rejects incomplete MCP tool entries', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.mcpTools[0] = {
    ...fixture.mcpTools[0],
    description: '',
    outputShape: []
  }
  delete fixture.mcpTools[0].failureExample
  delete fixture.mcpTools[0].recommendedNextActions

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpTools[0].description'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpTools[0].outputShape'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpTools[0].failureExample'))
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.path === 'mcpTools[0].recommendedNextActions'))
})

test('fixture checker rejects raw secret-like examples', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  fixture.cliCommands[0].successExample.command = 'node tools/dataspec-cli.mjs doctor --server https://user:pass@dataspec.local'

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'SECRET_LIKE_VALUE'))
})

test('fixture checker rejects unsafe context-budget examples', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'context-budget-plan') ?? fixture.cliCommands[0]
  command.successExample.command = 'node tools/dataspec-cli.mjs context-budget plan --project 1 --token-budget 2400 --query "Authorization: Bearer raw-secret" --format json'

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'SECRET_LIKE_VALUE'))
})

test('fixture checker rejects unsafe context-quality examples', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'context-quality-check') ?? fixture.cliCommands[0]
  command.successExample.output.nextContextActions = ['检查 Authorization: Bearer raw-secret 后重新导出。']

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'SECRET_LIKE_VALUE'))
})

test('fixture checker rejects unsafe contract import examples', async () => {
  const fixture = await loadContractFixtures(DEFAULT_FIXTURE_PATH)
  const command = fixture.cliCommands.find((item) => item.id === 'contract-import-preview') ?? fixture.cliCommands[0]
  command.successExample.output.candidateFields[0].displayName = '订单 Authorization: Bearer raw-secret'

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'SECRET_LIKE_VALUE'))
})

test('contract check cli prints json diagnostics for invalid fixture', async () => {
  const tempDir = await mkdtemp(path.join(tmpdir(), 'dataspec-contract-fixture-'))
  try {
    const invalidFixture = path.join(tempDir, 'invalid.json')
    await writeFile(invalidFixture, JSON.stringify({
      kind: 'dataspec-cli-mcp-contract-fixtures',
      schemaVersion: 1,
      cliCommands: [],
      mcpTools: [],
      mcpResources: [],
      mcpPrompts: [],
      mcpResourceTemplates: []
    }), 'utf8')
    const io = createIo()

    const code = await runContractCheckCli(['--fixture', invalidFixture, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 1)
    assert.equal(output.ok, false)
    assert.ok(output.diagnostics.some((diagnostic) => diagnostic.code === 'MISSING_REQUIRED_CLI_COMMAND'))
  } finally {
    await rm(tempDir, { recursive: true, force: true })
  }
})

test('contract check cli rejects option-like values', async () => {
  const fixtureIo = createIo()
  const fixtureCode = await runContractCheckCli(['--fixture', '-h'], fixtureIo)
  const formatIo = createIo()
  const formatCode = await runContractCheckCli(['--format', '--fixture', DEFAULT_FIXTURE_PATH], formatIo)

  assert.equal(fixtureCode, 2)
  assert.equal(formatCode, 2)
  assert.equal(fixtureIo.stdout, '')
  assert.equal(formatIo.stdout, '')

  const fixtureDiagnostic = JSON.parse(fixtureIo.stderr)
  const formatDiagnostic = JSON.parse(formatIo.stderr)
  assert.equal(fixtureDiagnostic.diagnostics[0].code, 'CHECK_FAILED')
  assert.equal(fixtureDiagnostic.diagnostics[0].path, 'cli')
  assert.equal(fixtureDiagnostic.diagnostics[0].message, '--fixture 需要参数值')
  assert.equal(formatDiagnostic.diagnostics[0].code, 'CHECK_FAILED')
  assert.equal(formatDiagnostic.diagnostics[0].path, 'cli')
  assert.equal(formatDiagnostic.diagnostics[0].message, '--format 需要参数值')
})

function createIo() {
  return {
    stdout: '',
    stderr: '',
    cwd: () => process.cwd(),
    writeOut(text) {
      this.stdout += text
    },
    writeErr(text) {
      this.stderr += text
    }
  }
}

function hasFixtureShapeField(output, field) {
  return hasFixtureShapePath(output, field.split('.'))
}

function hasFixtureShapePath(value, parts) {
  if (parts.length === 0) {
    return value !== undefined
  }
  const [part, ...rest] = parts
  if (part.endsWith('[]')) {
    const arrayValue = value?.[part.slice(0, -2)]
    if (!Array.isArray(arrayValue)) {
      return false
    }
    return rest.length === 0 || arrayValue.some((item) => hasFixtureShapePath(item, rest))
  }
  return hasFixtureShapePath(value?.[part], rest)
}
