#!/usr/bin/env node

import { readdir, readFile, stat } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const SCRIPT_PATH = fileURLToPath(import.meta.url)
const REPO_ROOT = path.resolve(path.dirname(SCRIPT_PATH), '..')
const KIND = 'dataspec.openspec-readiness'
const SCHEMA_VERSION = 1
const CHANGE_ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

const COMMAND_PATTERN = /`([^`\r\n]+)`/g
const VALIDATION_COMMAND_PATTERN = /^(?:openspec|node|mvn|pnpm|npm|npx|git|docker)\s/
const INLINE_PLACEHOLDER_PATTERN = /\b(?:TODO|TBD)\s*[:：]|待补充|人工补充|Generated TODO capability draft/i
const BARE_PLACEHOLDER_LINE_PATTERN = /^\s*(?:[-*]\s*)?(?:\[[ xX]\]\s*)?(?:\d+(?:\.\d+)*\.?\s*)?(?:TODO|TBD)\s*(?:[:：.。！!？?、,，;；-].*)?$/i
const SPEC_DELTA_REQUIREMENT_PATTERN = /^##\s+(?:ADDED|MODIFIED|REMOVED)\s+Requirements\b[\s\S]*?^###\s+Requirement:\s+\S/im

/**
 * 构建 OpenSpec change 的只读准备度报告。
 *
 * `artifacts` 由调用方提供，通常来自 `openspec/changes/<change-id>`；函数不读取磁盘、
 * 不执行验证命令，也不会修改 change。评分仅用于开工前提示，不能替代人工判断。
 *
 * @param {object} input 准备度检查输入。
 * @param {string} input.changeId OpenSpec change id，必须是短横线命名。
 * @param {string} input.changeRoot change 的仓库相对或绝对路径，仅用于报告展示。
 * @param {object} input.artifacts proposal/design/tasks/specs 文本集合。
 * @returns {object} AI 可读的 readiness report。
 */
export function buildReadinessReport(input = {}) {
  const changeId = String(input.changeId ?? '').trim()
  const changeRoot = normalizeDisplayPath(input.changeRoot ?? `openspec/changes/${changeId}`)
  const artifacts = normalizeArtifacts(input.artifacts ?? {})
  const checks = buildChecks(changeId, artifacts)
  const missingFacts = checks
    .filter((check) => check.status !== 'pass')
    .map(({ id, status, file, line, message, suggestedFix }) => ({
      code: id,
      severity: status === 'error' ? 'error' : 'warning',
      file,
      line,
      message,
      suggestedFix
    }))
  const readinessScore = Math.max(0, 100 - checks
    .filter((check) => check.status !== 'pass')
    .reduce((total, check) => total + check.weight, 0))
  const affectedSpecs = collectAffectedSpecs(artifacts.specs)
  const riskFlags = buildRiskFlags(checks, artifacts)
  const validationPlan = buildValidationPlan(changeId, artifacts)
  const humanQuestions = buildHumanQuestions(missingFacts, affectedSpecs)
  const readinessLevel = classifyReadiness(readinessScore, checks)

  return {
    kind: KIND,
    schemaVersion: SCHEMA_VERSION,
    change: {
      id: changeId,
      path: changeRoot,
      artifactStatus: buildArtifactStatus(artifacts)
    },
    readinessScore,
    readinessLevel,
    missingFacts,
    affectedSpecs,
    validationPlan,
    reviewBoundary: buildReviewBoundary(artifacts, riskFlags),
    riskFlags,
    humanQuestions,
    checks: checks.map(({ id, status, weight, file, line, message, suggestedFix }) => ({
      id,
      status,
      weight,
      file,
      line,
      message,
      suggestedFix
    })),
    nextActions: buildNextActions(readinessLevel, missingFacts)
  }
}

/**
 * 运行 OpenSpec readiness CLI。
 *
 * CLI 只读取 repo-local active change，输出 JSON 或文本报告；低分仍返回 0，
 * 只有参数错误、路径不安全、change 不存在或已归档时返回 2。
 *
 * @param {string[]} args 命令行参数。
 * @param {{writeOut(text:string):void, writeErr(text:string):void}} io 输出适配器。
 * @returns {Promise<number>} 进程退出码。
 */
export async function runReadinessCli(args = process.argv.slice(2), io = defaultIo()) {
  try {
    const options = parseArgs(args)
    if (options.help) {
      io.writeOut(helpText())
      return 0
    }
    if (!isSafeChangeId(options.changeId)) {
      throw new Error(`无效或不安全的 change id: ${options.changeId}`)
    }

    const root = path.resolve(options.root)
    const changeRoot = path.join(root, 'openspec', 'changes', options.changeId)
    if (!await exists(changeRoot)) {
      if (await archivedChangeExists(root, options.changeId)) {
        throw new Error(`OpenSpec change 已归档，readiness 只检查 active change: ${options.changeId}`)
      }
      throw new Error(`OpenSpec change 不存在: ${options.changeId}`)
    }

    const artifacts = await readChangeArtifacts(changeRoot)
    const report = buildReadinessReport({
      changeId: options.changeId,
      changeRoot: normalizeDisplayPath(path.relative(root, changeRoot)),
      artifacts
    })

    if (options.format === 'json') {
      io.writeOut(`${JSON.stringify(report, null, 2)}\n`)
    } else {
      io.writeOut(formatReadinessReportText(report))
    }
    return 0
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
    return 2
  }
}

export function formatReadinessReportText(report) {
  const lines = [
    'DataSpec OpenSpec 准备度检查',
    '',
    `change：${report.change.id}`,
    `状态：${report.readinessLevel}`,
    `分数：${report.readinessScore}`,
    `影响规格：${report.affectedSpecs.join('、') || '未声明'}`
  ]

  if (report.missingFacts.length > 0) {
    lines.push('', '缺口:')
    report.missingFacts.forEach((fact, index) => {
      const location = fact.file ? ` ${fact.file}${fact.line ? `:${fact.line}` : ''}` : ''
      lines.push(`${index + 1}. [${fact.severity}] ${fact.code}${location}`)
      lines.push(`   ${fact.message}`)
      lines.push(`   建议: ${fact.suggestedFix}`)
    })
  }

  lines.push('', '验证建议:')
  for (const item of report.validationPlan) {
    lines.push(`- ${item.command}`)
    lines.push(`  ${item.reason}`)
  }

  if (report.humanQuestions.length > 0) {
    lines.push('', '需要确认:')
    for (const question of report.humanQuestions) {
      lines.push(`- ${question}`)
    }
  }

  lines.push('', '下一步:')
  for (const action of report.nextActions) {
    lines.push(`- ${action}`)
  }
  return `${lines.join('\n')}\n`
}

function buildChecks(changeId, artifacts) {
  return [
    artifactCheck('PROPOSAL_MISSING', artifacts.proposal, 'proposal.md', 30, '缺少 proposal.md。', '补充 change 背景、目标、影响范围和边界。'),
    artifactCheck('TASKS_MISSING', artifacts.tasks, 'tasks.md', 25, '缺少 tasks.md。', '补充可执行任务、验证步骤和 Verification Evidence 入口。'),
    artifactCheck('SPEC_DELTA_MISSING', collectAffectedSpecs(artifacts.specs).length > 0 ? 'present' : '', 'specs/<capability>/spec.md', 15, '缺少有效 spec delta。', '在 specs/<capability>/spec.md 中补充 ADDED/MODIFIED/REMOVED Requirements 和至少一个 Requirement。'),
    sectionContentCheck({
      id: 'PROPOSAL_IMPACT_MISSING',
      text: artifacts.proposal,
      file: 'proposal.md',
      headingPattern: /^##\s+Impact\s*$/im,
      weight: 10,
      status: 'warning',
      message: 'proposal.md 缺少有内容的 Impact 小节，实施者难以判断影响范围。',
      suggestedFix: '补充 Impact，至少说明影响的工具、文档、规格和安全边界。'
    }),
    semanticCheck({
      id: 'ACCEPTANCE_CRITERIA_MISSING',
      texts: [artifacts.proposal, artifacts.design, artifacts.tasks, ...artifacts.specs.values()],
      file: 'proposal.md',
      weight: 12,
      requiredPattern: /验收标准|acceptance|Scenario:|####\s+Scenario/i,
      message: '缺少可验证的验收标准或 scenario。',
      suggestedFix: '补充用户可观察结果、失败条件和可运行验证命令。'
    }),
    semanticCheck({
      id: 'BOUNDARY_MISSING',
      texts: [artifacts.proposal, artifacts.design, artifacts.tasks],
      file: 'proposal.md',
      weight: 10,
      requiredPattern: /边界|非目标|Non-Goals|Non-Goals|不做|不自动|boundary/i,
      message: '缺少明确不做边界，实施时容易扩大范围。',
      suggestedFix: '补充 Non-Goals 或边界，说明第一版不处理的相邻能力。'
    }),
    semanticCheck({
      id: 'VALIDATION_COMMAND_MISSING',
      texts: [artifacts.proposal, artifacts.design, artifacts.tasks],
      file: 'tasks.md',
      weight: 10,
      requiredPattern: /`(?:openspec|node|mvn|pnpm|npm|npx|git|docker)\b[^`]*`/,
      message: '缺少明确验证命令。',
      suggestedFix: `至少补充 \`openspec validate ${changeId} --strict\` 和与改动范围匹配的测试命令。`
    }),
    placeholderCheck({
      id: 'PLACEHOLDER_TEXT_PRESENT',
      texts: [artifacts.proposal, artifacts.design, artifacts.tasks, ...artifacts.specs.values()],
      file: 'proposal.md',
      weight: 8,
      status: 'warning',
      message: 'artifacts 中仍包含 TODO/TBD/待补充等占位文本。',
      suggestedFix: '把占位内容替换为确定事实；确实未知的问题放入 humanQuestions 或 Open Questions。'
    })
  ].filter(Boolean)
}

