import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  buildStatusReport,
  formatStatusReportText,
  runStatusCheckCli
} from './dataspec-status-check.mjs'

const WORKFLOW_RECIPE_IDS = [
  'create-table',
  'review-pr-sql',
  'reverse-import-standards',
  'export-min-context',
  'standard-evidence-review'
]

const WORKFLOW_RECIPE_SUMMARY = 'create-table/review-pr-sql/reverse-import-standards/export-min-context/standard-evidence-review'

const CLEAN_TODO = `# DataSpec 待办路线图

## 下一步顺序

1. 当前状态：P6-1 到 P6-70 已完成第一版；2026-07-05 已归档 1 个已完成 active OpenSpec change（P6-70），active change 队列恢复为空。
2. 近期只保留 2 个优先行动项，后续开发默认从这里选，不再从 P6-71 到 P6-188 全量顺扫：P6-71、P6-72。

## 说明

参考 [归档](docs/archive/example.md)。
任务卡可从 ${WORKFLOW_RECIPE_SUMMARY} workflow recipe 生成。

### P6-70：SQL 规则调试器与可解释匹配面板
- 状态：已完成第一版，commit \`9bd40c9\`；OpenSpec change \`add-sql-rule-debugger\` 已于 2026-07-05 归档并同步主规格。
- 已完成能力：只读调试入口。
- 后续增强：更深 trace。
- 边界：不改变现有 lint 结果兼容字段。

### P6-71：数据库元数据增量缓存与变更指纹
- 状态：待办。
- 缺口：缺少 fingerprint。
- 验收标准：能输出变更摘要。
- 边界：不实时同步。

### P6-72：CLI/MCP 与服务端版本兼容握手
- 状态：待办。
- 缺口：缺少版本握手。
- 验收标准：版本不兼容时有诊断。
- 边界：不自动升级。
`

const CLEAN_README = `# DataSpec 数标

## 开发验证

文档状态检查：

\`\`\`bash
node tools/dataspec-status-check.mjs --format json
\`\`\`
`

const CLEAN_AI_CONTRACTS = `# AI 契约

## AI Context

- \`.dataspec/workflows.md\`: \`create-table\`、\`review-pr-sql\`、\`reverse-import-standards\`、\`export-min-context\`、\`standard-evidence-review\` 五个 recipe id。
`

test('buildStatusReport passes for a self-consistent TODO/OpenSpec snapshot', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  assert.equal(report.kind, 'dataspec.status-check')
  assert.equal(report.schemaVersion, 1)
  assert.equal(report.status, 'pass')
  assert.equal(report.summary.todoItems, 3)
  assert.deepEqual(report.summary.queueItems, ['P6-71', 'P6-72'])
  assert.equal(report.issues.length, 0)
  assert.deepEqual(report.summary.issueCodes, [])
  assert.match(report.nextActions[0], /检查通过/)
})

test('buildStatusReport reports workflow recipe contract drift in AI docs and TODO', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO.replace('/standard-evidence-review', ''),
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS.replace('、`standard-evidence-review`', ''),
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const aiContractIssue = report.issues.find((issue) => issue.code === 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT')
  const todoIssue = report.issues.find((issue) => issue.code === 'TODO_WORKFLOW_RECIPES_DRIFT')
  const workflowCheck = report.checks.find((check) => check.id === 'workflow-recipes')

  assert.equal(report.status, 'fail')
  assert.ok(aiContractIssue)
  assert.equal(aiContractIssue.file, 'docs/ai-contracts.md')
  assert.match(aiContractIssue.message, /standard-evidence-review/)
  assert.ok(todoIssue)
  assert.equal(todoIssue.file, 'TODO.md')
  assert.match(todoIssue.message, /standard-evidence-review/)
  assert.equal(workflowCheck.status, 'fail')
  assert.equal(workflowCheck.issueCount, 2)
})

test('buildStatusReport reports stale workflow recipe ids left in docs and TODO', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS.filter((id) => id !== 'standard-evidence-review'),
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const driftIssues = report.issues.filter((issue) =>
    issue.code === 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT' || issue.code === 'TODO_WORKFLOW_RECIPES_DRIFT'
  )

  assert.equal(report.status, 'fail')
  assert.equal(driftIssues.length, 2)
  assert.ok(driftIssues.every((issue) => /多余：standard-evidence-review/.test(issue.message)))
})

test('buildStatusReport reports missing AI contract document for workflow recipes', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: null,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const issue = report.issues.find((candidate) => candidate.code === 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.file, 'docs/ai-contracts.md')
  assert.match(issue.message, /无法确认/)
  assert.match(issue.message, /standard-evidence-review/)
})

