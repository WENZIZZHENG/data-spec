import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  buildReadinessReport,
  runReadinessCli
} from './dataspec-openspec-readiness.mjs'

const READY_PROPOSAL = `## Why

需要在开工前确认 OpenSpec change 是否具备足够上下文。

## What Changes

- 新增本地只读 readiness 检查。

## Capabilities

### New Capabilities
- \`openspec-readiness-check\`: 检查 OpenSpec change 准备度。

## Impact

- 工具范围：新增 tools/readiness。
- 验收标准：输出 JSON/text 报告。
- 边界：不自动实现、不自动归档、不写入 artifacts。
`

const READY_DESIGN = `## Context

本 change 用于开工前检查 OpenSpec artifacts。

## Goals / Non-Goals

**Goals:**
- 输出 readinessScore 和 missingFacts。

**Non-Goals:**
- 不替代人工判断。

## Decisions

1. 只读扫描 artifacts。

## Risks / Trade-offs

- [Risk] 静态规则可能误报。→ Mitigation：输出 humanQuestions。
`

const READY_SPEC = `## ADDED Requirements

### Requirement: Readiness report
DataSpec SHALL report OpenSpec readiness.

#### Scenario: Ready report
- **WHEN** a developer checks a complete change
- **THEN** DataSpec returns a READY report
`

const READY_TASKS = `## 1. 实现

- [ ] 1.1 新增 readiness 脚本。

## 2. 验证

- [ ] 2.1 运行 \`openspec validate add-ready-change --strict\`。
- [ ] 2.2 运行 \`node --test tools/dataspec-openspec-readiness.test.mjs\`。
- [ ] 2.3 运行 \`git diff --check\`。

## Verification Evidence

- 待实现后记录。
`

test('buildReadinessReport returns stable READY JSON for complete change artifacts', () => {
  const report = buildReadinessReport({
    changeId: 'add-ready-change',
    changeRoot: 'openspec/changes/add-ready-change',
    artifacts: {
      proposal: READY_PROPOSAL,
      design: READY_DESIGN,
      tasks: READY_TASKS,
      specs: new Map([['openspec-readiness-check', READY_SPEC]])
    }
  })

  assert.equal(report.kind, 'dataspec.openspec-readiness')
  assert.equal(report.schemaVersion, 1)
  assert.equal(report.change.id, 'add-ready-change')
  assert.equal(report.readinessLevel, 'READY')
  assert.ok(report.readinessScore >= 80)
  assert.deepEqual(report.affectedSpecs, ['openspec-readiness-check'])
  assert.deepEqual(report.missingFacts, [])
  assert.ok(report.validationPlan.some((item) => item.command === 'openspec validate add-ready-change --strict'))
  assert.ok(report.validationPlan.some((item) => item.command === 'node --test tools/dataspec-openspec-readiness.test.mjs'))
  assert.match(report.reviewBoundary.summary, /只读|人工|评审/)
  assert.ok(Array.isArray(report.checks))
  assert.ok(report.nextActions.some((item) => /实施/.test(item) || /验证/.test(item)))
})

test('buildReadinessReport ignores TODO phrase and non-command code spans', () => {
  const report = buildReadinessReport({
    changeId: 'add-ready-change',
    changeRoot: 'openspec/changes/add-ready-change',
    artifacts: {
      proposal: `${READY_PROPOSAL}\n\n说明：TODO 到 OpenSpec 是功能名称，不是占位符。\n`,
      design: `${READY_DESIGN}\n\n涉及 capability \`openspec-readiness-check\` 和目录 \`openspec/changes/<change-id>\`。\n`,
      tasks: READY_TASKS,
      specs: new Map([['openspec-readiness-check', READY_SPEC]])
    }
  })

  assert.equal(report.readinessLevel, 'READY')
  assert.ok(!report.missingFacts.some((item) => item.code === 'PLACEHOLDER_TEXT_PRESENT'))
  assert.ok(!report.validationPlan.some((item) => item.command === 'openspec validate'))
  assert.ok(!report.validationPlan.some((item) => item.command === 'openspec-readiness-check'))
  assert.ok(!report.validationPlan.some((item) => /<change-id>/.test(item.command)))
  assert.ok(!report.validationPlan.some((item) => item.command === 'openspec/changes/<change-id>'))
})

test('buildReadinessReport flags empty spec delta even when spec file exists', () => {
  const report = buildReadinessReport({
    changeId: 'add-empty-spec-change',
    changeRoot: 'openspec/changes/add-empty-spec-change',
    artifacts: {
      proposal: READY_PROPOSAL,
      design: READY_DESIGN,
      tasks: READY_TASKS,
      specs: new Map([['empty-spec', '']])
    }
  })

  assert.notEqual(report.readinessLevel, 'READY')
  assert.ok(report.missingFacts.some((item) => item.code === 'SPEC_DELTA_MISSING'))
  assert.deepEqual(report.affectedSpecs, [])
})

test('buildReadinessReport flags bare TODO placeholders but keeps allowed product phrase', () => {
  const report = buildReadinessReport({
    changeId: 'add-placeholder-change',
    changeRoot: 'openspec/changes/add-placeholder-change',
    artifacts: {
      proposal: `${READY_PROPOSAL}\n\n说明：TODO 到 OpenSpec 是功能名称。\n`,
      design: `${READY_DESIGN}\n\nTODO\n`,
      tasks: READY_TASKS,
      specs: new Map([['openspec-readiness-check', READY_SPEC]])
    }
  })

  assert.notEqual(report.readinessLevel, 'READY')
  assert.ok(report.missingFacts.some((item) => item.code === 'PLACEHOLDER_TEXT_PRESENT'))
})