function artifactCheck(id, text, file, weight, message, suggestedFix) {
  return {
    id,
    status: hasMeaningfulText(text) ? 'pass' : 'error',
    weight,
    file,
    line: undefined,
    message,
    suggestedFix
  }
}

function sectionContentCheck({ id, text, file, headingPattern, weight, status = 'error', message, suggestedFix }) {
  if (!hasMeaningfulText(text)) {
    return null
  }
  const section = readMarkdownSection(text, headingPattern)
  const matched = section !== null && hasMeaningfulText(section.body)
  return {
    id,
    status: matched ? 'pass' : status,
    weight,
    file,
    line: section?.line,
    message,
    suggestedFix
  }
}

function semanticCheck({ id, texts, file, weight, requiredPattern, invert = false, status = 'error', message, suggestedFix }) {
  const matched = texts.some((text) => hasMeaningfulText(text) && requiredPattern.test(text))
  const failed = invert ? matched : !matched
  return {
    id,
    status: failed ? status : 'pass',
    weight,
    file,
    line: undefined,
    message,
    suggestedFix
  }
}

function placeholderCheck({ id, texts, file, weight, status = 'warning', message, suggestedFix }) {
  const matched = texts.some((text) => hasPlaceholderText(text))
  return {
    id,
    status: matched ? status : 'pass',
    weight,
    file,
    line: undefined,
    message,
    suggestedFix
  }
}