test('buildStatusReport reports deterministic TODO and OpenSpec drift', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO.replace('P6-71、P6-72', 'P6-70、P6-404').replace('- 后续增强：更深 trace。', '- 缺口：旧缺口仍残留。'),
    readmeText: '# Missing tool entry\n',
    relativeFiles: new Set(['README.md', 'TODO.md', 'tools/dataspec-status-check.mjs']),
    openSpecChangeEntries: ['archive', 'add-sql-rule-debugger'],
    openSpecSpecEntries: []
  })

  const codes = report.issues.map((issue) => issue.code)
  assert.equal(report.status, 'fail')
  assert.ok(codes.includes('TODO_QUEUE_ITEM_NOT_PENDING'))
  assert.ok(codes.includes('TODO_QUEUE_ITEM_MISSING'))
  assert.ok(codes.includes('TODO_COMPLETED_HAS_GAP'))
  assert.ok(codes.includes('OPENSPEC_ACTIVE_CHANGE_LEFTOVER'))
  assert.ok(codes.includes('README_STATUS_CHECK_MISSING'))
  assert.ok(codes.includes('MARKDOWN_LINK_MISSING'))
  assert.ok(codes.includes('OPENSPEC_ARCHIVE_MISSING'))
  assert.ok(codes.includes('OPENSPEC_MAIN_SPEC_MISSING'))
  const queueMissingSummary = report.summary.issueCodes.find((item) => item.code === 'TODO_QUEUE_ITEM_MISSING')
  assert.equal(queueMissingSummary.count, 1)
  assert.equal(queueMissingSummary.severity, 'error')
  assert.match(report.nextActions[2], /TODO_QUEUE_ITEM_MISSING\(count=1,severity=error\)/)
  const text = formatStatusReportText(report)
  assert.match(text, /状态：fail/)
  assert.match(text, /检查项:/)
  assert.ok(text.indexOf('检查项:') < text.indexOf('问题明细:'))
  assert.match(text, /- todo-queue \(TODO 近期队列一致性\): status=fail issues=2 errors=2 warnings=0/)
  assert.match(text, /- readme-entry \(README 状态检查入口\): status=fail issues=1 errors=1 warnings=0/)
})

test('buildStatusReport reports queue count drift in queue line and summaries', () => {
  const todoText = CLEAN_TODO
    .replace('近期只保留 2 个优先行动项', '近期只保留 3 个优先行动项')
    .replace('## 说明', '## 说明\n\n顶部 3 项队列仍需同步。\n')
  const report = buildStatusReport({
    todoText,
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const codes = report.issues.map((issue) => issue.code)
  assert.equal(report.status, 'fail')
  assert.ok(codes.includes('TODO_QUEUE_COUNT_MISMATCH'))
  assert.ok(codes.includes('TODO_QUEUE_SUMMARY_COUNT_MISMATCH'))
})

test('buildStatusReport treats active changes as warning unless TODO claims queue is empty', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO.replace('active change 队列恢复为空。', 'active change 队列保留正在实施项。'),
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive', 'add-working-change', 'add-second-working-change'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  assert.equal(report.status, 'warn')
  assert.deepEqual(report.issues.map((issue) => issue.code), [
    'OPENSPEC_ACTIVE_CHANGE_PRESENT',
    'OPENSPEC_ACTIVE_CHANGE_PRESENT'
  ])
  assert.deepEqual(report.summary.issueCodes, [
    {
      code: 'OPENSPEC_ACTIVE_CHANGE_PRESENT',
      count: 2,
      severity: 'warning'
    }
  ])
  assert.equal(report.issues[0].severity, 'warning')
  const openSpecCheck = report.checks.find((check) => check.id === 'openspec-state')
  assert.equal(openSpecCheck.status, 'pass')
  assert.equal(openSpecCheck.warningCount, 2)
  assert.equal(openSpecCheck.errorCount, 0)
  assert.equal(report.nextActions[0], '优先处理或确认 severity=warning 的状态漂移，再重新运行本命令。')
  assert.equal(report.nextActions[1], '如果发现脚本误报，先补测试 fixture，再调整确定性规则。')
  assert.doesNotMatch(report.nextActions[0], /severity=error/)
  assert.match(report.nextActions[0], /severity=warning/)
  assert.equal(report.nextActions[2], '当前问题编码：OPENSPEC_ACTIVE_CHANGE_PRESENT(count=2,severity=warning)')
  assert.match(
    formatStatusReportText(report),
    /- openspec-state \(OpenSpec active\/archive\/main spec 一致性\): status=pass issues=2 errors=0 warnings=2/
  )
})

test('buildStatusReport reports stale non-empty active queue text when no active changes exist', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO.replace('active change 队列恢复为空。', 'active change 队列保留正在实施项。'),
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const issue = report.issues.find((candidate) => candidate.code === 'OPENSPEC_ACTIVE_QUEUE_TEXT_STALE')
  const openSpecCheck = report.checks.find((check) => check.id === 'openspec-state')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.file, 'TODO.md')
  assert.equal(issue.line, 5)
  assert.equal(openSpecCheck.status, 'fail')
  assert.equal(openSpecCheck.errorCount, 1)
})

