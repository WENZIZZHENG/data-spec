#!/usr/bin/env node

import { mkdir, readFile, stat, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const REPO_ROOT = path.resolve(path.dirname(SCRIPT_PATH), '..')
const KIND = 'dataspec.todo-openspec-handoff'
const SCHEMA_VERSION = 1

const FIELD_MAP = new Map([
  ['状态', 'status'],
  ['为什么做', 'why'],
  ['已有基础', 'foundation'],
  ['缺口', 'gap'],
  ['落地产物', 'deliverables'],
  ['验收标准', 'acceptance'],
  ['边界', 'boundary'],
  ['参考项目', 'references'],
  ['参考资料', 'references']
])

const TOKEN_MAP = [
  ['业务术语表', 'business-glossary'],
  ['术语表', 'glossary'],
  ['同义词', 'synonym'],
  ['词根', 'roots'],
  ['OpenSpec', 'openspec'],
  ['TODO', 'todo'],
  ['实施交接', 'handoff'],
  ['交接助手', 'handoff'],
  ['验证命令', 'validation-command'],
  ['推荐', 'advisor'],
  ['数据库', 'database'],
  ['反向导入', 'reverse-import'],
  ['字段', 'field'],
  ['规则', 'rule'],
  ['前端', 'frontend'],
  ['后端', 'backend'],
  ['质量', 'quality'],
  ['候选', 'candidate'],
  ['任务', 'task'],
  ['队列', 'queue'],
  ['命令', 'command'],
  ['面板', 'dashboard'],
  ['报告', 'report'],
  ['配置', 'config'],
  ['安全', 'security']
]

export function parseTodoItem(todoText, itemId) {
  const normalizedId = normalizeTodoId(itemId)
  const headingPattern = new RegExp(`^###\\s+${escapeRegExp(normalizedId)}[：:]\\s*(.+?)\\s*$`, 'm')
  const match = headingPattern.exec(todoText)
  if (!match) {
    throw new Error(`找不到 TODO 条目 ${normalizedId}`)
  }

  const start = match.index
  const afterHeading = start + match[0].length
  const rest = todoText.slice(afterHeading)
  const nextHeadingMatch = /^###\s+P\d+-\d+[：:]/m.exec(rest)
  const body = nextHeadingMatch ? rest.slice(0, nextHeadingMatch.index) : rest
  const fields = {}
  const rawLines = []

  for (const line of body.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed) {
      continue
    }
    rawLines.push(trimmed)
    const fieldMatch = /^-\s*([^：:]+)[：:]\s*(.*)$/.exec(trimmed)
    if (!fieldMatch) {
      continue
    }
    const key = FIELD_MAP.get(fieldMatch[1].trim())
    if (key) {
      fields[key] = fieldMatch[2].trim()
    }
  }

  return {
    id: normalizedId,
    title: match[1].trim(),
    fields,
    rawMarkdown: [`### ${normalizedId}：${match[1].trim()}`, ...rawLines].join('\n')
  }
}

export function buildOpenSpecDraft(item, options = {}) {
  const capability = sanitizeId(options.capability) || capabilityFromTitle(item)
  const changeId = sanitizeId(options.changeId) || changeIdFromCapability(capability)
  const openQuestions = buildOpenQuestions(item, changeId, capability)
  const files = {
    '.openspec.yaml': `schema: spec-driven\nid: ${changeId}\nstatus: proposed\n`,
    'proposal.md': renderProposal(item, capability),
    'design.md': renderDesign(item, changeId, capability, openQuestions),
    [`specs/${capability}/spec.md`]: renderSpec(item),
    'tasks.md': renderTasks(item)
  }

  return {
    kind: KIND,
    schemaVersion: SCHEMA_VERSION,
    todo: item,
    changeId,
    capability,
    files,
    openQuestions,
    nextActions: buildNextActions(changeId)
  }
}

export async function writeOpenSpecDraft(draft, outputDir, options = {}) {
  const targetRoot = path.resolve(outputDir)
  const changeDir = path.resolve(targetRoot, draft.changeId)
  if (!options.force && await exists(changeDir)) {
    throw new Error(`OpenSpec change 目录已存在: ${changeDir}；请更换 --change 或使用 --force`)
  }

  const entries = Object.entries(draft.files).map(([relativePath, content]) => ({
    target: resolveDraftTarget(changeDir, relativePath),
    content
  }))

  for (const { target, content } of entries) {
    await mkdir(path.dirname(target), { recursive: true })
    await writeFile(target, content, 'utf8')
  }
  return {
    changeDir,
    files: Object.keys(draft.files).map((relativePath) => path.join(changeDir, relativePath))
  }
}