function classifyReadiness(score, checks) {
  const missingCriticalArtifact = checks.some((check) =>
    check.status === 'error' && ['PROPOSAL_MISSING', 'TASKS_MISSING', 'SPEC_DELTA_MISSING'].includes(check.id)
  )
  if (score < 50 || missingCriticalArtifact) {
    return 'BLOCKED'
  }
  if (score < 80 || checks.some((check) => check.status !== 'pass')) {
    return 'NEEDS_REVIEW'
  }
  return 'READY'
}

function buildRiskFlags(checks, artifacts) {
  const flags = []
  if (checks.some((check) => check.id === 'SPEC_DELTA_MISSING' && check.status !== 'pass')) {
    flags.push({
      code: 'NO_SPEC_DELTA',
      severity: 'error',
      message: '缺少 spec delta，无法判断长期契约变化。'
    })
  }
  if (checks.some((check) => check.id === 'VALIDATION_COMMAND_MISSING' && check.status !== 'pass')) {
    flags.push({
      code: 'NO_VALIDATION_PLAN',
      severity: 'warning',
      message: '缺少验证命令，完成标准不可复跑。'
    })
  }
  if (/安全|凭据|权限|迁移|数据库|存储|OpenAPI|CLI|MCP|AI/i.test(joinArtifactTexts(artifacts))) {
    flags.push({
      code: 'HIGH_REVIEW_ATTENTION',
      severity: 'warning',
      message: '文本涉及安全、存储、外部协议或数据库等高关注词，提交前应按项目门禁确认评审范围。'
    })
  }
  return flags
}

