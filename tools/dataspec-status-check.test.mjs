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

test('buildStatusReport checks Markdown links in AI contract docs', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: `${CLEAN_AI_CONTRACTS}\n\n参考 [缺失契约](missing-contract.md)。\n`,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const issue = report.issues.find((candidate) =>
    candidate.code === 'MARKDOWN_LINK_MISSING' && candidate.file === 'docs/ai-contracts.md'
  )
  const markdownCheck = report.checks.find((check) => check.id === 'markdown-links')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.line, 8)
  assert.match(issue.message, /missing-contract\.md/)
  assert.equal(markdownCheck.status, 'fail')
  assert.equal(markdownCheck.errorCount, 1)
})

test('buildStatusReport checks Markdown links in OpenSpec main specs', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger'],
    openSpecSpecTexts: new Map([
      ['sql-rule-debugger', `# sql-rule-debugger Specification

## Purpose
用于稳定描述 SQL 规则调试器的可验证能力边界。

参考 [缺失说明](missing-spec-note.md)。

## Requirements
### Requirement: 展示 SQL 规则调试结果
系统必须向 AI 和人工用户提供稳定的调试结果说明。

#### Scenario: 读取调试结果
- **WHEN** 用户请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明
`]
    ])
  })

  const issue = report.issues.find((candidate) =>
    candidate.code === 'MARKDOWN_LINK_MISSING' && candidate.file === 'openspec/specs/sql-rule-debugger/spec.md'
  )
  const markdownCheck = report.checks.find((check) => check.id === 'markdown-links')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.line, 6)
  assert.match(issue.message, /missing-spec-note\.md/)
  assert.equal(markdownCheck.status, 'fail')
  assert.equal(markdownCheck.errorCount, 1)
})

test('buildStatusReport reports Scenario AND steps without text', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger'],
    openSpecSpecTexts: new Map([
      ['sql-rule-debugger', `# sql-rule-debugger Specification

## Purpose
用于稳定描述 SQL 规则调试器的可验证能力边界。

## Requirements
### Requirement: 展示 SQL 规则调试结果
系统必须向 AI 和人工用户提供稳定的调试结果说明。

#### Scenario: 读取调试结果
- **WHEN** 用户请求查看 SQL 规则调试结果
- **AND**
- **THEN** 系统返回可验证的匹配说明
`]
    ])
  })

  const issue = report.issues.find((candidate) =>
    candidate.code === 'OPENSPEC_SPEC_SCENARIO_STEP_TEXT_MISSING'
  )
  const openSpecCheck = report.checks.find((check) => check.id === 'openspec-state')

  assert.equal(report.status, 'fail')
  assert.ok(issue)
  assert.equal(issue.file, 'openspec/specs/sql-rule-debugger/spec.md')
  assert.equal(issue.line, 12)
  assert.match(issue.message, /AND/)
  assert.equal(openSpecCheck.errorCount, 1)
})

test('buildStatusReport reports empty Requirement and Scenario titles', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger'],
    openSpecSpecTexts: new Map([
      ['sql-rule-debugger', `# sql-rule-debugger Specification

## Purpose
用于稳定描述 SQL 规则调试器的可验证能力边界。

## Requirements
### Requirement:
系统必须给每个 Requirement 提供稳定标题。

#### Scenario: 读取调试结果
- **WHEN** 用户请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明

### Requirement: 展示 SQL 规则调试结果
系统必须向 AI 和人工用户提供稳定的调试结果说明。

#### Scenario:
- **WHEN** 用户请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明
`]
    ])
  })

  const requirementIssue = report.issues.find((candidate) =>
    candidate.code === 'OPENSPEC_SPEC_REQUIREMENT_TITLE_MISSING'
  )
  const scenarioIssue = report.issues.find((candidate) =>
    candidate.code === 'OPENSPEC_SPEC_SCENARIO_TITLE_MISSING'
  )
  const openSpecCheck = report.checks.find((check) => check.id === 'openspec-state')

  assert.equal(report.status, 'fail')
  assert.ok(requirementIssue)
  assert.equal(requirementIssue.file, 'openspec/specs/sql-rule-debugger/spec.md')
  assert.equal(requirementIssue.line, 7)
  assert.ok(scenarioIssue)
  assert.equal(scenarioIssue.file, 'openspec/specs/sql-rule-debugger/spec.md')
  assert.equal(scenarioIssue.line, 17)
  assert.equal(openSpecCheck.errorCount, 2)
})

