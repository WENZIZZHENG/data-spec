#!/usr/bin/env node

import { readdir, readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { supportedWorkflowRecipeIds } from './dataspec-workflows.mjs'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const REPO_ROOT = path.resolve(path.dirname(SCRIPT_PATH), '..')
const KIND = 'dataspec.status-check'
const SCHEMA_VERSION = 1
const OPENSPEC_PURPOSE_PLACEHOLDER_PATTERN = /TBD\s*-\s*created by archiving change|Update Purpose after archive/i
// 低于该长度的 Purpose 通常仍是模板式占位，AI 无法据此判断能力服务的项目目标。
const OPENSPEC_PURPOSE_MIN_MEANINGFUL_CHARS = 6
const SKIPPED_DIRS = new Set([
  '.git',
  'node_modules',
  'target',
  'dist',
  'build',
  '.idea',
  '.vscode'
])

const FIELD_MAP = new Map([
  ['状态', 'status'],
  ['为什么做', 'why'],
  ['已有基础', 'foundation'],
  ['缺口', 'gap'],
  ['已完成能力', 'done'],
  ['落地产物', 'deliverables'],
  ['验收标准', 'acceptance'],
  ['后续增强', 'followUp'],
  ['边界', 'boundary']
])

/**
 * 构建 DataSpec 本地状态报告。
 *
 * `workflowRecipeIds` 应来自 `supportedWorkflowRecipeIds()`；AI 可见 workflow recipe
 * 是跨 CLI、MCP、AI Context 和任务卡复用的契约，不能在文档中手写漂移。
 * `openSpecActiveChangeTexts` 使用仓库相对路径作为 key，仅表示仍在开发的 active change；
 * archive 历史文档由 CLI 跳过，避免旧链接阻塞当前状态检查。
 */
export function buildStatusReport(input = {}) {
  const todoText = String(input.todoText ?? '')
  const readmeText = String(input.readmeText ?? '')
  const aiContractsText = Object.hasOwn(input, 'aiContractsText')
    ? input.aiContractsText === null ? null : String(input.aiContractsText ?? '')
    : undefined
  const workflowRecipeIds = normalizeWorkflowRecipeIds(input.workflowRecipeIds ?? [])
  const relativeFiles = normalizeRelativeFiles(input.relativeFiles ?? new Set())
  const changeEntries = [...(input.openSpecChangeEntries ?? [])].map(String)
  const specEntries = new Set([...(input.openSpecSpecEntries ?? [])].map(String))
  const specTexts = normalizeOpenSpecSpecTexts(input.openSpecSpecTexts ?? new Map())
  const activeChangeTexts = normalizeOpenSpecActiveChangeTexts(input.openSpecActiveChangeTexts ?? new Map())
  const todoItems = parseTodoItems(todoText)
  const queueItems = parseQueueItems(todoText)
  const issues = []

  checkQueueItems(queueItems, todoItems, todoText, issues)
  checkCompletedItems(todoItems, issues)
  checkOpenSpecState(todoItems, changeEntries, specEntries, specTexts, relativeFiles, todoText, issues)
  checkReadmeToolEntry(readmeText, relativeFiles, issues)
  checkWorkflowRecipeContracts(todoText, aiContractsText, workflowRecipeIds, issues)
  checkMarkdownLinks('README.md', readmeText, relativeFiles, issues)
  checkMarkdownLinks('TODO.md', todoText, relativeFiles, issues)
  if (typeof aiContractsText === 'string') {
    checkMarkdownLinks('docs/ai-contracts.md', aiContractsText, relativeFiles, issues)
  }
  for (const [capability, specText] of specTexts) {
    checkMarkdownLinks(`openspec/specs/${capability}/spec.md`, specText, relativeFiles, issues)
  }
  for (const [file, changeText] of [...activeChangeTexts.entries()].sort(([left], [right]) => left.localeCompare(right))) {
    checkMarkdownLinks(file, changeText, relativeFiles, issues)
  }

  const errors = issues.filter((issue) => issue.severity === 'error').length
  const warnings = issues.filter((issue) => issue.severity === 'warning').length
  const status = errors > 0 ? 'fail' : warnings > 0 ? 'warn' : 'pass'

  return {
    kind: KIND,
    schemaVersion: SCHEMA_VERSION,
    status,
    summary: {
      todoItems: todoItems.length,
      queueItems,
      activeChanges: changeEntries.filter((entry) => entry !== 'archive'),
      specs: [...specEntries].sort(),
      totalIssues: issues.length,
      errors,
      warnings,
      issueCodes: buildIssueCodeSummary(issues)
    },
    checks: buildChecks(issues),
    issues,
    nextActions: buildNextActions(status, issues)
  }
}

export function formatStatusReportText(report) {
  const lines = [
    'DataSpec 文档状态检查',
    '',
    `状态：${report.status}`,
    `TODO 条目：${report.summary.todoItems}`,
    `近期队列：${report.summary.queueItems.join('、') || '无'}`,
    `active changes：${report.summary.activeChanges.join('、') || '无'}`,
    `问题：${report.summary.totalIssues} 个`
  ]

  if (Array.isArray(report.checks) && report.checks.length > 0) {
    lines.push('', '检查项:')
    for (const check of report.checks) {
      lines.push(
        `- ${check.id} (${check.name}): status=${check.status} issues=${check.issueCount} errors=${check.errorCount} warnings=${check.warningCount}`
      )
    }
  }

  if (report.issues.length > 0) {
    lines.push('', '问题明细:')
    report.issues.forEach((issue, index) => {
      const location = issue.file ? ` ${issue.file}${issue.line ? `:${issue.line}` : ''}` : ''
      lines.push(`${index + 1}. [${issue.severity}] ${issue.code}${location}`)
      lines.push(`   ${issue.message}`)
      lines.push(`   建议: ${issue.suggestedFix}`)
    })
  }

  lines.push('', '下一步:')
  for (const action of report.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${lines.join('\n')}\n`
}

export async function runStatusCheckCli(args = process.argv.slice(2), io = defaultIo()) {
  try {
    const options = parseArgs(args)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }

    const root = path.resolve(options.root)
    const [todoText, readmeText, aiContractsText, relativeFiles, changeEntries, specEntries, specTexts, changeTexts] = await Promise.all([
      readFile(path.resolve(root, options.todoPath), 'utf8'),
      readFile(path.resolve(root, options.readmePath), 'utf8'),
      readOptionalFile(path.resolve(root, 'docs', 'ai-contracts.md')),
      collectRelativePaths(root),
      readDirectoryNames(path.join(root, 'openspec', 'changes')),
      readDirectoryNames(path.join(root, 'openspec', 'specs')),
      readOpenSpecMainSpecTexts(path.join(root, 'openspec', 'specs')),
      readOpenSpecActiveChangeTexts(path.join(root, 'openspec', 'changes'))
    ])
    const report = buildStatusReport({
      todoText,
      readmeText,
      aiContractsText,
      workflowRecipeIds: supportedWorkflowRecipeIds(),
      relativeFiles,
      openSpecChangeEntries: changeEntries,
      openSpecSpecEntries: specEntries,
      openSpecSpecTexts: specTexts,
      openSpecActiveChangeTexts: changeTexts
    })

    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(report, null, 2)}\n`)
    } else {
      io.writeOut(formatStatusReportText(report))
    }
    return report.summary.errors > 0 ? 1 : 0
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
    return 2
  }
}