function buildValidationPlan(changeId, artifacts) {
  const commands = []
  const seen = new Set()
  addValidationCommand(commands, seen, `openspec validate ${changeId} --strict`, '校验当前 OpenSpec change artifacts。')
  for (const command of extractValidationCommands(joinArtifactTexts(artifacts))) {
    addValidationCommand(commands, seen, command, 'artifacts 中声明的验证命令。')
  }
  addValidationCommand(commands, seen, 'git diff --check', '提交前检查空白、冲突标记和行尾问题。')
  return commands
}

function addValidationCommand(commands, seen, command, reason) {
  const safeCommand = sanitizeSecretText(command).trim()
  if (!safeCommand || seen.has(safeCommand)) {
    return
  }
  seen.add(safeCommand)
  commands.push({ command: safeCommand, reason })
}

function extractValidationCommands(text) {
  const commands = []
  for (const match of String(text ?? '').matchAll(COMMAND_PATTERN)) {
    const command = match[1].trim()
    if (isConcreteValidationCommand(command)) {
      commands.push(command)
    }
  }
  return commands
}

function isConcreteValidationCommand(command) {
  if (!VALIDATION_COMMAND_PATTERN.test(command)) {
    return false
  }
  if (/[<>]/.test(command)) {
    return false
  }
  if (command === 'openspec validate') {
    return false
  }
  return true
}

function buildReviewBoundary(artifacts, riskFlags) {
  const text = joinArtifactTexts(artifacts)
  const declaresAgentReview = /子 agent|独立.*评审|code review|代码评审/i.test(text)
  return {
    summary: declaresAgentReview
      ? 'artifacts 已声明评审步骤；提交前按该边界执行只读或独立子 agent 评审。'
      : '默认按项目规则执行结构化自审；涉及外部协议、安全、存储或多模块时使用独立只读评审。',
    requiresIndependentAgent: riskFlags.some((flag) => flag.code === 'HIGH_REVIEW_ATTENTION'),
    source: declaresAgentReview ? 'artifacts' : 'project-default'
  }
}

function buildHumanQuestions(missingFacts, affectedSpecs) {
  const questions = []
  const codes = new Set(missingFacts.map((fact) => fact.code))
  if (codes.has('ACCEPTANCE_CRITERIA_MISSING')) {
    questions.push('验收标准是否足够测试化？需要补充哪些可运行命令或场景？')
  }
  if (codes.has('BOUNDARY_MISSING')) {
    questions.push('本次不做哪些相邻能力，避免实现时扩大范围？')
  }
  if (codes.has('PROPOSAL_IMPACT_MISSING') || affectedSpecs.length === 0) {
    questions.push('本次影响哪些 specs、工具、文档或用户可见流程？')
  }
  if (codes.has('VALIDATION_COMMAND_MISSING')) {
    questions.push('完成后至少需要运行哪些验证命令？')
  }
  if (questions.length === 0) {
    questions.push('准备度较高；实现前仍需确认最新用户目标是否和 change 范围一致。')
  }
  return questions
}

