import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildValidationAdvice,
  collectChangedPathsFromGitOutput,
  formatValidationAdviceText,
  runAdvisorCli
} from './dataspec-verify-advisor.mjs'

test('recommends backend, openspec, docs and diff checks from paths', () => {
  const advice = buildValidationAdvice([
    'dataspec-server/src/main/java/com/dataspec/lint/SqlLintService.java',
    'openspec/changes/add-x/specs/example/spec.md',
    'README.md'
  ])

  const commandIds = advice.commands.map((command) => command.id)
  assert.equal(advice.kind, 'dataspec.validation-advice')
  assert.equal(advice.schemaVersion, 1)
  assert.deepEqual(advice.inputPaths, [
    'dataspec-server/src/main/java/com/dataspec/lint/SqlLintService.java',
    'openspec/changes/add-x/specs/example/spec.md',
    'README.md'
  ])
  assert.ok(commandIds.indexOf('backend-tests') < commandIds.indexOf('diff-check'))
  assert.ok(commandIds.includes('openspec-validate'))
  assert.ok(commandIds.includes('status-check'))
  assert.ok(commandIds.includes('diff-check'))
  assert.match(advice.commands.find((command) => command.id === 'backend-tests').reason, /后端/)
  assert.equal(advice.summary.totalCommands, advice.commands.length)
})

test('recommends strict validation for a single active OpenSpec change', () => {
  const advice = buildValidationAdvice([
    'openspec/changes/add-field-quality/specs/field-quality/spec.md',
    'openspec/changes/add-field-quality/tasks.md'
  ])

  const openSpecCommand = advice.commands.find((command) => command.id === 'openspec-validate')
  assert.equal(openSpecCommand.command, 'openspec validate add-field-quality --strict')
  assert.equal(openSpecCommand.category, 'openspec')
  assert.equal(openSpecCommand.cwd, '.')
  assert.match(openSpecCommand.reason, /add-field-quality/)
  assert.ok(advice.commands.some((command) => command.id === 'diff-check'))
})

test('keeps all OpenSpec validation for multiple changes and main specs', () => {
  const multiChangeAdvice = buildValidationAdvice([
    'openspec/changes/add-a/tasks.md',
    'openspec/changes/add-b/tasks.md'
  ])
  const mainSpecAdvice = buildValidationAdvice(['openspec/specs/field-quality/spec.md'])
  const archiveAdvice = buildValidationAdvice(['openspec/changes/archive/2026-07-07-add-a/tasks.md'])

  assert.equal(
    multiChangeAdvice.commands.find((command) => command.id === 'openspec-validate').command,
    'openspec validate --all'
  )
  assert.equal(
    mainSpecAdvice.commands.find((command) => command.id === 'openspec-validate').command,
    'openspec validate --all'
  )
  assert.equal(
    archiveAdvice.commands.find((command) => command.id === 'openspec-validate').command,
    'openspec validate --all'
  )
})

test('falls back to all OpenSpec validation for unsafe change ids', () => {
  const advice = buildValidationAdvice(['openspec/changes/add-x && echo injected/tasks.md'])
  const openSpecCommand = advice.commands.find((command) => command.id === 'openspec-validate')

  assert.equal(openSpecCommand.command, 'openspec validate --all')
  assert.doesNotMatch(openSpecCommand.command, /&&|;|\|/)
})

test('falls back to all OpenSpec validation for traversal-like OpenSpec paths', () => {
  const parentAdvice = buildValidationAdvice(['openspec/changes/add-field-quality/../archive/2026-07-07-add-field-quality/tasks.md'])
  const sameDirAdvice = buildValidationAdvice(['openspec/changes/add-field-quality/./tasks.md'])

  assert.equal(parentAdvice.commands.find((command) => command.id === 'openspec-validate').command, 'openspec validate --all')
  assert.equal(sameDirAdvice.commands.find((command) => command.id === 'openspec-validate').command, 'openspec validate --all')
})

test('recognizes a single active OpenSpec change directory path', () => {
  const advice = buildValidationAdvice(['openspec/changes/add-field-quality'])

  assert.equal(
    advice.commands.find((command) => command.id === 'openspec-validate').command,
    'openspec validate add-field-quality --strict'
  )
})

test('recommends status check for TODO and status-check tool paths', () => {
  const advice = buildValidationAdvice([
    'TODO.md',
    'tools/dataspec-status-check.mjs',
    'tools/dataspec-status-check.test.mjs'
  ])

  const statusCheck = advice.commands.find((command) => command.id === 'status-check')
  const statusCheckTests = advice.commands.find((command) => command.id === 'status-check-tests')
  assert.equal(statusCheck.command, 'node tools/dataspec-status-check.mjs --format json')
  assert.equal(statusCheckTests.command, 'node --test tools/dataspec-status-check.test.mjs')
  assert.equal(statusCheck.cwd, '.')
  assert.match(statusCheck.reason, /README\/TODO\/OpenSpec/)
})

