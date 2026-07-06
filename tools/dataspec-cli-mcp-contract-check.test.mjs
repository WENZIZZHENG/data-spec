import assert from 'node:assert/strict'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
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
    ? { ...resource, description: 'drifted description' }
    : resource)
  fixture.mcpPrompts = fixture.mcpPrompts.map((prompt) => prompt.name === 'dataspec_create_table'
    ? { ...prompt, description: 'drifted description' }
    : prompt)

  const result = await validateContractFixtures({ fixture })

  assert.equal(result.ok, false)
  assert.ok(result.diagnostics.some((diagnostic) => diagnostic.code === 'MCP_RESOURCE_DESCRIPTION_MISMATCH'))
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
  fixture.cliCommands[0].successExample.command = 'node tools/dataspec-cli.mjs doctor --dataspec-token raw-secret'

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
  return field
    .replace(/\[\]/g, '')
    .split('.')
    .every((part, index, parts) => {
      const target = parts.slice(0, index + 1).reduce((value, key) => value?.[key], output)
      return target !== undefined
    })
}