function resolveDraftTarget(changeDir, relativePath) {
  const root = path.resolve(changeDir)
  const target = path.resolve(root, relativePath)
  if (target !== root && !target.startsWith(`${root}${path.sep}`)) {
    throw new Error(`生成文件路径越界: ${relativePath}`)
  }
  return target
}

export async function runHandoffCli(args = process.argv.slice(2), io = defaultIo()) {
  try {
    const options = parseArgs(args)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }

    const todoText = await readFile(options.todoPath, 'utf8')
    const item = parseTodoItem(todoText, options.itemId)
    const draft = buildOpenSpecDraft(item, {
      changeId: options.changeId,
      capability: options.capability
    })

    let writeResult = null
    if (!options.dryRun) {
      writeResult = await writeOpenSpecDraft(draft, options.outputDir, { force: options.force })
    }

    const result = toResult(draft, writeResult, options.dryRun)
    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    } else {
      io.writeOut(formatTextResult(result))
    }
    return 0
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
    return 2
  }
}

function parseArgs(args) {
  const options = {
    todoPath: path.join(REPO_ROOT, 'TODO.md'),
    outputDir: path.join(REPO_ROOT, 'openspec', 'changes'),
    itemId: null,
    changeId: null,
    capability: null,
    dryRun: false,
    force: false,
    format: 'text',
    help: false
  }

  for (let index = 0; index < args.length; index += 1) {
    const arg = args[index]
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (arg === '--dry-run') {
      options.dryRun = true
      continue
    }
    if (arg === '--force') {
      options.force = true
      continue
    }
    if (arg === '--item') {
      options.itemId = requireValue(args, index, '--item')
      index += 1
      continue
    }
    if (arg.startsWith('--item=')) {
      options.itemId = arg.slice('--item='.length)
      continue
    }
    if (arg === '--todo') {
      options.todoPath = path.resolve(requireValue(args, index, '--todo'))
      index += 1
      continue
    }
    if (arg.startsWith('--todo=')) {
      options.todoPath = path.resolve(arg.slice('--todo='.length))
      continue
    }
    if (arg === '--output-dir') {
      options.outputDir = path.resolve(requireValue(args, index, '--output-dir'))
      index += 1
      continue
    }
    if (arg.startsWith('--output-dir=')) {
      options.outputDir = path.resolve(arg.slice('--output-dir='.length))
      continue
    }
    if (arg === '--change') {
      options.changeId = requireValue(args, index, '--change')
      index += 1
      continue
    }
    if (arg.startsWith('--change=')) {
      options.changeId = arg.slice('--change='.length)
      continue
    }
    if (arg === '--capability') {
      options.capability = requireValue(args, index, '--capability')
      index += 1
      continue
    }
    if (arg.startsWith('--capability=')) {
      options.capability = arg.slice('--capability='.length)
      continue
    }
    if (arg === '--format') {
      options.format = normalizeFormat(requireValue(args, index, '--format'))
      index += 1
      continue
    }
    if (arg.startsWith('--format=')) {
      options.format = normalizeFormat(arg.slice('--format='.length))
      continue
    }
    if (arg.startsWith('-')) {
      throw new Error(`未知参数: ${arg}`)
    }
    if (!options.itemId) {
      options.itemId = arg
      continue
    }
    throw new Error(`未知位置参数: ${arg}`)
  }

  if (!options.itemId) {
    throw new Error('必须提供 --item <P6-x>')
  }
  options.itemId = normalizeTodoId(options.itemId)
  return options
}

function requireValue(args, index, name) {
  const value = args[index + 1]
  if (!value) {
    throw new Error(`${name} 需要参数值`)
  }
  return value
}

function normalizeFormat(value) {
  if (value !== 'text' && value !== 'json') {
    throw new Error('--format 只支持 text 或 json')
  }
  return value
}