test('recommends status check for project convention documents', () => {
  const advice = buildValidationAdvice(['AGENTS.md', 'SDD.md'])

  const commandIds = advice.commands.map((command) => command.id)
  const statusCheck = advice.commands.find((command) => command.id === 'status-check')
  assert.ok(commandIds.includes('status-check'))
  assert.ok(commandIds.includes('diff-check'))
  assert.equal(statusCheck.command, 'node tools/dataspec-status-check.mjs --format json')
  assert.match(statusCheck.reason, /项目规范/)
})

test('recommends status check for AI contract documentation paths', () => {
  const advice = buildValidationAdvice(['docs/ai-contracts.md'])

  const commandIds = advice.commands.map((command) => command.id)
  const statusCheck = advice.commands.find((command) => command.id === 'status-check')
  assert.ok(commandIds.includes('status-check'))
  assert.ok(commandIds.includes('diff-check'))
  assert.equal(statusCheck.command, 'node tools/dataspec-status-check.mjs --format json')
  assert.match(statusCheck.reason, /AI workflow recipe/)
})

test('recommends status check when workflow recipe source changes', () => {
  const advice = buildValidationAdvice(['tools/dataspec-workflows.mjs'])

  const commandIds = advice.commands.map((command) => command.id)
  assert.ok(commandIds.includes('cli-contract-tests'))
  assert.ok(commandIds.includes('task-card-tests'))
  assert.ok(commandIds.includes('status-check'))
  assert.ok(commandIds.includes('diff-check'))
})

test('recommends frontend tests, build and OpenAPI check for frontend contract paths', () => {
  const advice = buildValidationAdvice([
    'dataspec-web/src/views/SqlLint.vue',
    'dataspec-web/src/api/schema.ts',
    'dataspec-web/package.json'
  ])

  const commandIds = advice.commands.map((command) => command.id)
  assert.ok(commandIds.includes('frontend-tests'))
  assert.ok(commandIds.includes('frontend-build'))
  assert.ok(commandIds.includes('frontend-api-check'))
  assert.equal(advice.commands.find((command) => command.id === 'frontend-build').cwd, 'dataspec-web')
})

test('deduplicates CLI recommendations and renders readable text', () => {
  const advice = buildValidationAdvice([
    'tools/dataspec-cli.mjs',
    'tools/dataspec-cli.test.mjs',
    'tools/dataspec-mcp.mjs',
    'tools/fixtures/cli-mcp-contracts.json'
  ])

  const commandIds = advice.commands.map((command) => command.id)
  assert.equal(commandIds.filter((id) => id === 'cli-contract-tests').length, 1)
  assert.ok(commandIds.includes('cli-mcp-contract-fixture-check'))
  assert.ok(commandIds.includes('diff-check'))
  assert.match(formatValidationAdviceText(advice), /CLI\/MCP/)
  assert.match(formatValidationAdviceText(advice), /git diff --check/)
})

test('recommends fixture checker for CLI/MCP contract fixture paths', () => {
  const advice = buildValidationAdvice([
    'tools/dataspec-cli-mcp-contract-check.mjs',
    'tools/dataspec-cli-mcp-contract-check.test.mjs',
    'tools/fixtures/cli-mcp-contracts.json'
  ])

  assert.ok(advice.commands.some((command) => command.id === 'cli-contract-tests'))
  assert.match(
    advice.commands.find((command) => command.id === 'cli-contract-tests').command,
    /dataspec-cli-mcp-contract-check\.test\.mjs/
  )
  const fixtureCheck = advice.commands.find((command) => command.id === 'cli-mcp-contract-fixture-check')
  assert.equal(fixtureCheck.command, 'node tools/dataspec-cli-mcp-contract-check.mjs --format json')
  assert.match(fixtureCheck.reason, /contract fixture/)
})

test('recommends fixture checker when MCP descriptors change', () => {
  const advice = buildValidationAdvice(['tools/dataspec-mcp.mjs'])

  assert.ok(advice.commands.some((command) => command.id === 'cli-contract-tests'))
  assert.ok(advice.commands.some((command) => command.id === 'cli-mcp-contract-fixture-check'))
})