test('buildStatusReport does not duplicate empty Requirement and Scenario title issues', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: CLEAN_README,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger'],
    openSpecSpecTexts: new Map([
      ['sql-rule-debugger', `# sql-rule-debugger Specification

## Purpose
用于稳定描述 SQL 规则调试器的可验证能力边界。

## Requirements
### Requirement:
系统必须给每个 Requirement 提供稳定标题。

#### Scenario:
- **WHEN** 用户请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明

#### Scenario:${'   '}
- **WHEN** 用户再次请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明

### Requirement:${'   '}
系统必须避免把空标题二次识别为重复标题。

#### Scenario: 命名场景
- **WHEN** 用户请求查看 SQL 规则调试结果
- **THEN** 系统返回可验证的匹配说明
`]
    ])
  })

  const titleIssues = report.issues.filter((candidate) =>
    candidate.code === 'OPENSPEC_SPEC_REQUIREMENT_TITLE_MISSING' ||
    candidate.code === 'OPENSPEC_SPEC_SCENARIO_TITLE_MISSING'
  )
  const duplicateIssues = report.issues.filter((candidate) =>
    candidate.code === 'OPENSPEC_SPEC_REQUIREMENT_DUPLICATE' ||
    candidate.code === 'OPENSPEC_SPEC_SCENARIO_DUPLICATE'
  )

  assert.equal(report.status, 'fail')
  assert.equal(titleIssues.length, 4)
  assert.equal(duplicateIssues.length, 0)
})

test('buildStatusReport accepts angle-bracket Markdown links with spaces', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带空格文档](<docs/archive/example note.md>)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example note.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts URL-encoded Markdown link paths', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [编码空格文档](docs/archive/example%20note.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example note.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts Markdown link paths with query strings', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带 query 文档](docs/archive/example.md?raw=1#section)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport ignores non-file URI scheme Markdown links', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '参考 [FTP 资料](ftp://example.com/dataspec.md)、[电话](tel:+123456789) 和 [编辑器](vscode://file/docs/archive/example.md)。',
      '参考 [URN](urn:isbn:9787110000000) 和 [SFTP](sftp://example.com/dataspec.md)。',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport ignores protocol-relative Markdown links', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [协议相对资料](//example.com/dataspec.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts angle-bracket Markdown links with titles', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带标题文档](<docs/archive/example note.md> "示例说明")。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example note.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts angle-bracket Markdown links with closing parenthesis in path', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带括号文档](<docs/archive/example (draft).md>)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example (draft).md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts Markdown link paths with balanced parentheses', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带括号文档](docs/archive/example(draft).md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example(draft).md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport accepts Markdown link paths with escaped parentheses', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [转义括号文档](docs/archive/example\\(draft\\).md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example(draft).md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport does not close Markdown links on escaped closing parentheses', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [转义右括号文档](docs/archive/example\\).md) 后接 [缺失](docs/archive/missing.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/archive/example).md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
  assert.doesNotMatch(missingLinks[0].message, /docs\/archive\/example/)
})

test('buildStatusReport keeps later Markdown links after malformed parenthesis links', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [坏链接](docs/archive/example(draft.md) 后接 [缺失](docs/archive/missing.md))。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
  assert.doesNotMatch(missingLinks[0].message, /example\(draft/)
})

test('buildStatusReport checks multiple Markdown links on the same line', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [存在](docs/archive/example.md) 和 [缺失](docs/archive/missing.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
})

test('buildStatusReport checks Markdown links with escaped closing brackets in labels', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [带\\]括号标签](docs/archive/missing.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
})

test('buildStatusReport checks Markdown links with nested brackets in labels', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n参考 [外层 [内层] 标签](docs/archive/missing.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
})

test('buildStatusReport keeps later Markdown links after unclosed label brackets', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}\n这里有一个未闭合 [ 方括号，后面是 [缺失](docs/archive/missing.md)。\n`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing\.md/)
})

test('buildStatusReport ignores Markdown links inside fenced code blocks', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
\`\`\`md
[示例缺失链接](docs/archive/missing-from-example.md)
\`\`\`
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport keeps longer fenced code blocks open across shorter nested fences', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
\`\`\`\`md
\`\`\`md
[内部示例缺失链接](docs/archive/missing-from-nested-example.md)
\`\`\`
\`\`\`\`
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport ignores Markdown links inside tilde fenced code blocks', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
~~~md
[示例缺失链接](docs/archive/missing-from-tilde-example.md)
~~~
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport resumes Markdown link checks after fenced code blocks', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
\`\`\`md
[示例缺失链接](docs/archive/missing-inside-fence.md)
\`\`\`
参考 [真实缺失链接](docs/archive/missing-after-fence.md)。
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing-after-fence\.md/)
})

test('buildStatusReport ignores Markdown links inside inline code spans', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
行内示例 \`[忽略缺失链接](docs/archive/missing-inline-example.md)\` 后仍检查 [真实缺失链接](docs/archive/missing-after-inline.md)。
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing-after-inline\.md/)
})

test('buildStatusReport keeps link targets aligned after emoji inside inline code spans', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: `${CLEAN_README}
行内示例 \`😀 [忽略缺失链接](docs/archive/missing-inline-emoji-example.md)\` 后仍检查 [存在链接](docs/archive/example.md)。
`,
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport checks Markdown links between escaped backticks', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '转义反引号 \\`[真实缺失链接](docs/archive/missing-escaped-inline.md)\\` 仍应检查。',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing-escaped-inline\.md/)
})