export function parseTodoItems(todoText) {
  const lines = String(todoText ?? '').split(/\r?\n/)
  const items = []
  let current = null

  lines.forEach((line, index) => {
    const headingMatch = /^###\s+(P\d+-\d+)[：:]\s*(.+?)\s*$/.exec(line)
    if (headingMatch) {
      current = {
        id: headingMatch[1],
        title: headingMatch[2],
        line: index + 1,
        status: '',
        fields: {},
        bodyLines: []
      }
      items.push(current)
      return
    }

    if (!current) {
      return
    }
    if (/^###\s+P\d+-\d+[：:]/.test(line)) {
      return
    }
    current.bodyLines.push(line)
    const fieldMatch = /^-\s*([^：:]+)[：:]\s*(.*)$/.exec(line.trim())
    if (!fieldMatch) {
      return
    }
    const key = FIELD_MAP.get(fieldMatch[1].trim())
    if (!key) {
      return
    }
    current.fields[key] = fieldMatch[2].trim()
    if (key === 'status') {
      current.status = fieldMatch[2].trim()
    }
  })

  return items
}

function parseQueueItems(todoText) {
  const line = String(todoText ?? '').split(/\r?\n/).find((candidate) => candidate.includes('近期只保留'))
  if (!line) {
    return []
  }
  // 只读取队列冒号后的编号，避免把“P6-71 到 P6-188 全量顺扫”这类范围说明误判为待开工项。
  const queuePart = line.split(/[：:]/).pop() ?? line
  return [...new Set([...queuePart.matchAll(/P\d+-\d+/g)].map((match) => match[0]))]
}

function checkQueueItems(queueItems, todoItems, todoText, issues) {
  const itemById = new Map(todoItems.map((item) => [item.id, item]))
  const queueLine = findLine(todoText, (line) => line.includes('近期只保留'))
  const declaredCount = queueLine ? parseDeclaredQueueCount(queueLine.text) : null
  if (declaredCount !== null && declaredCount !== queueItems.length) {
    issues.push(issue({
      code: 'TODO_QUEUE_COUNT_MISMATCH',
      message: `近期队列声明 ${declaredCount} 个优先行动项，但实际列出 ${queueItems.length} 个。`,
      file: 'TODO.md',
      line: queueLine.line,
      suggestedFix: '同步顶部“近期只保留 N 个优先行动项”的数量和后面的 TODO 编号列表。'
    }))
  }
  for (const reference of findQueueCountReferences(todoText)) {
    if (reference.count === queueItems.length) {
      continue
    }
    issues.push(issue({
      code: 'TODO_QUEUE_SUMMARY_COUNT_MISMATCH',
      message: `TODO 摘要引用了 ${reference.count} 项队列，但当前近期队列实际为 ${queueItems.length} 项。`,
      file: 'TODO.md',
      line: reference.line,
      suggestedFix: '同步摘要中的近期队列数量，或从摘要中移除容易漂移的固定数字。'
    }))
  }
  for (const id of queueItems) {
    const item = itemById.get(id)
    if (!item) {
      issues.push(issue({
        code: 'TODO_QUEUE_ITEM_MISSING',
        message: `近期队列引用了不存在的 TODO 条目 ${id}。`,
        file: 'TODO.md',
        suggestedFix: `补齐 ${id} 条目，或从顶部近期队列移除该编号。`
      }))
      continue
    }
    if (!isPending(item)) {
      issues.push(issue({
        code: 'TODO_QUEUE_ITEM_NOT_PENDING',
        message: `近期队列中的 ${id} 当前状态不是待办：${item.status || '缺失状态'}。`,
        file: 'TODO.md',
        line: item.line,
        suggestedFix: `把 ${id} 从近期队列移除，或修正该条目的状态。`
      }))
    }
  }
}

function parseDeclaredQueueCount(line) {
  const match = /近期只保留\s*(\d+)\s*个/.exec(line)
  return match ? Number(match[1]) : null
}

function findQueueCountReferences(todoText) {
  return String(todoText ?? '').split(/\r?\n/).flatMap((line, index) => {
    const references = []
    for (const match of line.matchAll(/(?:顶部|近期).*?(\d+)\s*项(?:优先行动)?队列/g)) {
      if (line.includes('近期只保留')) {
        continue
      }
      references.push({
        line: index + 1,
        count: Number(match[1])
      })
    }
    return references
  })
}

function findLine(text, predicate) {
  const lines = String(text ?? '').split(/\r?\n/)
  for (let index = 0; index < lines.length; index += 1) {
    if (predicate(lines[index])) {
      return {
        line: index + 1,
        text: lines[index]
      }
    }
  }
  return null
}

function checkCompletedItems(todoItems, issues) {
  for (const item of todoItems) {
    if (isPending(item)) {
      continue
    }
    if (item.fields.gap) {
      issues.push(issue({
        code: 'TODO_COMPLETED_HAS_GAP',
        message: `${item.id} 已完成但仍保留“缺口”字段，容易被误判为未实现。`,
        file: 'TODO.md',
        line: item.line,
        suggestedFix: '将“缺口/落地产物/验收标准”改为“已完成能力/后续增强/验证证据”等完成态描述。'
      }))
    }
  }
}