function buildNextActions(level, missingFacts) {
  if (level === 'READY') {
    return [
      '可以进入实现；实现前仍建议先写失败测试。',
      '完成后把实际验证输出写入 Verification Evidence。'
    ]
  }
  const hasError = missingFacts.some((fact) => fact.severity === 'error')
  return [
    hasError ? '先补齐 error 级缺口，再开始实现。' : '先确认 warning 级缺口是否可接受。',
    '补充 artifacts 后重新运行 readiness 检查。',
    'readiness 通过不代表自动实现或自动归档，仍需按 AGENTS/SDD 验证和评审。'
  ]
}

function buildArtifactStatus(artifacts) {
  return {
    proposal: hasMeaningfulText(artifacts.proposal) ? 'present' : 'missing',
    design: hasMeaningfulText(artifacts.design) ? 'present' : 'missing',
    tasks: hasMeaningfulText(artifacts.tasks) ? 'present' : 'missing',
    specs: collectAffectedSpecs(artifacts.specs).length
  }
}

function normalizeArtifacts(artifacts) {
  return {
    proposal: String(artifacts.proposal ?? ''),
    design: String(artifacts.design ?? ''),
    tasks: String(artifacts.tasks ?? ''),
    specs: normalizeSpecs(artifacts.specs)
  }
}

function normalizeSpecs(specs) {
  if (specs instanceof Map) {
    return new Map([...specs.entries()].map(([key, value]) => [String(key), String(value ?? '')]))
  }
  if (Array.isArray(specs)) {
    return new Map(specs.map(([key, value]) => [String(key), String(value ?? '')]))
  }
  return new Map(Object.entries(specs ?? {}).map(([key, value]) => [String(key), String(value ?? '')]))
}

function joinArtifactTexts(artifacts) {
  return [
    artifacts.proposal,
    artifacts.design,
    artifacts.tasks,
    ...artifacts.specs.values()
  ].filter(Boolean).join('\n\n')
}

function collectAffectedSpecs(specs) {
  return [...specs.entries()]
    .filter(([, text]) => hasMeaningfulSpecDelta(text))
    .map(([capability]) => capability)
    .sort()
}

function hasMeaningfulSpecDelta(text) {
  return SPEC_DELTA_REQUIREMENT_PATTERN.test(String(text ?? ''))
}

function hasPlaceholderText(text) {
  const value = String(text ?? '')
  if (!hasMeaningfulText(value)) {
    return false
  }
  if (INLINE_PLACEHOLDER_PATTERN.test(value)) {
    return true
  }
  return value.split(/\r?\n/).some((line) => BARE_PLACEHOLDER_LINE_PATTERN.test(line))
}

function hasMeaningfulText(value) {
  return String(value ?? '').trim().length > 0
}