test('buildStatusReport ignores Markdown links with escaped opening brackets', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '转义标签 \\[忽略缺失链接](docs/archive/missing-escaped-label.md) 后仍检查 [真实缺失链接](docs/archive/missing-after-escaped-label.md)。',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing-after-escaped-label\.md/)
})

test('buildStatusReport checks reference-style Markdown link definitions', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '[缺失资料]: docs/archive/missing-reference-style.md "资料说明"',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'fail')
  assert.equal(missingLinks.length, 1)
  assert.match(missingLinks[0].message, /docs\/archive\/missing-reference-style\.md/)
})

test('buildStatusReport ignores Markdown footnote definitions when checking links', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '[^说明]: 这是一段脚注说明，不是相对链接目标。',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
})

test('buildStatusReport ignores tab-indented reference definitions inside code samples', () => {
  const report = buildStatusReport({
    todoText: CLEAN_TODO,
    readmeText: [
      CLEAN_README,
      '\t[示例链接]: docs/archive/missing-tab-indented-reference.md',
      ''
    ].join('\n'),
    aiContractsText: CLEAN_AI_CONTRACTS,
    workflowRecipeIds: WORKFLOW_RECIPE_IDS,
    relativeFiles: new Set([
      'README.md',
      'TODO.md',
      'docs/archive/example.md',
      'docs/ai-contracts.md',
      'tools/dataspec-status-check.mjs',
      'openspec/changes/archive/2026-07-05-add-sql-rule-debugger',
      'openspec/specs/sql-rule-debugger/spec.md'
    ]),
    openSpecChangeEntries: ['archive'],
    openSpecSpecEntries: ['sql-rule-debugger']
  })

  const missingLinks = report.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')

  assert.equal(report.status, 'pass')
  assert.equal(missingLinks.length, 0)
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

test('runStatusCheckCli reports missing Markdown links in active OpenSpec change docs and ignores archive docs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'add-link-coverage', 'specs', 'link-coverage'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger'),
      { recursive: true }
    )
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await writeFile(
      path.join(dir, 'TODO.md'),
      CLEAN_TODO.replace('active change 队列恢复为空。', 'active change 队列保留正在实施项。'),
      'utf8'
    )
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'add-link-coverage', 'proposal.md'),
      '# Proposal\n\n参考 [缺失方案资料](docs/missing-proposal.md)。\n',
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'add-link-coverage', 'tasks.md'),
      '# Tasks\n\n- [ ] 对齐 [缺失任务资料](missing-task.md)。\n',
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'add-link-coverage', 'specs', 'link-coverage', 'spec.md'),
      `## ADDED Requirements
### Requirement: Link coverage
DataSpec SHALL check active change links and reference [缺失 delta 资料](notes/missing-delta.md).

#### Scenario: Check active change links
- **WHEN** the status check reads active change docs
- **THEN** it reports broken relative links
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'proposal.md'),
      '# Archived proposal\n\n历史资料 [缺失但忽略](missing-archive.md)。\n',
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'tasks.md'),
      '# Archived tasks\n\n- [x] 历史任务 [缺失但忽略](missing-archive-task.md)。\n',
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger', 'spec.md'),
      '## ADDED Requirements\n### Requirement: Archived delta\n历史 delta 引用 [缺失但忽略](missing-archive-delta.md)。\n',
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)
    const missingLinks = output.issues.filter((issue) => issue.code === 'MARKDOWN_LINK_MISSING')
    const markdownCheck = output.checks.find((check) => check.id === 'markdown-links')

    assert.equal(code, 1)
    assert.equal(missingLinks.length, 3)
    assert.ok(missingLinks.every((issue) => issue.file.startsWith('openspec/changes/add-link-coverage/')))
    assert.ok(missingLinks.some((issue) => issue.file === 'openspec/changes/add-link-coverage/proposal.md'))
    assert.ok(missingLinks.some((issue) => issue.file === 'openspec/changes/add-link-coverage/tasks.md'))
    assert.ok(missingLinks.some((issue) => issue.file === 'openspec/changes/add-link-coverage/specs/link-coverage/spec.md'))
    assert.ok(!missingLinks.some((issue) => issue.message.includes('missing-archive.md')))
    assert.ok(!missingLinks.some((issue) => issue.message.includes('missing-archive-task.md')))
    assert.ok(!missingLinks.some((issue) => issue.message.includes('missing-archive-delta.md')))
    assert.equal(markdownCheck.status, 'fail')
    assert.equal(markdownCheck.errorCount, 3)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports missing and mismatched main OpenSpec spec titles', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-title'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'mismatched-title'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-title', 'spec.md'),
      `## Purpose