function checkOpenSpecState(todoItems, changeEntries, specEntries, specTexts, relativeFiles, todoText, issues) {
  const claimsActiveQueueEmpty = /active change\s*队列(?:恢复)?为空/.test(todoText)
  const activeChanges = new Set(changeEntries.filter((entry) => entry !== 'archive'))
  const staleQueueLine = activeChanges.size === 0 ? findNonEmptyActiveQueueClaim(todoText) : null
  if (staleQueueLine !== null) {
    issues.push(issue({
      code: 'OPENSPEC_ACTIVE_QUEUE_TEXT_STALE',
      message: 'TODO.md 声明 active change 队列仍有实施项，但 openspec/changes 下没有 active change。',
      file: 'TODO.md',
      line: staleQueueLine,
      suggestedFix: '将 active change 队列状态改为空，或恢复实际仍在实施的 openspec/changes/<change-id> 目录。'
    }))
  }
  for (const entry of changeEntries) {
    if (entry === 'archive') {
      continue
    }
    if (!claimsActiveQueueEmpty) {
      issues.push(issue({
        code: 'OPENSPEC_ACTIVE_CHANGE_PRESENT',
        severity: 'warning',
        message: `openspec/changes 根目录存在 active change：${entry}。`,
        file: `openspec/changes/${entry}`,
        suggestedFix: '如果该 change 正在实施，可保留；完成后再归档并同步 TODO/主规格。'
      }))
      continue
    }
    issues.push(issue({
      code: 'OPENSPEC_ACTIVE_CHANGE_LEFTOVER',
      message: `openspec/changes 根目录仍存在 active change：${entry}。`,
      file: `openspec/changes/${entry}`,
      suggestedFix: '确认该 change 是否仍在开发；已完成则归档，未完成则在 TODO 中保持待办状态。'
    }))
  }

  checkTodoActiveChangeReferences(todoText, activeChanges, issues)

  for (const item of todoItems) {
    const changeIds = extractArchivedChangeIds(item)
    for (const changeId of changeIds) {
      const archiveExists = archivedChangeExists(changeId, relativeFiles)
      if (!archiveExists) {
        issues.push(issue({
          code: 'OPENSPEC_ARCHIVE_MISSING',
          message: `${item.id} 声明 OpenSpec change \`${changeId}\` 已归档，但 archive 中未找到对应目录。`,
          file: 'TODO.md',
          line: item.line,
          suggestedFix: `归档 ${changeId}，或修正 ${item.id} 的状态说明。`
        }))
      }

      const expectedCapabilities = archivedSpecCapabilities(changeId, relativeFiles)
      const capabilities = expectedCapabilities.length > 0 ? expectedCapabilities : [changeId.replace(/^add-/, '')]
      for (const capability of capabilities) {
        const specPath = `openspec/specs/${capability}/spec.md`
        if (relativeFiles.has(specPath) || specEntries.has(capability)) {
          continue
        }
        issues.push(issue({
          code: 'OPENSPEC_MAIN_SPEC_MISSING',
          message: `${item.id} 对应的主规格 ${specPath} 不存在。`,
          file: 'TODO.md',
          line: item.line,
          suggestedFix: `确认 ${changeId} archive 是否已同步到 openspec/specs/${capability}/spec.md。`
        }))
      }
    }
  }

  checkOpenSpecSpecTitles(specTexts, issues)
  checkOpenSpecSpecPurposes(specTexts, issues)
  checkOpenSpecSpecRequirements(specTexts, issues)
}

function checkTodoActiveChangeReferences(todoText, activeChanges, issues) {
  for (const reference of extractTodoActiveChangeReferences(todoText)) {
    if (activeChanges.has(reference.changeId)) {
      continue
    }
    issues.push(issue({
      code: 'OPENSPEC_ACTIVE_CHANGE_REFERENCE_MISSING',
      message: `TODO.md 仍声明 OpenSpec change \`${reference.changeId}\` 处于 active/未归档状态，但 openspec/changes/${reference.changeId} 不存在。`,
      file: 'TODO.md',
      line: reference.line,
      suggestedFix: `把 ${reference.changeId} 的 TODO 状态改为已归档，或恢复 openspec/changes/${reference.changeId} active change 目录。`
    }))
  }
}

function checkOpenSpecSpecTitles(specTexts, issues) {
  for (const [capability, text] of specTexts) {
    const lines = String(text ?? '').split(/\r?\n/)
    const titleIndex = lines.findIndex((line) => line.trim().length > 0)
    const specPath = `openspec/specs/${capability}/spec.md`
    const expectedTitle = `# ${capability} Specification`
    if (titleIndex === -1 || !/^#\s+/.test(lines[titleIndex].trim())) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_TITLE_MISSING',
        message: `${specPath} 缺少顶层标题 ${expectedTitle}，AI 读取主规格时会缺少稳定 capability 标识。`,
        file: specPath,
        line: titleIndex === -1 ? 1 : titleIndex + 1,
        suggestedFix: `在文件开头补充 ${expectedTitle}。`
      }))
      continue
    }
    if (lines[titleIndex].trim() !== expectedTitle) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_TITLE_MISMATCH',
        message: `${specPath} 的顶层标题应为 ${expectedTitle}，当前标题会让 AI 将能力归属到错误 capability。`,
        file: specPath,
        line: titleIndex + 1,
        suggestedFix: `将顶层标题改为 ${expectedTitle}，并保持目录名与 capability 名一致。`
      }))
    }
  }
}

function checkOpenSpecSpecPurposes(specTexts, issues) {
  for (const [capability, text] of specTexts) {
    const lines = String(text ?? '').split(/\r?\n/)
    const purposeIndex = lines.findIndex((line) => /^##\s+Purpose\s*$/.test(line.trim()))
    const specPath = `openspec/specs/${capability}/spec.md`
    if (purposeIndex === -1) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_PURPOSE_MISSING',
        message: `${specPath} 缺少 ## Purpose 小节，AI 读取主规格时会缺少能力目的说明。`,
        file: specPath,
        line: 1,
        suggestedFix: '在 Requirements 前补充 ## Purpose，并用一句稳定中文说明该能力服务的项目目标。'
      }))
      continue
    }
    const purposeBody = collectPurposeBody(lines, purposeIndex)
    if (!purposeBody.hasBody) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_PURPOSE_EMPTY',
        message: `${specPath} 的 ## Purpose 小节为空，AI 读取主规格时会缺少能力目的说明。`,
        file: specPath,
        line: purposeIndex + 1,
        suggestedFix: '在 ## Purpose 下补充一句稳定中文能力目的说明，并确认不改变 Requirements/Scenario 语义。'
      }))
      continue
    }
    if (isPurposeTooShort(purposeBody.text)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_PURPOSE_TOO_SHORT',
        message: `${specPath} 的 ## Purpose 小节说明过短，AI 难以判断该能力服务的项目目标。`,
        file: specPath,
        line: purposeBody.firstLine,
        suggestedFix: '将 Purpose 扩展为一句稳定中文说明，至少点明能力对象、使用场景或对数据字段标准维护的价值。'
      }))
      continue
    }
    const placeholder = purposeBody.lines.find((line) => OPENSPEC_PURPOSE_PLACEHOLDER_PATTERN.test(line.text))
    if (!placeholder) {
      continue
    }
    issues.push(issue({
      code: 'OPENSPEC_SPEC_PURPOSE_PLACEHOLDER',
      message: `${specPath} 仍包含归档生成的默认 Purpose 占位，AI 读取主规格时会缺少能力目的说明。`,
      file: specPath,
      line: placeholder.line,
      suggestedFix: '将默认 Purpose 占位替换为一句稳定的中文能力目的说明，并确认不改变 Requirements/Scenario 语义。'
    }))
  }
}