function readMarkdownSection(text, headingPattern) {
  const lines = String(text ?? '').split(/\r?\n/)
  const index = lines.findIndex((line) => headingPattern.test(line))
  if (index === -1) {
    return null
  }
  const body = []
  for (let cursor = index + 1; cursor < lines.length; cursor += 1) {
    if (/^##\s+/.test(lines[cursor])) {
      break
    }
    body.push(lines[cursor])
  }
  return {
    line: index + 1,
    body: body.join('\n')
  }
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/]*@/gi, '$1')
    .replace(/jdbc:[^\s"'<>]+/gi, 'jdbc:***')
    .replace(/\b((?:postgres(?:ql)?|mysql|mariadb|sqlserver|oracle|mongodb|redis):\/\/)[^\s"'<>]+/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*)(?!\s*['"]?bearer\s+)(['"]?)[^,;}&\r\n]+\2/gi, '$1$2***$2')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(/((?:"|')?\b(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string|dsn)\b(?:"|')?\s*[:=]\s*)(['"]?)[^\s"',;}&]+\2/gi, '$1$2***$2')
}

async function readChangeArtifacts(changeRoot) {
  const specs = new Map()
  const specsDir = path.join(changeRoot, 'specs')
  for (const capability of await readDirectoryNames(specsDir)) {
    const specText = await readOptionalFile(path.join(specsDir, capability, 'spec.md'))
    if (specText !== null) {
      specs.set(capability, specText)
    }
  }
  return {
    proposal: await readOptionalFile(path.join(changeRoot, 'proposal.md')) ?? '',
    design: await readOptionalFile(path.join(changeRoot, 'design.md')) ?? '',
    tasks: await readOptionalFile(path.join(changeRoot, 'tasks.md')) ?? '',
    specs
  }
}

async function archivedChangeExists(root, changeId) {
  const archiveRoot = path.join(root, 'openspec', 'changes', 'archive')
  for (const entry of await readDirectoryNames(archiveRoot)) {
    if (entry === changeId || entry.replace(/^\d{4}-\d{2}-\d{2}-/, '') === changeId) {
      return true
    }
  }
  return false
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

function parseArgs(args) {
  const options = {
    root: REPO_ROOT,
    changeId: '',
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
      options.root = path.resolve(readValue(args, index, arg))
      index += 1
      continue
    }
    if (arg.startsWith('--root=')) {
      options.root = path.resolve(readInlineValue(arg, '--root'))
      continue
    }
    if (arg === '--change') {
      options.changeId = readValue(args, index, arg)
      index += 1
      continue
    }
    if (arg.startsWith('--change=')) {
      options.changeId = readInlineValue(arg, '--change')
      continue
    }
    if (arg === '--format') {
      options.format = normalizeFormat(readValue(args, index, arg))
      index += 1
      continue
    }
    if (arg.startsWith('--format=')) {
      options.format = normalizeFormat(readInlineValue(arg, '--format'))
      continue
    }
    if (!options.changeId && !arg.startsWith('-')) {
      options.changeId = arg
      continue
    }
    throw new Error(`未知参数: ${arg}`)
  }

  if (!options.help && !options.changeId) {
    throw new Error('必须提供 --change <change-id>')
  }
  return options
}

function readValue(args, index, arg) {
  const value = args[index + 1]
  if (!isOptionValue(value)) {
    throw new Error(`${arg} 需要取值`)
  }
  return value
}

function readInlineValue(arg, name) {
  const value = arg.slice(`${name}=`.length)
  if (!value) {
    throw new Error(`${name} 需要取值`)
  }
  return value
}

function isOptionValue(value) {
  return typeof value === 'string' && value.length > 0 && !value.startsWith('-')
}

function normalizeFormat(value) {
  if (value !== 'text' && value !== 'json') {
    throw new Error('--format 只支持 text 或 json')
  }
  return value
}

function isSafeChangeId(changeId) {
  return CHANGE_ID_PATTERN.test(String(changeId ?? ''))
}

function normalizeDisplayPath(filePath) {
  return String(filePath ?? '').replace(/\\/g, '/').replace(/^\.\//, '')
}

function defaultIo() {
  return {
    writeOut: (text) => process.stdout.write(text),
    writeErr: (text) => process.stderr.write(text)
  }
}

function helpText() {
  return `DataSpec OpenSpec 准备度检查

用法:
  node tools/dataspec-openspec-readiness.mjs --change <change-id> [--format text|json]
  node tools/dataspec-openspec-readiness.mjs --root <repo> --change <change-id>

说明:
  本工具只读扫描 openspec/changes/<change-id>，输出开工前准备度提示；
  不执行验证命令、不实现代码、不提交、不归档。
`
}

if (process.argv[1] && path.resolve(process.argv[1]) === SCRIPT_PATH) {
  process.exitCode = await runReadinessCli()
}