用于验证缺少主标题的规格。
## Requirements
### Requirement: Missing title example
DataSpec SHALL keep a title.

#### Scenario: Keep title
- **WHEN** the main spec is checked
- **THEN** DataSpec reports the missing title
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'mismatched-title', 'spec.md'),
      `# wrong-title Specification

## Purpose
用于验证主标题与 capability 目录名不一致的规格。
## Requirements
### Requirement: Mismatched title example
DataSpec SHALL keep the title aligned with the capability directory.

#### Scenario: Keep title aligned
- **WHEN** the main spec is checked
- **THEN** DataSpec reports the mismatched title
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const missingIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_TITLE_MISSING')
    const mismatchIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_TITLE_MISMATCH')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(missingIssue)
    assert.equal(missingIssue.file, 'openspec/specs/missing-title/spec.md')
    assert.equal(missingIssue.line, 1)
    assert.ok(mismatchIssue)
    assert.equal(mismatchIssue.file, 'openspec/specs/mismatched-title/spec.md')
    assert.equal(mismatchIssue.line, 1)
    assert.match(mismatchIssue.message, /mismatched-title/)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 2)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli ignores archived delta spec titles when checking main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# archived-delta-title-is-not-a-main-spec

## ADDED Requirements
### Requirement: Archived delta example
DataSpec SHALL ignore archived delta headings for main spec title checks.

#### Scenario: Ignore delta title
- **WHEN** status-check validates main spec titles
- **THEN** archived delta spec headings do not produce title issues
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code.startsWith('OPENSPEC_SPEC_TITLE_')).length, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli ignores archived delta spec Purpose when checking main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
占位。
## ADDED Requirements
### Requirement: Archived delta purpose example
DataSpec SHALL ignore archived delta purpose text for main spec purpose checks.

#### Scenario: Ignore delta purpose
- **WHEN** status-check validates main spec purposes
- **THEN** archived delta spec purpose text does not produce purpose issues
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code.startsWith('OPENSPEC_SPEC_PURPOSE_')).length, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli ignores archived delta spec Requirements when checking main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于验证归档 delta 不参与主规格 Requirements 检查。
## Requirements
### Requirement: Archived delta incomplete requirement
DataSpec SHALL ignore archived delta requirements for main spec checks.
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code.startsWith('OPENSPEC_SPEC_REQUIREMENT')).length, 0)
    assert.equal(output.issues.filter((issue) => issue.code.startsWith('OPENSPEC_SPEC_SCENARIO')).length, 0)
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

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
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

test('runStatusCheckCli does not treat placeholder wording outside Purpose as Purpose placeholder', async () => {
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
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint and may mention "Update Purpose after archive" as historical wording in requirements.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_PURPOSE_PLACEHOLDER').length, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports missing and empty Purpose in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-purpose'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'empty-purpose'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-purpose', 'spec.md'),
      `# missing-purpose Specification

## Requirements
### Requirement: Missing purpose example
DataSpec SHALL keep a requirement.

#### Scenario: Keep requirement
- **WHEN** the main spec is checked
- **THEN** the requirement remains executable
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'empty-purpose', 'spec.md'),
      `# empty-purpose Specification

## Purpose

## Requirements
### Requirement: Empty purpose example
DataSpec SHALL keep another requirement.

#### Scenario: Keep another requirement
- **WHEN** the main spec is checked
- **THEN** the requirement remains executable
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const missingIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_PURPOSE_MISSING')
    const emptyIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_PURPOSE_EMPTY')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(missingIssue)
    assert.equal(missingIssue.file, 'openspec/specs/missing-purpose/spec.md')
    assert.equal(missingIssue.line, 1)
    assert.ok(emptyIssue)
    assert.equal(emptyIssue.file, 'openspec/specs/empty-purpose/spec.md')
    assert.equal(emptyIssue.line, 3)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 2)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports too short Purpose in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'too-short-purpose'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'too-short-purpose', 'spec.md'),
      `# too-short-purpose Specification