function collectPurposeBody(lines, purposeIndex) {
  const bodyLines = []
  let firstLine
  for (let index = purposeIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      break
    }
    if (/^#{3,6}\s+/.test(line)) {
      continue
    }
    if (line) {
      firstLine ??= index + 1
      bodyLines.push({ text: line, line: index + 1 })
    }
  }
  return {
    hasBody: bodyLines.length > 0,
    text: bodyLines.map((line) => line.text).join(' '),
    firstLine,
    lines: bodyLines
  }
}

function isPurposeTooShort(text) {
  const meaningfulText = String(text ?? '').replace(/[\s\p{P}\p{S}]/gu, '')
  return meaningfulText.length < OPENSPEC_PURPOSE_MIN_MEANINGFUL_CHARS
}

function checkOpenSpecSpecRequirements(specTexts, issues) {
  for (const [capability, text] of specTexts) {
    const lines = String(text ?? '').split(/\r?\n/)
    const requirementsIndex = lines.findIndex((line) => /^##\s+Requirements\s*$/.test(line.trim()))
    const specPath = `openspec/specs/${capability}/spec.md`
    if (requirementsIndex === -1) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENTS_MISSING',
        message: `${specPath} 缺少 ## Requirements 小节，AI 读取主规格时会缺少可执行能力契约。`,
        file: specPath,
        line: 1,
        suggestedFix: '补充 ## Requirements，并至少包含一个 ### Requirement: 条目和对应 Scenario。'
      }))
      continue
    }
    if (!hasRequirementEntry(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENTS_EMPTY',
        message: `${specPath} 的 ## Requirements 小节缺少 ### Requirement: 条目，AI 无法稳定识别能力契约。`,
        file: specPath,
        line: requirementsIndex + 1,
        suggestedFix: '在 ## Requirements 下补充至少一个 ### Requirement: 条目，并保留 OpenSpec Scenario 结构。'
      }))
      continue
    }
    for (const requirement of findRequirementsWithMissingTitle(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENT_TITLE_MISSING',
        message: `${specPath} 的 Requirement 标题为空，AI 难以唯一引用能力契约。`,
        file: specPath,
        line: requirement.line,
        suggestedFix: '在 ### Requirement: 后补充稳定、唯一的能力契约标题。'
      }))
    }
    for (const duplicate of findDuplicateRequirements(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENT_DUPLICATE',
        message: `${specPath} 的 ${duplicate.title} 与 line ${duplicate.firstLine} 的 Requirement 标题重复，AI 难以唯一引用能力契约。`,
        file: specPath,
        line: duplicate.line,
        suggestedFix: '为重复的 Requirement 使用唯一标题，或合并为一个 Requirement 下的多个 Scenario。'
      }))
    }
    for (const requirement of findRequirementsWithoutBody(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENT_BODY_MISSING',
        message: `${specPath} 的 ${requirement.title} 缺少 Requirement 正文说明，AI 难以理解该能力契约的业务语义。`,
        file: specPath,
        line: requirement.line,
        suggestedFix: '在 Requirement 标题下、Scenario 前补充一句稳定说明，说明该能力必须满足的业务行为或约束。'
      }))
    }
    for (const requirement of findRequirementsWithoutScenario(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_REQUIREMENT_SCENARIO_MISSING',
        message: `${specPath} 的 ${requirement.title} 缺少 #### Scenario:，AI 无法把能力契约映射成可验证行为。`,
        file: specPath,
        line: requirement.line,
        suggestedFix: '在该 Requirement 下补充至少一个 #### Scenario:，描述 WHEN/THEN 或等价验收行为。'
      }))
    }
    for (const scenario of findScenariosWithMissingSteps(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_SCENARIO_STEPS_MISSING',
        message: `${specPath} 的 ${scenario.title} 缺少 - **WHEN** 或 - **THEN** 步骤，AI 难以稳定执行验收判断。`,
        file: specPath,
        line: scenario.line,
        suggestedFix: '在该 Scenario 下补齐 - **WHEN** 与 - **THEN** 步骤；可按需保留 - **AND** 作为补充条件。'
      }))
    }
    for (const scenario of findScenariosWithMissingTitle(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_SCENARIO_TITLE_MISSING',
        message: `${specPath} 的 Scenario 标题为空，AI 难以唯一引用验收行为。`,
        file: specPath,
        line: scenario.line,
        suggestedFix: '在 #### Scenario: 后补充稳定、唯一的验收场景标题。'
      }))
    }
    for (const step of findScenariosWithEmptyStepText(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_SCENARIO_STEP_TEXT_MISSING',
        message: `${specPath} 的 ${step.kind} 步骤缺少文本，AI 难以稳定执行验收判断。`,
        file: specPath,
        line: step.line,
        suggestedFix: `在 - **${step.kind}** 后补充具体触发条件或预期结果。`
      }))
    }
    for (const duplicate of findDuplicateScenarios(lines, requirementsIndex)) {
      issues.push(issue({
        code: 'OPENSPEC_SPEC_SCENARIO_DUPLICATE',
        message: `${specPath} 的 ${duplicate.title} 与 line ${duplicate.firstLine} 的 Scenario 标题重复，AI 难以唯一引用验收行为。`,
        file: specPath,
        line: duplicate.line,
        suggestedFix: '为同一 Requirement 下的重复 Scenario 使用唯一标题，或合并重复验收行为。'
      }))
    }
  }
}

function hasRequirementEntry(lines, requirementsIndex) {
  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return false
    }
    if (/^###\s+Requirement\s*:/.test(line)) {
      return true
    }
  }
  return false
}

function findDuplicateRequirements(lines, requirementsIndex) {
  const seen = new Map()
  const duplicates = []

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return duplicates
    }
    if (!/^###\s+Requirement\s*:/.test(line)) {
      continue
    }
    const normalizedTitle = line.replace(/^###\s+Requirement\s*:\s*/, '').trim().toLowerCase()
    if (!normalizedTitle) {
      continue
    }
    if (seen.has(normalizedTitle)) {
      duplicates.push({ line: index + 1, firstLine: seen.get(normalizedTitle), title: line })
      continue
    }
    seen.set(normalizedTitle, index + 1)
  }

  return duplicates
}

function findRequirementsWithMissingTitle(lines, requirementsIndex) {
  const missing = []
  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return missing
    }
    const match = /^###\s+Requirement\s*:\s*(?<title>.*)$/.exec(line)
    if (match && !match.groups.title.trim()) {
      missing.push({ line: index + 1 })
    }
  }
  return missing
}