test('buildReadinessReport redacts secrets from extracted validation commands', () => {
  const report = buildReadinessReport({
    changeId: 'add-secret-command-change',
    changeRoot: 'openspec/changes/add-secret-command-change',
    artifacts: {
      proposal: `${READY_PROPOSAL}\n\n- 验证：\`git clone https://token123@example.com/org/repo.git\`。\n`,
      design: `${READY_DESIGN}\n\n- 验证：\`curl -H "Authorization: Bearer raw-token" http://localhost/check\`。\n`,
      tasks: `${READY_TASKS}\n- [ ] 2.4 运行 \`node tool.mjs --dsn postgres://user:pass@example.com/db --password=secret123\`。\n`,
      specs: new Map([['openspec-readiness-check', READY_SPEC]])
    }
  })
  const commands = report.validationPlan.map((item) => item.command).join('\n')

  assert.doesNotMatch(commands, /token123|raw-token|secret123|user:pass/i)
  assert.match(commands, /\*\*\*/)
})

test('buildReadinessReport flags empty Impact section as missing impact', () => {
  const report = buildReadinessReport({
    changeId: 'add-empty-impact-change',
    changeRoot: 'openspec/changes/add-empty-impact-change',
    artifacts: {
      proposal: READY_PROPOSAL.replace(/## Impact[\s\S]*$/, '## Impact\n\n'),
      design: READY_DESIGN,
      tasks: READY_TASKS,
      specs: new Map([['openspec-readiness-check', READY_SPEC]])
    }
  })

  assert.notEqual(report.readinessLevel, 'READY')
  assert.ok(report.missingFacts.some((item) => item.code === 'PROPOSAL_IMPACT_MISSING'))
})

test('buildReadinessReport reports missing facts and questions for incomplete change artifacts', () => {
  const report = buildReadinessReport({
    changeId: 'add-incomplete-change',
    changeRoot: 'openspec/changes/add-incomplete-change',
    artifacts: {
      proposal: `## Why

需要处理一个待办。

## What Changes

- TODO。
`,
      design: '',
      tasks: `## 1. 实现

- [ ] 1.1 做完功能。
`,
      specs: new Map()
    }
  })

  const codes = report.missingFacts.map((item) => item.code)

  assert.equal(report.change.id, 'add-incomplete-change')
  assert.notEqual(report.readinessLevel, 'READY')
  assert.ok(report.readinessScore < 80)
  assert.ok(codes.includes('PROPOSAL_IMPACT_MISSING'))
  assert.ok(codes.includes('ACCEPTANCE_CRITERIA_MISSING'))
  assert.ok(codes.includes('BOUNDARY_MISSING'))
  assert.ok(codes.includes('SPEC_DELTA_MISSING'))
  assert.ok(codes.includes('VALIDATION_COMMAND_MISSING'))
  assert.ok(report.humanQuestions.some((item) => /验收|边界|影响/.test(item)))
  assert.ok(report.validationPlan.some((item) => item.command === 'openspec validate add-incomplete-change --strict'))
})

test('runReadinessCli rejects unsafe, missing, and archived changes with code 2', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-readiness-'))
  try {
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-08-add-old-change'), { recursive: true })

    const unsafeIo = createIo()
    const unsafeCode = await runReadinessCli(['--root', dir, '--change', '../bad', '--format', 'json'], unsafeIo)
    assert.equal(unsafeCode, 2)
    assert.match(unsafeIo.stderr, /change id|安全|无效|unsafe/i)

    const missingIo = createIo()
    const missingCode = await runReadinessCli(['--root', dir, '--change', 'add-missing-change', '--format', 'json'], missingIo)
    assert.equal(missingCode, 2)
    assert.match(missingIo.stderr, /不存在|not found/i)

    const archivedIo = createIo()
    const archivedCode = await runReadinessCli(['--root', dir, '--change', 'add-old-change', '--format', 'json'], archivedIo)
    assert.equal(archivedCode, 2)
    assert.match(archivedIo.stderr, /archive|归档/i)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runReadinessCli only treats exact archive change id as archived', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-readiness-'))
  try {
    await mkdir(path.join(dir, 'openspec', 'changes', 'archive', '2026-07-08-prefix-add-short-change'), { recursive: true })

    const io = createIo()
    const code = await runReadinessCli(['--root', dir, '--change', 'add-short-change', '--format', 'json'], io)

    assert.equal(code, 2)
    assert.match(io.stderr, /不存在|not found/i)
    assert.doesNotMatch(io.stderr, /archive|归档/i)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('runReadinessCli returns code 0 for low-score advisory report', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-readiness-'))
  try {
    const changeDir = path.join(dir, 'openspec', 'changes', 'add-low-score-change')
    await mkdir(changeDir, { recursive: true })
    await writeFile(path.join(changeDir, 'proposal.md'), '## Why\n\nTODO\n', 'utf8')
    await writeFile(path.join(changeDir, 'tasks.md'), '## 1. 实现\n\n- [ ] 1.1 TODO\n', 'utf8')

    const io = createIo()
    const code = await runReadinessCli(['--root', dir, '--change', 'add-low-score-change', '--format', 'json'], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.kind, 'dataspec.openspec-readiness')
    assert.equal(output.change.id, 'add-low-score-change')
    assert.equal(output.readinessLevel, 'BLOCKED')
    assert.ok(output.missingFacts.length > 0)
    assert.equal(io.stderr, '')
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