## Purpose
用于验证。
## Requirements
### Requirement: Too short purpose example
DataSpec SHALL keep a requirement.

#### Scenario: Keep requirement
- **WHEN** the main spec is checked
- **THEN** the requirement remains executable
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((candidate) => candidate.code === 'OPENSPEC_SPEC_PURPOSE_TOO_SHORT')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/too-short-purpose/spec.md')
    assert.equal(issue.line, 4)
    assert.match(issue.message, /过短/)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli accepts multi-line Purpose when combined text is meaningful', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'multi-line-purpose'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'multi-line-purpose', 'spec.md'),
      `# multi-line-purpose Specification

## Purpose
说明。
用于帮助 AI 判断字段标准维护场景。
## Requirements
### Requirement: Multi-line purpose example
DataSpec SHALL keep a requirement.

#### Scenario: Keep requirement
- **WHEN** the main spec is checked
- **THEN** the requirement remains executable
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_PURPOSE_TOO_SHORT').length, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli treats Purpose with only subheadings as empty', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'heading-only-purpose'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'heading-only-purpose', 'spec.md'),
      `# heading-only-purpose Specification

## Purpose
### Details

## Requirements
### Requirement: Heading only purpose example
DataSpec SHALL keep a requirement.

#### Scenario: Keep requirement
- **WHEN** the main spec is checked
- **THEN** the requirement remains executable
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)
    const issue = output.issues.find((candidate) => candidate.code === 'OPENSPEC_SPEC_PURPOSE_EMPTY')

    assert.equal(code, 1)
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/heading-only-purpose/spec.md')
    assert.equal(issue.line, 3)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports missing and empty Requirements in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-requirements'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'empty-requirements'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-requirements', 'spec.md'),
      `# missing-requirements Specification

## Purpose
用于验证缺少 Requirements 小节的主规格。
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'empty-requirements', 'spec.md'),
      `# empty-requirements Specification

## Purpose
用于验证没有 Requirement 条目的主规格。
## Requirements
说明文字不能替代稳定的 Requirement 条目。
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const missingIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENTS_MISSING')
    const emptyIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENTS_EMPTY')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(missingIssue)
    assert.equal(missingIssue.file, 'openspec/specs/missing-requirements/spec.md')
    assert.equal(missingIssue.line, 1)
    assert.ok(emptyIssue)
    assert.equal(emptyIssue.file, 'openspec/specs/empty-requirements/spec.md')
    assert.equal(emptyIssue.line, 5)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 2)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports duplicate Requirement titles in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'duplicate-requirement'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'duplicate-requirement', 'spec.md'),
      `# duplicate-requirement Specification

## Purpose
用于验证重复 Requirement 标题的主规格。
## Requirements
### Requirement: Duplicate title
DataSpec SHALL keep requirement titles unique.

#### Scenario: First requirement
- **WHEN** the first requirement is checked
- **THEN** DataSpec accepts it

### Requirement: Duplicate title
DataSpec SHALL report duplicate requirement titles.

#### Scenario: Duplicate requirement
- **WHEN** the duplicate requirement is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/duplicate-requirement/spec.md')
    assert.equal(issue.line, 13)
    assert.match(issue.message, /Duplicate title/)
    assert.match(issue.message, /line 6/)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli normalizes duplicate Requirement titles before comparison', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'normalized-duplicate-requirement'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'normalized-duplicate-requirement', 'spec.md'),
      `# normalized-duplicate-requirement Specification

## Purpose
用于验证重复 Requirement 标题比较会忽略首尾空白和大小写。
## Requirements
### Requirement: Duplicate title
DataSpec SHALL normalize requirement titles before comparing duplicates.

#### Scenario: First normalized requirement
- **WHEN** the first requirement is checked
- **THEN** DataSpec accepts it

### Requirement:   duplicate TITLE${'  '}
DataSpec SHALL report normalized duplicate requirement titles.