function findRequirementsWithoutBody(lines, requirementsIndex) {
  const missing = []
  let currentRequirement = null
  let hasBody = false
  let inScenario = false

  const flushRequirement = () => {
    if (currentRequirement && !hasBody) {
      missing.push(currentRequirement)
    }
  }

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      flushRequirement()
      return missing
    }
    if (/^###\s+/.test(line)) {
      flushRequirement()
      currentRequirement = /^###\s+Requirement\s*:/.test(line)
        ? { line: index + 1, title: line }
        : null
      hasBody = false
      inScenario = false
      continue
    }
    if (currentRequirement && /^####\s+Scenario\s*:/.test(line)) {
      inScenario = true
      continue
    }
    if (currentRequirement && !inScenario && isRequirementBodyLine(line)) {
      hasBody = true
    }
  }

  flushRequirement()
  return missing
}

function isRequirementBodyLine(line) {
  if (!line) {
    return false
  }
  return !/^-\s+\*\*(WHEN|THEN|AND)\*\*/i.test(line)
}

function findRequirementsWithoutScenario(lines, requirementsIndex) {
  const missing = []
  let currentRequirement = null
  let hasScenario = false

  const flushRequirement = () => {
    if (currentRequirement && !hasScenario) {
      missing.push(currentRequirement)
    }
  }

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      flushRequirement()
      return missing
    }
    if (/^###\s+/.test(line)) {
      flushRequirement()
      currentRequirement = /^###\s+Requirement\s*:/.test(line)
        ? { line: index + 1, title: line }
        : null
      hasScenario = false
      continue
    }
    if (currentRequirement && /^####\s+Scenario\s*:/.test(line)) {
      hasScenario = true
    }
  }

  flushRequirement()
  return missing
}

function findScenariosWithMissingSteps(lines, requirementsIndex) {
  const missing = []
  let inRequirement = false
  let currentScenario = null

  const flushScenario = () => {
    if (currentScenario && (!currentScenario.hasWhen || !currentScenario.hasThen)) {
      missing.push(currentScenario)
    }
  }

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      flushScenario()
      return missing
    }
    if (/^###\s+/.test(line)) {
      flushScenario()
      currentScenario = null
      inRequirement = /^###\s+Requirement\s*:/.test(line)
      continue
    }
    if (!inRequirement) {
      continue
    }
    if (/^####\s+Scenario\s*:/.test(line)) {
      flushScenario()
      currentScenario = { line: index + 1, title: line, hasWhen: false, hasThen: false }
      continue
    }
    if (currentScenario && /^-\s+\*\*WHEN\*\*/i.test(line)) {
      currentScenario.hasWhen = true
    }
    if (currentScenario && /^-\s+\*\*THEN\*\*/i.test(line)) {
      currentScenario.hasThen = true
    }
  }

  flushScenario()
  return missing
}

function findScenariosWithEmptyStepText(lines, requirementsIndex) {
  const missing = []
  let inRequirement = false
  let inScenario = false

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return missing
    }
    if (/^###\s+/.test(line)) {
      inRequirement = /^###\s+Requirement\s*:/.test(line)
      inScenario = false
      continue
    }
    if (!inRequirement) {
      continue
    }
    if (/^####\s+Scenario\s*:/.test(line)) {
      inScenario = true
      continue
    }
    if (!inScenario) {
      continue
    }
    const stepMatch = /^-\s+\*\*(WHEN|THEN|AND)\*\*\s*(?<text>.*)$/i.exec(line)
    if (stepMatch && !stepMatch.groups.text.trim()) {
      missing.push({ line: index + 1, kind: stepMatch[1].toUpperCase() })
    }
  }

  return missing
}

function findScenariosWithMissingTitle(lines, requirementsIndex) {
  const missing = []
  let inRequirement = false
  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return missing
    }
    if (/^###\s+/.test(line)) {
      inRequirement = /^###\s+Requirement\s*:/.test(line)
      continue
    }
    if (!inRequirement) {
      continue
    }
    const match = /^####\s+Scenario\s*:\s*(?<title>.*)$/.exec(line)
    if (match && !match.groups.title.trim()) {
      missing.push({ line: index + 1 })
    }
  }
  return missing
}