function toResult(draft, writeResult, dryRun) {
  return {
    kind: draft.kind,
    schemaVersion: draft.schemaVersion,
    todo: {
      id: draft.todo.id,
      title: draft.todo.title,
      fields: draft.todo.fields
    },
    changeId: draft.changeId,
    capability: draft.capability,
    dryRun,
    changeDir: writeResult?.changeDir ?? null,
    files: Object.keys(draft.files),
    openQuestions: draft.openQuestions,
    nextActions: draft.nextActions
  }
}

function formatTextResult(result) {
  const lines = [
    'DataSpec TODO 到 OpenSpec 交接',
    '',
    `TODO: ${result.todo.id} ${result.todo.title}`,
    `change: ${result.changeId}`,
    `capability: ${result.capability}`,
    `mode: ${result.dryRun ? 'dry-run' : 'write'}`,
    ''
  ]
  lines.push('文件:')
  for (const file of result.files) {
    lines.push(`- ${file}`)
  }
  lines.push('', '人工确认:')
  for (const question of result.openQuestions) {
    lines.push(`- ${question}`)
  }
  lines.push('', '下一步:')
  for (const action of result.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${lines.join('\n')}\n`
}

function renderProposal(item, capability) {
  return `## Why

${valueOrPlaceholder(item.fields.why, 'TODO 条目未提供为什么做，需要人工补充。')}

## What Changes

- 基于 TODO ${item.id}「${item.title}」生成 OpenSpec 草稿。
- 保留已有基础、缺口、落地产物、验收标准和边界，供实施前人工确认。
- 第一版只生成可审阅草稿，不自动实现、提交或归档生成的 change。

## Capabilities

### New Capabilities
- \`${capability}\`: ${item.title} 的待办能力范围，需在实施前人工确认命名和边界。

### Modified Capabilities
- 无。

## Impact

- 已有基础：${valueOrPlaceholder(item.fields.foundation, 'TODO 条目未提供已有基础。')}
- 缺口：${valueOrPlaceholder(item.fields.gap, 'TODO 条目未提供缺口。')}
- 落地产物：${valueOrPlaceholder(item.fields.deliverables, 'TODO 条目未提供落地产物。')}
- 验收标准：${valueOrPlaceholder(item.fields.acceptance, 'TODO 条目未提供验收标准。')}
- 边界：${valueOrPlaceholder(item.fields.boundary, 'TODO 条目未提供边界。')}
`
}

function renderDesign(item, changeId, capability, openQuestions) {
  return `## Context

该草稿由 TODO ${item.id}「${item.title}」生成，目标是把待办条目转成 OpenSpec-first 的实施入口。实施前必须人工确认生成内容是否保留了原始边界，并按实际代码上下文补充设计细节。

## Goals / Non-Goals

**Goals:**
- ${valueOrPlaceholder(item.fields.deliverables, '根据 TODO 补充具体落地产物。')}
- ${valueOrPlaceholder(item.fields.acceptance, '根据 TODO 补充验收标准。')}

**Non-Goals:**
- ${valueOrPlaceholder(item.fields.boundary, '根据 TODO 补充不做边界。')}
- 不自动实现代码，不自动归档 \`${changeId}\`。

## Decisions

1. **先确认能力命名。**
   - change id: \`${changeId}\`
   - capability: \`${capability}\`
   - 原因：TODO 中文标题只能提供粗粒度语义，实施前需要确认英文命名是否准确。

2. **保留 TODO 原始验收和边界。**
   - 原因：避免生成草稿在进入实现前扩大范围。

## Risks / Trade-offs

- [Risk] TODO 描述仍偏粗，直接实现可能漏掉数据模型或接口细节。→ Mitigation：先处理 Open Questions，再开始编码。
- [Risk] 自动生成的 spec 只覆盖第一版验收。→ Mitigation：实施前按代码现状补充更具体 scenario。

## Open Questions

${openQuestions.map((question) => `- ${question}`).join('\n')}
`
}

function renderSpec(item) {
  return `## ADDED Requirements

### Requirement: Generated TODO capability draft
The implementation SHALL preserve the selected TODO item's intent, acceptance criteria, and explicit boundary.

#### Scenario: Preserve TODO acceptance criteria
- **WHEN** the generated OpenSpec change is reviewed
- **THEN** it SHALL include the TODO acceptance criteria: ${valueOrPlaceholder(item.fields.acceptance, 'TODO 条目未提供验收标准')}
- **AND** it SHALL include the TODO boundary: ${valueOrPlaceholder(item.fields.boundary, 'TODO 条目未提供边界')}

#### Scenario: Require human confirmation before implementation
- **WHEN** the generated draft contains open questions or inferred names
- **THEN** implementers SHALL confirm scope, naming, and validation commands before writing product code.
`
}

function renderTasks(item) {
  return `## 1. 草稿确认

- [ ] 1.1 人工确认 OpenSpec 草稿中的 change id、capability、验收标准和边界。
- [ ] 1.2 补充或删除 Open Questions，确保需求可实施。

## 2. 测试先行

- [ ] 2.1 根据验收标准新增失败测试：${valueOrPlaceholder(item.fields.acceptance, 'TODO 条目未提供验收标准')}
- [ ] 2.2 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [ ] 3.1 按最小改动实现：${valueOrPlaceholder(item.fields.deliverables, 'TODO 条目未提供落地产物')}
- [ ] 3.2 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [ ] 4.1 运行 \`openspec validate <change-id> --strict\`。
- [ ] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [ ] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [ ] 4.4 完成提交并归档 OpenSpec change。
`
}

function buildOpenQuestions(item, changeId, capability) {
  const questions = [
    `确认生成的 change id \`${changeId}\` 和 capability \`${capability}\` 是否准确。`,
    '确认 TODO 验收标准是否足够测试化；不足时先补充 scenario。',
    '确认不做边界是否完整，避免实施时扩大范围。'
  ]
  for (const [field, label] of [
    ['why', '为什么做'],
    ['gap', '缺口'],
    ['deliverables', '落地产物'],
    ['acceptance', '验收标准'],
    ['boundary', '边界']
  ]) {
    if (!item.fields[field]) {
      questions.push(`TODO 缺少「${label}」，实施前需要补齐。`)
    }
  }
  return questions
}

function buildNextActions(changeId) {
  return [
    `人工确认 openspec/changes/${changeId}/ 下的 proposal/design/spec/tasks。`,
    `运行 openspec validate ${changeId} --strict。`,
    '确认无误后再进入实现；不要把生成草稿视为已完成需求。'
  ]
}

function capabilityFromTitle(item) {
  const tokens = []
  for (const [needle, token] of TOKEN_MAP) {
    if (item.title.includes(needle) && !tokens.includes(token) && !tokens.some((existing) => existing.includes(token))) {
      tokens.push(token)
    }
  }
  if (tokens.length > 0) {
    return sanitizeId(tokens.join('-'))
  }
  return sanitizeId(`${item.id.toLowerCase()}-todo-handoff`)
}

function changeIdFromCapability(capability) {
  return capability.startsWith('add-') ? capability : `add-${capability}`
}

function sanitizeId(value) {
  if (!value) {
    return ''
  }
  return String(value)
    .trim()
    .replace(/([a-z])([A-Z])/g, '$1-$2')
    .replace(/[^A-Za-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/-+/g, '-')
    .toLowerCase()
}

function normalizeTodoId(itemId) {
  const value = String(itemId ?? '').trim().toUpperCase()
  if (!/^P\d+-\d+$/.test(value)) {
    throw new Error(`无效 TODO id: ${itemId}`)
  }
  return value
}

function valueOrPlaceholder(value, placeholder) {
  return value && value.trim() ? value.trim() : placeholder
}

async function exists(filePath) {
  try {
    await stat(filePath)
    return true
  } catch (error) {
    if (error.code === 'ENOENT') {
      return false
    }
    throw error
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function defaultIo() {
  return {
    writeOut: (text) => process.stdout.write(text),
    writeErr: (text) => process.stderr.write(text)
  }
}

function helpText() {
  return `DataSpec TODO 到 OpenSpec 交接助手

用法:
  node tools/dataspec-todo-openspec-handoff.mjs --item P6-48 --dry-run --format json
  node tools/dataspec-todo-openspec-handoff.mjs --item P6-48 --change add-business-glossary --capability business-glossary

说明:
  本工具只生成 OpenSpec 草稿，不实现代码、不提交、不归档。
`
}

if (process.argv[1] && path.resolve(process.argv[1]) === SCRIPT_PATH) {
  process.exitCode = await runHandoffCli()
}