#### Scenario: Normalized duplicate requirement
- **WHEN** the normalized duplicate is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(output.issues.length, 1)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].file, 'openspec/specs/normalized-duplicate-requirement/spec.md')
    assert.equal(issues[0].line, 13)
    assert.match(issues[0].message, /line 6/)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli normalizes internal whitespace in duplicate Requirement titles', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'internal-space-duplicate-requirement'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'internal-space-duplicate-requirement', 'spec.md'),
      `# internal-space-duplicate-requirement Specification

## Purpose
用于验证重复 Requirement 标题比较会压缩内部空白。
## Requirements
### Requirement: Duplicate title
DataSpec SHALL normalize repeated whitespace before comparing requirement titles.

#### Scenario: First requirement title
- **WHEN** the first requirement is checked
- **THEN** DataSpec accepts it

### Requirement: Duplicate   title
DataSpec SHALL report duplicate titles even when internal spacing differs.

#### Scenario: Internal whitespace duplicate
- **WHEN** the spaced duplicate is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(output.issues.length, 1)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].file, 'openspec/specs/internal-space-duplicate-requirement/spec.md')
    assert.equal(issues[0].line, 13)
    assert.match(issues[0].message, /line 6/)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Requirement entries without Scenario in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-scenario'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-scenario', 'spec.md'),
      `# missing-scenario Specification

## Purpose
用于验证 Requirement 缺少 Scenario 的主规格。
## Requirements
### Requirement: Missing scenario
DataSpec SHALL keep executable scenarios close to each requirement.

## Notes
#### Scenario: Too late
- **WHEN** a scenario appears in another section
- **THEN** it must not satisfy the previous requirement
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_SCENARIO_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/missing-scenario/spec.md')
    assert.equal(issue.line, 6)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Requirement entries without body text in main OpenSpec specs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-requirement-body'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-requirement-body', 'spec.md'),
      `# missing-requirement-body Specification

## Purpose
用于验证 Requirement 缺少正文说明的主规格。
## Requirements
### Requirement: Missing body

#### Scenario: Body is required
- **WHEN** status-check reads a requirement
- **THEN** it reports a requirement without body text
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_BODY_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/missing-requirement-body/spec.md')
    assert.equal(issue.line, 6)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Requirement body with punctuation-only text', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'punctuation-only-requirement-body'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'punctuation-only-requirement-body', 'spec.md'),
      `# punctuation-only-requirement-body Specification

## Purpose
用于验证 Requirement 正文不能只包含标点。
## Requirements
### Requirement: Punctuation-only body
：

#### Scenario: Body needs meaning
- **WHEN** status-check reads a requirement body
- **THEN** it reports punctuation-only body text
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((candidate) => candidate.code === 'OPENSPEC_SPEC_REQUIREMENT_BODY_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/punctuation-only-requirement-body/spec.md')
    assert.equal(issue.line, 6)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli does not count bare scenario step markers as Requirement body text', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'bare-scenario-steps'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'bare-scenario-steps', 'spec.md'),
      `# bare-scenario-steps Specification

## Purpose
用于验证裸 GIVEN/WHEN/THEN 步骤不能替代 Requirement 正文。
## Requirements
### Requirement: Bare steps
- **GIVEN** the author writes preconditions without a Scenario heading
- **WHEN** the author writes scenario steps without a Scenario heading
- **THEN** status-check still requires Requirement body text
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const bodyIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_BODY_MISSING')
    const scenarioIssue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_SCENARIO_MISSING')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(bodyIssue)
    assert.equal(bodyIssue.file, 'openspec/specs/bare-scenario-steps/spec.md')
    assert.equal(bodyIssue.line, 6)
    assert.ok(scenarioIssue)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli ignores Scenario entries under non-Requirement headings', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'scenario-under-notes'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'scenario-under-notes', 'spec.md'),
      `# scenario-under-notes Specification

## Purpose
用于验证非 Requirement 三级标题下的 Scenario 不会归属到前一个 Requirement。
## Requirements
### Requirement: Missing direct scenario
DataSpec SHALL keep scenarios directly under the requirement they verify.

### Notes
#### Scenario: Wrong parent heading
- **WHEN** a scenario appears under notes
- **THEN** it must not satisfy the previous requirement
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_REQUIREMENT_SCENARIO_MISSING')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/scenario-under-notes/spec.md')
    assert.equal(issue.line, 6)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Scenario entries without WHEN or THEN steps', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'missing-scenario-step'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'missing-scenario-step', 'spec.md'),
      `# missing-scenario-step Specification

## Purpose
用于验证 Scenario 缺少 WHEN 或 THEN 步骤的主规格。
## Requirements
### Requirement: Executable scenario steps
DataSpec SHALL keep each scenario executable.

#### Scenario: Missing then step
- **WHEN** the main spec is checked
- **AND** the scenario omits the outcome
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_STEPS_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/missing-scenario-step/spec.md')
    assert.equal(issue.line, 9)
    assert.match(issue.message, /WHEN.*THEN/)
    assert.equal(openSpecCheck.status, 'fail')
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Scenario GIVEN, WHEN or THEN steps without text', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'empty-given-step'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'empty-scenario-step'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'empty-then-step'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'empty-given-step', 'spec.md'),
      `# empty-given-step Specification