test('recommends TODO handoff tests for handoff tool paths', () => {
  const advice = buildValidationAdvice([
    'tools/dataspec-todo-openspec-handoff.mjs',
    'tools/dataspec-todo-openspec-handoff.test.mjs'
  ])

  const commandIds = advice.commands.map((command) => command.id)
  assert.ok(commandIds.includes('todo-openspec-handoff-tests'))
  assert.equal(
    advice.commands.find((command) => command.id === 'todo-openspec-handoff-tests').command,
    'node --test tools/dataspec-todo-openspec-handoff.test.mjs'
  )
})

test('recommends index-refs tests for code reference index tool paths', () => {
  const advice = buildValidationAdvice(['tools/dataspec-code-refs.mjs'])

  const commandIds = advice.commands.map((command) => command.id)
  const indexRefsTests = advice.commands.find((command) => command.id === 'code-refs-tests')
  assert.ok(commandIds.includes('code-refs-tests'))
  assert.ok(commandIds.includes('diff-check'))
  assert.equal(indexRefsTests.command, 'node --test --test-name-pattern "index-refs" tools/dataspec-cli.test.mjs')
  assert.match(indexRefsTests.reason, /字段引用索引/)
})

test('recommends task-card tests for task card protocol tool paths', () => {
  const advice = buildValidationAdvice([
    'tools/dataspec-task-card.mjs',
    'tools/dataspec-task-card.test.mjs'
  ])

  const commandIds = advice.commands.map((command) => command.id)
  const taskCardTests = advice.commands.find((command) => command.id === 'task-card-tests')
  assert.equal(commandIds.filter((id) => id === 'task-card-tests').length, 1)
  assert.ok(commandIds.includes('diff-check'))
  assert.equal(
    taskCardTests.command,
    'node --test --test-name-pattern "task-card|task card|create_task_card|render_task_card" tools/dataspec-task-card.test.mjs tools/dataspec-cli.test.mjs tools/dataspec-mcp.test.mjs'
  )
  assert.match(taskCardTests.reason, /AI 任务卡/)
})

test('cli supports --changed json output with injected changed paths', async () => {
  const io = createIo()
  const code = await runAdvisorCli(['--changed', '--format', 'json'], io, {
    getChangedPaths: async () => [
      'docker-compose.local.yml',
      'tools/dataspec-local-smoke.mjs',
      'openspec/changes/add-field-quality/tasks.md'
    ]
  })

  const output = JSON.parse(io.stdout)
  const commandIds = output.commands.map((command) => command.id)
  assert.equal(code, 0)
  assert.ok(commandIds.includes('local-smoke-tests'))
  assert.ok(commandIds.includes('docker-compose-config'))
  assert.equal(
    output.commands.find((command) => command.id === 'openspec-validate').command,
    'openspec validate add-field-quality --strict'
  )
  assert.deepEqual(output.nextActions.slice(0, 1), ['先运行推荐命令中耗时最短且最贴近本次改动的检查。'])
})

test('collects tracked and untracked git changed paths without duplicates', () => {
  assert.deepEqual(
    collectChangedPathsFromGitOutput(
      'README.md\ntools/dataspec-cli.mjs\n',
      'tools/dataspec-verify-advisor.mjs\nREADME.md\n'
    ),
    ['README.md', 'tools/dataspec-cli.mjs', 'tools/dataspec-verify-advisor.mjs']
  )
})

test('normalizes absolute workspace paths before matching validation rules', () => {
  const root = process.cwd()
  const advice = buildValidationAdvice([
    `${root}\\tools\\dataspec-status-check.mjs`,
    `${root.replace(/\\/g, '/')}/TODO.md`
  ])

  const commandIds = advice.commands.map((command) => command.id)
  assert.deepEqual(advice.inputPaths, [
    'tools/dataspec-status-check.mjs',
    'TODO.md'
  ])
  assert.ok(commandIds.includes('status-check-tests'))
  assert.ok(commandIds.includes('status-check'))
  assert.ok(commandIds.includes('diff-check'))
})

test('collects NUL-delimited git changed paths', () => {
  assert.deepEqual(
    collectChangedPathsFromGitOutput(
      'README.md\0docs/example note.md\0',
      'tools/dataspec-verify-advisor.mjs\0README.md\0'
    ),
    ['README.md', 'docs/example note.md', 'tools/dataspec-verify-advisor.mjs']
  )
})

test('preserves significant whitespace in NUL-delimited git paths', () => {
  assert.deepEqual(
    collectChangedPathsFromGitOutput(
      ' leading.md\0trailing.md \0./docs\\example.md\0',
      ''
    ),
    [' leading.md', 'trailing.md ', 'docs/example.md']
  )
})

function createIo() {
  return {
    stdout: '',
    stderr: '',
    writeOut(text) {
      this.stdout += text
    },
    writeErr(text) {
      this.stderr += text
    }
  }
}