function findDuplicateScenarios(lines, requirementsIndex) {
  const duplicates = []
  let inRequirement = false
  let seenScenarios = new Map()

  for (let index = requirementsIndex + 1; index < lines.length; index += 1) {
    const line = lines[index].trim()
    if (/^##\s+/.test(line)) {
      return duplicates
    }
    if (/^###\s+/.test(line)) {
      inRequirement = /^###\s+Requirement\s*:/.test(line)
      seenScenarios = new Map()
      continue
    }
    if (!inRequirement || !/^####\s+Scenario\s*:/.test(line)) {
      continue
    }
    const normalizedTitle = line.replace(/^####\s+Scenario\s*:\s*/, '').trim().toLowerCase()
    if (!normalizedTitle) {
      continue
    }
    if (seenScenarios.has(normalizedTitle)) {
      duplicates.push({ line: index + 1, firstLine: seenScenarios.get(normalizedTitle), title: line })
      continue
    }
    seenScenarios.set(normalizedTitle, index + 1)
  }

  return duplicates
}

function checkReadmeToolEntry(readmeText, relativeFiles, issues) {
  if (!relativeFiles.has('tools/dataspec-status-check.mjs')) {
    return
  }
  if (readmeText.includes('dataspec-status-check.mjs')) {
    return
  }
  issues.push(issue({
    code: 'README_STATUS_CHECK_MISSING',
    message: 'README 未说明文档状态检查工具入口。新增完成项后后续 agent 不容易发现该验证命令。',
    file: 'README.md',
    suggestedFix: '在 README 的验证工具小节加入 `node tools/dataspec-status-check.mjs --format json`。'
  }))
}

function checkWorkflowRecipeContracts(todoText, aiContractsText, workflowRecipeIds, issues) {
  if (workflowRecipeIds.length === 0) {
    return
  }

  if (aiContractsText === null) {
    issues.push(issue({
      code: 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT',
      message: `docs/ai-contracts.md 缺失，无法确认 AI workflow recipe id 清单：${workflowRecipeIds.join('、')}。`,
      file: 'docs/ai-contracts.md',
      suggestedFix: '恢复 `docs/ai-contracts.md`，并同步 `.dataspec/workflows.md` 的 recipe id 列表。'
    }))
  } else if (aiContractsText !== undefined) {
    const workflowLine = findLine(aiContractsText, (line) =>
      line.includes('.dataspec/workflows.md') && line.includes('recipe')
    )
    pushWorkflowRecipeDriftIssue({
      code: 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT',
      file: 'docs/ai-contracts.md',
      line: workflowLine?.line,
      targetText: workflowLine?.text ?? '',
      workflowRecipeIds,
      messagePrefix: 'AI 契约文档中的 `.dataspec/workflows.md` recipe id 清单漂移',
      suggestedFix: '同步 `docs/ai-contracts.md` 中 `.dataspec/workflows.md` 的 recipe id 列表，确保它包含 `supportedWorkflowRecipeIds()` 的全部结果。',
      issues
    })
  }

  const taskCardLine = findLine(todoText, (line) =>
    line.includes('任务卡') && line.includes('workflow recipe')
  )
  pushWorkflowRecipeDriftIssue({
    code: 'TODO_WORKFLOW_RECIPES_DRIFT',
    file: 'TODO.md',
    line: taskCardLine?.line,
    targetText: taskCardLine?.text ?? '',
    workflowRecipeIds,
    messagePrefix: 'TODO 任务卡 workflow recipe 摘要漂移',
    suggestedFix: '同步 TODO 中任务卡已完成能力的 workflow recipe 列表，避免项目状态说明滞后于实际 recipe catalog。',
    issues
  })
}

function pushWorkflowRecipeDriftIssue({
  code,
  file,
  line,
  targetText,
  workflowRecipeIds,
  messagePrefix,
  suggestedFix,
  issues
}) {
  const declaredRecipeIds = parseWorkflowRecipeIds(targetText)
  const expectedRecipeIds = new Set(workflowRecipeIds)
  const missingRecipeIds = workflowRecipeIds.filter((id) => !declaredRecipeIds.has(id))
  const extraRecipeIds = [...declaredRecipeIds].filter((id) => !expectedRecipeIds.has(id)).sort()
  if (missingRecipeIds.length === 0 && extraRecipeIds.length === 0) {
    return
  }
  const details = []
  if (missingRecipeIds.length > 0) {
    details.push(`缺少：${missingRecipeIds.join('、')}`)
  }
  if (extraRecipeIds.length > 0) {
    details.push(`多余：${extraRecipeIds.join('、')}`)
  }
  issues.push(issue({
    code,
    message: `${messagePrefix}，${details.join('；')}。`,
    file,
    line,
    suggestedFix
  }))
}

function parseWorkflowRecipeIds(text) {
  return new Set([...String(text ?? '').matchAll(/\b[a-z][a-z0-9]*(?:-[a-z0-9]+)+\b/g)].map((match) => match[0]))
}

function checkMarkdownLinks(file, text, relativeFiles, issues) {
  const baseDir = path.posix.dirname(file) === '.' ? '' : path.posix.dirname(file)
  const lines = String(text ?? '').split(/\r?\n/)
  let fence = null
  lines.forEach((line, index) => {
    const trimmed = line.trim()
    const fenceMatch = /^(?<marker>`{3,}|~{3,})(?<rest>.*)$/.exec(trimmed)
    if (fenceMatch && !fence) {
      const marker = fenceMatch.groups.marker
      fence = { char: marker[0], length: marker.length }
      return
    }
    if (fenceMatch && fence) {
      const marker = fenceMatch.groups.marker
      const rest = fenceMatch.groups.rest
      if (marker[0] === fence.char && marker.length >= fence.length && rest.trim() === '') {
        fence = null
      }
      return
    }
    if (fence) {
      return
    }
    const rawTargets = [
      ...extractMarkdownLinkTargets(line),
      ...extractMarkdownReferenceDefinitionTargets(line)
    ]
    for (const rawTarget of rawTargets) {
      const target = parseMarkdownLinkTarget(rawTarget)
      if (shouldSkipLink(target)) {
        continue
      }
      const withoutAnchor = target.split('#')[0]
      if (!withoutAnchor) {
        continue
      }
      const decodedPath = decodeMarkdownLinkPath(withoutAnchor)
      const normalized = normalizeRelativePath(path.posix.normalize(path.posix.join(baseDir, decodedPath)))
      if (!relativeFiles.has(normalized)) {
        issues.push(issue({
          code: 'MARKDOWN_LINK_MISSING',
          message: `${file} 中的相对链接不存在：${target}`,
          file,
          line: index + 1,
          suggestedFix: '修正链接目标，或补齐对应文档文件。'
        }))
      }
    }
  })
}

function extractMarkdownLinkTargets(line) {
  const targets = []
  const searchableLine = maskMarkdownCodeSpans(line)
  let cursor = 0
  while (cursor < searchableLine.length) {
    const labelStart = searchableLine.indexOf('[', cursor)
    if (labelStart === -1) {
      break
    }
    if (isEscapedMarkdownDelimiter(searchableLine, labelStart)) {
      cursor = labelStart + 1
      continue
    }
    const labelEnd = searchableLine.indexOf(']', labelStart + 1)
    if (labelEnd === -1) {
      break
    }
    if (searchableLine[labelEnd + 1] !== '(') {
      cursor = labelEnd + 1
      continue
    }

    const targetStart = labelEnd + 2
    if (searchableLine[targetStart] === '<') {
      const angleEnd = searchableLine.indexOf('>', targetStart + 1)
      if (angleEnd === -1) {
        cursor = targetStart + 1
        continue
      }
      const parenEnd = searchableLine.indexOf(')', angleEnd + 1)
      if (parenEnd === -1) {
        cursor = angleEnd + 1
        continue
      }
      targets.push(line.slice(targetStart, parenEnd).trim())
      cursor = parenEnd + 1
      continue
    }

    const parenEnd = searchableLine.indexOf(')', targetStart)
    if (parenEnd === -1) {
      cursor = targetStart
      continue
    }
    targets.push(line.slice(targetStart, parenEnd).trim())
    cursor = parenEnd + 1
  }
  return targets
}

function extractMarkdownReferenceDefinitionTargets(line) {
  const match = /^ {0,3}\[(?<label>[^\]]+)\]:[ \t]*(?<target>.+?)\s*$/.exec(line)
  if (!match) {
    return []
  }
  const label = match.groups.label.trim()
  if (label.startsWith('^')) {
    return []
  }
  return [match.groups.target.trim()]
}

function maskMarkdownCodeSpans(line) {
  const chars = String(line ?? '').split('')
  let cursor = 0
  while (cursor < chars.length) {
    if (chars[cursor] !== '`') {
      cursor += 1
      continue
    }
    if (isEscapedMarkdownDelimiter(chars, cursor)) {
      cursor += 1
      continue
    }

    const openingEnd = findBacktickRunEnd(chars, cursor)
    const runLength = openingEnd - cursor
    const closingStart = findClosingBacktickRun(chars, openingEnd, runLength)
    if (closingStart === -1) {
      cursor = openingEnd
      continue
    }

    // 只屏蔽能按 Markdown 规则闭合的行内代码，避免未闭合反引号吞掉真实链接。
    const closingEnd = closingStart + runLength
    for (let index = cursor; index < closingEnd; index += 1) {
      chars[index] = ' '
    }
    cursor = closingEnd
  }
  return chars.join('')
}

function isEscapedMarkdownDelimiter(chars, index) {
  let slashCount = 0
  for (let cursor = index - 1; cursor >= 0 && chars[cursor] === '\\'; cursor -= 1) {
    slashCount += 1
  }
  return slashCount % 2 === 1
}

function findBacktickRunEnd(chars, start) {
  let end = start
  while (end < chars.length && chars[end] === '`') {
    end += 1
  }
  return end
}

function findClosingBacktickRun(chars, start, runLength) {
  let cursor = start
  while (cursor < chars.length) {
    if (chars[cursor] !== '`') {
      cursor += 1
      continue
    }
    const runEnd = findBacktickRunEnd(chars, cursor)
    if (runEnd - cursor === runLength) {
      return cursor
    }
    cursor = runEnd
  }
  return -1
}

function parseMarkdownLinkTarget(rawTarget) {
  if (rawTarget.startsWith('<')) {
    const endIndex = rawTarget.indexOf('>')
    if (endIndex > 0) {
      return rawTarget.slice(1, endIndex)
    }
  }
  return rawTarget.split(/\s+/)[0]
}

function decodeMarkdownLinkPath(filePath) {
  try {
    return decodeURI(filePath)
  } catch {
    return filePath
  }
}

function extractArchivedChangeIds(item) {
  return [...item.bodyLines.join('\n').matchAll(/OpenSpec change\s+`([^`]+)`\s+已于.+?归档/g)]
    .map((match) => normalizeOpenSpecChangeId(match[1]))
    .filter(Boolean)
}

function archivedChangeExists(changeId, relativeFiles) {
  const marker = `-${changeId}`
  return [...relativeFiles].some((filePath) =>
    filePath.startsWith('openspec/changes/archive/') &&
    (filePath.endsWith(marker) || filePath.includes(`${marker}/`))
  )
}

function archivedSpecCapabilities(changeId, relativeFiles) {
  const marker = `-${changeId}/specs/`
  const capabilities = new Set()
  for (const filePath of relativeFiles) {
    if (!filePath.startsWith('openspec/changes/archive/') || !filePath.includes(marker) || !filePath.endsWith('/spec.md')) {
      continue
    }
    const afterSpecs = filePath.slice(filePath.indexOf(marker) + marker.length)
    const [capability, fileName] = afterSpecs.split('/')
    if (capability && fileName === 'spec.md') {
      capabilities.add(capability)
    }
  }
  return [...capabilities].sort()
}

function extractTodoActiveChangeReferences(todoText) {
  const references = []
  String(todoText ?? '').split(/\r?\n/).forEach((line, index) => {
    if (!/(?:保持|保留|仍为|仍处于)\s*active|暂未(?:自动)?归档|暂不自动归档|后续按需归档/i.test(line)) {
      return
    }
    for (const match of line.matchAll(/OpenSpec change\s+`([^`]+)`/g)) {
      const changeId = normalizeOpenSpecChangeId(match[1])
      if (!changeId) {
        continue
      }
      references.push({
        changeId,
        line: index + 1
      })
    }
  })
  return references
}