test('buildStatusReport accepts cleared active queue wording when no active changes exist', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO.replace('active change 队列恢复为空。', 'active change 队列已清空。'),
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  assert.equal(report.status, 'pass')
  assert.equal(report.issues.length, 0)
})

test('buildStatusReport reports stale TODO active OpenSpec change references', () => {
  const todoText = CLEAN_TODO.replace(
    'active change 队列恢复为空。',
    'OpenSpec change `openspec/changes/add-code-field-reference-index` 暂未自动归档。'
  )
  const report = buildStatusReport({
    todoText,
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/changes/archive/2026-07-07-add-code-field-reference-index',
      'openspec/specs/sql-rule-debugger/spec.md',
      'openspec/specs/code-field-reference-index/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger', 'code-field-reference-index']
  })

  const issue = report.issues.find((candidate) => candidate.code === 'OPENSPEC_ACTIVE_CHANGE_REFERENCE_MISSING')
  const openSpecCheck = report.checks.find((check) => check.id === 'openspec-state')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.file, 'TODO.md')
  assert.equal(issue.line, 5)
  assert.match(issue.message, /add-code-field-reference-index/)
  assert.equal(openSpecCheck.status, 'fail')
  assert.equal(openSpecCheck.errorCount, 1)
})

test('buildStatusReport normalizes archived OpenSpec change path references', () => {
  const todoText = CLEAN_TODO.replace(
    'OpenSpec change `add-sql-rule-debugger` 已于 2026-07-05 归档并同步主规格。',
    'OpenSpec change `openspec/changes/add-sql-rule-debugger` 已于 2026-07-05 归档并同步主规格。'
  )
  const report = buildStatusReport({
    todoText,
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  assert.equal(report.status, 'pass')
  assert.equal(report.issues.length, 0)
})

test('buildStatusReport resolves main specs from archived OpenSpec delta specs', () => {
  const todoText = CLEAN_TODO.replace(
    'OpenSpec change `add-sql-rule-debugger` 已于 2026-07-05 归档并同步主规格。',
    'OpenSpec change `add-testcontainers-db-integration-tests` 已于 2026-07-07 归档并同步主规格。'
  )
  const report = buildStatusReport({
    todoText,
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-07-add-testcontainers-db-integration-tests/specs/db-testcontainers-integration-tests/spec.md',
      'openspec/specs/db-testcontainers-integration-tests/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['db-testcontainers-integration-tests']
  })

  assert.equal(report.status, 'pass')
  assert.equal(report.issues.length, 0)
})

test('buildStatusReport reports missing main spec for multi-capability archives', () => {
  const todoText = CLEAN_TODO.replace(
    'OpenSpec change `add-sql-rule-debugger` 已于 2026-07-05 归档并同步主规格。',
    'OpenSpec change `add-composite-change` 已于 2026-07-07 归档并同步主规格。'
  )
  const report = buildStatusReport({
    todoText,
    readmeText: CLEAN_README,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-07-add-composite-change/specs/cap-a/spec.md',
      'openspec/changes/archive/2026-07-07-add-composite-change/specs/cap-b/spec.md',
      'openspec/specs/cap-a/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['cap-a']
  })

  const issue = report.issues.find((candidate) => candidate.code === 'OPENSPEC_MAIN_SPEC_MISSING')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.match(issue.message, /openspec\/specs\/cap-b\/spec\.md/)
})

test('runStatusCheckCli supports json output and returns non-zero on errors', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO.replace('P6-71、P6-72', 'P6-404'), 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(output.issues.some((issue) => issue.code === 'TODO_QUEUE_ITEM_MISSING'))
    const queueMissingSummary = output.summary.issueCodes.find((item) => item.code === 'TODO_QUEUE_ITEM_MISSING')
    assert.equal(queueMissingSummary.count, 1)
    assert.equal(queueMissingSummary.severity, 'error')
    assert.match(output.nextActions[2], /TODO_QUEUE_ITEM_MISSING\(count=1,severity=error\)/)
    assert.match(output.nextActions[0], /severity=error/)
    const todoQueueCheck = output.checks.find((check) => check.id === 'todo-queue')
    assert.equal(todoQueueCheck.errorCount, 2)
    assert.equal(todoQueueCheck.warningCount, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports placeholder Purpose in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
TBD - created by archiving change add-sql-rule-debugger. Update Purpose after archive.
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((candidate) => candidate.code === 'OPENSPEC_SPEC_PURPOSE_PLACEHOLDER')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/sql-rule-debugger/spec.md')
    assert.equal(issue.line, 4)
    assert.match(issue.message, /Purpose/)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
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