## Purpose
用于验证 Scenario GIVEN 步骤不能只有标记。
## Requirements
### Requirement: Executable given text
DataSpec SHALL keep scenario preconditions readable.

#### Scenario: Empty given text
- **GIVEN**
- **WHEN** status-check reads a scenario
- **THEN** status-check reports empty step text
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'empty-scenario-step', 'spec.md'),
      `# empty-scenario-step Specification

## Purpose
用于验证 Scenario WHEN 步骤不能只有标记。
## Requirements
### Requirement: Executable step text
DataSpec SHALL keep scenario steps readable.

#### Scenario: Empty when text
- **WHEN**
- **THEN** status-check reports empty step text
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'empty-then-step', 'spec.md'),
      `# empty-then-step Specification

## Purpose
用于验证 Scenario THEN 步骤不能只有标记。
## Requirements
### Requirement: Executable then text
DataSpec SHALL keep scenario outcomes readable.

#### Scenario: Empty then text
- **WHEN** status-check reads a scenario
- **THEN**
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_STEP_TEXT_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(issues.length, 3)
    assert.deepEqual(
      issues.map((issue) => [issue.file, issue.line]).sort(),
      [
        ['openspec/specs/empty-given-step/spec.md', 10],
        ['openspec/specs/empty-scenario-step/spec.md', 10],
        ['openspec/specs/empty-then-step/spec.md', 11]
      ]
    )
    assert.ok(issues.some((issue) => /GIVEN/.test(issue.message)))
    assert.ok(issues.some((issue) => /WHEN/.test(issue.message)))
    assert.ok(issues.some((issue) => /THEN/.test(issue.message)))
    assert.equal(openSpecCheck.errorCount, 3)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports Scenario steps with punctuation-only text', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'punctuation-only-step'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'punctuation-only-step', 'spec.md'),
      `# punctuation-only-step Specification

## Purpose
用于验证 Scenario 步骤不能只包含标点。
## Requirements
### Requirement: Executable step text
DataSpec SHALL require meaningful Scenario step text.

#### Scenario: Punctuation only when text
- **WHEN**：
- **THEN** status-check reports punctuation-only step text
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)
    const issues = output.issues.filter((candidate) => candidate.code === 'OPENSPEC_SPEC_SCENARIO_STEP_TEXT_MISSING')

    assert.equal(code, 1)
    assert.equal(issues.length, 1)
    const issue = issues[0]
    assert.equal(issue.file, 'openspec/specs/punctuation-only-step/spec.md')
    assert.equal(issue.line, 10)
    assert.match(issue.message, /WHEN/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports duplicate Scenario titles within one Requirement', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'duplicate-scenario'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'duplicate-scenario', 'spec.md'),
      `# duplicate-scenario Specification

## Purpose
用于验证同一 Requirement 下重复 Scenario 标题的主规格。
## Requirements
### Requirement: Scenario titles are unique per requirement
DataSpec SHALL keep scenario titles unique within one requirement.

#### Scenario: Duplicate scenario title
- **WHEN** the first scenario is checked
- **THEN** DataSpec accepts it