function normalizeOpenSpecChangeId(value) {
  return String(value ?? '').trim().replace(/\\/g, '/').split('/').filter(Boolean).pop() ?? ''
}

function findNonEmptyActiveQueueClaim(todoText) {
  const lines = String(todoText ?? '').split(/\r?\n/)
  const queuePattern = /active change\s*队列/i
  const nonEmptyPattern = /保留|存在|仍有|仍处于|正在实施|未归档/i
  for (const [index, line] of lines.entries()) {
    if (!queuePattern.test(line) || /为空/.test(line) || !nonEmptyPattern.test(line)) {
      continue
    }
    return index + 1
  }
  return null
}

function buildChecks(issues) {
  const definitions = [
    ['todo-queue', 'TODO 近期队列一致性'],
    ['todo-completed', 'TODO 完成态字段一致性'],
    ['openspec-state', 'OpenSpec active/archive/main spec 一致性'],
    ['readme-entry', 'README 状态检查入口'],
    ['workflow-recipes', 'AI workflow recipe 契约文档一致性'],
    ['markdown-links', 'Markdown 相对链接']
  ]
  return definitions.map(([id, name]) => {
    const related = issues.filter((item) => codeBelongsToCheck(item.code, id))
    const errorCount = related.filter((item) => item.severity === 'error').length
    const warningCount = related.filter((item) => item.severity === 'warning').length
    return {
      id,
      name,
      status: errorCount > 0 ? 'fail' : 'pass',
      issueCount: related.length,
      errorCount,
      warningCount
    }
  })
}

function codeBelongsToCheck(code, checkId) {
  if (checkId === 'todo-queue') {
    return code.startsWith('TODO_QUEUE_')
  }
  if (checkId === 'todo-completed') {
    return code === 'TODO_COMPLETED_HAS_GAP'
  }
  if (checkId === 'openspec-state') {
    return code.startsWith('OPENSPEC_')
  }
  if (checkId === 'readme-entry') {
    return code === 'README_STATUS_CHECK_MISSING'
  }
  if (checkId === 'workflow-recipes') {
    return code === 'AI_CONTRACT_WORKFLOW_RECIPES_DRIFT' || code === 'TODO_WORKFLOW_RECIPES_DRIFT'
  }
  if (checkId === 'markdown-links') {
    return code === 'MARKDOWN_LINK_MISSING'
  }
  return false
}

function buildIssueCodeSummary(issues) {
  const byCode = new Map()
  for (const item of issues) {
    const existing = byCode.get(item.code)
    if (!existing) {
      byCode.set(item.code, {
        code: item.code,
        count: 1,
        severity: item.severity
      })
      continue
    }
    existing.count += 1
    if (item.severity === 'error') {
      existing.severity = 'error'
    }
  }
  return [...byCode.values()]
}

function buildNextActions(status, issues) {
  if (status === 'pass') {
    return ['状态一致性检查通过；提交前仍需按变更范围运行对应测试和 `git diff --check`。']
  }
  const hasError = issues.some((item) => item.severity === 'error')
  const issueCodeSummary = buildIssueCodeSummary(issues)
    .map((item) => `${item.code}(count=${item.count},severity=${item.severity})`)
    .join('、')
  return [
    hasError
      ? '优先修复 severity=error 的状态漂移，再重新运行本命令。'
      : '优先处理或确认 severity=warning 的状态漂移，再重新运行本命令。',
    '如果发现脚本误报，先补测试 fixture，再调整确定性规则。',
    `当前问题编码：${issueCodeSummary}`
  ]
}

function issue({ code, message, file, line, suggestedFix, severity = 'error' }) {
  return {
    severity,
    code,
    message,
    file,
    line,
    suggestedFix
  }
}

function isPending(item) {
  return item.status.includes('待办')
}

function shouldSkipLink(target) {
  return /^(https?:|mailto:|file:|app:|#)/i.test(target)
}

async function collectRelativePaths(root) {
  const result = new Set()
  await walk(root, root, result)
  return result
}

async function walk(root, current, result) {
  let entries = []
  try {
    entries = await readdir(current, { withFileTypes: true })
  } catch {
    return
  }
  for (const entry of entries) {
    if (entry.isDirectory() && SKIPPED_DIRS.has(entry.name)) {
      continue
    }
    const absolute = path.join(current, entry.name)
    const relative = normalizeRelativePath(path.relative(root, absolute))
    result.add(relative)
    if (entry.isDirectory()) {
      await walk(root, absolute, result)
    }
  }
}

async function readDirectoryNames(dir) {
  try {
    const entries = await readdir(dir, { withFileTypes: true })
    return entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name).sort()
  } catch (error) {
    if (error.code === 'ENOENT') {
      return []
    }
    throw error
  }
}

async function readOpenSpecMainSpecTexts(specsDir) {
  const result = new Map()
  const capabilities = await readDirectoryNames(specsDir)
  await Promise.all(capabilities.map(async (capability) => {
    const specText = await readOptionalFile(path.join(specsDir, capability, 'spec.md'))
    if (specText !== null) {
      result.set(capability, specText)
    }
  }))
  return result
}

async function readOpenSpecActiveChangeTexts(changesDir) {
  const result = new Map()
  const changeIds = (await readDirectoryNames(changesDir)).filter((changeId) => changeId !== 'archive')
  for (const changeId of changeIds) {
    await addOptionalOpenSpecChangeText(result, changesDir, changeId, 'proposal.md')
    await addOptionalOpenSpecChangeText(result, changesDir, changeId, 'tasks.md')

    const specDir = path.join(changesDir, changeId, 'specs')
    const capabilities = await readDirectoryNames(specDir)
    for (const capability of capabilities) {
      await addOptionalOpenSpecChangeText(result, changesDir, changeId, `specs/${capability}/spec.md`)
    }
  }
  return result
}

async function addOptionalOpenSpecChangeText(result, changesDir, changeId, changeRelativePath) {
  const text = await readOptionalFile(path.join(changesDir, changeId, ...changeRelativePath.split('/')))
  if (text === null) {
    return
  }
  // active change 是当前工作态；archive 历史文档不参与该检查，避免旧链接阻塞新开发。
  result.set(`openspec/changes/${changeId}/${changeRelativePath}`, text)
}

async function readOptionalFile(filePath) {
  try {
    return await readFile(filePath, 'utf8')
  } catch (error) {
    if (error.code === 'ENOENT') {
      return null
    }
    throw error
  }
}

function normalizeRelativeFiles(files) {
  return new Set([...files].map((file) => normalizeRelativePath(file)))
}

function normalizeOpenSpecSpecTexts(specTexts) {
  if (specTexts instanceof Map) {
    return new Map([...specTexts.entries()].map(([key, value]) => [String(key), String(value ?? '')]))
  }
  if (Array.isArray(specTexts)) {
    return new Map(specTexts.map(([key, value]) => [String(key), String(value ?? '')]))
  }
  return new Map(Object.entries(specTexts).map(([key, value]) => [String(key), String(value ?? '')]))
}

function normalizeOpenSpecActiveChangeTexts(changeTexts) {
  if (changeTexts instanceof Map) {
    return new Map([...changeTexts.entries()].map(([key, value]) => [normalizeRelativePath(key), String(value ?? '')]))
  }
  if (Array.isArray(changeTexts)) {
    return new Map(changeTexts.map(([key, value]) => [normalizeRelativePath(key), String(value ?? '')]))
  }
  return new Map(Object.entries(changeTexts).map(([key, value]) => [normalizeRelativePath(key), String(value ?? '')]))
}

function normalizeWorkflowRecipeIds(ids) {
  return [...new Set([...ids].map((id) => String(id).trim()).filter(Boolean))].sort()
}

function normalizeRelativePath(filePath) {
  return String(filePath ?? '').replace(/\\/g, '/').replace(/^\.\//, '')
}

function parseArgs(args) {
  const options = {
    root: REPO_ROOT,
    todoPath: 'TODO.md',
    readmePath: 'README.md',
    format: 'text',
    help: false
  }

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (arg === '--root') {
      options.root = readValue(args, index, arg)
      index += 1
      continue
    }
    if (arg.startsWith('--root=')) {
      options.root = arg.slice('--root='.length)
      continue
    }
    if (arg === '--todo') {
      options.todoPath = readValue(args, index, arg)
      index += 1
      continue
    }
    if (arg.startsWith('--todo=')) {
      options.todoPath = arg.slice('--todo='.length)
      continue
    }
    if (arg === '--readme') {
      options.readmePath = readValue(args, index, arg)
      index += 1
      continue
    }
    if (arg.startsWith('--readme=')) {
      options.readmePath = arg.slice('--readme='.length)
      continue
    }
    if (arg === '--format') {
      options.format = normalizeFormat(readValue(args, index, arg))
      index += 1
      continue
    }
    if (arg.startsWith('--format=')) {
      options.format = normalizeFormat(arg.slice('--format='.length))
      continue
    }
    throw new Error(`未知参数: ${arg}`)
  }

  return options
}

function readValue(args, index, arg) {
  const value = args[index + 1]
  if (!value) {
    throw new Error(`${arg} 需要取值`)
  }
  return value
}

function normalizeFormat(value) {
  if (value !== 'text' && value !== 'json') {
    throw new Error('--format 只支持 text 或 json')
  }
  return value
}

function defaultIo() {
  return {
    writeOut: (text) => process.stdout.write(text),
    writeErr: (text) => process.stderr.write(text)
  }
}

function helpText() {
  return `DataSpec 文档状态检查

用法:
  node tools/dataspec-status-check.mjs [--format text|json]
  node tools/dataspec-status-check.mjs --root <repo> --todo TODO.md --readme README.md

说明:
  本工具只做 TODO/README/OpenSpec/AI workflow recipe 文档的确定性状态检查，不联网、不读取业务数据。
`
}

if (process.argv[1] && path.resolve(process.argv[1]) === SCRIPT_PATH) {
  process.exitCode = await runStatusCheckCli()
}