#### Scenario: Duplicate scenario title
- **WHEN** the duplicate scenario is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issue = output.issues.find((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.ok(issue)
    assert.equal(issue.file, 'openspec/specs/duplicate-scenario/spec.md')
    assert.equal(issue.line, 13)
    assert.match(issue.message, /Duplicate scenario title/)
    assert.match(issue.message, /line 9/)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli allows duplicate Scenario titles across different Requirements', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'scenario-scope'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'scenario-scope', 'spec.md'),
      `# scenario-scope Specification

## Purpose
用于验证 Scenario 标题去重只在单个 Requirement 内生效。
## Requirements
### Requirement: First scoped behavior
DataSpec SHALL allow scenario names to repeat across different requirements.

#### Scenario: Shared scenario title
- **WHEN** the first requirement is checked
- **THEN** DataSpec accepts the scenario title

### Requirement: Second scoped behavior
DataSpec SHALL reset scenario duplicate tracking for each requirement.

#### Scenario: Shared scenario title
- **WHEN** the second requirement is checked
- **THEN** DataSpec accepts the same scenario title in the new requirement
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.status, 'pass')
    assert.equal(output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_DUPLICATE').length, 0)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli normalizes duplicate Scenario titles before comparison', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'normalized-duplicate-scenario'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'normalized-duplicate-scenario', 'spec.md'),
      `# normalized-duplicate-scenario Specification

## Purpose
用于验证重复 Scenario 标题比较会忽略首尾空白和大小写。
## Requirements
### Requirement: Scenario title normalization
DataSpec SHALL normalize scenario titles before comparing duplicates.

#### Scenario: Shared scenario title
- **WHEN** the first scenario is checked
- **THEN** DataSpec accepts it

#### Scenario:   shared SCENARIO title${'  '}
- **WHEN** the normalized duplicate scenario is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(output.issues.length, 1)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].file, 'openspec/specs/normalized-duplicate-scenario/spec.md')
    assert.equal(issues[0].line, 13)
    assert.match(issues[0].message, /line 9/)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli normalizes internal whitespace in duplicate Scenario titles', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'internal-space-duplicate-scenario'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'internal-space-duplicate-scenario', 'spec.md'),
      `# internal-space-duplicate-scenario Specification

## Purpose
用于验证重复 Scenario 标题比较会压缩内部空白。
## Requirements
### Requirement: Scenario title normalization
DataSpec SHALL normalize repeated whitespace before comparing scenario titles.

#### Scenario: Shared scenario title
- **WHEN** the first scenario is checked
- **THEN** DataSpec accepts it

#### Scenario: Shared   scenario title
- **WHEN** the spaced duplicate scenario is checked
- **THEN** DataSpec reports it
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_DUPLICATE')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(output.issues.length, 1)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].file, 'openspec/specs/internal-space-duplicate-scenario/spec.md')
    assert.equal(issues[0].line, 13)
    assert.match(issues[0].message, /line 9/)
    assert.equal(openSpecCheck.errorCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runStatusCheckCli reports only the Scenario missing steps among multiple scenarios', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-status-check-'))
  try {
    await mkdir(path.join(dir, 'docs', 'archive'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-05-add-sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'sql-rule-debugger'), { recursive: true })
    await mkdir(path.join(dir, 'openspec', 'specs', 'multi-scenario-step'), { recursive: true })
    await writeFile(path.join(dir, 'TODO.md'), CLEAN_TODO, 'utf8')
    await writeFile(path.join(dir, 'README.md'), CLEAN_README, 'utf8')
    await writeFile(path.join(dir, 'docs', 'ai-contracts.md'), CLEAN_AI_CONTRACTS, 'utf8')
    await writeFile(path.join(dir, 'docs', 'archive', 'example.md'), '# Archive\n', 'utf8')
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'sql-rule-debugger', 'spec.md'),
      `# sql-rule-debugger Specification

## Purpose
用于解释 SQL 规则命中原因，帮助 AI 和开发者定位 lint 结果。
## Requirements
### Requirement: SQL rule debug endpoint
DataSpec SHALL expose a read-only SQL rule debug endpoint.

#### Scenario: Debug rule hit
- **WHEN** the user requests a rule explanation
- **THEN** DataSpec returns evidence
`,
      'utf8'
    )
    await writeFile(
      path.join(dir, 'openspec', 'specs', 'multi-scenario-step', 'spec.md'),
      `# multi-scenario-step Specification

## Purpose
用于验证同一 Requirement 下多个 Scenario 的步骤检查。
## Requirements
### Requirement: Multiple scenarios
DataSpec SHALL evaluate each scenario independently.

#### Scenario: Complete first scenario
- **WHEN** the first scenario is checked
- **THEN** DataSpec accepts it

#### Scenario: Missing then in second scenario
- **WHEN** the second scenario is checked
- **AND** it omits the outcome
`,
      'utf8'
    )
    const io = createIo()

    const code = await runStatusCheckCli(['--root', dir, '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    const issues = output.issues.filter((issue) => issue.code === 'OPENSPEC_SPEC_SCENARIO_STEPS_MISSING')
    const openSpecCheck = output.checks.find((check) => check.id === 'openspec-state')

    assert.equal(code, 1)
    assert.equal(output.status, 'fail')
    assert.equal(output.issues.length, 1)
    assert.equal(issues.length, 1)
    assert.equal(issues[0].file, 'openspec/specs/multi-scenario-step/spec.md')
    assert.equal(issues[0].line, 13)
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
