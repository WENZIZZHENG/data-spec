#!/usr/bin/env node

import { execFile } from 'node:child_process'
import { chmod, lstat, mkdir, open, readdir, readFile, realpath, rm, stat, writeFile } from 'node:fs/promises'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { inflateRawSync } from 'node:zlib'
import { promisify } from 'node:util'
import { loadDataSpecConfig, resolveDefaultPaths } from './dataspec-config.mjs'
import {
  formatWorkflowListText,
  formatWorkflowRecipeText,
  getWorkflowRecipe,
  supportedWorkflowRecipeIds,
  workflowCatalogPayload
} from './dataspec-workflows.mjs'
import {
  createTaskCard,
  renderTaskCardMarkdown,
  updateTaskCardStep
} from './dataspec-task-card.mjs'
import {
  buildCodeFieldReferenceIndex,
  formatCodeFieldReferenceIndexText
} from './dataspec-code-refs.mjs'
import {
  buildCodeFieldPatchPlan,
  formatCodeFieldPatchPlanMarkdown
} from './dataspec-code-patch-plan.mjs'
import {
  runConsumerCompatibilityCli
} from './dataspec-consumer-compat-check.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
const CLI_VERSION = '0.1.0'
const DEFAULT_GITHUB_API = 'https://api.github.com'
const DATASPEC_REVIEW_MARKER = '<!-- dataspec-sql-review -->'
const DATASPEC_INLINE_REVIEW_PREFIX = 'dataspec-inline-review'
const TOOLS_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.dirname(TOOLS_DIR)
const DATASPEC_WEB_DIR = path.join(REPO_ROOT, 'dataspec-web')
const OPENAPI_SCHEMA_PATH = path.join(DATASPEC_WEB_DIR, 'src', 'api', 'schema.ts')
const DEFAULT_INIT_PATHS = ['sql', 'db/migrations']
const DATASPEC_AGENTS_START = '<!-- dataspec-agents:start -->'
const DATASPEC_AGENTS_END = '<!-- dataspec-agents:end -->'
const DATASPEC_HOOK_START = '# dataspec-install-hook:start'
const DATASPEC_HOOK_END = '# dataspec-install-hook:end'
const DATASPEC_JSON_MARKER = 'local-sql-check-hooks@1'
const CONTEXT_CACHE_DIR = path.join('.dataspec', 'context')
const CACHE_METADATA_FILE = 'cache-metadata.json'
const DEFAULT_CONTEXT_CACHE_TTL_DAYS = 7
const MS_PER_DAY = 24 * 60 * 60 * 1000
const TEST_DATA_MAX_FIELDS = 100
const TEST_DATA_MAX_CASES_PER_FIELD = 3
const TEST_DATA_MAX_SEED_ROWS = 50
const ZIP_LOCAL_FILE_HEADER = 0x04034b50
const ZIP_CENTRAL_DIRECTORY_HEADER = 0x02014b50
const ZIP_END_OF_CENTRAL_DIRECTORY = 0x06054b50
const ZIP_METHOD_STORED = 0
const ZIP_METHOD_DEFLATED = 8
const ZIP_FLAG_ENCRYPTED = 0x0001
const execFileAsync = promisify(execFile)
const SKIPPED_SCAN_DIRECTORIES = new Set([
  '.git',
  '.idea',
  '.vscode',
  'build',
  'dist',
  'node_modules',
  'target'
])
const CONTEXT_QUALITY_CATEGORIES = [
  {
    id: 'manifest',
    label: 'AI Context manifest',
    weight: 15,
    critical: true
  },
  {
    id: 'fieldCatalog',
    label: '字段目录',
    weight: 25,
    critical: true
  },
  {
    id: 'rules',
    label: '数据库规则',
    weight: 20,
    critical: true
  },
  {
    id: 'schemaRegistry',
    label: 'schema registry/契约',
    weight: 15,
    critical: true
  },
  {
    id: 'prompts',
    label: 'Prompt 模板',
    weight: 10,
    critical: false
  },
  {
    id: 'businessTerms',
    label: '业务术语',
    weight: 5,
    critical: false
  },
  {
    id: 'examples',
    label: '样例或证据',
    weight: 10,
    critical: false
  }
]

class DataSpecCliError extends Error {
  constructor(message, diagnostic) {
    super(message)
    this.diagnostic = diagnostic
  }
}

export async function runCli(argv, io = processIo(), fetchFn = globalThis.fetch) {
  try {
    if (!fetchFn) {
      throw new Error('当前 Node 版本不支持 fetch，请使用 Node.js 18+')
    }
    const [command, ...rest] = argv
    if (!command || command === '--help' || command === '-h') {
      io.writeOut(helpText())
      return 0
    }
    if (command === 'lint') {
      return await runLint(rest, io, fetchFn)
    }
    if (command === 'lint-debug') {
      return await runLintDebug(rest, io, fetchFn)
    }
    if (command === 'lint-files') {
      return await runLintFiles(rest, io, fetchFn)
    }
    if (command === 'fixed-sql' || command === 'fixedsql') {
      return await runFixedSql(rest, io)
    }
    if (command === 'changed') {
      return await runChanged(rest, io)
    }
    if (command === 'lint-changed') {
      return await runLintChanged(rest, io, fetchFn)
    }
    if (command === 'install-hook' || command === 'install-hooks') {
      return await runInstallHook(rest, io)
    }
    if (command === 'index-refs') {
      return await runIndexRefs(rest, io)
    }
    if (command === 'code-patch' || command === 'codepatch') {
      return await runCodePatch(rest, io)
    }
    if (command === 'review-pr') {
      return await runReviewPr(rest, io, fetchFn)
    }
    if (command === 'export-context') {
      return await runExportContext(rest, io, fetchFn)
    }
    if (command === 'context-budget' || command === 'contextbudget') {
      return await runContextBudget(rest, io, fetchFn)
    }
    if (command === 'context-quality' || command === 'contextquality') {
      return await runContextQuality(rest, io)
    }
    if (command === 'suggest-field') {
      return await runSuggestField(rest, io, fetchFn)
    }
    if (command === 'search-fields') {
      return await runSearchFields(rest, io, fetchFn)
    }
    if (command === 'ref' || command === 'refs') {
      return await runRef(rest, io, fetchFn)
    }
    if (command === 'ai-output' || command === 'aioutput') {
      return await runAiOutput(rest, io, fetchFn)
    }
    if (command === 'generate-ddl') {
      return await runGenerateDdl(rest, io, fetchFn)
    }
    if (command === 'table-standards' || command === 'tablestandards') {
      return await runTableStandards(rest, io, fetchFn)
    }
    if (command === 'field-knowledge' || command === 'fieldknowledge') {
      return await runFieldKnowledge(rest, io, fetchFn)
    }
    if (command === 'field-semantics' || command === 'fieldsemantics') {
      return await runFieldSemantics(rest, io, fetchFn)
    }
    if (command === 'metric-definitions' || command === 'metricdefinitions') {
      return await runMetricDefinitions(rest, io, fetchFn)
    }
    if (command === 'synthetic-examples' || command === 'synthetic-example') {
      return await runSyntheticExamples(rest, io, fetchFn)
    }
    if (command === 'test-data' || command === 'testdata') {
      return await runTestData(rest, io, fetchFn)
    }
    if (command === 'consumer-compat' || command === 'consumercompat') {
      return await runConsumerCompatibility(rest, io)
    }
    if (command === 'contract-import' || command === 'contractimport') {
      return await runContractImport(rest, io, fetchFn)
    }
    if (command === 'schema-plan') {
      return await runSchemaPlan(rest, io, fetchFn)
    }
    if (command === 'comment-plan') {
      return await runCommentPlan(rest, io, fetchFn)
    }
    if (command === 'init') {
      return await runInit(rest, io, fetchFn)
    }
    if (command === 'doctor') {
      return await runDoctor(rest, io, fetchFn)
    }
    if (command === 'compat' || command === 'compatibility') {
      return await runCompat(rest, io, fetchFn)
    }
    if (command === 'evidence') {
      return await runEvidence(rest, io, fetchFn)
    }
    if (command === 'task' || command === 'tasks') {
      return await runTask(rest, io, fetchFn)
    }
    if (command === 'quality-gate' || command === 'quality') {
      return await runQualityGate(rest, io, fetchFn)
    }
    if (command === 'profile' || command === 'profiles') {
      return await runProfile(rest, io, fetchFn)
    }
    if (command === 'contract' || command === 'contracts') {
      return await runContract(rest, io, fetchFn)
    }
    if (command === 'capability' || command === 'capabilities') {
      return await runCapability(rest, io, fetchFn)
    }
    if (command === 'bootstrap') {
      return await runBootstrap(rest, io, fetchFn)
    }
    if (command === 'workflow' || command === 'workflows') {
      return runWorkflow(rest, io)
    }
    if (command === 'task-card' || command === 'taskcard') {
      return await runTaskCard(rest, io)
    }
    throw new Error(`未知命令: ${command}\n\n${helpText()}`)
  } catch (error) {
    io.writeErr(formatCliError(error))
    return 2
  }
}

async function runLint(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'format',
    'server',
    'dataspec-token',
    'idempotency-key',
    'idempotencyKey',
    'profile',
    'task-type',
    'taskType'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  const sqlPath = positional[0]
  if (!sqlPath) {
    throw new Error('lint 需要提供 SQL 文件路径或 -')
  }
  if (positional.length > 1) {
    throw new Error(`lint 只接受一个 SQL 输入路径，收到: ${positional.slice(1).join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json' && format !== 'text') {
    throw new Error('lint 当前仅支持 --format json 或 text')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const idempotencyKey = resolveIdempotencyKey(options)
  const profileSelection = resolveProfileSelection(options, config)
  const sql = sqlPath === '-' ? await io.readStdin() : await readFile(sqlPath, 'utf8')

  const response = await fetchFn(`${server}/api/lint`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }, idempotencyKey),
    body: JSON.stringify({ sql, projectId, ...profileSelection })
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatLintText(result))
  }
  return Number(result.errorCount ?? 0) > 0 ? 1 : 0
}

async function runLintDebug(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'format',
    'server',
    'dataspec-token',
    'profile',
    'task-type',
    'taskType',
    'fix-mode',
    'fixMode',
    'max-risk',
    'maxRisk',
    'include-explanations',
    'includeExplanations',
    'enable-rule',
    'enableRule',
    'disable-rule',
    'disableRule'
  ], [], ['enable-rule', 'enableRule', 'disable-rule', 'disableRule'])
  const config = loadDataSpecConfig(cliCwd(io))
  const sqlPath = positional[0]
  if (!sqlPath) {
    throw new Error('lint-debug 需要提供 SQL 文件路径或 -')
  }
  if (positional.length > 1) {
    throw new Error(`lint-debug 只接受一个 SQL 输入路径，收到: ${positional.slice(1).join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('lint-debug 当前仅支持 --format json')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const profileSelection = resolveProfileSelection(options, config)
  const fixPolicy = resolveCliFixPolicy(options)
  const sql = sqlPath === '-' ? await io.readStdin() : await readFile(sqlPath, 'utf8')
  const body = { sql, projectId, ...profileSelection }
  if (fixPolicy) {
    body.fixPolicy = fixPolicy
  }

  const response = await fetchFn(`${server}/api/lint/debug`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

function runWorkflow(args, io) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'list' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, ['format'])
    if (positional.length > 0) {
      throw new Error(`workflow list 不接受位置参数: ${positional.join(', ')}`)
    }
    const format = options.format ?? 'text'
    if (format === 'json') {
      io.writeOut(`${JSON.stringify(workflowCatalogPayload(), null, 2)}\n`)
      return 0
    }
    if (format !== 'text') {
      throw new Error('workflow list 仅支持 --format text|json')
    }
    io.writeOut(`${formatWorkflowListText()}\n`)
    return 0
  }
  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, ['format'])
    const recipeId = positional[0]
    if (!recipeId) {
      throw new Error('workflow show 需要提供 recipe id')
    }
    if (positional.length > 1) {
      throw new Error(`workflow show 只接受一个 recipe id，收到: ${positional.slice(1).join(', ')}`)
    }
    const recipe = getWorkflowRecipe(recipeId)
    if (!recipe) {
      throw new Error(`未知 workflow recipe: ${recipeId}。支持的 recipe: ${supportedWorkflowRecipeIds().join(', ')}`)
    }
    const format = options.format ?? 'text'
    if (format === 'json') {
      io.writeOut(`${JSON.stringify({
        kind: 'dataspec-workflow-recipe',
        schemaVersion: 1,
        recipe
      }, null, 2)}\n`)
      return 0
    }
    if (format !== 'text') {
      throw new Error('workflow show 仅支持 --format text|json')
    }
    io.writeOut(`${formatWorkflowRecipeText(recipe)}\n`)
    return 0
  }
  throw new Error(`未知 workflow 子命令: ${subcommand}。支持: list, show`)
}

async function runTaskCard(args, io) {
  const [subcommand, ...rest] = args
  if (subcommand === 'create') {
    return await runTaskCardCreate(rest, io)
  }
  if (subcommand === 'show') {
    return await runTaskCardShow(rest, io)
  }
  if (subcommand === 'update') {
    return await runTaskCardUpdate(rest, io)
  }
  throw new Error(`未知 task-card 子命令: ${subcommand ?? ''}。支持: create, show, update`)
}

async function runTaskCardCreate(args, io) {
  const { positional, options } = parseArgs(args, ['workflow', 'goal', 'project', 'input', 'format', 'output'], [], ['input'])
  if (positional.length > 0) {
    throw new Error(`task-card create 不接受位置参数: ${positional.join(', ')}`)
  }
  if (!options.workflow) {
    throw new Error('task-card create 需要提供 --workflow <id>')
  }
  if (!options.goal) {
    throw new Error('task-card create 需要提供 --goal <text>')
  }
  const format = options.format ?? 'json'
  if (!['json', 'markdown'].includes(format)) {
    throw new Error('task-card create 仅支持 --format json|markdown')
  }
  const projectId = parseOptionalProjectId(options.project)
  const output = options.output ? resolveTaskCardOutputPath(options.output, cliCwd(io)) : null
  const inputs = parseTaskCardInputs(options.input)
  const card = createTaskCard({
    workflowId: options.workflow,
    projectId,
    goal: options.goal,
    inputs,
    outputPath: output ? formatOutputPath(output) : undefined
  })
  const content = format === 'markdown'
    ? renderTaskCardMarkdown(card)
    : `${JSON.stringify(card, null, 2)}\n`
  if (output) {
    await mkdir(path.dirname(output), { recursive: true })
    await writeFile(output, ensureTrailingNewline(content), 'utf8')
  } else {
    io.writeOut(ensureTrailingNewline(content))
  }
  return 0
}

async function runTaskCardShow(args, io) {
  const { positional, options } = parseArgs(args, ['file', 'format'])
  if (positional.length > 0) {
    throw new Error(`task-card show 不接受位置参数: ${positional.join(', ')}`)
  }
  const filePath = resolveTaskCardInputPath(options.file, cliCwd(io))
  const format = options.format ?? 'json'
  if (!['json', 'markdown'].includes(format)) {
    throw new Error('task-card show 仅支持 --format json|markdown')
  }
  const card = JSON.parse(await readFile(filePath, 'utf8'))
  io.writeOut(format === 'markdown'
    ? ensureTrailingNewline(renderTaskCardMarkdown(card))
    : `${JSON.stringify(card, null, 2)}\n`)
  return 0
}

async function runTaskCardUpdate(args, io) {
  const { positional, options } = parseArgs(args, ['file', 'step', 'status', 'artifact', 'notes', 'resume-command', 'resumeCommand', 'format'])
  if (positional.length > 0) {
    throw new Error(`task-card update 不接受位置参数: ${positional.join(', ')}`)
  }
  if (!options.step) {
    throw new Error('task-card update 需要提供 --step <id>')
  }
  if (!options.status) {
    throw new Error('task-card update 需要提供 --status <status>')
  }
  const filePath = resolveTaskCardInputPath(options.file, cliCwd(io))
  const format = options.format ?? 'json'
  if (!['json', 'markdown'].includes(format)) {
    throw new Error('task-card update 仅支持 --format json|markdown')
  }
  const card = JSON.parse(await readFile(filePath, 'utf8'))
  const updated = updateTaskCardStep(card, {
    stepId: options.step,
    status: String(options.status).trim().toUpperCase(),
    artifact: options.artifact,
    notes: options.notes,
    resumeCommand: options.resumeCommand ?? options['resume-command']
  })
  await writeFile(filePath, `${JSON.stringify(updated, null, 2)}\n`, 'utf8')
  io.writeOut(format === 'markdown'
    ? ensureTrailingNewline(renderTaskCardMarkdown(updated))
    : `${JSON.stringify(updated, null, 2)}\n`)
  return 0
}

async function runLintFiles(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'format',
    'server',
    'dataspec-token',
    'idempotency-key',
    'idempotencyKey',
    'delivery-package',
    'batch-package',
    'profile',
    'task-type',
    'taskType'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  const inputPaths = positional.length > 0 ? positional : resolveDefaultPaths(config)
  if (inputPaths.length === 0) {
    throw new Error('lint-files 需要提供至少一个 SQL 文件或目录路径')
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const deliveryPackagePath = resolveDeliveryPackagePath(options)
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const idempotencyKey = resolveIdempotencyKey(options)
  const output = await lintSqlFiles(inputPaths, projectId, server, fetchFn, apiToken, resolveProfileSelection(options, config), idempotencyKey)
  if (deliveryPackagePath) {
    await writeDeliveryPackage(deliveryPackagePath, buildLintFilesDeliveryPackage(output, projectId))
  }
  io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  return output.summary.failedFiles > 0 ? 1 : 0
}

async function runFixedSql(args, io) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'patch') {
    throw new Error(`fixed-sql 仅支持 patch 子命令: ${subcommand ?? ''}`.trim())
  }
  const { positional, options } = parseArgs(rest, ['lint-result', 'target', 'format', 'confirm'], ['apply'])
  if (positional.length > 0) {
    throw new Error(`fixed-sql patch 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('fixed-sql patch 当前仅支持 --format json')
  }

  const cwd = await realpath(cliCwd(io))
  const lintResultPath = await resolveFixedSqlPatchExistingPath(options['lint-result'], cwd, 'lint-result')
  const targetFilePath = await resolveFixedSqlPatchExistingPath(options.target, cwd, 'target')
  const lintResult = parseFixedSqlPatchLintResult(await readFile(lintResultPath, 'utf8'), options['lint-result'])
  const targetContent = await readFile(targetFilePath, 'utf8')
  const plan = buildFixedSqlPatchPlan({
    cwd,
    lintResultPath,
    targetPath: targetFilePath,
    targetContent,
    lintResult,
    confirm: options.confirm
  })

  if (!options.apply) {
    io.writeOut(`${JSON.stringify(plan, null, 2)}\n`)
    if (plan.dryRunResult.status === 'CONFLICT') {
      throw new DataSpecCliError('fixed-sql patch 检测到冲突，未写入目标文件。', {
        code: 'FIXED_SQL_PATCH_CONFLICT',
        category: 'VALIDATION',
        retryable: false,
        suggestedAction: '重新运行 SQL lint 生成新 fixedSql，或手工处理冲突后重试。',
        conflictWarnings: plan.conflictWarnings
      })
    }
    return 0
  }

  if (!options.confirm) {
    throw new DataSpecCliError('fixed-sql patch apply 需要 --confirm <planHash>；请先运行 dry-run 并人工确认计划。', {
      code: 'FIXED_SQL_PATCH_CONFIRM_REQUIRED',
      category: 'VALIDATION',
      retryable: false,
      suggestedAction: '先运行 fixed-sql patch dry-run，审查 unifiedDiff 后使用输出的 planHash 确认。'
    })
  }
  if (plan.dryRunResult.status !== 'READY' || plan.planHash !== options.confirm) {
    throw new DataSpecCliError('fixed-sql patch apply 确认失败或计划不可应用，未写入目标文件。', {
      code: 'FIXED_SQL_PATCH_APPLY_BLOCKED',
      category: 'VALIDATION',
      retryable: false,
      suggestedAction: '重新运行 dry-run，确认 targetPath、unifiedDiff 和最新 planHash 后再 apply。',
      conflictWarnings: plan.conflictWarnings
    })
  }

  const patchSource = selectFixedSqlPatchSource(lintResult, {
    cwd,
    targetPath: targetFilePath,
    targetContent
  })
  await writeFixedSqlPatchTarget({
    cwd,
    targetInputPath: options.target,
    expectedPath: targetFilePath,
    expectedSha256: plan.currentFileSha256,
    fixedSql: patchSource.fixedSql
  })
  io.writeOut(`${JSON.stringify({
    ...plan,
    dryRunResult: {
      ...plan.dryRunResult,
      status: 'APPLIED',
      willWrite: true,
      confirmed: true,
      applied: true
    },
    applyCommand: null,
    nextActions: [
      '补丁已写入目标 SQL 文件。',
      '请运行项目 SQL 校验、代码评审和版本控制 diff 检查后再提交。'
    ]
  }, null, 2)}\n`)
  return 0
}

async function runChanged(args, io) {
  const { positional, options } = parseArgs(args, [
    'project',
    'format',
    'server',
    'profile',
    'task-type',
    'taskType'
  ])
  if (positional.length > 0) {
    throw new Error(`changed 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'text'
  if (!['text', 'json'].includes(format)) {
    throw new Error('changed 仅支持 --format text|json')
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const output = publicChangedWorkflowPayload(await buildChangedWorkflow(config, options))
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  } else {
    io.writeOut(formatChangedText(output))
  }
  return 0
}

async function runLintChanged(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'format',
    'server',
    'dataspec-token',
    'idempotency-key',
    'idempotencyKey',
    'profile',
    'task-type',
    'taskType'
  ])
  if (positional.length > 0) {
    throw new Error(`lint-changed 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'json'
  if (!['json', 'text'].includes(format)) {
    throw new Error('lint-changed 仅支持 --format json|text')
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const changed = await buildChangedWorkflow(config, options)
  let lint = emptyLintOutput()
  const diagnostics = [...changed.diagnostics]

  if (changed.files.sql.length > 0) {
    const projectId = parseProjectId(options.project ?? config.projectId)
    const server = normalizeServer(options.server ?? config.server)
    const apiToken = resolveDataSpecToken(options, config)
    lint = await lintSqlFiles(
      changed.files.sql.map((item) => item.absolutePath),
      projectId,
      server,
      fetchFn,
      apiToken,
      resolveProfileSelection(options, config),
      resolveIdempotencyKey(options)
    )
  } else if (changed.summary.totalFiles > 0 && !diagnostics.some((item) => item.code === 'NO_CHANGED_SQL_FILES')) {
    diagnostics.push(changedWorkflowDiagnostic('NO_CHANGED_SQL_FILES'))
  }

  const publicLint = publicLintChangedOutput(lint, changed)
  const output = {
    kind: 'dataspec.lint-changed-workflow',
    schemaVersion: 1,
    changed: publicChangedWorkflowPayload(changed),
    contextRecommendation: changed.contextRecommendation,
    lint: publicLint,
    summary: {
      changedFiles: changed.summary.totalFiles,
      changedSqlFiles: changed.summary.sqlFiles,
      failedFiles: lint.summary.failedFiles,
      errorCount: lint.summary.errorCount,
      warningCount: lint.summary.warningCount,
      suggestionCount: lint.summary.suggestionCount
    },
    diagnostics,
    nextActions: buildLintChangedNextActions(changed, lint, diagnostics)
  }

  if (format === 'json') {
    io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  } else {
    io.writeOut(formatLintChangedText(output))
  }
  return lint.summary.failedFiles > 0 ? 1 : 0
}

async function runInstallHook(args, io) {
  const { positional, options } = parseArgs(args, ['hook', 'format'], ['with-vscode'])
  if (positional.length > 0) {
    throw new Error(`install-hook 不接受位置参数: ${positional.join(', ')}`)
  }
  const hookName = options.hook ?? 'pre-commit'
  if (hookName !== 'pre-commit') {
    throw new Error(`install-hook 当前仅支持 --hook pre-commit`)
  }
  const format = options.format ?? 'text'
  if (!['json', 'text'].includes(format)) {
    throw new Error('install-hook 仅支持 --format text|json')
  }

  const config = loadDataSpecConfig(cliCwd(io))
  const gitRoot = await findGitRoot(config.rootDir)
  if (!gitRoot) {
    const output = installHookOutput({
      gitRoot: config.rootDir,
      hookName,
      withVscode: Boolean(options['with-vscode']),
      writtenFiles: [],
      skippedFiles: [],
      diagnostics: [installHookDiagnostic('NO_GIT_REPOSITORY')]
    })
    writeInstallHookOutput(io, output, format)
    return 2
  }

  const writtenFiles = []
  const skippedFiles = []
  const diagnostics = []
  const hookPath = await resolveGitHookPath(gitRoot, 'pre-commit')
  await writeManagedArtifact({
    filePath: hookPath,
    rootDir: gitRoot,
    allowedRootDir: path.dirname(path.dirname(hookPath)),
    displayPath: '.git/hooks/pre-commit',
    artifact: 'pre-commit-hook',
    marker: DATASPEC_HOOK_START,
    content: renderPreCommitHook(),
    writtenFiles,
    skippedFiles,
    diagnostics,
    unmanagedCode: 'HOOK_EXISTS_UNMANAGED',
    command: 'lint-changed --format json',
    makeExecutable: true
  })

  if (options['with-vscode']) {
    const vscodeDir = path.join(gitRoot, '.vscode')
    await writeManagedArtifact({
      filePath: path.join(vscodeDir, 'tasks.json'),
      rootDir: gitRoot,
      allowedRootDir: gitRoot,
      artifact: 'vscode-task',
      marker: DATASPEC_JSON_MARKER,
      content: renderVsCodeTasks(),
      writtenFiles,
      skippedFiles,
      diagnostics,
      unmanagedCode: 'VSCODE_FILE_EXISTS_UNMANAGED',
      command: 'lint-changed --format text'
    })
    await writeManagedArtifact({
      filePath: path.join(vscodeDir, 'dataspec-problem-matcher.json'),
      rootDir: gitRoot,
      allowedRootDir: gitRoot,
      artifact: 'vscode-problem-matcher',
      marker: DATASPEC_JSON_MARKER,
      content: renderVsCodeProblemMatcher(),
      writtenFiles,
      skippedFiles,
      diagnostics,
      unmanagedCode: 'VSCODE_FILE_EXISTS_UNMANAGED',
      command: 'lint-changed --format text'
    })
  }

  const output = installHookOutput({
    gitRoot,
    hookName,
    withVscode: Boolean(options['with-vscode']),
    writtenFiles,
    skippedFiles,
    diagnostics
  })
  writeInstallHookOutput(io, output, format)
  return diagnostics.length > 0 ? 2 : 0
}

async function writeManagedArtifact({
  filePath,
  rootDir,
  allowedRootDir = rootDir,
  displayPath,
  artifact,
  marker,
  content,
  writtenFiles,
  skippedFiles,
  diagnostics,
  unmanagedCode,
  command,
  makeExecutable = false
}) {
  const relativePath = displayPath ?? toPosixPath(path.relative(rootDir, filePath))
  const targetSafety = await validateManagedArtifactTarget(filePath, allowedRootDir)
  if (!targetSafety.ok) {
    skippedFiles.push({
      path: relativePath,
      artifact,
      reason: targetSafety.reason,
      command
    })
    diagnostics.push(installHookDiagnostic(targetSafety.code, { path: relativePath }))
    return
  }
  const existing = await readTextIfExists(filePath)
  // 只刷新 DataSpec marker 管理的文件，避免误覆盖用户已有 hook 或编辑器配置。
  if (existing !== null && !existing.includes(marker)) {
    skippedFiles.push({
      path: relativePath,
      artifact,
      reason: 'unmanaged-file',
      command
    })
    diagnostics.push(installHookDiagnostic(unmanagedCode, { path: relativePath }))
    return
  }

  await mkdir(path.dirname(filePath), { recursive: true })
  await writeFile(filePath, content, 'utf8')
  if (makeExecutable) {
    await chmod(filePath, 0o755)
  }
  writtenFiles.push({
    path: relativePath,
    artifact,
    action: existing === null ? 'created' : 'refreshed',
    command
  })
}

async function resolveGitHookPath(gitRoot, hookName) {
  const gitPath = (await execGit(gitRoot, ['rev-parse', '--git-path', `hooks/${hookName}`])).trim()
  return path.isAbsolute(gitPath)
    ? path.resolve(gitPath)
    : path.resolve(gitRoot, gitPath)
}

async function validateManagedArtifactTarget(filePath, allowedRootDir) {
  const targetPath = path.resolve(filePath)
  const rootDir = path.resolve(allowedRootDir)
  if (!isPathInside(rootDir, targetPath)) {
    return { ok: false, code: 'MANAGED_FILE_OUTSIDE_ROOT', reason: 'outside-managed-root' }
  }
  const rootStatus = await lstatIfExists(rootDir)
  if (rootStatus?.isSymbolicLink()) {
    return { ok: false, code: 'MANAGED_FILE_IS_SYMLINK', reason: 'symlink-root' }
  }
  const targetStatus = await lstatIfExists(targetPath)
  if (targetStatus?.isSymbolicLink()) {
    return { ok: false, code: 'MANAGED_FILE_IS_SYMLINK', reason: 'symlink-target' }
  }
  for (const parentDir of parentDirsBetween(rootDir, path.dirname(targetPath))) {
    const parentStatus = await lstatIfExists(parentDir)
    if (parentStatus?.isSymbolicLink()) {
      return { ok: false, code: 'MANAGED_FILE_IS_SYMLINK', reason: 'symlink-parent' }
    }
  }
  return { ok: true }
}

async function lstatIfExists(filePath) {
  try {
    return await lstat(filePath)
  } catch (error) {
    if (error.code === 'ENOENT' || error.code === 'ENOTDIR') {
      return null
    }
    throw error
  }
}

function parentDirsBetween(rootDir, leafDir) {
  const root = path.resolve(rootDir)
  let current = path.resolve(leafDir)
  const dirs = []
  while (current !== root && isPathInside(root, current)) {
    dirs.push(current)
    const parent = path.dirname(current)
    if (parent === current) {
      break
    }
    current = parent
  }
  return dirs.reverse()
}

function installHookOutput({ gitRoot, hookName, withVscode, writtenFiles, skippedFiles, diagnostics }) {
  return sanitizeSecretValue({
    kind: 'dataspec.local-sql-check.install-hook',
    schemaVersion: 1,
    hook: {
      name: hookName,
      path: '.git/hooks/pre-commit',
      command: 'node tools/dataspec-cli.mjs lint-changed --format json',
      managedMarker: DATASPEC_HOOK_START
    },
    repository: {
      rootDir: '.',
      gitRoot: gitRoot ? toPosixPath(path.relative(gitRoot, gitRoot) || '.') : null
    },
    writtenFiles,
    skippedFiles,
    diagnostics,
    safety: {
      writesProject: true,
      writesGitHooks: true,
      writesVSCodeSettings: Boolean(withVscode),
      overwritesUnmanagedFiles: false,
      storesCredentials: false,
      callsRemoteServiceDuringInstall: false,
      callsRemoteServiceWhenHookRuns: true,
      remoteWrites: false,
      sensitiveInputs: ['DATASPEC_TOKEN', '.dataspec/config.json apiToken'],
      generatedFilesDoNotStoreSecrets: true
    },
    nextActions: buildInstallHookNextActions(diagnostics)
  })
}

function buildInstallHookNextActions(diagnostics) {
  const codes = diagnostics.map((item) => item.code)
  if (codes.includes('NO_GIT_REPOSITORY')) {
    return [
      '切换到业务 git 仓库后重新运行 dataspec install-hook。',
      '确认 .dataspec/config.json 位于业务仓库内，并配置 defaultPaths。'
    ]
  }
  if (codes.includes('HOOK_EXISTS_UNMANAGED') || codes.includes('VSCODE_FILE_EXISTS_UNMANAGED')) {
    return [
      '手动合并 DataSpec 命令到已有本地配置，避免覆盖用户脚本。',
      '需要保留 AI 可读输出时，在 pre-commit 中调用 dataspec lint-changed --format json。',
      '需要 VS Code 跳转时，在 task 中调用 dataspec lint-changed --format text。'
    ]
  }
  return [
    '提交前会自动运行 dataspec lint-changed --format json。',
    '如生成了 VS Code task，可运行 “DataSpec: lint changed SQL” 查看可跳转诊断。',
    'CI/GitHub Review 仍应保留，local hook 只作为提交前前置反馈。'
  ]
}

function installHookDiagnostic(code, details = {}) {
  const diagnostics = {
    NO_GIT_REPOSITORY: {
      severity: 'WARNING',
      message: '当前目录不在 git 仓库内，无法安装 pre-commit hook。',
      suggestedAction: '切换到业务 git 仓库后重试，或先运行 git init。'
    },
    HOOK_EXISTS_UNMANAGED: {
      severity: 'WARNING',
      message: `目标 hook 已存在且不是 DataSpec 管理文件，已跳过: ${details.path ?? '.git/hooks/pre-commit'}`,
      suggestedAction: '请手动把 dataspec lint-changed --format json 合并到已有 pre-commit hook。'
    },
    MANAGED_FILE_IS_SYMLINK: {
      severity: 'WARNING',
      message: `目标路径或父目录是符号链接，已跳过以避免写出仓库边界: ${details.path ?? '-'}`,
      suggestedAction: '请移除符号链接或手动创建本地 DataSpec hook/task 配置。'
    },
    MANAGED_FILE_OUTSIDE_ROOT: {
      severity: 'WARNING',
      message: `目标路径不在允许的本地写入根目录内，已跳过: ${details.path ?? '-'}`,
      suggestedAction: '请在业务 git 仓库内重新运行 install-hook，或手动合并 DataSpec 命令。'
    },
    VSCODE_FILE_EXISTS_UNMANAGED: {
      severity: 'WARNING',
      message: `VS Code 配置已存在且不是 DataSpec 管理文件，已跳过: ${details.path ?? '.vscode'}`,
      suggestedAction: '请手动合并 DataSpec task/problem matcher，或先备份现有文件后再重试。'
    }
  }
  return {
    code,
    path: details.path ?? null,
    ...diagnostics[code]
  }
}

function writeInstallHookOutput(io, output, format) {
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  } else {
    io.writeOut(formatInstallHookText(output))
  }
}

function formatInstallHookText(output) {
  const lines = ['DataSpec Local SQL Check']
  lines.push(`Hook: ${output.hook.name}`)
  lines.push(`Written files: ${output.writtenFiles.length}`)
  for (const file of output.writtenFiles) {
    lines.push(`- ${file.path}: ${file.command}`)
  }
  if (output.skippedFiles.length > 0) {
    lines.push(`Skipped files: ${output.skippedFiles.length}`)
    for (const file of output.skippedFiles) {
      lines.push(`- ${file.path}: ${file.reason}`)
    }
  }
  if (output.diagnostics.length > 0) {
    lines.push('Diagnostics:')
    for (const diagnostic of output.diagnostics) {
      lines.push(`- [${diagnostic.severity}] ${diagnostic.code}: ${diagnostic.message}`)
      lines.push(`  next: ${diagnostic.suggestedAction}`)
    }
  }
  lines.push('Next actions:')
  for (const action of output.nextActions) {
    lines.push(`- ${action}`)
  }
  lines.push('')
  return lines.join('\n')
}

function renderPreCommitHook() {
  return `#!/bin/sh
${DATASPEC_HOOK_START}
# Managed by DataSpec CLI. Keep credentials in local env or DataSpec config.
if [ -n "$DATASPEC_CLI" ]; then
  sh -c "$DATASPEC_CLI lint-changed --format json"
else
  node tools/dataspec-cli.mjs lint-changed --format json
fi
status=$?
if [ "$status" -ne 0 ]; then
  echo "DataSpec SQL check failed. Fix ERROR issues or run dataspec lint-changed --format json for details." >&2
  exit "$status"
fi
${DATASPEC_HOOK_END}
`
}

function renderVsCodeTasks() {
  const matcher = dataspecSqlProblemMatcher()
  return `${JSON.stringify({
    version: '2.0.0',
    dataspecManaged: 'local-sql-check-hooks@1',
    tasks: [
      {
        label: 'DataSpec: lint changed SQL',
        type: 'shell',
        command: 'node tools/dataspec-cli.mjs lint-changed --format text',
        problemMatcher: matcher,
        group: 'test',
        presentation: {
          reveal: 'always',
          panel: 'dedicated',
          clear: true
        }
      }
    ]
  }, null, 2)}\n`
}

function renderVsCodeProblemMatcher() {
  return `${JSON.stringify({
    dataspecManaged: 'local-sql-check-hooks@1',
    problemMatcher: [dataspecSqlProblemMatcher()]
  }, null, 2)}\n`
}

function dataspecSqlProblemMatcher() {
  return {
    name: 'dataspec-sql',
    owner: 'dataspec',
    fileLocation: ['relative', '${workspaceFolder}'],
    pattern: {
      regexp: '^(.+):(\\d+):(\\d+):\\s+(ERROR|WARNING|INFO|SUGGESTION)\\s+([^\\s]+)\\s+-\\s+(.*(?:\\s+suggestion:\\s+.*)?)$',
      file: 1,
      line: 2,
      column: 3,
      severity: 4,
      code: 5,
      message: 6
    }
  }
}

async function runIndexRefs(args, io) {
  const { positional, options } = parseArgs(args, [
    'field',
    'alias',
    'path',
    'format'
  ], [], ['field', 'alias', 'path'])
  if (positional.length > 0) {
    throw new Error(`index-refs 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'json'
  if (!['text', 'json'].includes(format)) {
    throw new Error('index-refs 仅支持 --format text|json')
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const fieldNames = optionValues(options.field)
  const aliases = optionValues(options.alias)
  const explicitPaths = optionValues(options.path)
  const scanPaths = explicitPaths.length > 0 ? explicitPaths : resolveDefaultPaths(config)
  if (scanPaths.length === 0) {
    throw new DataSpecCliError('未配置 .dataspec/config.json 的 defaultPaths，已停止以避免扫描整个业务仓库。', {
      code: 'DATASPEC_DEFAULT_PATHS_MISSING',
      category: 'CONFIGURATION',
      severity: 'WARNING',
      retryable: true,
      suggestedAction: '传入 --path <file|dir>，或运行 dataspec init --default-path <path> 配置 defaultPaths 后重试。'
    })
  }
  let output
  try {
    output = await buildCodeFieldReferenceIndex({
      fieldNames,
      aliases,
      scanPaths,
      rootDir: explicitPaths.length > 0 ? cliCwd(io) : config.rootDir,
      outputRootDir: config.configPath ? config.rootDir : cliCwd(io)
    })
  } catch (error) {
    if (error?.diagnostic) {
      throw new DataSpecCliError(error.message, error.diagnostic)
    }
    throw error
  }
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  } else {
    io.writeOut(formatCodeFieldReferenceIndexText(output))
  }
  return 0
}

async function runCodePatch(args, io) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'plan') {
    throw new Error(`code-patch 仅支持 plan 子命令: ${subcommand ?? ''}`.trim())
  }
  const { positional, options } = parseArgs(rest, [
    'field',
    'to-field',
    'from-type',
    'to-type',
    'enum-change',
    'alias',
    'path',
    'format'
  ], [], ['enum-change', 'alias', 'path'])
  if (positional.length > 0) {
    throw new Error(`code-patch plan 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'json'
  if (!['json', 'markdown'].includes(format)) {
    throw new Error('code-patch plan 仅支持 --format json|markdown')
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const explicitPaths = optionValues(options.path)
  const scanPaths = explicitPaths.length > 0 ? explicitPaths : resolveDefaultPaths(config)
  if (scanPaths.length === 0) {
    throw new DataSpecCliError('未配置 .dataspec/config.json 的 defaultPaths，已停止以避免扫描整个业务仓库。', {
      code: 'DATASPEC_DEFAULT_PATHS_MISSING',
      category: 'CONFIGURATION',
      severity: 'WARNING',
      retryable: true,
      suggestedAction: '传入 --path <file|dir>，或运行 dataspec init --default-path <path> 配置 defaultPaths 后重试。'
    })
  }
  let output
  try {
    output = await buildCodeFieldPatchPlan({
      fieldName: options.field,
      aliases: optionValues(options.alias),
      renameTo: options['to-field'],
      fromType: options['from-type'],
      toType: options['to-type'],
      enumChanges: optionValues(options['enum-change']),
      scanPaths,
      rootDir: explicitPaths.length > 0 ? cliCwd(io) : config.rootDir,
      outputRootDir: config.configPath ? config.rootDir : cliCwd(io)
    })
  } catch (error) {
    if (error?.diagnostic) {
      throw new DataSpecCliError(error.message, error.diagnostic)
    }
    throw error
  }
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  } else {
    io.writeOut(formatCodeFieldPatchPlanMarkdown(output))
  }
  return 0
}

async function runReviewPr(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'repo',
    'pr',
    'token',
    'server',
    'github-api',
    'dataspec-token',
    'idempotency-key',
    'idempotencyKey',
    'format'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length === 0) {
    throw new Error('review-pr 需要提供至少一个 SQL 文件或目录路径')
  }
  const format = options.format ?? 'text'
  if (!['text', 'json'].includes(format)) {
    throw new Error('review-pr 仅支持 --format text|json')
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const repo = parseRepository(options.repo)
  const prNumber = parsePositiveInteger(options.pr, 'pull request number')
  const token = options.token ?? process.env.GITHUB_TOKEN
  if (!token) {
    throw new Error('review-pr 需要提供 --token <token> 或设置 GITHUB_TOKEN')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const githubApi = normalizeServer(options['github-api'] ?? DEFAULT_GITHUB_API)
  const lintOutput = await lintSqlFiles(positional, projectId, server, fetchFn, apiToken, {}, resolveIdempotencyKey(options))
  const inlineResult = hasReviewIssues(lintOutput)
    ? await publishInlineReviewComments({ repo, prNumber, token, githubApi, lintOutput, fetchFn })
    : emptyInlineResult()
  const body = buildReviewMarkdown(lintOutput, inlineResult.summary)
  const action = await upsertPullRequestComment({
    repo,
    prNumber,
    token,
    githubApi,
    body,
    fetchFn
  })
  if (format === 'json') {
    io.writeOut(`${JSON.stringify({
      reviewCommentAction: action,
      summary: lintOutput.summary,
      inline: inlineResult.summary,
      files: lintOutput.files
    }, null, 2)}\n`)
  } else {
    io.writeOut(
      `已${action === 'updated' ? '更新' : '创建'} DataSpec Review 评论；inline 创建 ${inlineResult.summary.inlineCommentsCreated}，跳过 ${inlineResult.summary.inlineCommentsSkipped}，fallback ${inlineResult.summary.fallbackIssues}\n`
    )
  }
  return lintOutput.summary.failedFiles > 0 ? 1 : 0
}

async function runExportContext(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'output',
    'server',
    'dataspec-token',
    'scope',
    'query',
    'status',
    'limit',
    'snapshot-id',
    'snapshotId',
    'snapshot-version',
    'snapshotVersion',
    'cache-ttl-days',
    'profile',
    'task-type',
    'taskType'
  ], ['cache'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`export-context 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const output = options.output
  const shouldCache = Boolean(options.cache)
  if (!output && !shouldCache) {
    throw new Error('export-context 需要提供 --output <zip> 或 --cache')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const exportOptions = { ...options, ...resolveProfileSelection(options, config) }
  const url = buildAiContextPackageUrl(server, projectId, exportOptions)
  const response = await fetchFn(url, { headers: dataSpecHeaders(apiToken) })
  if (!response.ok) {
    throw new Error(`导出 AI Context 失败，HTTP ${response.status}`)
  }
  const bytes = Buffer.from(await response.arrayBuffer())
  if (output) {
    await mkdir(path.dirname(output), { recursive: true })
    await writeFile(output, bytes)
    io.writeOut(`已导出 ${output}\n`)
  }
  if (shouldCache) {
    const cache = await writeAiContextCache({
      bytes,
      rootDir: config.rootDir,
      projectId,
      server,
      options: exportOptions,
      ttlDays: parseCacheTtlDays(options['cache-ttl-days'])
    })
    io.writeOut(`已缓存 AI Context 到 ${cache.cacheDir}\n`)
  }
  return 0
}

async function runContextBudget(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'plan') {
    throw new Error(`context-budget 仅支持 plan 子命令: ${subcommand ?? ''}`.trim())
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'token-budget',
    'tokenBudget',
    'format',
    'server',
    'dataspec-token',
    'profile',
    'task-type',
    'taskType',
    'scope',
    'query',
    'status',
    'limit',
    'target-table',
    'targetTable',
    'target-file',
    'targetFile'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`context-budget plan 不接受位置参数: ${positional.join(', ')}`)
  }
  const tokenBudgetValue = options.tokenBudget ?? options['token-budget']
  if (tokenBudgetValue === undefined) {
    throw new Error('context-budget plan 需要提供 --token-budget <n>')
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const tokenBudget = parsePositiveInteger(tokenBudgetValue, 'token budget')
  const format = options.format ?? 'json'
  if (!['json', 'text'].includes(format)) {
    throw new Error('context-budget plan 仅支持 --format json|text')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const profileSelection = resolveProfileSelection(options, config)
  const body = removeUndefinedValues({
    projectId,
    tokenBudget,
    ...profileSelection,
    scope: normalizeOptionalCliText(options.scope),
    query: normalizeOptionalCliText(options.query),
    status: normalizeOptionalCliText(options.status),
    limit: options.limit === undefined ? undefined : parseLimit(options.limit),
    targetTable: normalizeOptionalCliText(options.targetTable ?? options['target-table']),
    targetFile: normalizeOptionalCliText(options.targetFile ?? options['target-file'])
  })
  const response = await fetchFn(`${server}/api/ai-context/budget/plan`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  })
  const payload = await readJsonResponse(response)
  const plan = unwrapResponse(payload)
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(plan, null, 2)}\n`)
  } else {
    io.writeOut(formatContextBudgetPlanText(plan))
  }
  return 0
}

async function runContextQuality(args, io) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'check') {
    throw new Error(`context-quality 仅支持 check 子命令: ${subcommand ?? ''}`.trim())
  }
  const { positional, options } = parseArgs(rest, [
    'context-dir',
    'contextDir',
    'context-zip',
    'contextZip',
    'budget-plan',
    'budgetPlan',
    'format'
  ])
  if (positional.length > 0) {
    throw new Error(`context-quality check 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'json'
  if (!['json', 'text'].includes(format)) {
    throw new Error('context-quality check 仅支持 --format json|text')
  }
  const inputOptions = [
    ['context-dir', options.contextDir ?? options['context-dir']],
    ['context-zip', options.contextZip ?? options['context-zip']],
    ['budget-plan', options.budgetPlan ?? options['budget-plan']]
  ].filter(([, value]) => value !== undefined)
  if (inputOptions.length === 0) {
    throw new Error('context-quality check 需要提供 --context-dir、--context-zip 或 --budget-plan')
  }
  if (inputOptions.length > 1) {
    throw new Error('context-quality check 只能提供一个输入源')
  }

  const [sourceType, inputPath] = inputOptions[0]
  const cwd = cliCwd(io)
  const input = {
    sourceType,
    path: displayInputPath(inputPath, cwd)
  }
  let result
  if (sourceType === 'context-dir') {
    result = evaluateContextQualityFromEntries(input, await readContextDirectoryEntries(path.resolve(cwd, inputPath)))
  } else if (sourceType === 'context-zip') {
    result = evaluateContextQualityFromEntries(input, await readContextZipEntries(path.resolve(cwd, inputPath)))
  } else {
    result = evaluateContextQualityFromBudgetPlan(input, await readContextBudgetPlanFile(path.resolve(cwd, inputPath)))
  }

  if (format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatContextQualityText(result))
  }
  return 0
}

async function readContextDirectoryEntries(rootDir) {
  const rootStat = await lstat(rootDir)
  if (!rootStat.isDirectory()) {
    throw new Error(`context-quality check 的 --context-dir 不是目录: ${rootDir}`)
  }
  const entries = []
  async function walk(currentDir, relativeDir = '') {
    for (const dirent of await readdir(currentDir, { withFileTypes: true })) {
      const fullPath = path.join(currentDir, dirent.name)
      const relativePath = relativeDir ? path.join(relativeDir, dirent.name) : dirent.name
      if (dirent.isDirectory()) {
        if (!SKIPPED_SCAN_DIRECTORIES.has(dirent.name)) {
          await walk(fullPath, relativePath)
        }
        continue
      }
      const fileStat = await lstat(fullPath)
      if (fileStat.isSymbolicLink() || !fileStat.isFile()) {
        continue
      }
      const content = await readFile(fullPath)
      entries.push(contextQualityEntry(toPosixPath(relativePath), content))
    }
  }
  await walk(rootDir)
  return entries.sort((left, right) => left.path.localeCompare(right.path))
}

async function readContextZipEntries(zipPath) {
  const bytes = await readFile(zipPath)
  return normalizeZipEntries(readZipEntries(bytes))
    .filter(({ entry, relativePath }) => relativePath && !entry.directory)
    .map(({ entry, relativePath }) => contextQualityEntry(toPosixPath(relativePath), entry.content))
    .sort((left, right) => left.path.localeCompare(right.path))
}

async function readContextBudgetPlanFile(planPath) {
  const payload = JSON.parse(await readFile(planPath, 'utf8'))
  const plan = payload?.data && payload?.code ? payload.data : payload
  if (!plan || typeof plan !== 'object') {
    throw new Error('context-quality check 的 --budget-plan 必须是 JSON 对象')
  }
  return plan
}

function contextQualityEntry(relativePath, content) {
  const buffer = Buffer.from(content)
  return {
    path: relativePath,
    sizeBytes: buffer.length,
    textSample: buffer.toString('utf8', 0, Math.min(buffer.length, 4096))
  }
}

function evaluateContextQualityFromEntries(input, entries) {
  const coverageByCategory = buildContextCoverage(entries)
  const missingCriticalResources = missingCriticalContextResources(coverageByCategory)
  const truncatedResources = entries
    .filter((entry) => contextEntryLooksTruncated(entry))
    .map((entry) => ({
      path: entry.path,
      reason: '资源内容出现截断标记或 token budget exceeded 提示。'
    }))
  const score = contextQualityScoreFromCoverage(coverageByCategory, truncatedResources.length)
  const qualityLevel = contextQualityLevel(score)
  return {
    kind: 'dataspec-ai-context-quality-check',
    schemaVersion: 1,
    input,
    contextQualityScore: score,
    qualityLevel,
    tokenBudgetBreakdown: buildContextFileTokenBreakdown(entries, coverageByCategory),
    missingCriticalResources,
    truncatedResources,
    coverageByCategory,
    taskFitHints: contextTaskFitHints(qualityLevel, missingCriticalResources, truncatedResources, null),
    nextContextActions: contextNextActions(qualityLevel, missingCriticalResources, truncatedResources, [])
  }
}

function evaluateContextQualityFromBudgetPlan(input, plan) {
  validateContextBudgetPlan(plan)
  const selectedArtifacts = Array.isArray(plan.selectedArtifacts) ? plan.selectedArtifacts : []
  const droppedArtifacts = Array.isArray(plan.droppedArtifacts) ? plan.droppedArtifacts : []
  const coverageByCategory = emptyContextCoverage()
  coverageByCategory.manifest.present = true
  coverageByCategory.manifest.resourceCount = 1
  coverageByCategory.manifest.sampleResources.push('budget-plan.json')
  for (const item of selectedArtifacts) {
    for (const categoryId of detectContextCategories(String(item.artifact ?? ''), `${item.reason ?? ''}\n${item.riskImpact ?? ''}`)) {
      markBudgetPlanCoverage(coverageByCategory[categoryId], item, true)
    }
  }
  for (const item of droppedArtifacts) {
    for (const categoryId of detectContextCategories(String(item.artifact ?? ''), `${item.reason ?? ''}\n${item.riskImpact ?? ''}`)) {
      markBudgetPlanCoverage(coverageByCategory[categoryId], item, false)
    }
  }
  const missingCriticalResources = CONTEXT_QUALITY_CATEGORIES
    .filter((category) => category.critical)
    .filter((category) => !coverageByCategory[category.id].present)
    .map((category) => missingCriticalResource(
      category,
      coverageByCategory[category.id].droppedArtifactCount > 0
        ? '预算计划丢弃了该关键资源。'
        : '预算计划未选择该关键资源。'
    ))
  const droppedEstimatedTokens = droppedArtifacts.reduce((sum, item) => sum + Number(item.estimatedTokens ?? 0), 0)
  const tokenBudgetBreakdown = {
    source: 'budget-plan',
    tokenBudget: numberOrNull(plan.estimation?.tokenBudget ?? plan.request?.tokenBudget),
    selectedEstimatedTokens: numberOrNull(plan.estimation?.selectedEstimatedTokens),
    totalEstimatedTokens: numberOrNull(plan.estimation?.totalEstimatedTokens),
    droppedEstimatedTokens,
    budgetUtilization: ratioOrNull(plan.estimation?.selectedEstimatedTokens, plan.estimation?.tokenBudget ?? plan.request?.tokenBudget)
  }
  const score = contextBudgetPlanQualityScore(plan.qualityRisk, missingCriticalResources.length, tokenBudgetBreakdown)
  const qualityLevel = contextQualityLevel(score)
  const seedActions = [
    ...(Array.isArray(plan.fallbackSteps) ? plan.fallbackSteps : []),
    ...(Array.isArray(plan.recommendedNextActions) ? plan.recommendedNextActions : [])
  ]
  return {
    kind: 'dataspec-ai-context-quality-check',
    schemaVersion: 1,
    input,
    contextQualityScore: score,
    qualityLevel,
    tokenBudgetBreakdown,
    missingCriticalResources,
    truncatedResources: [],
    coverageByCategory,
    taskFitHints: contextTaskFitHints(qualityLevel, missingCriticalResources, [], String(plan.qualityRisk ?? '').toUpperCase()),
    nextContextActions: contextNextActions(qualityLevel, missingCriticalResources, [], seedActions)
  }
}

function validateContextBudgetPlan(plan) {
  const qualityRisk = String(plan.qualityRisk ?? '').toUpperCase()
  if (
    plan.kind !== 'dataspec-ai-context-budget-plan' ||
    Number(plan.schemaVersion) !== 1 ||
    !plan.estimation ||
    typeof plan.estimation !== 'object' ||
    !Array.isArray(plan.selectedArtifacts) ||
    !Array.isArray(plan.droppedArtifacts) ||
    !['LOW', 'MEDIUM', 'HIGH'].includes(qualityRisk)
  ) {
    throw new Error('context-quality check 的 --budget-plan 必须是 context-budget plan JSON，且包含 kind、schemaVersion、estimation、selectedArtifacts、droppedArtifacts 和 qualityRisk。')
  }
}

function emptyContextCoverage() {
  return {
    ...Object.fromEntries(CONTEXT_QUALITY_CATEGORIES.map((category) => [
    category.id,
    {
      present: false,
      resourceCount: 0,
      estimatedBytes: 0,
      sampleResources: [],
      selectedArtifactCount: 0,
      droppedArtifactCount: 0,
      droppedEstimatedTokens: 0
    }
  ])),
    unclassified: {
      present: false,
      resourceCount: 0,
      estimatedBytes: 0,
      sampleResources: [],
      selectedArtifactCount: 0,
      droppedArtifactCount: 0,
      droppedEstimatedTokens: 0
    }
  }
}

function buildContextCoverage(entries) {
  const coverage = emptyContextCoverage()
  for (const entry of entries) {
    const categoryIds = detectContextCategories(entry.path, entry.textSample)
    if (categoryIds.length === 0) {
      markContextEntryCoverage(coverage.unclassified, entry)
      continue
    }
    for (const categoryId of categoryIds) {
      const item = coverage[categoryId]
      markContextEntryCoverage(item, entry)
    }
  }
  return coverage
}

function markContextEntryCoverage(coverage, entry) {
  coverage.present = true
  coverage.resourceCount += 1
  coverage.estimatedBytes += entry.sizeBytes
  if (coverage.sampleResources.length < 5) {
    coverage.sampleResources.push(entry.path)
  }
}

function detectContextCategories(resourcePath, textSample = '') {
  const normalizedPath = resourcePath.toLowerCase().replace(/[_\-\s./]/g, '')
  const lowerText = textSample.toLowerCase()
  const categories = []
  if (resourcePath.toLowerCase().endsWith('manifest.json') || lowerText.includes('dataspec-ai-context-manifest')) {
    categories.push('manifest')
  }
  if (normalizedPath.includes('fieldcatalog') || lowerText.includes('字段目录') || lowerText.includes('"fields"')) {
    categories.push('fieldCatalog')
  }
  if (normalizedPath.includes('databaserules') || normalizedPath.includes('rules') || lowerText.includes('命名规则')) {
    categories.push('rules')
  }
  if (normalizedPath.includes('schemaregistry') || normalizedPath.includes('contract') || normalizedPath.includes('openapi')) {
    categories.push('schemaRegistry')
  }
  if (normalizedPath.includes('prompt') || lowerText.includes('提示词')) {
    categories.push('prompts')
  }
  if (normalizedPath.includes('glossary') || normalizedPath.includes('businessterm') || lowerText.includes('业务术语')) {
    categories.push('businessTerms')
  }
  if (normalizedPath.includes('example') || normalizedPath.includes('sample') || normalizedPath.includes('evidence')) {
    categories.push('examples')
  }
  return [...new Set(categories)]
}

function missingCriticalContextResources(coverageByCategory) {
  return CONTEXT_QUALITY_CATEGORIES
    .filter((category) => category.critical && !coverageByCategory[category.id].present)
    .map((category) => missingCriticalResource(category, '上下文中未发现该关键资源。'))
}

function missingCriticalResource(category, reason) {
  return {
    category: category.id,
    label: category.label,
    reason,
    nextAction: `重新导出 AI Context 并包含 ${category.label}。`
  }
}

function contextEntryLooksTruncated(entry) {
  return /truncated|截断|省略|token budget exceeded|omitted due to budget/i.test(`${entry.path}\n${entry.textSample}`)
}

function contextQualityScoreFromCoverage(coverageByCategory, truncatedCount) {
  const score = CONTEXT_QUALITY_CATEGORIES.reduce((sum, category) => {
    return sum + (coverageByCategory[category.id].present ? category.weight : 0)
  }, 0)
  return clampContextQualityScore(score - Math.min(20, truncatedCount * 10))
}

function contextBudgetPlanQualityScore(qualityRisk, missingCriticalCount, tokenBudgetBreakdown) {
  const baseScore = {
    LOW: 85,
    MEDIUM: 65,
    HIGH: 35
  }[String(qualityRisk ?? '').toUpperCase()] ?? 60
  const overflowPenalty = tokenBudgetBreakdown.totalEstimatedTokens && tokenBudgetBreakdown.tokenBudget && tokenBudgetBreakdown.totalEstimatedTokens > tokenBudgetBreakdown.tokenBudget * 2
    ? 5
    : 0
  return clampContextQualityScore(baseScore - missingCriticalCount * 5 - overflowPenalty)
}

function clampContextQualityScore(score) {
  return Math.max(0, Math.min(100, Math.round(score)))
}

function contextQualityLevel(score) {
  if (score >= 80) {
    return 'HIGH'
  }
  if (score >= 50) {
    return 'MEDIUM'
  }
  return 'LOW'
}

function buildContextFileTokenBreakdown(entries, coverageByCategory) {
  const totalBytes = entries.reduce((sum, entry) => sum + entry.sizeBytes, 0)
  return {
    source: 'context-files',
    resourceCount: entries.length,
    totalBytes,
    estimatedTokens: Math.ceil(totalBytes / 4),
    byCategory: Object.fromEntries(Object.entries(coverageByCategory).map(([categoryId, coverage]) => [
      categoryId,
      {
        estimatedBytes: coverage.estimatedBytes,
        estimatedTokens: Math.ceil(coverage.estimatedBytes / 4),
        resourceCount: coverage.resourceCount
      }
    ]))
  }
}

function markBudgetPlanCoverage(coverage, artifact, selected) {
  if (selected) {
    coverage.present = true
    coverage.selectedArtifactCount += 1
    coverage.resourceCount += 1
    if (coverage.sampleResources.length < 5 && artifact.artifact) {
      coverage.sampleResources.push(artifact.artifact)
    }
  } else {
    coverage.droppedArtifactCount += 1
    coverage.droppedEstimatedTokens += Number(artifact.estimatedTokens ?? 0)
  }
}

function contextTaskFitHints(qualityLevel, missingCriticalResources, truncatedResources, qualityRisk) {
  const hints = []
  if (qualityRisk === 'HIGH') {
    hints.push('qualityRisk=HIGH，当前预算计划存在高风险，不建议直接执行依赖完整字段/契约的 AI 任务。')
  }
  if (qualityLevel === 'HIGH') {
    hints.push('上下文关键资源较完整，适合继续执行读写前分析、DDL 草案或 SQL 修复类任务。')
  } else if (qualityLevel === 'MEDIUM') {
    hints.push('上下文可用于初步分析，但执行建表、迁移或复杂 SQL 前应先补齐缺失/截断资源。')
  } else {
    hints.push('上下文不足，不建议直接交给 AI 执行高风险任务。')
  }
  if (missingCriticalResources.length > 0) {
    hints.push(`缺少关键资源: ${missingCriticalResources.map((item) => item.label).join(', ')}。`)
  }
  if (truncatedResources.length > 0) {
    hints.push(`发现 ${truncatedResources.length} 个疑似截断资源，AI 可能缺少完整规则或字段说明。`)
  }
  return hints
}

function contextNextActions(qualityLevel, missingCriticalResources, truncatedResources, seedActions) {
  const actions = [...seedActions]
  for (const resource of missingCriticalResources) {
    actions.push(resource.nextAction)
  }
  if (truncatedResources.length > 0) {
    actions.push('提高 tokenBudget 或收窄 scope/query 后重新导出，避免关键资源被截断。')
  }
  if (qualityLevel === 'LOW') {
    actions.push('停止高风险 AI 写入或迁移任务，先补导出字段目录、规则和 schema registry。')
  }
  if (actions.length === 0) {
    actions.push('可以继续使用当前 AI Context；执行写入前仍需运行 lint 和质量门禁。')
  }
  return uniqueNonEmptyStrings(actions)
}

function uniqueNonEmptyStrings(values) {
  const seen = new Set()
  const result = []
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (!text || seen.has(text)) {
      continue
    }
    seen.add(text)
    result.push(text)
  }
  return result
}

function numberOrNull(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function ratioOrNull(numerator, denominator) {
  const left = Number(numerator)
  const right = Number(denominator)
  if (!Number.isFinite(left) || !Number.isFinite(right) || right <= 0) {
    return null
  }
  return Math.round((left / right) * 1000) / 1000
}

function displayInputPath(inputPath, cwd) {
  const resolved = path.resolve(cwd, inputPath)
  const relative = path.relative(cwd, resolved)
  if (relative === '') {
    return '.'
  }
  if (!relative.startsWith('..') && !path.isAbsolute(relative)) {
    return toPosixPath(relative)
  }
  return toPosixPath(resolved)
}

async function runSuggestField(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'format', 'server', 'limit', 'dataspec-token'])
  const config = loadDataSpecConfig(cliCwd(io))
  const query = positional[0]
  if (!query) {
    throw new Error('suggest-field 需要提供业务字段描述')
  }
  if (positional.length > 1) {
    throw new Error(`suggest-field 只接受一个描述参数，收到: ${positional.slice(1).join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const limit = parseLimit(options.limit)
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const url = `${server}/api/fields/suggest?projectId=${encodeURIComponent(projectId)}&query=${encodeURIComponent(query)}&limit=${encodeURIComponent(limit)}`
  const response = await fetchFn(url, { headers: dataSpecHeaders(apiToken) })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

async function runSearchFields(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'query',
    'category',
    'tag',
    'status',
    'sensitive',
    'source-batch',
    'sourceBatchId',
    'dsl',
    'dsl-file',
    'format',
    'server',
    'limit',
    'dataspec-token'
  ], ['stdin'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 1) {
    throw new Error(`search-fields 最多接受一个 query 参数，收到: ${positional.slice(1).join(', ')}`)
  }
  if (positional[0] && options.query) {
    throw new Error('search-fields 的 query 请使用位置参数或 --query 之一，不要同时传入')
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const hasDslInput = options.dsl !== undefined || options['dsl-file'] !== undefined || options.stdin === true
  if (hasDslInput) {
    const legacyOptions = [
      positional[0],
      options.query,
      options.category,
      options.tag,
      options.status,
      options.sensitive,
      options.sourceBatchId,
      options['source-batch']
    ].filter((value) => value !== undefined)
    if (legacyOptions.length > 0) {
      throw new Error('search-fields 使用 DSL 输入时不要同时传 legacy query/category/tag/status/sensitive/sourceBatchId 参数')
    }
    const server = normalizeServer(options.server ?? config.server)
    const apiToken = resolveDataSpecToken(options, config)
    const standardQuery = await readStandardQueryDsl(options, io)
    const response = await fetchFn(`${server}/api/standard-query/search`, {
      method: 'POST',
      headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ projectId, ...standardQuery })
    })
    const payload = await readJsonResponse(response)
    const result = unwrapResponse(payload)
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'query', positional[0] ?? options.query)
  appendOptionalParam(params, 'category', options.category)
  appendOptionalParam(params, 'tag', options.tag)
  appendOptionalParam(params, 'status', options.status)
  appendOptionalParam(params, 'sourceBatchId', options.sourceBatchId ?? options['source-batch'])
  if (options.sensitive !== undefined) {
    params.set('sensitive', String(parseOptionalBoolean(options.sensitive, 'sensitive')))
  }
  params.set('limit', String(parseLimit(options.limit, 20)))
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/fields/search?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
  return 0
}

async function readStandardQueryDsl(options, io) {
  const sources = [options.dsl !== undefined, options['dsl-file'] !== undefined, options.stdin === true]
    .filter(Boolean)
    .length
  if (sources !== 1) {
    throw new Error('search-fields DSL 输入需要且只能选择 --dsl、--dsl-file 或 --stdin')
  }
  let content
  if (options.dsl !== undefined) {
    content = options.dsl
  } else if (options['dsl-file'] !== undefined) {
    content = await readFile(path.resolve(cliCwd(io), options['dsl-file']), 'utf8')
  } else {
    content = await io.readStdin()
  }
  try {
    const parsed = JSON.parse(content)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('DSL JSON 必须是对象')
    }
    if (parsed.projectId !== undefined) {
      throw new Error('DSL JSON 不要包含 projectId，请使用 --project 指定')
    }
    return parsed
  } catch (error) {
    throw new Error(`search-fields DSL JSON 无效: ${sanitizeSecretText(error.message)}`)
  }
}

async function runRef(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'resolve') {
    throw new Error('未知 ref 子命令。支持: resolve')
  }
  const { positional, options } = parseArgs(
    rest,
    ['project', 'type', 'ref', 'format', 'server', 'dataspec-token'],
    [],
    ['ref']
  )
  if (positional.length > 0) {
    throw new Error(`ref resolve 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const projectId = parseProjectId(options.project ?? config.projectId)
  const refType = requiredOption(options.type, 'type', 'ref resolve')
  const refs = optionValues(options.ref).map((item) => String(item).trim()).filter(Boolean)
  if (refs.length === 0) {
    throw new Error('ref resolve 需要至少提供一个 --ref <value>')
  }
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('ref resolve 当前仅支持 --format json')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/standard-references/resolve`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ projectId, refType, refs })
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
  return 0
}

async function runAiOutput(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'check') {
    throw new Error('未知 ai-output 子命令。支持: check')
  }
  const { positional, options } = parseArgs(
    rest,
    ['project', 'type', 'file', 'snapshot-ref', 'snapshotRef', 'format', 'server', 'dataspec-token'],
    ['stdin']
  )
  if (positional.length > 0) {
    throw new Error(`ai-output check 不接受位置参数: ${positional.join(', ')}`)
  }
  if (options.file && options.stdin) {
    throw new Error('ai-output check 的 --file 和 --stdin 只能选择一个')
  }
  if (!options.file && !options.stdin) {
    throw new Error('ai-output check 需要提供 --file <path> 或 --stdin')
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const projectId = parseProjectId(options.project ?? config.projectId)
  const contentType = normalizeAiOutputPostCheckContentType(requiredOption(options.type, 'type', 'ai-output check'))
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('ai-output check 当前仅支持 --format json')
  }
  const content = options.stdin
    ? await io.readStdin()
    : await readFile(path.resolve(cliCwd(io), options.file), 'utf8')
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/ai-output/check`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(removeUndefinedValues({
      projectId,
      contentType,
      content,
      snapshotRef: options.snapshotRef ?? options['snapshot-ref']
    }))
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
  return result?.status === 'PASS' ? 0 : 1
}

async function runGenerateDdl(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'template', 'table', 'format', 'server', 'dataspec-token'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`generate-ddl 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const templateId = parsePositiveInteger(options.template, 'template id')
  const tableName = options.table
  if (!tableName) {
    throw new Error('generate-ddl 需要提供 --table <name>')
  }
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const url = `${server}/api/generator/ddl/preview?projectId=${encodeURIComponent(projectId)}&templateId=${encodeURIComponent(templateId)}&tableName=${encodeURIComponent(tableName)}`
  const response = await fetchFn(url, { headers: dataSpecHeaders(apiToken) })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

async function runTableStandards(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand === 'list' || !subcommand || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`table-standards list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json') {
      throw new Error('table-standards list 当前仅支持 --format json')
    }
    const projectId = parseProjectId(options.project ?? config.projectId)
    const result = await fetchTableStandards({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId
    })
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, [
      'project',
      'template',
      'business-object',
      'businessObject',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`table-standards show 不接受位置参数: ${positional.join(', ')}`)
    }
    const templateId = options.template === undefined ? undefined : parsePositiveInteger(options.template, 'template id')
    const businessObject = options.businessObject ?? options['business-object']
    if ((templateId === undefined && !businessObject) || (templateId !== undefined && businessObject)) {
      throw new Error('table-standards show 需要且只能二选一提供 --template <id> 或 --business-object <key>')
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json') {
      throw new Error('table-standards show 当前仅支持 --format json')
    }
    const projectId = parseProjectId(options.project ?? config.projectId)
    const result = await fetchTableStandards({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId,
      templateId,
      businessObject
    })
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    return 0
  }

  throw new Error(`未知 table-standards 子命令: ${subcommand}。支持: list, show`)
}

async function runSyntheticExamples(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'generate') {
    throw new Error('synthetic-examples 支持子命令: generate')
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'scenario',
    'max-cases',
    'maxCases',
    'format',
    'server',
    'dataspec-token'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`synthetic-examples generate 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const scenario = normalizeSyntheticScenario(options.scenario)
  const format = options.format ?? 'json'
  if (format !== 'json' && format !== 'text') {
    throw new Error('synthetic-examples generate 仅支持 --format text|json')
  }
  const maxCasesOption = options.maxCases ?? options['max-cases']
  const maxCases = maxCasesOption === undefined ? undefined : parsePositiveInteger(maxCasesOption, 'maxCases')
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  params.set('scenario', scenario)
  appendOptionalParam(params, 'maxCases', maxCases)
  const response = await fetchFn(`${server}/api/synthetic-examples/generate?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(format === 'json'
    ? `${JSON.stringify(result, null, 2)}\n`
    : formatSyntheticExamplesText(result))
  return 0
}

async function runConsumerCompatibility(args, io) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'check') {
    throw new Error('consumer-compat 支持子命令: check')
  }
  return await runConsumerCompatibilityCli(rest, io)
}

async function runTestData(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'generate') {
    throw new Error('test-data 支持子命令: generate')
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'field',
    'field-name',
    'fieldName',
    'object-scenario',
    'objectScenario',
    'max-fields',
    'maxFields',
    'cases-per-field',
    'casesPerField',
    'seed-row-count',
    'seedRowCount',
    'dialect',
    'format',
    'server',
    'dataspec-token'
  ], [], ['field', 'field-name', 'fieldName'])
  if (positional.length > 0) {
    throw new Error(`test-data generate 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const format = options.format ?? 'json'
  if (format !== 'json' && format !== 'text') {
    throw new Error('test-data generate 仅支持 --format text|json')
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const fieldNames = [
    ...optionValues(options.field),
    ...optionValues(options['field-name']),
    ...optionValues(options.fieldName)
  ].map((item) => String(item).trim()).filter(Boolean)
  if (fieldNames.length > TEST_DATA_MAX_FIELDS) {
    throw new Error(`maxFields 超过安全上限 ${TEST_DATA_MAX_FIELDS}`)
  }
  const req = removeUndefinedValues({
    projectId,
    fieldNames: fieldNames.length > 0 ? fieldNames : undefined,
    objectScenario: options.objectScenario ?? options['object-scenario'],
    maxFields: parseBoundedPositiveInteger(options.maxFields ?? options['max-fields'], 'maxFields', TEST_DATA_MAX_FIELDS),
    casesPerField: parseBoundedPositiveInteger(options.casesPerField ?? options['cases-per-field'], 'casesPerField', TEST_DATA_MAX_CASES_PER_FIELD),
    seedRowCount: parseBoundedPositiveInteger(options.seedRowCount ?? options['seed-row-count'], 'seedRowCount', TEST_DATA_MAX_SEED_ROWS),
    dialect: options.dialect
  })
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/test-data/package/generate`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(req)
  })
  const payload = await readJsonResponse(response)
  const result = sanitizeSecretValue(unwrapResponse(payload))
  io.writeOut(format === 'json'
    ? `${JSON.stringify(result, null, 2)}\n`
    : formatStandardTestDataPackageText(result))
  return 0
}

async function fetchTableStandards({ server, apiToken, fetchFn, projectId, templateId, businessObject }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'templateId', templateId)
  appendOptionalParam(params, 'businessObject', businessObject)
  const response = await fetchFn(`${server}/api/table-standards?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function runFieldKnowledge(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand === 'list' || !subcommand || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'query',
      'status',
      'field-id',
      'fieldId',
      'limit',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`field-knowledge list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'field-knowledge list')
    const projectId = parseProjectId(options.project ?? config.projectId)
    const fieldId = optionalPositiveInteger(options.fieldId ?? options['field-id'], 'field id')
    const result = await fetchFieldKnowledgeCards({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId,
      query: options.query,
      status: options.status,
      fieldId,
      limit: parseLimit(options.limit, 20)
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, [
      'project',
      'field-id',
      'fieldId',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 1) {
      throw new Error(`field-knowledge show 只接受一个 fieldId，收到: ${positional.slice(1).join(', ')}`)
    }
    if (positional[0] && (options.fieldId !== undefined || options['field-id'] !== undefined)) {
      throw new Error('field-knowledge show 的 fieldId 请使用位置参数或 --field-id 之一，不要同时传入')
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'field-knowledge show')
    const projectId = parseProjectId(options.project ?? config.projectId)
    const fieldId = parsePositiveInteger(positional[0] ?? options.fieldId ?? options['field-id'], 'field id')
    const result = await fetchFieldKnowledgeCard({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId,
      fieldId
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  throw new Error(`未知 field-knowledge 子命令: ${subcommand}。支持: list, show`)
}

async function runFieldSemantics(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand === 'list' || !subcommand || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'field-id',
      'fieldId',
      'rule-type',
      'ruleType',
      'query',
      'limit',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`field-semantics list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'field-semantics list')
    const projectId = parseProjectId(options.project ?? config.projectId)
    const result = await fetchFieldSemanticRules({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId,
      fieldId: optionalPositiveInteger(options.fieldId ?? options['field-id'], 'field id'),
      ruleType: options.ruleType ?? options['rule-type'],
      query: options.query,
      limit: options.limit === undefined ? undefined : parseLimit(options.limit, 20)
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, ['format', 'server', 'dataspec-token'])
    const id = positional[0]
    if (!id) {
      throw new Error('field-semantics show 需要提供 rule id')
    }
    if (positional.length > 1) {
      throw new Error(`field-semantics show 只接受一个 rule id，收到: ${positional.slice(1).join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'field-semantics show')
    const result = await fetchFieldSemanticRule({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      id: parsePositiveInteger(id, 'rule id')
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  throw new Error(`未知 field-semantics 子命令: ${subcommand}。支持: list, show`)
}

async function runMetricDefinitions(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand === 'list' || !subcommand || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'query',
      'status',
      'field-id',
      'fieldId',
      'metric-key',
      'metricKey',
      'limit',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`metric-definitions list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'metric-definitions list')
    const projectId = parseProjectId(options.project ?? config.projectId)
    const result = await fetchMetricDefinitions({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      projectId,
      query: options.query,
      status: options.status,
      fieldId: optionalPositiveInteger(options.fieldId ?? options['field-id'], 'field id'),
      metricKey: options.metricKey ?? options['metric-key'],
      limit: options.limit === undefined ? undefined : parseLimit(options.limit, 20)
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, ['format', 'server', 'dataspec-token'])
    const id = positional[0]
    if (!id) {
      throw new Error('metric-definitions show 需要提供 metric definition id')
    }
    if (positional.length > 1) {
      throw new Error(`metric-definitions show 只接受一个 metric definition id，收到: ${positional.slice(1).join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    assertJsonFormat(options.format, 'metric-definitions show')
    const result = await fetchMetricDefinition({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      id: parsePositiveInteger(id, 'metric definition id')
    })
    io.writeOut(`${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`)
    return 0
  }

  throw new Error(`未知 metric-definitions 子命令: ${subcommand}。支持: list, show`)
}

function assertJsonFormat(format, commandLabel) {
  const resolved = format ?? 'json'
  if (resolved !== 'json') {
    throw new Error(`${commandLabel} 当前仅支持 --format json`)
  }
}

function optionalPositiveInteger(value, label) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return parsePositiveInteger(value, label)
}

async function fetchFieldKnowledgeCards({ server, apiToken, fetchFn, projectId, query, status, fieldId, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'status', status)
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'limit', limit)
  const response = await fetchFn(`${server}/api/field-knowledge-cards?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function fetchFieldKnowledgeCard({ server, apiToken, fetchFn, projectId, fieldId }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  const response = await fetchFn(`${server}/api/field-knowledge-cards/${encodeURIComponent(fieldId)}?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function fetchFieldSemanticRules({ server, apiToken, fetchFn, projectId, fieldId, ruleType, query, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'ruleType', ruleType)
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'limit', limit)
  const response = await fetchFn(`${server}/api/field-semantics?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function fetchFieldSemanticRule({ server, apiToken, fetchFn, id }) {
  const response = await fetchFn(`${server}/api/field-semantics/${encodeURIComponent(id)}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function fetchMetricDefinitions({ server, apiToken, fetchFn, projectId, query, status, fieldId, metricKey, limit }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'query', query)
  appendOptionalParam(params, 'status', status)
  appendOptionalParam(params, 'fieldId', fieldId)
  appendOptionalParam(params, 'metricKey', metricKey)
  appendOptionalParam(params, 'limit', limit)
  const response = await fetchFn(`${server}/api/metric-definitions?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function fetchMetricDefinition({ server, apiToken, fetchFn, id }) {
  const response = await fetchFn(`${server}/api/metric-definitions/${encodeURIComponent(id)}`, {
    headers: dataSpecHeaders(apiToken)
  })
  const payload = await readJsonResponse(response)
  return unwrapResponse(payload)
}

async function runContractImport(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'preview') {
    throw new Error('contract-import 支持子命令: preview')
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'source-kind',
    'sourceKind',
    'input',
    'max-candidates',
    'maxCandidates',
    'format',
    'server',
    'dataspec-token'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`contract-import preview 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const sourceKind = normalizeContractSourceKind(options.sourceKind ?? options['source-kind'])
  const inputOption = options.input
  if (!inputOption) {
    throw new Error('contract-import preview 需要提供 --input <path>')
  }
  const format = options.format ?? 'json'
  if (format !== 'json' && format !== 'text') {
    throw new Error('contract-import preview 仅支持 --format text|json')
  }
  const maxCandidatesOption = options.maxCandidates ?? options['max-candidates']
  const maxCandidates = maxCandidatesOption === undefined
    ? undefined
    : parsePositiveInteger(maxCandidatesOption, 'maxCandidates')
  const cwd = cliCwd(io)
  const inputPath = path.resolve(cwd, inputOption)
  let contractContent
  try {
    contractContent = await readFile(inputPath, 'utf8')
  } catch {
    throw new Error(`contract-import preview 输入文件不可读: ${sanitizeSecretText(inputOption)}`)
  }

  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const body = removeUndefinedValues({
    projectId,
    sourceKind,
    sourcePath: formatInputSourcePath(inputPath, cwd),
    contractContent,
    maxCandidates
  })
  const response = await fetchFn(`${server}/api/contract-import/preview`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(format === 'json'
    ? `${JSON.stringify(result, null, 2)}\n`
    : formatContractImportPreviewText(result))
  return 0
}

async function runSchemaPlan(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, [
    'project',
    'database-type',
    'host',
    'port',
    'database',
    'database-name',
    'schema',
    'username',
    'password',
    'password-env',
    'table',
    'metadata-cache-mode',
    'format',
    'server',
    'dataspec-token'
  ], [], ['table'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`schema-plan 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('schema-plan 当前仅支持 --format json')
  }
  const databaseName = options.database ?? options['database-name']
  const tableNames = Array.isArray(options.table) ? options.table : options.table ? [options.table] : []
  if (tableNames.length === 0) {
    throw new Error('schema-plan 需要至少提供一个 --table <name>')
  }
  const req = {
    projectId,
    databaseType: requiredOption(options['database-type'], 'database-type'),
    host: requiredOption(options.host, 'host'),
    port: options.port === undefined ? undefined : parsePositiveInteger(options.port, 'database port'),
    databaseName: requiredOption(databaseName, 'database'),
    schemaName: options.schema,
    username: requiredOption(options.username, 'username'),
    password: resolveDatabasePassword(options),
    tableNames
  }
  if (options['metadata-cache-mode'] !== undefined) {
    req.metadataCacheMode = optionalMetadataCacheMode(options['metadata-cache-mode'], 'schema-plan')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/reverse-import/database/schema-plan`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(removeUndefinedValues(req))
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

async function runCommentPlan(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'preview') {
    throw new Error(`comment-plan 仅支持 preview 子命令: ${subcommand ?? ''}`.trim())
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'database-type',
    'host',
    'port',
    'database',
    'database-name',
    'schema',
    'username',
    'password',
    'password-env',
    'table',
    'metadata-cache-mode',
    'format',
    'server',
    'dataspec-token'
  ], [], ['table'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`comment-plan preview 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const format = options.format ?? 'json'
  if (format !== 'json' && format !== 'text') {
    throw new Error('comment-plan preview 仅支持 --format json|text')
  }
  const databaseName = options.database ?? options['database-name']
  const tableNames = Array.isArray(options.table) ? options.table : options.table ? [options.table] : []
  if (tableNames.length === 0) {
    throw new Error('comment-plan preview 需要至少提供一个 --table <name>')
  }
  const req = {
    projectId,
    databaseType: requiredOption(options['database-type'], 'database-type', 'comment-plan preview'),
    host: requiredOption(options.host, 'host', 'comment-plan preview'),
    port: options.port === undefined ? undefined : parsePositiveInteger(options.port, 'database port'),
    databaseName: requiredOption(databaseName, 'database', 'comment-plan preview'),
    schemaName: options.schema,
    username: requiredOption(options.username, 'username', 'comment-plan preview'),
    password: resolveDatabasePassword(options, 'comment-plan preview'),
    tableNames
  }
  if (options['metadata-cache-mode'] !== undefined) {
    req.metadataCacheMode = optionalMetadataCacheMode(options['metadata-cache-mode'], 'comment-plan preview')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const response = await fetchFn(`${server}/api/reverse-import/database/comment-plan`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(removeUndefinedValues(req))
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(format === 'json'
    ? `${JSON.stringify(sanitizeSecretValue(result), null, 2)}\n`
    : formatCommentPlanPreviewText(result))
  return 0
}

async function runDoctor(args, io, fetchFn) {
  const { positional, options } = parseArgs(
    args,
    ['project', 'format', 'server', 'dataspec-token', 'profile', 'task-type', 'taskType'],
    ['check-openapi']
  )
  if (positional.length > 0) {
    throw new Error(`doctor 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const format = options.format ?? 'text'
  if (format !== 'text' && format !== 'json') {
    throw new Error('doctor 当前仅支持 --format text 或 json')
  }
  const result = await buildDoctorResult({ config, options, io, fetchFn })
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatDoctorText(result))
  }
  return result.ok ? 0 : 1
}

async function runCompat(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'check' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'check' ? rest : args, ['format', 'server', 'dataspec-token'])
    if (positional.length > 0) {
      throw new Error(`compat check 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json' && format !== 'text') {
      throw new Error('compat check 当前仅支持 --format text 或 json')
    }
    const server = normalizeServer(options.server ?? config.server)
    const apiToken = resolveDataSpecToken(options, config)
    try {
      const payload = await fetchVersionCompatibility({
        server,
        apiToken,
        fetchFn,
        client: 'cli',
        clientVersion: CLI_VERSION
      })
      const result = {
        ...payload,
        localCliVersion: CLI_VERSION,
        server
      }
      if (format === 'json') {
        io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
      } else {
        io.writeOut(formatCompatibilityText(result))
      }
      return result.compatibility?.compatible === false ? 1 : 0
    } catch (error) {
      const diagnostic = normalizeVersionCompatibilityFetchError(error)
      const result = {
        kind: 'dataspec.version-compatibility-check',
        ok: false,
        localCliVersion: CLI_VERSION,
        server,
        diagnostic
      }
      if (format === 'json') {
        io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
      } else {
        io.writeErr(formatCompatibilityErrorText(result))
      }
      return 2
    }
  }
  throw new Error(`未知 compat 子命令: ${subcommand}。支持: check`)
}

async function runProfile(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'list' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'format',
      'server',
      'dataspec-token',
      'profile',
      'task-type',
      'taskType'
    ])
    if (positional.length > 0) {
      throw new Error(`profile list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const projectId = parseProjectId(options.project ?? config.projectId)
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('profile list 仅支持 --format text|json')
    }
    const server = normalizeServer(options.server ?? config.server)
    const apiToken = resolveDataSpecToken(options, config)
    const catalog = await fetchProfileCatalog({
      server,
      projectId,
      apiToken,
      fetchFn,
      selection: resolveProfileSelection(options, config)
    })
    if (format === 'json') {
      io.writeOut(`${JSON.stringify(catalog, null, 2)}\n`)
    } else {
      io.writeOut(formatProfileCatalogText(catalog))
    }
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, [
      'project',
      'format',
      'server',
      'dataspec-token',
      'profile',
      'task-type',
      'taskType'
    ])
    const config = loadDataSpecConfig(cliCwd(io))
    const selection = resolveProfileSelection(options, config)
    const profileKey = positional[0] ?? selection.profileId ?? selection.taskType
    if (!profileKey) {
      throw new Error('profile show 需要提供 profile id、taskType 或配置 aiProfile/taskType')
    }
    if (positional.length > 1) {
      throw new Error(`profile show 只接受一个 profile 或 taskType，收到: ${positional.slice(1).join(', ')}`)
    }
    const projectId = parseProjectId(options.project ?? config.projectId)
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('profile show 仅支持 --format text|json')
    }
    const server = normalizeServer(options.server ?? config.server)
    const apiToken = resolveDataSpecToken(options, config)
    const detail = await fetchProfileDetail({ server, projectId, apiToken, fetchFn, profileKey })
    if (!detail.profile) {
      throw new Error(`未知 AI profile 或 taskType: ${profileKey}。支持的 profile: ${(detail.supportedProfileIds ?? []).join(', ')}`)
    }
    if (format === 'json') {
      io.writeOut(`${JSON.stringify(detail, null, 2)}\n`)
    } else {
      io.writeOut(formatProfileDetailText(detail))
    }
    return 0
  }
  throw new Error(`未知 profile 子命令: ${subcommand}。支持: list, show`)
}

async function runContract(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'list' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`contract list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('contract list 仅支持 --format text|json')
    }
    const catalog = await fetchContractCatalog({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn
    })
    io.writeOut(format === 'json'
      ? `${JSON.stringify(catalog, null, 2)}\n`
      : formatContractCatalogText(catalog))
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, ['project', 'format', 'server', 'dataspec-token'])
    const contractId = positional[0]
    if (!contractId) {
      throw new Error('contract show 需要提供 contractId')
    }
    if (positional.length > 1) {
      throw new Error(`contract show 只接受一个 contractId，收到: ${positional.slice(1).join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('contract show 仅支持 --format text|json')
    }
    const detail = await fetchContractDetail({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      contractId
    })
    io.writeOut(format === 'json'
      ? `${JSON.stringify(detail, null, 2)}\n`
      : formatContractDetailText(detail))
    return 0
  }

  if (subcommand === 'check') {
    const { positional, options } = parseArgs(rest, ['project', 'format', 'server', 'dataspec-token'])
    if (positional.length > 0) {
      throw new Error(`contract check 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('contract check 仅支持 --format text|json')
    }
    const catalog = await fetchContractCatalog({
      server: normalizeServer(options.server ?? config.server),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn
    })
    const result = checkContractRegistry(catalog)
    io.writeOut(format === 'json'
      ? `${JSON.stringify(result, null, 2)}\n`
      : formatContractCheckText(result))
    return result.ok ? 0 : 2
  }

  throw new Error(`未知 contract 子命令: ${subcommand}。支持: list, show, check`)
}

async function runCapability(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'list' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'format',
      'server',
      'dataspec-token'
    ])
    if (positional.length > 0) {
      throw new Error(`capability list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('capability list 仅支持 --format text|json')
    }
    const catalog = await fetchCapabilityCatalog({
      server: normalizeServer(options.server ?? config.server),
      projectId: optionalProjectId(options.project ?? config.projectId),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn
    })
    io.writeOut(format === 'json'
      ? `${JSON.stringify(catalog, null, 2)}\n`
      : formatCapabilityCatalogText(catalog))
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, ['project', 'format', 'server', 'dataspec-token'])
    const capabilityId = positional[0]
    if (!capabilityId) {
      throw new Error('capability show 需要提供 capability id')
    }
    if (positional.length > 1) {
      throw new Error(`capability show 只接受一个 capability id，收到: ${positional.slice(1).join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('capability show 仅支持 --format text|json')
    }
    const capability = await fetchCapabilityDetail({
      server: normalizeServer(options.server ?? config.server),
      projectId: optionalProjectId(options.project ?? config.projectId),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      capabilityId
    })
    io.writeOut(format === 'json'
      ? `${JSON.stringify(capability, null, 2)}\n`
      : formatCapabilityDetailText(capability))
    return 0
  }

  if (subcommand === 'check') {
    const { positional, options } = parseArgs(rest, ['project', 'format', 'server', 'dataspec-token'])
    if (positional.length > 0) {
      throw new Error(`capability check 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (!['json', 'text'].includes(format)) {
      throw new Error('capability check 仅支持 --format text|json')
    }
    const catalog = await fetchCapabilityCatalog({
      server: normalizeServer(options.server ?? config.server),
      projectId: optionalProjectId(options.project ?? config.projectId),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn
    })
    const result = checkCapabilityCatalog(catalog)
    io.writeOut(format === 'json'
      ? `${JSON.stringify(result, null, 2)}\n`
      : formatCapabilityCheckText(result))
    return result.ok ? 0 : 2
  }

  throw new Error(`未知 capability 子命令: ${subcommand}。支持: list, show, check`)
}

async function runEvidence(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'export') {
    throw new Error('未知 evidence 子命令。支持: export')
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'source-type',
    'sourceType',
    'source-id',
    'sourceId',
    'source-title',
    'sourceTitle',
    'payload',
    'format',
    'output',
    'server',
    'dataspec-token'
  ])
  if (positional.length > 0) {
    throw new Error(`evidence export 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const format = options.format ?? 'json'
  if (!['json', 'zip'].includes(format)) {
    throw new Error('evidence export 仅支持 --format json|zip')
  }
  if (format === 'zip' && !options.output) {
    throw new Error('evidence export --format zip 需要提供 --output <zip>')
  }
  const output = options.output ? resolveOutputInsideCwd(options.output, cliCwd(io)) : null
  const req = await buildEvidenceRequest(options, config)
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  if (format === 'zip') {
    const response = await fetchFn(`${server}/api/evidence-packages/download`, {
      method: 'POST',
      headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify(req)
    })
    if (!response.ok) {
      throw toDataSpecCliError(await readResponseJson(response), response.status)
    }
    await mkdir(path.dirname(output), { recursive: true })
    await writeFile(output, Buffer.from(await response.arrayBuffer()))
    io.writeOut(`已导出 evidence package: ${formatOutputPath(output)}\n`)
    return 0
  }
  const result = await fetchEvidencePackage({ server, apiToken, fetchFn, req })
  if (output) {
    await mkdir(path.dirname(output), { recursive: true })
    await writeFile(output, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    io.writeOut(`已导出 evidence package: ${formatOutputPath(output)}\n`)
  } else {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  }
  return 0
}

async function runTask(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (!subcommand || subcommand === 'list' || subcommand.startsWith('--')) {
    const { positional, options } = parseArgs(subcommand === 'list' ? rest : args, [
      'project',
      'format',
      'server',
      'dataspec-token',
      'status',
      'task-type',
      'taskType',
      'current',
      'size'
    ])
    if (positional.length > 0) {
      throw new Error(`task list 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json') {
      throw new Error('task list 仅支持 --format json')
    }
    const result = await fetchTaskRuns({
      server: normalizeServer(options.server ?? config.server),
      projectId: parseProjectId(options.project ?? config.projectId),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      status: normalizeOptionalCliText(options.status),
      taskType: normalizeOptionalCliText(options.taskType ?? options['task-type']),
      current: parseLimit(options.current, 1),
      size: parseLimit(options.size, 10)
    })
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    return 0
  }

  if (subcommand === 'failures') {
    const { positional, options } = parseArgs(rest, [
      'project',
      'format',
      'server',
      'dataspec-token',
      'limit'
    ])
    if (positional.length > 0) {
      throw new Error(`task failures 不接受位置参数: ${positional.join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json') {
      throw new Error('task failures 仅支持 --format json')
    }
    const result = await fetchRecentTaskFailures({
      server: normalizeServer(options.server ?? config.server),
      projectId: parseProjectId(options.project ?? config.projectId),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn,
      limit: parseLimit(options.limit, 10)
    })
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    return 0
  }

  if (subcommand === 'show') {
    const { positional, options } = parseArgs(rest, [
      'project',
      'format',
      'server',
      'dataspec-token'
    ])
    const taskRunId = positional[0]
    if (!taskRunId) {
      throw new Error('task show 需要提供 task run id')
    }
    if (positional.length > 1) {
      throw new Error(`task show 只接受一个 task run id，收到: ${positional.slice(1).join(', ')}`)
    }
    const config = loadDataSpecConfig(cliCwd(io))
    const format = options.format ?? 'json'
    if (format !== 'json') {
      throw new Error('task show 仅支持 --format json')
    }
    const result = await fetchTaskRunDetail({
      server: normalizeServer(options.server ?? config.server),
      projectId: parseProjectId(options.project ?? config.projectId),
      taskRunId: parsePositiveInteger(taskRunId, 'task run id'),
      apiToken: resolveDataSpecToken(options, config),
      fetchFn
    })
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
    return 0
  }

  throw new Error(`未知 task 子命令: ${subcommand}。支持: list, failures, show`)
}

async function runBootstrap(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'format', 'server', 'dataspec-token'])
  if (positional.length > 0) {
    throw new Error(`bootstrap 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const format = options.format ?? 'json'
  if (!['json', 'text'].includes(format)) {
    throw new Error('bootstrap 仅支持 --format text|json')
  }
  const server = normalizeServer(options.server ?? config.server)
  const projectId = parseOptionalProjectId(options.project ?? config.projectId)
  const apiToken = resolveDataSpecToken(options, config)
  const bootstrap = await fetchSessionBootstrapWithFallback({
    server,
    projectId,
    apiToken,
    fetchFn
  })
  io.writeOut(format === 'json'
    ? `${JSON.stringify(bootstrap, null, 2)}\n`
    : formatSessionBootstrapText(bootstrap))
  return bootstrap.status === 'READY' ? 0 : 1
}

async function runQualityGate(args, io, fetchFn) {
  const [subcommand, ...rest] = args
  if (subcommand !== 'check') {
    throw new Error(`未知 quality-gate 子命令: ${subcommand ?? '(empty)'}。支持: check`)
  }
  const { positional, options } = parseArgs(rest, [
    'project',
    'format',
    'server',
    'dataspec-token'
  ])
  if (positional.length > 0) {
    throw new Error(`quality-gate check 不接受位置参数: ${positional.join(', ')}`)
  }
  const config = loadDataSpecConfig(cliCwd(io))
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('quality-gate check 仅支持 --format json')
  }
  const result = await fetchQualityGateEvaluation({
    server: normalizeServer(options.server ?? config.server),
    projectId: parseProjectId(options.project ?? config.projectId),
    apiToken: resolveDataSpecToken(options, config),
    fetchFn
  })
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return result?.status === 'FAIL' ? 1 : 0
}

async function runInit(args, io, fetchFn) {
  const { positional, options } = parseArgs(
    args,
    ['project', 'server', 'default-path', 'format', 'dataspec-token'],
    ['force', 'with-agents'],
    ['default-path']
  )
  if (positional.length > 0) {
    throw new Error(`init 不接受位置参数: ${positional.join(', ')}`)
  }
  const format = options.format ?? 'text'
  if (format !== 'text' && format !== 'json') {
    throw new Error('init 当前仅支持 --format text 或 json')
  }

  const existingConfig = loadDataSpecConfig(cliCwd(io))
  const rootDir = existingConfig.rootDir
  const projectId = parseProjectId(options.project ?? existingConfig.projectId)
  const server = normalizeServer(options.server ?? existingConfig.server ?? DEFAULT_SERVER)
  const defaultPaths = resolveInitDefaultPaths(options['default-path'], existingConfig.defaultPaths)
  const force = Boolean(options.force)
  const configPath = path.join(rootDir, '.dataspec', 'config.json')
  const readmePath = path.join(rootDir, '.dataspec', 'README.md')

  const fileResults = []
  fileResults.push(await writeInitFile(configPath, renderInitConfig(projectId, server, defaultPaths), force))
  fileResults.push(await writeInitFile(readmePath, renderInitReadme({ projectId, server, defaultPaths }), force))
  if (options['with-agents']) {
    fileResults.push(await writeAgentsFragment(rootDir, renderAgentsFragment({ projectId, server, defaultPaths }), force))
  }

  const initializedConfig = loadDataSpecConfig(rootDir)
  const doctor = await buildDoctorResult({
    config: initializedConfig,
    options: {
      project: String(projectId),
      server,
      'dataspec-token': options['dataspec-token']
    },
    io,
    fetchFn
  })
  const result = {
    ok: doctor.ok,
    rootDir,
    configPath,
    writtenFiles: fileResults.filter((item) => item.action === 'written').map((item) => item.path),
    skippedFiles: fileResults.filter((item) => item.action === 'skipped').map((item) => item.path),
    defaultPaths,
    doctor
  }

  if (format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatInitText(result))
  }
  return doctor.ok ? 0 : 1
}

function parseArgs(args, allowedOptions, flagOptions = [], repeatableOptions = []) {
  const positional = []
  const options = {}
  const allowedOptionSet = new Set(allowedOptions)
  const flagOptionSet = new Set(flagOptions)
  const repeatableOptionSet = new Set(repeatableOptions)
  for (let i = 0; i < args.length; i += 1) {
    const token = args[i]
    if (!token.startsWith('--')) {
      positional.push(token)
      continue
    }
    const name = token.slice(2)
    if (!allowedOptionSet.has(name) && !flagOptionSet.has(name)) {
      throw new Error(`未知参数: ${token}`)
    }
    if (flagOptionSet.has(name)) {
      options[name] = true
      continue
    }
    const value = args[i + 1]
    if (!value || value.startsWith('--')) {
      throw new Error(`缺少参数值: ${token}`)
    }
    if (options[name] !== undefined && !repeatableOptionSet.has(name)) {
      throw new Error(`参数不可重复: --${name}`)
    }
    if (repeatableOptionSet.has(name)) {
      options[name] = [...(options[name] ?? []), value]
    } else {
      options[name] = value
    }
    i += 1
  }
  return { positional, options }
}

function optionValues(value) {
  if (value === undefined || value === null) {
    return []
  }
  return Array.isArray(value) ? value : [value]
}

function requiredOption(value, name, commandName = 'schema-plan') {
  if (value === undefined || value === null || String(value).trim() === '') {
    throw new Error(`${commandName} 需要提供 --${name}`)
  }
  return value
}

function optionalMetadataCacheMode(value, commandName) {
  if (value === undefined || value === null || String(value).trim() === '') {
    return undefined
  }
  const normalized = String(value).trim().toUpperCase()
  if (!['AUTO', 'REFRESH', 'BYPASS'].includes(normalized)) {
    throw new Error(`${commandName} 的 --metadata-cache-mode 仅支持 AUTO、REFRESH 或 BYPASS`)
  }
  return normalized
}

function resolveDatabasePassword(options, commandName = 'schema-plan') {
  if (options.password !== undefined && options['password-env'] !== undefined) {
    throw new Error(`${commandName} 的 --password 和 --password-env 不能同时使用`)
  }
  if (options['password-env'] !== undefined) {
    const envName = options['password-env']
    const value = process.env[envName]
    if (value === undefined) {
      throw new Error(`环境变量 ${envName} 未设置`)
    }
    return value
  }
  return options.password
}

function removeUndefinedValues(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined))
}

function buildAiContextPackageUrl(server, projectId, options) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'profileId', options.profileId)
  appendOptionalParam(params, 'taskType', options.taskType)
  appendOptionalParam(params, 'scope', options.scope)
  appendOptionalParam(params, 'query', options.query)
  appendOptionalParam(params, 'status', options.status)
  const snapshotId = options.snapshotId ?? options['snapshot-id']
  if (snapshotId !== undefined && snapshotId !== null && String(snapshotId).trim() !== '') {
    params.set('snapshotId', String(parsePositiveInteger(snapshotId, 'snapshot id')))
  }
  appendOptionalParam(params, 'snapshotVersion', options.snapshotVersion ?? options['snapshot-version'])
  if (options.limit !== undefined) {
    params.set('limit', String(parseLimit(options.limit)))
  }
  return `${server}/api/ai-context/package/download?${params.toString()}`
}

async function writeAiContextCache({ bytes, rootDir, projectId, server, options, ttlDays }) {
  const cacheDir = path.resolve(rootDir, CONTEXT_CACHE_DIR)
  ensureContextCachePath(rootDir, cacheDir)
  const zipEntries = readZipEntries(bytes)
  const normalizedEntries = normalizeZipEntries(zipEntries)
  const extracted = []
  const manifest = parseAiContextManifest(normalizedEntries)

  await rm(cacheDir, { recursive: true, force: true })
  await mkdir(cacheDir, { recursive: true })

  for (const { entry, relativePath } of normalizedEntries) {
    if (!relativePath || entry.directory) {
      continue
    }
    const targetPath = resolveCacheTarget(cacheDir, relativePath)
    await mkdir(path.dirname(targetPath), { recursive: true })
    await writeFile(targetPath, entry.content)
    extracted.push(toPosixPath(relativePath))
  }

  const now = new Date()
  const metadata = sanitizeMetadata({
    kind: 'dataspec-ai-context-cache',
    schemaVersion: 1,
    projectId,
    server: safeServerForMetadata(server),
    exportedAt: now.toISOString(),
    expiresAt: new Date(now.getTime() + ttlDays * MS_PER_DAY).toISOString(),
    ttlDays,
    exportOptions: aiContextExportOptionsForMetadata(options),
    contentHash: sha256Hex(bytes),
    standard: manifest?.standard ?? null,
    sourcePackage: manifest
      ? {
          schemaVersion: manifest.schemaVersion,
          kind: manifest.kind,
          generatedAt: manifest.generatedAt,
          contextScope: manifest.contextScope
        }
      : null,
    files: [...extracted, CACHE_METADATA_FILE].sort()
  })
  await writeFile(path.join(cacheDir, CACHE_METADATA_FILE), `${JSON.stringify(metadata, null, 2)}\n`, 'utf8')
  return { cacheDir, metadata }
}

function readZipEntries(bytes) {
  const buffer = Buffer.from(bytes)
  const eocdOffset = findEndOfCentralDirectory(buffer)
  const totalEntries = buffer.readUInt16LE(eocdOffset + 10)
  const centralDirectoryOffset = buffer.readUInt32LE(eocdOffset + 16)
  assertZipRange(buffer, centralDirectoryOffset, 0, '中央目录偏移')
  const entries = []
  let offset = centralDirectoryOffset
  for (let index = 0; index < totalEntries; index += 1) {
    assertZipRange(buffer, offset, 46, '中央目录条目')
    if (buffer.readUInt32LE(offset) !== ZIP_CENTRAL_DIRECTORY_HEADER) {
      throw new Error('AI Context zip 中央目录格式无效')
    }
    const flags = buffer.readUInt16LE(offset + 8)
    const method = buffer.readUInt16LE(offset + 10)
    const compressedSize = buffer.readUInt32LE(offset + 20)
    const nameLength = buffer.readUInt16LE(offset + 28)
    const extraLength = buffer.readUInt16LE(offset + 30)
    const commentLength = buffer.readUInt16LE(offset + 32)
    const localHeaderOffset = buffer.readUInt32LE(offset + 42)
    assertZipRange(buffer, offset + 46, nameLength + extraLength + commentLength, '中央目录条目名称')
    const name = buffer.toString('utf8', offset + 46, offset + 46 + nameLength)
    if ((flags & ZIP_FLAG_ENCRYPTED) !== 0) {
      throw new Error(`AI Context zip 包含加密条目，无法缓存: ${name}`)
    }
    entries.push(readZipEntryContent(buffer, { name, method, compressedSize, localHeaderOffset }))
    offset += 46 + nameLength + extraLength + commentLength
  }
  return entries
}

function readZipEntryContent(buffer, entry) {
  assertZipRange(buffer, entry.localHeaderOffset, 30, `本地文件头: ${entry.name}`)
  if (buffer.readUInt32LE(entry.localHeaderOffset) !== ZIP_LOCAL_FILE_HEADER) {
    throw new Error(`AI Context zip 本地文件头格式无效: ${entry.name}`)
  }
  const localNameLength = buffer.readUInt16LE(entry.localHeaderOffset + 26)
  const localExtraLength = buffer.readUInt16LE(entry.localHeaderOffset + 28)
  const dataStart = entry.localHeaderOffset + 30 + localNameLength + localExtraLength
  assertZipRange(buffer, dataStart, entry.compressedSize, `文件内容: ${entry.name}`)
  const compressed = buffer.subarray(dataStart, dataStart + entry.compressedSize)
  const directory = entry.name.endsWith('/')
  if (directory) {
    return { name: entry.name, directory, content: Buffer.alloc(0) }
  }
  if (entry.method === ZIP_METHOD_STORED) {
    return { name: entry.name, directory, content: Buffer.from(compressed) }
  }
  if (entry.method === ZIP_METHOD_DEFLATED) {
    return { name: entry.name, directory, content: inflateRawSync(compressed) }
  }
  throw new Error(`AI Context zip 使用不支持的压缩方法 ${entry.method}: ${entry.name}`)
}

function normalizeZipEntries(entries) {
  return entries.map((entry) => ({
    entry,
    relativePath: normalizeCacheEntryPath(entry.name)
  }))
}

function findEndOfCentralDirectory(buffer) {
  const minOffset = Math.max(0, buffer.length - 0xffff - 22)
  for (let offset = buffer.length - 22; offset >= minOffset; offset -= 1) {
    if (buffer.readUInt32LE(offset) === ZIP_END_OF_CENTRAL_DIRECTORY) {
      return offset
    }
  }
  throw new Error('AI Context zip 缺少中央目录结束标记')
}

function assertZipRange(buffer, offset, length, label) {
  if (offset < 0 || length < 0 || offset + length > buffer.length) {
    throw new Error(`AI Context zip ${label} 超出文件范围`)
  }
}

function parseAiContextManifest(normalizedEntries) {
  const manifestEntry = normalizedEntries.find(({ relativePath }) => relativePath === 'manifest.json')?.entry
  if (!manifestEntry) {
    return null
  }
  try {
    return JSON.parse(manifestEntry.content.toString('utf8'))
  } catch (error) {
    return {
      parseError: error.message
    }
  }
}

function normalizeCacheEntryPath(entryName) {
  const normalized = entryName.replaceAll('\\', '/')
  if (normalized.startsWith('/') || /^[A-Za-z]:/.test(normalized)) {
    throw new Error(`AI Context zip 包含不安全的绝对路径: ${entryName}`)
  }
  const parts = normalized.split('/').filter(Boolean)
  if (parts.includes('..')) {
    throw new Error(`AI Context zip 包含越界路径: ${entryName}`)
  }
  const stripped = parts[0] === '.dataspec' ? parts.slice(1) : parts
  if (stripped.length === 0) {
    return ''
  }
  return path.join(...stripped)
}

function ensureContextCachePath(rootDir, cacheDir) {
  const expected = path.resolve(rootDir, CONTEXT_CACHE_DIR)
  if (cacheDir !== expected) {
    throw new Error(`非法 AI Context 缓存目录: ${cacheDir}`)
  }
}

function resolveCacheTarget(cacheDir, relativePath) {
  const targetPath = path.resolve(cacheDir, relativePath)
  if (!isPathInside(cacheDir, targetPath)) {
    throw new Error(`AI Context zip 条目写入越界: ${relativePath}`)
  }
  return targetPath
}

function isPathInside(parentDir, targetPath) {
  const relative = path.relative(parentDir, targetPath)
  return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative))
}

function aiContextExportOptionsForMetadata(options) {
  const snapshotId = options.snapshotId ?? options['snapshot-id']
  const snapshotVersion = options.snapshotVersion ?? options['snapshot-version']
  return sanitizeMetadata({
    scope: options.scope ?? null,
    query: options.query ?? null,
    status: options.status ?? null,
    limit: options.limit !== undefined ? parseLimit(options.limit) : null,
    profileId: options.profileId ?? null,
    taskType: options.taskType ?? null,
    snapshotId: snapshotId !== undefined ? parsePositiveInteger(snapshotId, 'snapshot id') : null,
    snapshotVersion: snapshotVersion ?? null
  })
}

function parseCacheTtlDays(value) {
  if (value === undefined || value === null || value === '') {
    return DEFAULT_CONTEXT_CACHE_TTL_DAYS
  }
  return parsePositiveInteger(value, 'cache ttl days')
}

function sha256Hex(bytes) {
  return createHash('sha256').update(bytes).digest('hex')
}

function toPosixPath(filePath) {
  return filePath.split(path.sep).join('/')
}

function safeServerForMetadata(server) {
  try {
    const url = new URL(server)
    url.username = ''
    url.password = ''
    url.search = ''
    url.hash = ''
    return redactSecrets(url.toString().replace(/\/+$/, ''))
  } catch (error) {
    return redactSecrets(server)
  }
}

function sanitizeMetadata(value) {
  if (typeof value === 'string') {
    return redactSecrets(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeMetadata(item))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, sanitizeMetadata(item)]))
  }
  return value
}

function redactSecrets(value) {
  return sanitizeSecretText(value)
}

function appendOptionalParam(params, key, value) {
  if (value !== undefined && value !== null && String(value).trim() !== '') {
    params.set(key, String(value).trim())
  }
}

async function buildDoctorResult({ config, options, io, fetchFn }) {
  const server = normalizeServer(options.server ?? config.server)
  const projectId = options.project || config.projectId ? parseProjectId(options.project ?? config.projectId) : undefined
  const apiToken = resolveDataSpecToken(options, config)
  const checks = []

  checks.push(buildConfigCheck(config))
  const apiDocsResult = await checkApiDocs(server, fetchFn, apiToken)
  checks.push(apiDocsResult.check)
  checks.push(await checkAuth(server, fetchFn, apiToken))
  checks.push(await checkProject(server, fetchFn, projectId, apiToken))
  checks.push(await checkVersionCompatibility({
    server,
    apiToken,
    fetchFn,
    serverReachable: apiDocsResult.check.status === 'pass'
  }))
  checks.push(await checkDefaultPaths(config))
  checks.push(await checkAiProfile({
    config,
    options,
    server,
    projectId,
    apiToken,
    fetchFn,
    serverReachable: apiDocsResult.check.status === 'pass'
  }))
  checks.push(await checkOpenapiStatus({
    server,
    apiDocsReachable: apiDocsResult.check.status === 'pass',
    checkDrift: Boolean(options['check-openapi']),
    runDriftCheck: io.checkOpenapiDrift
  }))
  checks.push(await checkContextCache({
    config,
    server,
    projectId,
    apiToken,
    fetchFn,
    serverReachable: apiDocsResult.check.status === 'pass'
  }))

  return {
    ok: checks.every((check) => check.status !== 'fail'),
    server,
    projectId: projectId ?? null,
    configPath: config.configPath,
    checks
  }
}

function buildConfigCheck(config) {
  if (config.configPath) {
    return passCheck('config', `已读取配置: ${config.configPath}`, {
      configPath: config.configPath,
      rootDir: config.rootDir
    })
  }
  return warnCheck('config', '未找到 .dataspec/config.json，将使用命令行参数和默认服务地址', {
    rootDir: config.rootDir
  })
}

async function checkApiDocs(server, fetchFn, apiToken) {
  try {
    const response = await fetchFn(`${server}/api-docs`, { headers: dataSpecHeaders(apiToken) })
    if (!response.ok) {
      return { check: failCheck('server', `DataSpec 服务不可用，/api-docs 返回 HTTP ${response.status}`) }
    }
    return { check: passCheck('server', `DataSpec 服务可访问: ${server}`) }
  } catch (error) {
    return { check: failCheck('server', `DataSpec 服务不可访问: ${error.message}`) }
  }
}

async function checkAuth(server, fetchFn, apiToken) {
  if (!apiToken) {
    return warnCheck('auth', '未提供 API token；安全模式关闭时可以忽略')
  }
  try {
    const response = await fetchFn(`${server}/api/auth/me`, {
      headers: dataSpecHeaders(apiToken)
    })
    const principal = unwrapResponse(await readJsonResponse(response))
    return passCheck('auth', `API token 有效，当前操作者: ${principal.operatorName ?? '未知'}`, principal)
  } catch (error) {
    return failCheck('auth', `API token 校验失败: ${error.message}`)
  }
}

async function checkProject(server, fetchFn, projectId, apiToken) {
  if (!projectId) {
    return failCheck('project', '缺少 projectId，请提供 --project <id> 或 .dataspec/config.json 的 projectId')
  }
  try {
    const response = await fetchFn(`${server}/api/projects/${encodeURIComponent(projectId)}`, {
      headers: dataSpecHeaders(apiToken)
    })
    const project = unwrapResponse(await readJsonResponse(response))
    return passCheck('project', `项目可访问: ${project.name ?? projectId}`, {
      id: project.id ?? projectId,
      name: project.name
    })
  } catch (error) {
    return failCheck('project', `项目不可访问: ${error.message}`)
  }
}

async function checkVersionCompatibility({ server, apiToken, fetchFn, serverReachable }) {
  const details = {
    localCliVersion: CLI_VERSION
  }
  if (!serverReachable) {
    return warnCheck('compatibility', 'DataSpec 服务不可用，跳过版本兼容检查', {
      ...details,
      status: 'UNKNOWN',
      nextActions: ['服务恢复后运行 dataspec compat check --format json。']
    })
  }
  try {
    const payload = await fetchVersionCompatibility({
      server,
      apiToken,
      fetchFn,
      client: 'cli',
      clientVersion: CLI_VERSION
    })
    const compatibility = payload.compatibility ?? {}
    const checkDetails = {
      ...details,
      serverVersion: payload.serverVersion ?? null,
      apiSchemaHash: payload.apiSchemaHash ?? null,
      minCliVersion: payload.minCliVersion ?? null,
      status: compatibility.status ?? 'UNKNOWN',
      compatible: compatibility.compatible ?? null,
      nextActions: compatibility.nextActions ?? payload.upgradeHints ?? []
    }
    if (compatibility.compatible === false) {
      return failCheck('compatibility', 'CLI/MCP 与服务端版本不兼容', checkDetails)
    }
    if (compatibility.status === 'UNKNOWN') {
      return warnCheck('compatibility', '服务端无法确认 CLI/MCP 版本兼容性', checkDetails)
    }
    return passCheck('compatibility', `版本兼容: server=${payload.serverVersion ?? '未知'}, minCli=${payload.minCliVersion ?? '未知'}`, checkDetails)
  } catch (error) {
    const diagnostic = normalizeVersionCompatibilityFetchError(error)
    return warnCheck('compatibility', `版本兼容检查不可用: ${diagnostic.message}`, {
      ...details,
      status: 'UNKNOWN',
      diagnostic,
      nextActions: [diagnostic.suggestedAction]
    })
  }
}

async function checkDefaultPaths(config) {
  if (config.defaultPaths.length === 0) {
    return warnCheck('defaultPaths', '未配置 defaultPaths；lint-files 未传路径时仍需手动提供')
  }
  const paths = resolveDefaultPaths(config)
  const missing = []
  for (const inputPath of paths) {
    try {
      await stat(inputPath)
    } catch (error) {
      missing.push(inputPath)
    }
  }
  if (missing.length > 0) {
    return failCheck('defaultPaths', `defaultPaths 存在不可访问路径: ${missing.join(', ')}`, {
      paths,
      missing
    })
  }
  return passCheck('defaultPaths', `defaultPaths 可访问: ${paths.join(', ')}`, { paths })
}

async function checkAiProfile({ config, options, server, projectId, apiToken, fetchFn, serverReachable }) {
  const selection = resolveProfileSelection(options, config)
  const profileKey = selection.profileId ?? selection.taskType
  const details = {
    profileId: selection.profileId ?? null,
    taskType: selection.taskType ?? null,
    source: profileKey ? 'config-or-options' : 'service-default'
  }
  if (!profileKey) {
    return passCheck('ai-profile', '未配置 AI profile；将使用服务端默认 create-table', details)
  }
  if (!serverReachable) {
    return warnCheck('ai-profile', '本地 AI profile 配置形状有效，但 DataSpec 服务不可用，无法远端校验', {
      ...details,
      remoteStatus: 'unavailable'
    })
  }
  try {
    const detail = await fetchProfileDetail({ server, projectId, apiToken, fetchFn, profileKey })
    if (!detail.profile) {
      return failCheck('ai-profile', `未知 AI profile 或 taskType: ${profileKey}`, {
        ...details,
        supportedProfileIds: detail.supportedProfileIds ?? [],
        supportedTaskTypes: detail.supportedTaskTypes ?? []
      })
    }
    return passCheck('ai-profile', `AI profile 可用: ${detail.profile.profileId}`, {
      ...details,
      profileId: detail.profile.profileId,
      taskType: detail.profile.taskType,
      recommendedCommand: detail.profile.recommendedCommands?.[0] ?? null,
      diagnostics: detail.diagnostics ?? []
    })
  } catch (error) {
    return failCheck('ai-profile', `AI profile 远端校验失败: ${error.message}`, details)
  }
}

async function fetchProfileCatalog({ server, projectId, apiToken, fetchFn, selection = {} }) {
  const params = new URLSearchParams()
  params.set('projectId', String(projectId))
  appendOptionalParam(params, 'profile', selection.profileId ?? selection.taskType)
  const response = await fetchFn(`${server}/api/ai-profiles?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchProfileDetail({ server, projectId, apiToken, fetchFn, profileKey }) {
  const params = new URLSearchParams()
  appendOptionalParam(params, 'projectId', projectId)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  const response = await fetchFn(`${server}/api/ai-profiles/${encodeURIComponent(profileKey)}${suffix}`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchVersionCompatibility({ server, apiToken, fetchFn, client, clientVersion }) {
  const params = new URLSearchParams()
  params.set('client', client)
  params.set('clientVersion', clientVersion)
  const response = await fetchFn(`${server}/api/capabilities/version?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchContractCatalog({ server, apiToken, fetchFn }) {
  const response = await fetchFn(`${server}/api/contracts`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchContractDetail({ server, apiToken, fetchFn, contractId }) {
  const response = await fetchFn(`${server}/api/contracts/${encodeURIComponent(contractId)}`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchCapabilityCatalog({ server, projectId, apiToken, fetchFn }) {
  try {
    const params = new URLSearchParams()
    appendOptionalParam(params, 'projectId', projectId)
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await fetchFn(`${server}/api/capabilities${suffix}`, {
      headers: dataSpecHeaders(apiToken)
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeCapabilityFetchError(error)
  }
}

async function fetchCapabilityDetail({ server, projectId, apiToken, fetchFn, capabilityId }) {
  try {
    const params = new URLSearchParams()
    appendOptionalParam(params, 'projectId', projectId)
    const suffix = params.toString() ? `?${params.toString()}` : ''
    const response = await fetchFn(`${server}/api/capabilities/${encodeURIComponent(capabilityId)}${suffix}`, {
      headers: dataSpecHeaders(apiToken)
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeCapabilityFetchError(error, { capabilityId })
  }
}

async function fetchEvidencePackage({ server, apiToken, fetchFn, req }) {
  const response = await fetchFn(`${server}/api/evidence-packages`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify(req)
  })
  return unwrapResponse(await readJsonResponse(response))
}

async function fetchTaskRuns({ server, projectId, apiToken, fetchFn, status, taskType, current, size }) {
  try {
    const params = new URLSearchParams()
    params.set('projectId', String(projectId))
    appendOptionalParam(params, 'status', status)
    appendOptionalParam(params, 'taskType', taskType)
    params.set('current', String(current))
    params.set('size', String(size))
    const response = await fetchFn(`${server}/api/ai-task-runs?${params.toString()}`, {
      headers: dataSpecHeaders(apiToken)
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeTaskRunFetchError(error)
  }
}

async function fetchRecentTaskFailures({ server, projectId, apiToken, fetchFn, limit }) {
  try {
    const params = new URLSearchParams()
    params.set('projectId', String(projectId))
    params.set('limit', String(limit))
    const response = await fetchFn(`${server}/api/ai-task-runs/recent-failures?${params.toString()}`, {
      headers: dataSpecHeaders(apiToken)
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeTaskRunFetchError(error)
  }
}

async function fetchTaskRunDetail({ server, projectId, taskRunId, apiToken, fetchFn }) {
  try {
    const params = new URLSearchParams()
    params.set('projectId', String(projectId))
    const response = await fetchFn(`${server}/api/ai-task-runs/${encodeURIComponent(taskRunId)}?${params.toString()}`, {
      headers: dataSpecHeaders(apiToken)
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeTaskRunFetchError(error)
  }
}

async function fetchQualityGateEvaluation({ server, projectId, apiToken, fetchFn }) {
  try {
    const response = await fetchFn(`${server}/api/quality-gate/evaluate`, {
      method: 'POST',
      headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ projectId })
    })
    return unwrapResponse(await readJsonResponse(response))
  } catch (error) {
    throw normalizeQualityGateFetchError(error)
  }
}

async function fetchSessionBootstrapWithFallback({ server, projectId, apiToken, fetchFn }) {
  try {
    return await fetchSessionBootstrap({ server, projectId, apiToken, fetchFn })
  } catch (error) {
    if (error instanceof DataSpecCliError) {
      if (isBootstrapAuthorizationFailure(error)) {
        return buildRejectedBootstrapPackage({ server, projectId, apiToken, error })
      }
      throw error
    }
    return buildLocalBootstrapFallback({ server, projectId, apiToken, error })
  }
}

async function fetchSessionBootstrap({ server, projectId, apiToken, fetchFn }) {
  const params = new URLSearchParams()
  appendOptionalParam(params, 'projectId', projectId)
  params.set('server', server)
  const response = await fetchFn(`${server}/api/bootstrap/session?${params.toString()}`, {
    headers: dataSpecHeaders(apiToken)
  })
  return unwrapResponse(await readJsonResponse(response))
}

function buildLocalBootstrapFallback({ server, projectId, apiToken, error }) {
  const message = `DataSpec 服务不可访问: ${error?.message ?? 'unknown error'}`
  const commandServer = safeServerForMetadata(server)
  const doctorCommand = projectId
    ? `dataspec doctor --project ${projectId} --server ${commandServer} --format json`
    : `dataspec doctor --server ${commandServer} --format json`
  return {
    kind: 'dataspec-ai-session-bootstrap',
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: 'BLOCKED',
    projectId: projectId ?? null,
    server: safeServerForMetadata(server),
    authMode: apiToken ? 'TOKEN_PRESENT' : 'TOKEN_MISSING',
    specVersion: 'unavailable',
    standardSnapshot: {
      snapshotId: null,
      projectId: projectId ?? null,
      specVersion: 'unavailable',
      specHash: null,
      versioned: false,
      source: 'server-unavailable'
    },
    availableCapabilities: [],
    recommendedCommands: [
      doctorCommand,
      'dataspec capability list --format json',
      projectId
        ? `dataspec bootstrap --project ${projectId} --server ${commandServer} --format json`
        : `dataspec bootstrap --project <id> --server ${commandServer} --format json`
    ].map((command) => sanitizeSecretText(command)),
    knownRisks: [
      '当前无法连接 DataSpec 服务，不能确认项目、标准版本和远端能力状态。',
      'fallback 启动包只包含本地配置和恢复建议，不代表 lint、Context、反向导入或 DDL 能力可用。'
    ],
    docsRefs: ['README.md#ai-会话启动包', 'README.md#cli'],
    checks: [
      {
        name: 'server',
        status: 'fail',
        message: sanitizeSecretText(message),
        nextAction: '启动 DataSpec 后端或修正 --server/.dataspec/config.json。'
      },
      {
        name: 'project',
        status: projectId ? 'warn' : 'fail',
        message: projectId ? `本地已配置 projectId: ${projectId}，但尚未远端校验。` : '缺少 projectId。',
        nextAction: projectId ? '服务恢复后重新运行 bootstrap。' : '提供 --project <id> 或更新 .dataspec/config.json。'
      }
    ],
    nextActions: [
      {
        code: 'RUN_DOCTOR',
        severity: 'error',
        message: sanitizeSecretText(message),
        command: sanitizeSecretText(doctorCommand),
        docsRef: 'README.md#cli',
        retryable: true
      },
      {
        code: 'START_DATASPEC_SERVER',
        severity: 'error',
        message: '启动 DataSpec 后端服务后重试 bootstrap。',
        command: 'mvn spring-boot:run',
        docsRef: 'README.md#本地启动',
        retryable: true
      }
    ]
  }
}

function isBootstrapAuthorizationFailure(error) {
  return error?.diagnostic?.httpStatus === 401 || error?.diagnostic?.httpStatus === 403
}

function buildRejectedBootstrapPackage({ server, projectId, apiToken, error }) {
  const diagnostic = error.diagnostic ?? fallbackDataSpecDiagnostic(0, error.message)
  const commandServer = safeServerForMetadata(server)
  const doctorCommand = projectId
    ? `dataspec doctor --project ${projectId} --server ${commandServer} --format json`
    : `dataspec doctor --server ${commandServer} --format json`
  const isForbidden = diagnostic.httpStatus === 403
  const message = sanitizeSecretText(error.message ?? diagnostic.suggestedAction)
  const checks = [
    {
      name: 'server',
      status: 'pass',
      message: 'DataSpec 服务已响应 bootstrap 请求。',
      nextAction: '继续处理认证或项目访问诊断。'
    },
    {
      name: isForbidden ? 'project' : 'auth',
      status: 'fail',
      message,
      nextAction: sanitizeSecretText(diagnostic.suggestedAction)
    }
  ]
  if (!projectId) {
    checks.push({
      name: 'project',
      status: 'fail',
      message: '缺少 projectId。',
      nextAction: '提供 --project <id> 或更新 .dataspec/config.json。'
    })
  }
  return {
    kind: 'dataspec-ai-session-bootstrap',
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    status: 'BLOCKED',
    projectId: projectId ?? null,
    server: commandServer,
    authMode: apiToken ? 'TOKEN_PRESENT' : 'TOKEN_MISSING',
    specVersion: 'unavailable',
    standardSnapshot: {
      snapshotId: null,
      projectId: projectId ?? null,
      specVersion: 'unavailable',
      specHash: null,
      versioned: false,
      source: 'server-rejected'
    },
    availableCapabilities: [],
    recommendedCommands: [
      doctorCommand,
      'dataspec capability list --format json',
      projectId
        ? `dataspec bootstrap --project ${projectId} --server ${commandServer} --format json`
        : `dataspec bootstrap --project <id> --server ${commandServer} --format json`
    ].map((command) => sanitizeSecretText(command)),
    knownRisks: [
      'DataSpec 服务已响应，但当前 token 或项目访问未通过校验。',
      '在修复认证或项目访问前，不应继续执行 lint、Context、反向导入或 DDL 生成。'
    ],
    docsRefs: [diagnostic.docsRef ?? 'README.md#安全基线', 'README.md#ai-会话启动包'],
    checks,
    nextActions: [
      {
        code: diagnostic.code ?? (isForbidden ? 'PROJECT_ACCESS_DENIED' : 'AUTH_TOKEN_MISSING_OR_INVALID'),
        severity: 'error',
        message,
        command: sanitizeSecretText(doctorCommand),
        docsRef: diagnostic.docsRef ?? 'README.md#安全基线',
        retryable: Boolean(diagnostic.retryable)
      }
    ]
  }
}

async function buildEvidenceRequest(options, config) {
  const sourceType = normalizeEvidenceSourceType(options['source-type'] ?? options.sourceType)
  const sourceId = options['source-id'] ?? options.sourceId
  const payload = options.payload ? JSON.parse(await readFile(options.payload, 'utf8')) : {}
  const req = {
    ...payload,
    projectId: optionalProjectId(options.project ?? config.projectId ?? payload.projectId),
    sourceType,
    sourceId: sourceId === undefined || sourceId === null || sourceId === '' ? payload.sourceId : parsePositiveInteger(sourceId, 'source id'),
    sourceTitle: options['source-title'] ?? options.sourceTitle ?? payload.sourceTitle
  }
  if (!req.sourceType) {
    throw new Error('evidence export 需要提供 --source-type <AI_JOB|SQL_CHECK|COVERAGE_REPORT|AI_BATCH_RUN|AI_TASK_RUN>')
  }
  if (req.sourceType !== 'COVERAGE_REPORT' && !req.sourceId) {
    throw new Error(`${req.sourceType} 需要提供 --source-id <id>`)
  }
  return req
}

function normalizeEvidenceSourceType(value) {
  if (!value) {
    return undefined
  }
  return String(value).trim().replaceAll('-', '_').toUpperCase()
}

function optionalProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return parseProjectId(value)
}

const REQUIRED_CONTRACT_IDS = [
  'field',
  'enum-dict',
  'rule-config',
  'template',
  'standard-snapshot',
  'lint-result',
  'ai-evidence-package',
  'ai-context-manifest',
  'ai-context-field-catalog',
  'ai-task-profile'
]

function checkContractRegistry(catalog) {
  const diagnostics = []
  if (catalog?.kind !== 'dataspec-schema-registry') {
    diagnostics.push(contractDiagnostic('fail', 'INVALID_KIND', 'registry kind 必须是 dataspec-schema-registry'))
  }
  if (!Number.isInteger(catalog?.schemaVersion)) {
    diagnostics.push(contractDiagnostic('fail', 'MISSING_SCHEMA_VERSION', 'registry 缺少整数 schemaVersion'))
  }
  if (!catalog?.registryVersion) {
    diagnostics.push(contractDiagnostic('fail', 'MISSING_REGISTRY_VERSION', 'registry 缺少 registryVersion'))
  }
  if (!catalog?.compatibilityPolicy?.breakingChangePolicy) {
    diagnostics.push(contractDiagnostic('fail', 'MISSING_COMPATIBILITY_POLICY', 'registry 缺少 breakingChangePolicy'))
  }

  const contracts = Array.isArray(catalog?.contracts) ? catalog.contracts : []
  const byId = new Map(contracts.map((contract) => [contract.contractId, contract]))
  for (const contractId of REQUIRED_CONTRACT_IDS) {
    const contract = byId.get(contractId)
    if (!contract) {
      diagnostics.push(contractDiagnostic('fail', 'MISSING_CONTRACT', `缺少核心 contract: ${contractId}`))
      continue
    }
    if (!contract.schemaVersion) {
      diagnostics.push(contractDiagnostic('fail', 'MISSING_CONTRACT_SCHEMA_VERSION', `${contractId} 缺少 schemaVersion`))
    }
    if (!Array.isArray(contract.stableFields) || contract.stableFields.length === 0) {
      diagnostics.push(contractDiagnostic('fail', 'MISSING_STABLE_FIELDS', `${contractId} 缺少 stableFields`))
    }
    if (!contract.jsonSchemaRef) {
      diagnostics.push(contractDiagnostic('fail', 'MISSING_JSON_SCHEMA_REF', `${contractId} 缺少 jsonSchemaRef`))
    }
    if (!Array.isArray(contract.deprecatedFields)) {
      diagnostics.push(contractDiagnostic('fail', 'INVALID_DEPRECATED_FIELDS', `${contractId} deprecatedFields 必须是数组`))
    }
  }

  if (diagnostics.length === 0) {
    diagnostics.push(contractDiagnostic('pass', 'REGISTRY_READY', 'schema registry 可用于 AI/CLI/MCP 契约确认'))
  }
  return {
    kind: 'dataspec-contract-check',
    schemaVersion: 1,
    ok: diagnostics.every((item) => item.status !== 'fail'),
    registryVersion: catalog?.registryVersion ?? null,
    contractCount: contracts.length,
    diagnostics
  }
}

function contractDiagnostic(status, code, message) {
  return { status, code, message }
}

const REQUIRED_CAPABILITY_IDS = [
  'capability-catalog',
  'doctor',
  'export-ai-context',
  'lint-sql',
  'sql-rule-debugger',
  'search-fields',
  'suggest-fields',
  'generate-ddl',
  'reverse-import',
  'coverage-report',
  'schema-registry',
  'export-evidence-package',
  'workflow-recipes',
  'ai-task-profiles',
  'domain-starter-kits',
  'session-bootstrap'
]

function checkCapabilityCatalog(catalog) {
  const diagnostics = []
  if (catalog?.kind !== 'dataspec-ai-capability-catalog') {
    diagnostics.push(capabilityDiagnostic('fail', 'INVALID_KIND', 'catalog kind 必须是 dataspec-ai-capability-catalog'))
  }
  if (!Number.isInteger(catalog?.schemaVersion)) {
    diagnostics.push(capabilityDiagnostic('fail', 'MISSING_SCHEMA_VERSION', 'catalog 缺少整数 schemaVersion'))
  }
  if (!catalog?.catalogVersion) {
    diagnostics.push(capabilityDiagnostic('fail', 'MISSING_CATALOG_VERSION', 'catalog 缺少 catalogVersion'))
  }
  const capabilities = Array.isArray(catalog?.capabilities) ? catalog.capabilities : []
  const byId = new Map(capabilities.map((capability) => [capability.id, capability]))
  for (const capabilityId of REQUIRED_CAPABILITY_IDS) {
    const capability = byId.get(capabilityId)
    if (!capability) {
      diagnostics.push(capabilityDiagnostic('fail', 'MISSING_CAPABILITY', `缺少核心 capability: ${capabilityId}`))
      continue
    }
    if (!capability.summary) {
      diagnostics.push(capabilityDiagnostic('fail', 'MISSING_SUMMARY', `${capabilityId} 缺少 summary`))
    }
    if (!capability.writeRisk) {
      diagnostics.push(capabilityDiagnostic('fail', 'MISSING_WRITE_RISK', `${capabilityId} 缺少 writeRisk`))
    }
    const safetyDiagnostics = validateCapabilitySafety(capabilityId, capability)
    diagnostics.push(...safetyDiagnostics)
    if (!Array.isArray(capability.preflightChecks)) {
      diagnostics.push(capabilityDiagnostic('fail', 'INVALID_PREFLIGHT_CHECKS', `${capabilityId} preflightChecks 必须是数组`))
    }
    if (!Array.isArray(capability.nextActions)) {
      diagnostics.push(capabilityDiagnostic('fail', 'INVALID_NEXT_ACTIONS', `${capabilityId} nextActions 必须是数组`))
    }
  }
  if (diagnostics.length === 0) {
    diagnostics.push(capabilityDiagnostic('pass', 'CAPABILITY_CATALOG_READY', 'capability catalog 可用于 AI/CLI/MCP 能力发现'))
  }
  return {
    kind: 'dataspec-capability-check',
    schemaVersion: 1,
    ok: diagnostics.every((item) => item.status !== 'fail'),
    catalogVersion: catalog?.catalogVersion ?? null,
    capabilityCount: capabilities.length,
    diagnostics
  }
}

function capabilityDiagnostic(status, code, message) {
  return { status, code, message }
}

function validateCapabilitySafety(capabilityId, capability) {
  const safety = capability?.safety
  if (!safety || typeof safety !== 'object' || Array.isArray(safety)) {
    return [capabilityDiagnostic('fail', 'MISSING_SAFETY', `${capabilityId} 缺少 safety metadata`)]
  }
  const diagnostics = []
  for (const field of ['readOnly', 'writesProject', 'requiresDryRun', 'supportsUndo', 'requiresIdempotencyKey']) {
    if (typeof safety[field] !== 'boolean') {
      diagnostics.push(capabilityDiagnostic('fail', 'INVALID_SAFETY_FIELD', `${capabilityId} safety.${field} 必须是 boolean`))
    }
  }
  if (!Array.isArray(safety.sensitiveInputs)) {
    diagnostics.push(capabilityDiagnostic('fail', 'INVALID_SAFETY_FIELD', `${capabilityId} safety.sensitiveInputs 必须是数组`))
  }
  if (!Array.isArray(safety.nextActions)) {
    diagnostics.push(capabilityDiagnostic('fail', 'INVALID_SAFETY_FIELD', `${capabilityId} safety.nextActions 必须是数组`))
  }
  return diagnostics
}

function normalizeCapabilityFetchError(error, options = {}) {
  if (error instanceof DataSpecCliError) {
    if (options.capabilityId && error.diagnostic?.httpStatus === 404) {
      return new DataSpecCliError(error.message, {
        ...error.diagnostic,
        code: 'CAPABILITY_NOT_FOUND',
        category: 'PARAMETER',
        retryable: false,
        suggestedAction: `运行 dataspec capability list --format json 查看可用能力，再重试 capability show ${options.capabilityId}。`,
        docsRef: 'README.md#ai-能力清单'
      })
    }
    return error
  }
  return new DataSpecCliError(
    `读取 capability catalog 失败: ${error?.message ?? 'DataSpec 服务不可用'}`,
    {
      code: 'DATASPEC_SERVER_UNAVAILABLE',
      category: 'NETWORK',
      retryable: true,
      suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置。',
      docsRef: 'README.md#cli',
      httpStatus: null
    }
  )
}

function normalizeVersionCompatibilityFetchError(error) {
  const message = sanitizeSecretText(error?.message ?? 'DataSpec 服务不可用')
  const base = error instanceof DataSpecCliError && error.diagnostic
    ? sanitizeSecretValue(error.diagnostic)
    : {}
  return {
    ...base,
    code: 'VERSION_COMPATIBILITY_UNAVAILABLE',
    category: base.category ?? 'NETWORK',
    retryable: base.retryable ?? true,
    message: `版本兼容检查失败: ${message}`,
    suggestedAction: '先确认 DataSpec 服务地址和 token；必要时运行 dataspec doctor --format json。',
    docsRef: 'README.md#版本兼容握手',
    httpStatus: base.httpStatus ?? null
  }
}

function normalizeTaskRunFetchError(error) {
  if (error instanceof DataSpecCliError) {
    return error
  }
  return new DataSpecCliError(
    `读取 AI task run 失败: ${error?.message ?? 'DataSpec 服务不可用'}`,
    {
      code: 'DATASPEC_SERVER_UNAVAILABLE',
      category: 'NETWORK',
      retryable: true,
      suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置；然后重试 dataspec task failures --format json。',
      docsRef: 'README.md#cli',
      httpStatus: null
    }
  )
}

function normalizeQualityGateFetchError(error) {
  if (error instanceof DataSpecCliError) {
    return error
  }
  const message = sanitizeSecretText(error?.message ?? 'DataSpec 服务不可用')
  return new DataSpecCliError(
    `读取质量门禁失败: ${message}`,
    {
      code: 'DATASPEC_SERVER_UNAVAILABLE',
      category: 'NETWORK',
      retryable: true,
      suggestedAction: '先运行 dataspec doctor --format json 检查服务、server URL、token 和项目配置；然后重试 dataspec quality-gate check --format json。',
      docsRef: 'README.md#cli',
      httpStatus: null
    }
  )
}

function resolveProfileSelection(options = {}, config = {}) {
  const explicitProfile = normalizeOptionalCliText(options.profile)
  const explicitTaskType = normalizeOptionalCliText(options.taskType ?? options['task-type'])
  if (explicitProfile || explicitTaskType) {
    return {
      profileId: explicitProfile,
      taskType: explicitTaskType
    }
  }
  return {
    profileId: normalizeOptionalCliText(config.aiProfile),
    taskType: normalizeOptionalCliText(config.taskType)
  }
}

function resolveCliFixPolicy(options = {}) {
  const mode = normalizeEnumOption(options.fixMode ?? options['fix-mode'], 'fix-mode', ['GENERATE', 'DRY_RUN', 'DISABLED'])
  const maxRiskLevel = normalizeEnumOption(options.maxRisk ?? options['max-risk'], 'max-risk', ['LOW', 'MEDIUM', 'HIGH'])
  const includeExplanations = parseOptionalBoolean(
    options.includeExplanations ?? options['include-explanations'],
    'include-explanations'
  )
  const enabledRuleCodes = normalizeRuleCodeOptions(options.enableRule ?? options['enable-rule'])
  const disabledRuleCodes = normalizeRuleCodeOptions(options.disableRule ?? options['disable-rule'])

  const policy = {}
  if (mode) {
    policy.mode = mode
  }
  if (maxRiskLevel) {
    policy.maxRiskLevel = maxRiskLevel
  }
  if (enabledRuleCodes?.length) {
    policy.enabledRuleCodes = enabledRuleCodes
  }
  if (disabledRuleCodes?.length) {
    policy.disabledRuleCodes = disabledRuleCodes
  }
  if (includeExplanations !== undefined) {
    policy.includeExplanations = includeExplanations
  }
  return Object.keys(policy).length > 0 ? policy : null
}

function normalizeEnumOption(value, label, allowedValues) {
  const normalized = normalizeOptionalCliText(value)
  if (!normalized) {
    return undefined
  }
  const enumValue = normalized.replaceAll('-', '_').toUpperCase()
  if (!allowedValues.includes(enumValue)) {
    throw new Error(`无效 ${label}: ${value}。支持: ${allowedValues.join(', ')}`)
  }
  return enumValue
}

function normalizeRuleCodeOptions(value) {
  if (value === undefined || value === null) {
    return undefined
  }
  const values = Array.isArray(value) ? value : [value]
  const result = values
    .flatMap((item) => String(item).split(','))
    .map((item) => item.trim())
    .filter(Boolean)
  return [...new Set(result)]
}

function normalizeOptionalCliText(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const normalized = String(value).trim()
  return normalized || undefined
}

async function checkOpenapiStatus({ server, apiDocsReachable, checkDrift, runDriftCheck = runOpenapiDriftCheck }) {
  if (!apiDocsReachable) {
    return failCheck('openapi', '无法检查 OpenAPI：/api-docs 不可访问')
  }
  try {
    await stat(OPENAPI_SCHEMA_PATH)
  } catch (error) {
    return failCheck('openapi', `缺少本地 OpenAPI 类型文件: ${OPENAPI_SCHEMA_PATH}`)
  }
  if (!checkDrift) {
    return passCheck('openapi', 'OpenAPI 文档可访问，本地 schema.ts 存在；完整漂移检查可运行 --check-openapi')
  }
  try {
    const result = await runDriftCheck(server)
    return result.ok
      ? passCheck('openapi', result.message)
      : failCheck('openapi', result.message)
  } catch (error) {
    return failCheck('openapi', `OpenAPI 漂移检查失败: ${error.message}`)
  }
}

async function checkContextCache({ config, server, projectId, apiToken, fetchFn, serverReachable }) {
  const metadataPath = path.join(config.rootDir, CONTEXT_CACHE_DIR, CACHE_METADATA_FILE)
  const refreshCommand = 'dataspec export-context --cache'
  let metadata
  try {
    metadata = JSON.parse(await readFile(metadataPath, 'utf8'))
  } catch (error) {
    if (error.code === 'ENOENT') {
      return warnCheck('context-cache', `未找到 AI Context 缓存；可运行 ${refreshCommand}`, {
        cacheStatus: 'missing',
        metadataPath
      })
    }
    return warnCheck('context-cache', `AI Context 缓存不可读: ${error.message}`, {
      cacheStatus: 'unreadable',
      metadataPath
    })
  }

  const cacheStatus = cacheFreshness(metadata)
  const details = {
    cacheStatus,
    metadataPath,
    projectId: metadata.projectId ?? null,
    exportedAt: metadata.exportedAt ?? null,
    expiresAt: metadata.expiresAt ?? null,
    source: metadata.standard?.source ?? null,
    specVersion: metadata.standard?.specVersion ?? null,
    specHash: metadata.standard?.specHash ?? null
  }
  if (cacheStatus === 'unreadable') {
    return warnCheck('context-cache', `AI Context 缓存 metadata 缺少有效 expiresAt，请运行 ${refreshCommand} 刷新`, details)
  }

  if (!serverReachable) {
    if (cacheStatus === 'stale') {
      return warnCheck('context-cache', `DataSpec 服务不可用，AI Context 缓存已过期；离线使用前建议尽快运行 ${refreshCommand}`, details)
    }
    return passCheck('context-cache', 'DataSpec 服务不可用，但存在未过期 AI Context 缓存，可离线只读使用', details)
  }

  if (projectId) {
    const remote = await fetchRemoteAiContextMetadata(server, projectId, fetchFn, apiToken)
    if (remote.status === 'ok' && standardMetadataDiffers(metadata.standard, remote.standard)) {
      return failCheck('context-cache', `AI Context 缓存与远端标准不一致，请运行 ${refreshCommand} 刷新`, {
        ...details,
        cacheStatus: 'remote-different',
        remoteStandard: remote.standard
      })
    }
    if (remote.status === 'warn') {
      return warnCheck('context-cache', `无法读取远端 AI Context metadata: ${remote.message}`, details)
    }
  }

  if (cacheStatus === 'stale') {
    return warnCheck('context-cache', `AI Context 缓存已过期，请运行 ${refreshCommand} 刷新`, details)
  }
  return passCheck('context-cache', 'AI Context 缓存可用', details)
}

function cacheFreshness(metadata) {
  const expiresAt = Date.parse(metadata?.expiresAt ?? '')
  if (!Number.isFinite(expiresAt)) {
    return 'unreadable'
  }
  return Date.now() > expiresAt ? 'stale' : 'fresh'
}

async function fetchRemoteAiContextMetadata(server, projectId, fetchFn, apiToken) {
  try {
    const response = await fetchFn(buildAiContextPackageUrl(server, projectId, {}), {
      headers: dataSpecHeaders(apiToken)
    })
    if (!response.ok) {
      return { status: 'warn', message: `HTTP ${response.status}` }
    }
    const entries = normalizeZipEntries(readZipEntries(Buffer.from(await response.arrayBuffer())))
    return { status: 'ok', standard: parseAiContextManifest(entries)?.standard ?? null }
  } catch (error) {
    return { status: 'warn', message: error.message }
  }
}

function standardMetadataDiffers(cached, remote) {
  if (!cached || !remote) {
    return false
  }
  if (cached.specHash && remote.specHash) {
    return cached.specHash !== remote.specHash
  }
  return ['specVersion', 'source'].some((key) => cached[key] && remote[key] && cached[key] !== remote[key])
}

async function runOpenapiDriftCheck(server) {
  const { checkOpenapiSchema } = await import(pathToFileURL(path.join(DATASPEC_WEB_DIR, 'scripts', 'check-openapi-schema.mjs')).href)
  return await checkOpenapiSchema({
    argv: ['--source', `${server}/api-docs`],
    cwd: DATASPEC_WEB_DIR
  })
}

function passCheck(name, message, details = undefined) {
  return buildCheck(name, 'pass', message, details)
}

function warnCheck(name, message, details = undefined) {
  return buildCheck(name, 'warn', message, details)
}

function failCheck(name, message, details = undefined) {
  return buildCheck(name, 'fail', message, details)
}

function buildCheck(name, status, message, details) {
  const check = { name, status, message }
  if (details !== undefined) {
    check.details = details
  }
  return check
}

function formatDoctorText(result) {
  const lines = [
    'DataSpec Doctor',
    `server: ${result.server}`,
    `projectId: ${result.projectId ?? '未配置'}`,
    ''
  ]
  for (const check of result.checks) {
    lines.push(`[${check.status.toUpperCase()}] ${check.name}: ${check.message}`)
  }
  lines.push('', result.ok ? '结果: 可用' : '结果: 存在需要处理的问题', '')
  return lines.join('\n')
}

function formatCompatibilityText(result) {
  const compatibility = result.compatibility ?? {}
  const lines = [
    'DataSpec Version Compatibility',
    `server: ${result.server}`,
    `serverVersion: ${result.serverVersion ?? '未知'}`,
    `localCliVersion: ${result.localCliVersion}`,
    `minCliVersion: ${result.minCliVersion ?? '未知'}`,
    `apiSchemaHash: ${result.apiSchemaHash ?? '未知'}`,
    `status: ${compatibility.status ?? 'UNKNOWN'}`,
    ''
  ]
  for (const reason of compatibility.reasons ?? []) {
    lines.push(`- ${reason}`)
  }
  for (const action of compatibility.nextActions ?? result.upgradeHints ?? []) {
    lines.push(`next: ${action}`)
  }
  lines.push('')
  return lines.join('\n')
}

function formatCompatibilityErrorText(result) {
  return [
    'DataSpec Version Compatibility',
    `server: ${result.server}`,
    `localCliVersion: ${result.localCliVersion}`,
    `错误: ${result.diagnostic.message}`,
    `next: ${result.diagnostic.suggestedAction}`,
    ''
  ].join('\n')
}

function formatProfileCatalogText(catalog) {
  const lines = [
    'DataSpec AI Profiles',
    `projectId: ${catalog.projectId ?? '未配置'}`,
    `selected: ${catalog.selectedProfileId ?? catalog.defaultProfileId ?? '未配置'}`,
    ''
  ]
  for (const profile of catalog.profiles ?? []) {
    lines.push(`- ${profile.profileId} (${profile.taskType}) ${profile.displayName ?? ''}`.trim())
    if (profile.description) {
      lines.push(`  ${profile.description}`)
    }
    if (profile.contextScope) {
      lines.push(`  context: ${formatProfileScope(profile.contextScope)}`)
    }
    if (profile.fixedSqlPolicy) {
      lines.push(`  fixedSql: ${profile.fixedSqlPolicy.mode ?? 'DEFAULT'} / ${profile.fixedSqlPolicy.maxRiskLevel ?? 'DEFAULT'}`)
    }
  }
  lines.push('')
  return lines.join('\n')
}

function formatProfileDetailText(detail) {
  const profile = detail.profile
  const lines = [
    'DataSpec AI Profile',
    `projectId: ${detail.projectId ?? '未配置'}`,
    `profile: ${profile.profileId}`,
    `taskType: ${profile.taskType}`,
    `name: ${profile.displayName ?? ''}`,
    ''
  ]
  if (profile.description) {
    lines.push(profile.description, '')
  }
  lines.push(`context: ${formatProfileScope(profile.contextScope ?? {})}`)
  if (profile.fixedSqlPolicy) {
    lines.push(`fixedSql: ${profile.fixedSqlPolicy.mode ?? 'DEFAULT'} / ${profile.fixedSqlPolicy.maxRiskLevel ?? 'DEFAULT'}`)
  }
  if (profile.outputFormat) {
    lines.push(`output: ${profile.outputFormat.format ?? 'json'}`)
  }
  if ((profile.recommendedCommands ?? []).length > 0) {
    lines.push('', 'recommended commands:')
    for (const command of profile.recommendedCommands) {
      lines.push(`  - ${command}`)
    }
  }
  if ((detail.diagnostics ?? []).length > 0) {
    lines.push('', 'diagnostics:')
    for (const diagnostic of detail.diagnostics) {
      lines.push(`  - [${diagnostic.status}] ${diagnostic.code}: ${diagnostic.message}`)
    }
  }
  lines.push('')
  return lines.join('\n')
}

function formatProfileScope(scope) {
  return [
    scope.scope ? `scope=${scope.scope}` : null,
    scope.query ? `query=${scope.query}` : null,
    scope.status ? `status=${scope.status}` : null,
    scope.limit ? `limit=${scope.limit}` : null
  ].filter(Boolean).join(', ') || 'default'
}

function formatContractCatalogText(catalog) {
  const lines = [
    'DataSpec Schema Registry',
    `kind: ${catalog.kind ?? '-'}`,
    `schemaVersion: ${catalog.schemaVersion ?? '-'}`,
    `registryVersion: ${catalog.registryVersion ?? '-'}`,
    ''
  ]
  for (const contract of catalog.contracts ?? []) {
    lines.push(`- ${contract.contractId}@${contract.schemaVersion} ${contract.displayName ?? ''}`.trim())
    if (contract.description) {
      lines.push(`  ${contract.description}`)
    }
    if ((contract.stableFields ?? []).length > 0) {
      lines.push(`  stableFields: ${contract.stableFields.slice(0, 8).join(', ')}${contract.stableFields.length > 8 ? ', ...' : ''}`)
    }
  }
  lines.push('')
  return lines.join('\n')
}

function formatContractDetailText(contract) {
  const lines = [
    'DataSpec Schema Contract',
    `contractId: ${contract.contractId}`,
    `schemaVersion: ${contract.schemaVersion}`,
    `name: ${contract.displayName ?? ''}`,
    `jsonSchemaRef: ${contract.jsonSchemaRef ?? '-'}`,
    ''
  ]
  if (contract.description) {
    lines.push(contract.description, '')
  }
  if ((contract.stableFields ?? []).length > 0) {
    lines.push('stable fields:')
    for (const field of contract.stableFields) {
      lines.push(`  - ${field}`)
    }
  }
  if ((contract.deprecatedFields ?? []).length > 0) {
    lines.push('', 'deprecated fields:')
    for (const field of contract.deprecatedFields) {
      lines.push(`  - ${field.fieldPath}: ${field.replacement ?? '-'} (${field.reason ?? '-'})`)
    }
  }
  if (contract.compatibility?.breakingChangePolicy) {
    lines.push('', `breaking changes: ${contract.compatibility.breakingChangePolicy}`)
  }
  lines.push('')
  return lines.join('\n')
}

function formatContractCheckText(result) {
  const lines = [
    'DataSpec Contract Check',
    `registryVersion: ${result.registryVersion ?? '-'}`,
    `contractCount: ${result.contractCount ?? 0}`,
    ''
  ]
  for (const diagnostic of result.diagnostics ?? []) {
    lines.push(`[${diagnostic.status.toUpperCase()}] ${diagnostic.code}: ${diagnostic.message}`)
  }
  lines.push('', result.ok ? '结果: 可用' : '结果: 契约 registry 不可用', '')
  return lines.join('\n')
}

function formatCapabilityCatalogText(catalog) {
  const lines = [
    'DataSpec AI Capability Catalog',
    `kind: ${catalog.kind ?? '-'}`,
    `schemaVersion: ${catalog.schemaVersion ?? '-'}`,
    `catalogVersion: ${catalog.catalogVersion ?? '-'}`,
    `projectId: ${catalog.projectId ?? '-'}`,
    ''
  ]
  for (const capability of catalog.capabilities ?? []) {
    lines.push(`- ${capability.id} [${capability.writeRisk ?? '-'}] ${capability.title ?? ''}`.trim())
    if (capability.summary) {
      lines.push(`  ${capability.summary}`)
    }
    const surfaces = [
      ...(capability.apiEndpoints ?? []).slice(0, 2),
      ...(capability.cliCommands ?? []).slice(0, 1),
      ...(capability.mcpResources ?? []).slice(0, 1),
      ...(capability.mcpTools ?? []).slice(0, 1)
    ]
    if (surfaces.length > 0) {
      lines.push(`  surfaces: ${surfaces.join(' | ')}`)
    }
  }
  lines.push('')
  return lines.join('\n')
}

function formatCapabilityDetailText(capability) {
  const lines = [
    'DataSpec AI Capability',
    `id: ${capability.id}`,
    `category: ${capability.category ?? '-'}`,
    `status: ${capability.status ?? '-'}`,
    `stability: ${capability.stability ?? '-'}`,
    `requiresProject: ${Boolean(capability.requiresProject)}`,
    `writeRisk: ${capability.writeRisk ?? '-'}`,
    ''
  ]
  appendCapabilitySafetyText(lines, capability.safety)
  if (capability.summary) {
    lines.push(capability.summary, '')
  }
  appendTextList(lines, 'api endpoints', capability.apiEndpoints)
  appendTextList(lines, 'cli commands', capability.cliCommands)
  appendTextList(lines, 'mcp resources', capability.mcpResources)
  appendTextList(lines, 'mcp tools', capability.mcpTools)
  appendTextList(lines, 'preflight checks', capability.preflightChecks)
  appendTextList(lines, 'next actions', capability.nextActions)
  if (capability.docsRef) {
    lines.push('', `docs: ${capability.docsRef}`)
  }
  lines.push('')
  return lines.join('\n')
}

function appendCapabilitySafetyText(lines, safety) {
  if (!safety || typeof safety !== 'object') {
    return
  }
  lines.push('safety:')
  lines.push(`  readOnly: ${Boolean(safety.readOnly)}`)
  lines.push(`  writesProject: ${Boolean(safety.writesProject)}`)
  lines.push(`  requiresDryRun: ${Boolean(safety.requiresDryRun)}`)
  lines.push(`  supportsUndo: ${Boolean(safety.supportsUndo)}`)
  lines.push(`  requiresIdempotencyKey: ${Boolean(safety.requiresIdempotencyKey)}`)
  lines.push(`  sensitiveInputs: ${Array.isArray(safety.sensitiveInputs) && safety.sensitiveInputs.length > 0 ? safety.sensitiveInputs.join(', ') : '-'}`)
  appendTextList(lines, '  safety next actions', safety.nextActions)
  lines.push('')
}

function formatCapabilityCheckText(result) {
  const lines = [
    'DataSpec Capability Check',
    `catalogVersion: ${result.catalogVersion ?? '-'}`,
    `capabilityCount: ${result.capabilityCount ?? 0}`,
    ''
  ]
  for (const diagnostic of result.diagnostics ?? []) {
    lines.push(`[${diagnostic.status.toUpperCase()}] ${diagnostic.code}: ${diagnostic.message}`)
  }
  lines.push('', result.ok ? '结果: 可用' : '结果: capability catalog 不可用', '')
  return lines.join('\n')
}

function formatContextBudgetPlanText(plan) {
  const selected = plan.selectedArtifacts ?? []
  const dropped = plan.droppedArtifacts ?? []
  const lines = [
    'AI Context 预算计划',
    `projectId: ${plan.projectId ?? plan.request?.projectId ?? '-'}`,
    `qualityRisk: ${plan.qualityRisk ?? '-'}`,
    `estimatedTokens: ${plan.estimation?.selectedEstimatedTokens ?? '-'} / ${plan.estimation?.tokenBudget ?? '-'}`,
    `estimationMethod: ${plan.estimation?.estimationMethod ?? '-'}`,
    `selectedArtifacts: ${selected.length}`
  ]
  selected.forEach((item) => {
    lines.push(`  - ${item.artifact} (${item.estimatedTokens ?? '-'} tokens)`)
  })
  lines.push(`droppedArtifacts: ${dropped.length}`)
  dropped.forEach((item) => {
    lines.push(`  - ${item.artifact} (${item.estimatedTokens ?? '-'} tokens): ${item.riskImpact ?? ''}`)
  })
  lines.push(
    'recommendedExportParams:',
    `  scope: ${plan.recommendedExportParams?.scope ?? '-'}`,
    `  query: ${plan.recommendedExportParams?.query ?? '-'}`,
    `  status: ${plan.recommendedExportParams?.status ?? '-'}`,
    `  limit: ${plan.recommendedExportParams?.limit ?? '-'}`,
    `diagnostics: ${(plan.diagnostics ?? []).length}`
  )
  ;(plan.diagnostics ?? []).forEach((item) => {
    lines.push(`  - ${item}`)
  })
  lines.push('recommendedNextActions:')
  ;(plan.recommendedNextActions ?? []).forEach((item) => {
    lines.push(`  - ${item}`)
  })
  return `${lines.join('\n')}\n`
}

function formatContextQualityText(result) {
  const lines = [
    'AI Context 质量检查',
    `input: ${result.input?.sourceType ?? '-'} ${result.input?.path ?? '-'}`,
    `contextQualityScore: ${result.contextQualityScore}`,
    `qualityLevel: ${result.qualityLevel}`,
    `tokenSource: ${result.tokenBudgetBreakdown?.source ?? '-'}`,
    `missingCriticalResources: ${(result.missingCriticalResources ?? []).length}`
  ]
  ;(result.missingCriticalResources ?? []).forEach((item) => {
    lines.push(`  - ${item.category}: ${item.reason}`)
  })
  lines.push(`truncatedResources: ${(result.truncatedResources ?? []).length}`)
  ;(result.truncatedResources ?? []).forEach((item) => {
    lines.push(`  - ${item.path}: ${item.reason}`)
  })
  lines.push('taskFitHints:')
  ;(result.taskFitHints ?? []).forEach((item) => {
    lines.push(`  - ${item}`)
  })
  lines.push('nextContextActions:')
  ;(result.nextContextActions ?? []).forEach((item) => {
    lines.push(`  - ${item}`)
  })
  return `${lines.join('\n')}\n`
}

function formatSyntheticExamplesText(result) {
  const lines = [
    'DataSpec Synthetic Examples',
    `scenario: ${redactSecrets(result.scenario ?? '-')}`,
    `projectId: ${result.projectId ?? '-'}`,
    `specHash: ${redactSecrets(result.specHash ?? '-')}`,
    `goodSql: ${(result.goodSql ?? []).length}`,
    `badSql: ${(result.badSql ?? []).length}`,
    `ddlPreviewInputs: ${(result.ddlPreviewInputs ?? []).length}`,
    `fieldSuggestionQuestions: ${(result.fieldSuggestionQuestions ?? []).length}`,
    `standardQaCases: ${(result.standardQaCases ?? []).length}`,
    `expectedDiagnostics: ${(result.expectedDiagnostics ?? []).length}`,
    `readOnly: ${Boolean(result.safety?.readOnly)}`,
    `writesProject: ${Boolean(result.safety?.writesProject)}`,
    `containsRealBusinessRows: ${Boolean(result.safety?.containsRealBusinessRows)}`,
    `externalLlmUsed: ${Boolean(result.safety?.externalLlmUsed)}`
  ]
  if (result.sourceSummary) {
    lines.push(
      `standardFieldCount: ${result.sourceSummary.standardFieldCount ?? '-'}`,
      `templateCount: ${result.sourceSummary.templateCount ?? '-'}`,
      `fallbackUsed: ${Boolean(result.sourceSummary.fallbackUsed)}`
    )
  }
  appendTextList(lines, 'next actions', (result.nextActions ?? []).map((item) => redactSecrets(item)))
  return `${lines.join('\n')}\n`
}

function formatStandardTestDataPackageText(result) {
  const coverage = result.coverageReport ?? {}
  const safety = result.safety ?? {}
  const lines = [
    'DataSpec Standard Test Data Package',
    `projectId: ${result.projectId ?? '-'}`,
    `specHash: ${redactSecrets(result.specHash ?? '-')}`,
    `testDataCases: ${(result.testDataCases ?? []).length}`,
    `seedProfiles: ${(result.seedProfiles ?? []).length}`,
    `mockPayloads: ${(result.mockPayloads ?? []).length}`,
    `selectedFieldCount: ${coverage.selectedFieldCount ?? '-'}`,
    `generatedCaseCount: ${coverage.generatedCaseCount ?? '-'}`,
    `missingConstraints: ${(coverage.missingConstraints ?? []).length}`,
    `requiresBusinessReview: ${Boolean(coverage.requiresBusinessReview)}`,
    `readOnly: ${Boolean(safety.readOnly)}`,
    `writesProject: ${Boolean(safety.writesProject)}`,
    `writesBusinessRepo: ${Boolean(safety.writesBusinessRepo)}`,
    `containsRealBusinessRows: ${Boolean(safety.containsRealBusinessRows)}`,
    `externalNetworkUsed: ${Boolean(safety.externalNetworkUsed)}`,
    `externalLlmUsed: ${Boolean(safety.externalLlmUsed)}`
  ]
  appendTextList(lines, 'diagnostics', (result.diagnostics ?? []).map((item) => {
    const code = item?.code ?? item?.id ?? 'DIAGNOSTIC'
    const message = item?.message ?? item?.reason ?? ''
    return redactSecrets(message ? `${code}: ${message}` : String(code))
  }))
  appendTextList(lines, 'next actions', (result.nextActions ?? []).map((item) => redactSecrets(item)))
  return `${lines.join('\n')}\n`
}

function formatContractImportPreviewText(result) {
  const lines = [
    'DataSpec Contract Import Preview',
    `sourceKind: ${redactSecrets(result.sourceKind ?? '-')}`,
    `projectId: ${result.projectId ?? '-'}`,
    `sourcePath: ${redactSecrets(result.sourcePath ?? '-')}`,
    `contractHash: ${redactSecrets(result.contractHash ?? '-')}`,
    `sourceFieldCount: ${result.summary?.sourceFieldCount ?? '-'}`,
    `candidateFields: ${(result.candidateFields ?? []).length}`,
    `diagnostics: ${(result.diagnostics ?? []).length}`,
    `readOnly: ${Boolean(result.safety?.readOnly)}`,
    `writesProject: ${Boolean(result.safety?.writesProject)}`,
    `externalNetworkUsed: ${Boolean(result.safety?.externalNetworkUsed)}`,
    `externalLlmUsed: ${Boolean(result.safety?.externalLlmUsed)}`,
    `containsRealBusinessRows: ${Boolean(result.safety?.containsRealBusinessRows)}`
  ]
  const reviewRequired = (result.candidateFields ?? [])
    .filter((item) => item.recommendedAction === 'REVIEW_REQUIRED')
    .length
  const mergeExisting = (result.candidateFields ?? [])
    .filter((item) => item.recommendedAction === 'MERGE_EXISTING')
    .length
  lines.push(`reviewRequired: ${reviewRequired}`)
  lines.push(`mergeExisting: ${mergeExisting}`)
  appendTextList(lines, 'next actions', (result.nextActions ?? []).map((item) => redactSecrets(item)))
  return `${lines.join('\n')}\n`
}

function formatCommentPlanPreviewText(result) {
  const summary = result.summary ?? {}
  const executableChangeCount = Number(summary.executableChangeCount ?? 0)
  const dryRunSqlText = typeof result.dryRunSql === 'string' ? result.dryRunSql.trim() : ''
  const hasExecutableDryRunSql = executableChangeCount > 0 && dryRunSqlText !== ''
  const lines = [
    'DataSpec COMMENT patch plan',
    `risk=${redactSecrets(result.riskLevel ?? '-')}`,
    `totalChanges=${executableChangeCount}`,
    `unsupported=${summary.unsupportedCount ?? 0}`,
    `dryRunSql=${hasExecutableDryRunSql ? 'yes' : 'no'}`,
    `metadataFingerprint=${redactSecrets(String(result.metadataFingerprint ?? '-').slice(0, 12))}`,
    `planHash=${redactSecrets(String(result.planHash ?? '-').slice(0, 12))}`
  ]
  if (result.dialectSupport) {
    lines.push(
      `tableCommentSqlSupported=${Boolean(result.dialectSupport.tableCommentSqlSupported)}`,
      `columnCommentSqlSupported=${Boolean(result.dialectSupport.columnCommentSqlSupported)}`
    )
  }
  if (result.rollbackHint) {
    lines.push(`rollbackHint=${redactSecrets(result.rollbackHint)}`)
  }
  appendTextList(lines, 'next actions', (result.nextActions ?? []).map((item) => redactSecrets(item)))
  return `${lines.join('\n')}\n`
}

function formatSessionBootstrapText(bootstrap) {
  const lines = [
    'DataSpec AI Session Bootstrap',
    `status: ${bootstrap.status ?? '-'}`,
    `server: ${bootstrap.server ?? '-'}`,
    `projectId: ${bootstrap.projectId ?? '-'}`,
    `authMode: ${bootstrap.authMode ?? '-'}`,
    `specVersion: ${bootstrap.specVersion ?? '-'}`,
    ''
  ]
  appendTextList(lines, 'recommended commands', bootstrap.recommendedCommands)
  appendTextList(lines, 'known risks', bootstrap.knownRisks)
  if ((bootstrap.checks ?? []).length > 0) {
    lines.push('checks:')
    for (const check of bootstrap.checks) {
      lines.push(`  - [${String(check.status ?? '-').toUpperCase()}] ${check.name ?? '-'}: ${check.message ?? '-'}`)
      if (check.nextAction) {
        lines.push(`    next: ${check.nextAction}`)
      }
    }
  }
  if ((bootstrap.nextActions ?? []).length > 0) {
    lines.push('next actions:')
    for (const action of bootstrap.nextActions) {
      lines.push(`  - ${action.code ?? '-'}: ${action.message ?? '-'}`)
      if (action.command) {
        lines.push(`    ${action.command}`)
      }
    }
  }
  lines.push('')
  return lines.join('\n')
}

function appendTextList(lines, title, values = []) {
  if (!Array.isArray(values) || values.length === 0) {
    return
  }
  lines.push(title + ':')
  for (const value of values) {
    lines.push(`  - ${value}`)
  }
}

function resolveInitDefaultPaths(rawValue, existingPaths = []) {
  const requested = rawValue === undefined
    ? []
    : Array.isArray(rawValue) ? rawValue : [rawValue]
  const source = requested.length > 0
    ? requested
    : existingPaths.length > 0 ? existingPaths : DEFAULT_INIT_PATHS
  const paths = source
    .map((item) => String(item).trim())
    .filter(Boolean)
  if (paths.length === 0) {
    throw new Error('init 至少需要一个 defaultPath')
  }
  return [...new Set(paths)]
}

async function writeInitFile(filePath, content, force) {
  if (await pathExists(filePath) && !force) {
    return { path: filePath, action: 'skipped' }
  }
  await mkdir(path.dirname(filePath), { recursive: true })
  await writeFile(filePath, content, 'utf8')
  return { path: filePath, action: 'written' }
}

async function writeAgentsFragment(rootDir, fragment, force) {
  const agentsPath = path.join(rootDir, 'AGENTS.md')
  const block = `${DATASPEC_AGENTS_START}\n${fragment.trimEnd()}\n${DATASPEC_AGENTS_END}\n`
  const current = await readTextIfExists(agentsPath)
  if (current === null) {
    await writeFile(agentsPath, block, 'utf8')
    return { path: agentsPath, action: 'written' }
  }

  const startIndex = current.indexOf(DATASPEC_AGENTS_START)
  const endIndex = current.indexOf(DATASPEC_AGENTS_END)
  const hasStart = startIndex >= 0
  const hasEnd = endIndex >= 0
  if (hasStart !== hasEnd || (hasStart && endIndex < startIndex)) {
    throw new Error('AGENTS.md 中 DataSpec marker 不完整，请手动修复后重试')
  }
  if (hasStart && !force) {
    return { path: agentsPath, action: 'skipped' }
  }
  const nextContent = hasStart
    ? replaceMarkedBlock(current, startIndex, endIndex, block)
    : appendBlock(current, block)
  await writeFile(agentsPath, nextContent, 'utf8')
  return { path: agentsPath, action: 'written' }
}

function replaceMarkedBlock(content, startIndex, endIndex, block) {
  const before = content.slice(0, startIndex).trimEnd()
  const after = content.slice(endIndex + DATASPEC_AGENTS_END.length).trimStart()
  return joinMarkdownBlocks(before, block.trimEnd(), after)
}

function appendBlock(content, block) {
  return joinMarkdownBlocks(content.trimEnd(), block.trimEnd())
}

function joinMarkdownBlocks(...blocks) {
  return `${blocks.filter(Boolean).join('\n\n')}\n`
}

async function readTextIfExists(filePath) {
  try {
    return await readFile(filePath, 'utf8')
  } catch (error) {
    if (error.code === 'ENOENT') {
      return null
    }
    throw error
  }
}

async function pathExists(filePath) {
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

function renderInitConfig(projectId, server, defaultPaths) {
  return `${JSON.stringify({ projectId, server, defaultPaths }, null, 2)}\n`
}

function renderInitReadme({ projectId, server, defaultPaths }) {
  return `# DataSpec 初始化

本目录由 \`dataspec init\` 生成，供 DataSpec CLI、MCP 和 AI agent 在当前业务仓库读取数据库标准。

## 当前配置

- projectId: ${projectId}
- server: ${server}
- defaultPaths: ${defaultPaths.map((item) => `\`${item}\``).join(', ')}

## 常用命令

如果业务仓库没有 \`tools/dataspec-cli.mjs\`，请把下面示例替换为团队实际使用的 DataSpec CLI 路径或封装脚本。

\`\`\`bash
node tools/dataspec-cli.mjs bootstrap --format json
node tools/dataspec-cli.mjs doctor --format json
node tools/dataspec-cli.mjs changed --format json
node tools/dataspec-cli.mjs lint-changed --format json
node tools/dataspec-cli.mjs install-hook --with-vscode --format json
node tools/dataspec-cli.mjs lint-files --format json
node tools/dataspec-cli.mjs export-context --output dataspec-ai-context.zip
node tools/dataspec-cli.mjs export-context --cache
node tools/dataspec-cli.mjs export-context --scope changed --query 用户订单 --cache
\`\`\`

## Token

不要把明文 API token 写入可提交文件。需要安全模式访问时，优先在本机或 CI 中设置：

\`\`\`bash
export DATASPEC_TOKEN=ds_xxx
\`\`\`

也可以在单次命令中使用 \`--dataspec-token <token>\`。

## 给 AI agent 的约定

- 新会话第一步运行 \`bootstrap --format json\`，读取 projectId、server、authMode、specVersion、availableCapabilities、recommendedCommands、knownRisks 和 nextActions。
- 修改 SQL、migration 或 ORM entity 前，先运行 \`doctor\` 确认 DataSpec 可用。
- 处理本次 git 变更时，先运行 \`changed --format json\` 获取变更文件、SQL 子集和最小 Context 建议。
- 只检查本次 SQL 变更时，运行 \`lint-changed --format json\`；它不会扫描 defaultPaths 之外的大目录。
- 需要提交前自动检查和 VS Code 跳转时，显式运行 \`install-hook --with-vscode --format json\`；该命令不会覆盖非 DataSpec 管理的用户 hook 或编辑器配置。
- 未显式传路径时，\`lint-files\` 会读取 \`.dataspec/config.json\` 的 \`defaultPaths\`。
- 需要完整上下文时，运行 \`export-context --cache\` 并让 AI 读取 \`.dataspec/context/\`；单个建表或修 SQL 任务可加 \`--scope field --query <关键词>\` 导出按需包。
`
}

function renderAgentsFragment({ projectId, server, defaultPaths }) {
  return `# DataSpec 数据库规范

当前仓库已接入 DataSpec project ${projectId}（${server}）。

在创建或修改数据库 schema、SQL migration、ORM entity 或数据字典前：

- 修改后先运行 \`node tools/dataspec-verify-advisor.mjs --changed --format json\` 获取建议验证命令。
- 新会话第一步运行 \`node tools/dataspec-cli.mjs bootstrap --format json\`，读取当前项目、标准版本、可用能力、风险提示和 nextActions。
- 先运行 \`node tools/dataspec-cli.mjs doctor --format json\`。
- 优先运行 \`node tools/dataspec-cli.mjs changed --format json\` 获取本次 git 变更、SQL 子集和最小 Context 建议。
- 只对本次 SQL 变更运行 \`node tools/dataspec-cli.mjs lint-changed --format json\`。
- 对默认路径运行 \`node tools/dataspec-cli.mjs lint-files --format json\`。
- 如果仓库没有 \`tools/dataspec-cli.mjs\`，先替换为团队实际使用的 DataSpec CLI 路径或封装脚本。
- 默认扫描路径：${defaultPaths.map((item) => `\`${item}\``).join(', ')}。
- 安全模式下使用 \`DATASPEC_TOKEN\` 或 \`--dataspec-token\`，不要把明文 token 写入仓库。
- 不确定字段命名时，先用 \`suggest-field\` 或 \`.dataspec/context/\` 中的 AI Context 查找标准字段。`
}

function formatInitText(result) {
  const lines = [
    'DataSpec Init',
    `root: ${result.rootDir}`,
    `config: ${result.configPath}`,
    ''
  ]
  if (result.writtenFiles.length > 0) {
    lines.push('已写入:')
    for (const filePath of result.writtenFiles) {
      lines.push(`  - ${filePath}`)
    }
    lines.push('')
  }
  if (result.skippedFiles.length > 0) {
    lines.push('已跳过:')
    for (const filePath of result.skippedFiles) {
      lines.push(`  - ${filePath}`)
    }
    lines.push('')
  }
  lines.push(formatDoctorText(result.doctor).trimEnd())
  lines.push('', '下一步:', '  - 设置 DATASPEC_TOKEN（如果后端开启安全模式）', '  - node tools/dataspec-cli.mjs lint-files --format json', '')
  return lines.join('\n')
}

function parseProjectId(value) {
  if (!value) {
    throw new Error('需要提供 --project <id> 或 .dataspec/config.json 的 projectId')
  }
  return parsePositiveInteger(value, 'project id')
}

function normalizeSyntheticScenario(value) {
  const scenario = String(value ?? '').trim().toLowerCase()
  const supported = ['user', 'order', 'payment', 'audit']
  if (!supported.includes(scenario)) {
    throw new Error(`synthetic-examples generate 需要 --scenario <${supported.join('|')}>`)
  }
  return scenario
}

function normalizeContractSourceKind(value) {
  const sourceKind = String(value ?? '').trim().toLowerCase()
  const supported = ['openapi', 'json-schema', 'protobuf']
  if (!supported.includes(sourceKind)) {
    throw new Error(`contract-import preview 需要 --source-kind <${supported.join('|')}>`)
  }
  return sourceKind
}

function parseOptionalProjectId(value) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return parsePositiveInteger(value, 'project id')
}

function parsePositiveInteger(value, label) {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`无效 ${label}: ${value}`)
  }
  return parsed
}

function parseBoundedPositiveInteger(value, label, max) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = parsePositiveInteger(value, label)
  if (parsed > max) {
    throw new Error(`${label} 超过安全上限 ${max}: ${value}`)
  }
  return parsed
}

function parseRepository(value) {
  if (!value || !/^[^/\s]+\/[^/\s]+$/.test(value)) {
    throw new Error(`无效 repo: ${value}`)
  }
  return value
}

function parseLimit(value, fallback = 5) {
  if (value === undefined || value === null || value === '') {
    return fallback
  }
  const limit = Number(value)
  if (!Number.isInteger(limit) || limit <= 0) {
    throw new Error(`无效 limit: ${value}`)
  }
  return limit
}

function parseOptionalBoolean(value, label) {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (typeof value === 'boolean') {
    return value
  }
  const normalized = String(value).trim().toLowerCase()
  if (['true', '1', 'yes'].includes(normalized)) {
    return true
  }
  if (['false', '0', 'no'].includes(normalized)) {
    return false
  }
  throw new Error(`无效 ${label}: ${value}`)
}

async function lintSqlFiles(paths, projectId, server, fetchFn, apiToken, profileSelection = {}, idempotencyKey = null) {
  const files = await collectSqlFiles(paths)
  const results = []

  for (const filePath of files) {
    const sql = await readFile(filePath, 'utf8')
    const fileIdempotencyKey = scopedIdempotencyKey(idempotencyKey, filePath)
    const response = await fetchFn(`${server}/api/lint`, {
      method: 'POST',
      headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }, fileIdempotencyKey),
      body: JSON.stringify({ sql, projectId, ...profileSelection })
    })
    const payload = await readJsonResponse(response)
    results.push({
      path: formatOutputPath(filePath),
      result: unwrapResponse(payload)
    })
  }

  return {
    summary: summarizeLintResults(results),
    files: results
  }
}

async function collectSqlFiles(paths) {
  const sqlFiles = []
  for (const inputPath of paths) {
    await collectSqlFilesFromPath(path.resolve(inputPath), sqlFiles)
  }
  return [...new Set(sqlFiles)].sort((left, right) => left.localeCompare(right))
}

async function collectSqlFilesFromPath(inputPath, sqlFiles) {
  const entries = await readdir(inputPath, { withFileTypes: true }).catch(async (error) => {
    if (error.code !== 'ENOTDIR') {
      throw error
    }
    return null
  })
  if (!entries) {
    if (inputPath.toLowerCase().endsWith('.sql')) {
      sqlFiles.push(inputPath)
    }
    return
  }
  for (const entry of entries) {
    const childPath = path.join(inputPath, entry.name)
    if (entry.isDirectory()) {
      if (shouldSkipDirectory(entry.name)) {
        continue
      }
      await collectSqlFilesFromPath(childPath, sqlFiles)
      continue
    }
    if (entry.isFile() && entry.name.toLowerCase().endsWith('.sql')) {
      sqlFiles.push(childPath)
    }
  }
}

function shouldSkipDirectory(name) {
  return SKIPPED_SCAN_DIRECTORIES.has(name)
}

function summarizeLintResults(results) {
  return results.reduce((summary, item) => {
    const result = item.result ?? {}
    const errorCount = Number(result.errorCount ?? 0)
    summary.totalFiles += 1
    summary.failedFiles += errorCount > 0 ? 1 : 0
    summary.errorCount += errorCount
    summary.warningCount += Number(result.warningCount ?? 0)
    summary.suggestionCount += Number(result.suggestionCount ?? 0)
    return summary
  }, {
    totalFiles: 0,
    failedFiles: 0,
    errorCount: 0,
    warningCount: 0,
    suggestionCount: 0
  })
}

async function buildChangedWorkflow(config, options = {}) {
  if (config.defaultPaths.length === 0) {
    return buildChangedPayload({
      config,
      options,
      git: { available: false, rootDir: null },
      files: emptyChangedFiles(),
      diagnostics: [changedWorkflowDiagnostic('DATASPEC_DEFAULT_PATHS_MISSING')]
    })
  }

  const gitRoot = await findGitRoot(config.rootDir)
  if (!gitRoot) {
    return buildChangedPayload({
      config,
      options,
      git: { available: false, rootDir: null },
      files: emptyChangedFiles(),
      diagnostics: [changedWorkflowDiagnostic('NO_GIT_REPOSITORY')]
    })
  }

  const gitPaths = await collectGitChangedPaths(gitRoot, config)
  const files = classifyChangedFiles(gitPaths, gitRoot, config)
  const diagnostics = files.all.length === 0 ? [changedWorkflowDiagnostic('NO_CHANGED_FILES')] : []
  return buildChangedPayload({
    config,
    options,
    git: { available: true, rootDir: gitRoot },
    files,
    diagnostics
  })
}

function buildChangedPayload({ config, options, git, files, diagnostics }) {
  const contextRecommendation = buildChangedContextRecommendation(files, config, options)
  return {
    kind: 'dataspec.changed-workflow',
    schemaVersion: 1,
    config: changedConfigMetadata(config, options),
    git: {
      available: git.available,
      rootDir: git.rootDir ? toPosixPath(path.relative(config.rootDir, git.rootDir) || '.') : null
    },
    files,
    summary: {
      totalFiles: files.all.length,
      sqlFiles: files.sql.length,
      otherFiles: files.other.length,
      ignoredFiles: files.ignored.length,
      defaultPathCount: config.defaultPaths.length
    },
    contextRecommendation,
    diagnostics,
    nextActions: buildChangedNextActions(files, contextRecommendation, diagnostics)
  }
}

function publicChangedWorkflowPayload(output) {
  return {
    ...output,
    files: publicChangedFiles(output.files)
  }
}

function publicLintChangedOutput(lint, changed) {
  return {
    ...lint,
    files: lint.files.map((file, index) => ({
      ...file,
      path: changed.files.sql[index]?.path ?? file.path
    }))
  }
}

function publicChangedFiles(files) {
  return {
    all: files.all.map(publicChangedFileItem),
    sql: files.sql.map(publicChangedFileItem),
    other: files.other.map(publicChangedFileItem),
    ignored: files.ignored.map(publicChangedFileItem)
  }
}

function publicChangedFileItem(file) {
  const { absolutePath, ...publicFile } = file
  return publicFile
}

function changedConfigMetadata(config, options) {
  return {
    configPath: config.configPath ? toPosixPath(path.relative(config.rootDir, config.configPath)) : null,
    rootDir: '.',
    projectId: parseOptionalProjectId(options.project ?? config.projectId) ?? null,
    server: options.server || config.server ? safeServerForMetadata(normalizeServer(options.server ?? config.server)) : null,
    defaultPaths: config.defaultPaths.map((item) => toPosixPath(item)),
    profileId: normalizeOptionalCliText(options.profile ?? config.aiProfile) ?? null,
    taskType: normalizeOptionalCliText(options.taskType ?? options['task-type'] ?? config.taskType) ?? null
  }
}

async function findGitRoot(cwd) {
  try {
    const stdout = await execGit(cwd, ['rev-parse', '--show-toplevel'])
    return path.resolve(stdout.trim())
  } catch {
    return null
  }
}

async function collectGitChangedPaths(gitRoot, config) {
  const defaultPathspecs = gitDefaultPathspecs(gitRoot, config)
  const untrackedArgs = ['ls-files', '-z', '--others', '--exclude-standard']
  if (defaultPathspecs.length > 0) {
    untrackedArgs.push('--', ...defaultPathspecs)
  }
  const [unstaged, staged, untracked] = await Promise.all([
    execGit(gitRoot, ['diff', '--name-only', '-z', '--diff-filter=ACMRT']),
    execGit(gitRoot, ['diff', '--cached', '--name-only', '-z', '--diff-filter=ACMRT']),
    execGit(gitRoot, untrackedArgs)
  ])
  return uniqueSorted([
    ...parseGitPathList(unstaged),
    ...parseGitPathList(staged),
    ...parseGitPathList(untracked)
  ])
}

function gitDefaultPathspecs(gitRoot, config) {
  return resolveDefaultPaths(config)
    .map((defaultPath) => path.relative(gitRoot, defaultPath))
    .filter((relativePath) => relativePath && !relativePath.startsWith('..') && !path.isAbsolute(relativePath))
    .map((relativePath) => toPosixPath(relativePath))
}

async function execGit(cwd, args) {
  const { stdout } = await execFileAsync('git', args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 10 * 1024 * 1024
  })
  return stdout
}

function parseGitPathList(output) {
  return output
    .split('\0')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => item.replaceAll('\\', '/'))
}

function classifyChangedFiles(gitPaths, gitRoot, config) {
  const defaultRoots = resolveDefaultPaths(config)
  const files = emptyChangedFiles()
  for (const gitPath of gitPaths) {
    const absolutePath = path.resolve(gitRoot, gitPath)
    const item = changedFileItem(absolutePath, gitPath, gitRoot, config)
    if (!defaultRoots.some((defaultRoot) => isPathInside(path.resolve(defaultRoot), absolutePath))) {
      files.ignored.push(item)
      continue
    }
    files.all.push(item)
    if (isSqlCandidateFile(item.path)) {
      files.sql.push(item)
    } else {
      files.other.push(item)
    }
  }
  files.all.sort(compareChangedFile)
  files.sql.sort(compareChangedFile)
  files.other.sort(compareChangedFile)
  files.ignored.sort(compareChangedFile)
  return files
}

function changedFileItem(absolutePath, gitPath, gitRoot, config) {
  const pathFromConfigRoot = path.relative(config.rootDir, absolutePath)
  const outputPath = pathFromConfigRoot && !pathFromConfigRoot.startsWith('..') && !path.isAbsolute(pathFromConfigRoot)
    ? pathFromConfigRoot
    : path.relative(gitRoot, absolutePath)
  return {
    path: toPosixPath(outputPath),
    gitPath: toPosixPath(gitPath),
    absolutePath,
    kind: isSqlCandidateFile(outputPath) ? 'sql' : 'other'
  }
}

function emptyChangedFiles() {
  return {
    all: [],
    sql: [],
    other: [],
    ignored: []
  }
}

function emptyLintOutput() {
  return {
    summary: {
      totalFiles: 0,
      failedFiles: 0,
      errorCount: 0,
      warningCount: 0,
      suggestionCount: 0
    },
    files: []
  }
}

function isSqlCandidateFile(filePath) {
  return String(filePath).toLowerCase().endsWith('.sql')
}

function compareChangedFile(left, right) {
  return left.path.localeCompare(right.path)
}

function uniqueSorted(items) {
  return [...new Set(items)].sort((left, right) => left.localeCompare(right))
}

function changedWorkflowDiagnostic(code) {
  const diagnostics = {
    DATASPEC_DEFAULT_PATHS_MISSING: {
      severity: 'WARNING',
      message: '未配置 .dataspec/config.json 的 defaultPaths，已停止以避免扫描整个业务仓库。',
      suggestedAction: '运行 dataspec init --default-path <path>，或在 .dataspec/config.json 中配置 defaultPaths 后重试。'
    },
    NO_GIT_REPOSITORY: {
      severity: 'WARNING',
      message: '当前目录不在 git 仓库内，无法判断本次变更文件。',
      suggestedAction: '切换到业务 git 仓库后重试，或先运行 git init 建立仓库。'
    },
    NO_CHANGED_FILES: {
      severity: 'INFO',
      message: '配置路径内没有检测到 git 变更文件。',
      suggestedAction: '修改 SQL/模型文件后重试，或确认 defaultPaths 是否覆盖本次工作目录。'
    },
    NO_CHANGED_SQL_FILES: {
      severity: 'INFO',
      message: '本次配置路径内没有变更 SQL 文件，已跳过 lint 调用。',
      suggestedAction: '如需检查 SQL，请修改或新增 .sql 文件；如只需上下文，可运行 dataspec changed --format json。'
    }
  }
  return {
    code,
    ...diagnostics[code]
  }
}

function buildChangedContextRecommendation(files, config, options = {}) {
  const query = buildChangedContextQuery(files)
  const commandParts = ['dataspec', 'export-context']
  const projectId = parseOptionalProjectId(options.project ?? config.projectId)
  if (projectId) {
    commandParts.push('--project', String(projectId))
  }
  commandParts.push('--scope', 'changed')
  commandParts.push('--query', quoteCommandArg(query))
  commandParts.push('--cache')
  return {
    scope: 'changed',
    query,
    command: commandParts.join(' '),
    reason: files.all.length > 0
      ? '基于本次 git 变更文件名生成最小上下文检索词。'
      : '暂无变更文件，保留 changed scope 作为恢复后的推荐入口。',
    evidenceFiles: files.all.slice(0, 20).map((item) => item.path)
  }
}

function buildChangedContextQuery(files) {
  const tokens = []
  for (const file of files.all) {
    const parsed = path.posix.parse(file.path)
    tokens.push(...pathTokens(parsed.dir), ...pathTokens(parsed.name))
  }
  const uniqueTokens = uniqueSorted(tokens.filter((item) => item.length >= 2 && !['sql', 'db', 'src'].includes(item)))
  return uniqueTokens.slice(0, 12).join(' ') || 'changed'
}

function pathTokens(value) {
  return String(value ?? '')
    .split(/[^\p{L}\p{N}]+/u)
    .map((item) => item.trim())
    .filter(Boolean)
}

function quoteCommandArg(value) {
  const text = String(value ?? '')
  if (/^[A-Za-z0-9_./:-]+$/.test(text)) {
    return text
  }
  return `"${text.replaceAll('\\', '\\\\').replaceAll('"', '\\"')}"`
}

function buildChangedNextActions(files, contextRecommendation, diagnostics) {
  const codes = diagnostics.map((item) => item.code)
  if (codes.includes('DATASPEC_DEFAULT_PATHS_MISSING')) {
    return [
      '在 .dataspec/config.json 中配置 defaultPaths，或重新运行 dataspec init --default-path <path>。',
      '配置后再次运行 dataspec changed --format json。'
    ]
  }
  if (codes.includes('NO_GIT_REPOSITORY')) {
    return [
      '切换到业务 git 仓库后重新运行 dataspec changed --format json，或先运行 git init。',
      '确认 .dataspec/config.json 位于业务仓库内。'
    ]
  }
  if (codes.includes('NO_CHANGED_FILES')) {
    return [
      '确认本次修改位于 defaultPaths 覆盖范围内。',
      '如刚新增文件，确认文件未被 .gitignore 忽略。'
    ]
  }
  const actions = []
  if (files.sql.length > 0) {
    actions.push('运行 dataspec lint-changed --format json 检查本次变更 SQL。')
  } else {
    actions.push('本次没有 SQL 变更；可先根据文件名判断是否需要导出最小上下文。')
  }
  actions.push(`运行 ${contextRecommendation.command} 获取最小 AI Context。`)
  return actions
}

function buildLintChangedNextActions(changed, lint, diagnostics) {
  const codes = diagnostics.map((item) => item.code)
  if (codes.includes('NO_CHANGED_SQL_FILES')) {
    return [
      '运行 dataspec changed --format json 查看本次非 SQL 变更和最小上下文建议。',
      `如需要字段标准上下文，运行 ${changed.contextRecommendation.command}。`
    ]
  }
  if (diagnostics.length > 0) {
    return changed.nextActions
  }
  if (lint.summary.errorCount > 0) {
    return [
      '优先修复 ERROR 级 SQL 标准问题。',
      `需要相关字段标准时运行 ${changed.contextRecommendation.command}。`
    ]
  }
  return [
    'SQL 变更未发现 ERROR 级问题。',
    `需要继续给 AI 补上下文时运行 ${changed.contextRecommendation.command}。`
  ]
}

function formatLintChangedText(output) {
  const lines = ['DataSpec Lint Changed']
  lines.push(`Changed files: ${output.summary.changedFiles}`)
  lines.push(`Changed SQL files: ${output.summary.changedSqlFiles}`)
  lines.push(`Failed files: ${output.summary.failedFiles}`)
  lines.push(`ERROR: ${output.summary.errorCount}`)
  lines.push(`WARNING: ${output.summary.warningCount}`)
  lines.push(`SUGGESTION: ${output.summary.suggestionCount}`)

  const issueLines = buildLintChangedIssueLines(output.lint.files)
  if (issueLines.length > 0) {
    lines.push('', 'Issues:', ...issueLines)
  }

  if (output.diagnostics.length > 0) {
    lines.push('', 'Diagnostics:')
    for (const diagnostic of output.diagnostics) {
      lines.push(`${diagnostic.severity ?? 'INFO'} ${diagnostic.code}: ${oneLine(diagnostic.message)}`)
      if (diagnostic.suggestedAction) {
        lines.push(`next: ${oneLine(diagnostic.suggestedAction)}`)
      }
    }
  }

  lines.push('', 'Next actions:')
  for (const action of output.nextActions) {
    lines.push(`- ${oneLine(action)}`)
  }
  lines.push('')
  return lines.join('\n')
}

function buildLintChangedIssueLines(files) {
  const lines = []
  for (const file of files ?? []) {
    for (const issue of file.result?.issues ?? []) {
      lines.push(formatLintChangedIssueLine(file.path, issue))
    }
  }
  return lines
}

function formatLintChangedIssueLine(filePath, issue) {
  const line = parseOptionalNumber(issue.line) ?? parseOptionalNumber(issue.startLine) ?? 1
  const column = parseOptionalNumber(issue.column) ?? parseOptionalNumber(issue.startColumn) ?? 1
  const severity = oneLine(issue.severity ?? 'INFO').toUpperCase()
  const rule = oneLine(issue.ruleCode ?? issue.rule ?? issue.code ?? 'dataspec_sql_issue')
  const message = oneLine(issue.message ?? issue.description ?? 'SQL 标准问题')
  const suggestion = oneLine(issue.suggestion ?? issue.suggestedAction ?? issue.replacement ?? '')
  const suggestionText = suggestion ? ` suggestion: ${suggestion}` : ''
  return `${toPosixPath(filePath)}:${line}:${column}: ${severity} ${rule} - ${message}${suggestionText}`
}

function formatChangedText(output) {
  const lines = ['DataSpec Changed Workflow']
  lines.push(`Changed files: ${output.summary.totalFiles}`)
  lines.push(`SQL files: ${output.summary.sqlFiles}`)
  lines.push(`Ignored outside defaultPaths: ${output.summary.ignoredFiles}`)
  if (output.diagnostics.length > 0) {
    lines.push('', 'Diagnostics:')
    for (const diagnostic of output.diagnostics) {
      lines.push(`- [${diagnostic.severity}] ${diagnostic.code}: ${diagnostic.message}`)
      lines.push(`  next: ${diagnostic.suggestedAction}`)
    }
  }
  if (output.files.sql.length > 0) {
    lines.push('', 'SQL:')
    for (const file of output.files.sql) {
      lines.push(`- ${file.path}`)
    }
  }
  lines.push('', 'Context:')
  lines.push(`- ${output.contextRecommendation.command}`)
  lines.push('', 'Next actions:')
  for (const action of output.nextActions) {
    lines.push(`- ${action}`)
  }
  lines.push('')
  return lines.join('\n')
}

function oneLine(value) {
  return String(sanitizeSecretText(value) ?? '')
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function resolveDeliveryPackagePath(options) {
  if (options['delivery-package'] && options['batch-package']) {
    throw new Error('请只使用 --delivery-package 或 --batch-package 之一')
  }
  return options['delivery-package'] ?? options['batch-package']
}

async function writeDeliveryPackage(outputPath, deliveryPackage) {
  await mkdir(path.dirname(path.resolve(outputPath)), { recursive: true })
  await writeFile(outputPath, `${JSON.stringify(deliveryPackage, null, 2)}\n`, 'utf8')
}

function resolveOutputInsideCwd(outputPath, cwd) {
  if (!outputPath) {
    throw new Error('缺少输出路径')
  }
  const baseDir = path.resolve(cwd)
  const target = path.resolve(baseDir, outputPath)
  if (!isPathInside(baseDir, target)) {
    throw new Error(`输出路径越界: ${outputPath}`)
  }
  return target
}

function resolveFixedSqlPatchInputPath(inputPath, cwd, label) {
  if (!inputPath) {
    throw new Error(`fixed-sql patch 需要提供 --${label} <path>`)
  }
  try {
    return resolveOutputInsideCwd(inputPath, cwd)
  } catch (error) {
    if (/输出路径越界/.test(error.message)) {
      throw new Error(`fixed-sql patch ${label} 路径必须位于当前工作目录: ${inputPath}`)
    }
    throw error
  }
}

async function resolveFixedSqlPatchExistingPath(inputPath, cwd, label) {
  const resolvedPath = resolveFixedSqlPatchInputPath(inputPath, cwd, label)
  const entry = await lstat(resolvedPath)
  if (entry.isSymbolicLink()) {
    throw new Error(`fixed-sql patch ${label} 不允许使用符号链接: ${inputPath}`)
  }
  const baseDir = await realpath(cwd)
  const realTarget = await realpath(resolvedPath)
  if (!isPathInside(baseDir, realTarget)) {
    throw new Error(`fixed-sql patch ${label} 路径必须位于当前工作目录: ${inputPath}`)
  }
  return realTarget
}

async function writeFixedSqlPatchTarget({ cwd, targetInputPath, expectedPath, expectedSha256, fixedSql }) {
  const targetPath = await resolveFixedSqlPatchExistingPath(targetInputPath, cwd, 'target')
  if (targetPath !== expectedPath) {
    throw new DataSpecCliError('fixed-sql patch apply 目标路径在确认后发生变化，未写入目标文件。', {
      code: 'FIXED_SQL_PATCH_TARGET_CHANGED',
      category: 'VALIDATION',
      retryable: false,
      suggestedAction: '重新运行 dry-run，确认目标路径和 planHash 后再 apply。'
    })
  }
  const file = await open(targetPath, 'r+')
  try {
    const currentContent = await file.readFile({ encoding: 'utf8' })
    if (sha256Hex(currentContent) !== expectedSha256) {
      throw new DataSpecCliError('fixed-sql patch apply 写入前检测到目标文件已变化，未写入目标文件。', {
        code: 'FIXED_SQL_PATCH_TARGET_DRIFTED_BEFORE_WRITE',
        category: 'VALIDATION',
        retryable: false,
        suggestedAction: '重新运行 dry-run，审查最新 diff 和 planHash 后再 apply。'
      })
    }
    await file.truncate(0)
    await file.write(fixedSql, 0, 'utf8')
  } finally {
    await file.close()
  }
}

function parseFixedSqlPatchLintResult(content, sourcePath) {
  try {
    return JSON.parse(content)
  } catch (error) {
    throw new Error(`fixed-sql patch 无法解析 lint-result JSON: ${sourcePath ?? '<memory>'}`)
  }
}

/**
 * 构建 fixedSql 文件补丁计划。
 *
 * 该函数的返回值会被 CLI、AI agent 和人工确认流程读取，因此只暴露相对路径、hash、
 * 脱敏 diff 与下一步动作；真实 fixedSql 仅在显式 apply 且确认 hash 匹配后写入文件。
 */
export function buildFixedSqlPatchPlan({ cwd, targetPath, targetContent, lintResult, confirm = null, lintResultPath = '<json>' }) {
  const target = normalizeFixedSqlPatchTarget(cwd, targetPath)
  const lintResultCommandPath = lintResultPath === '<json>'
    ? '<json>'
    : normalizeFixedSqlPatchRelativePath(cwd, lintResultPath, 'lint-result').relativePath
  const patchSource = selectFixedSqlPatchSource(lintResult, {
    cwd,
    targetPath: target.absolutePath,
    targetContent
  })
  const currentFileSha256 = sha256Hex(targetContent)
  const lintOriginalSha256 = sha256Hex(patchSource.originalSql)
  const fixedSqlSha256 = sha256Hex(patchSource.fixedSql)
  const rawUnifiedDiff = buildUnifiedDiff(target.relativePath, patchSource.originalSql, patchSource.fixedSql)
  const unifiedDiff = sanitizeSecretText(rawUnifiedDiff)
  const planHash = createFixedSqlPatchPlanHash({
    targetPath: target.relativePath,
    currentFileSha256,
    lintOriginalSha256,
    fixedSqlSha256,
    unifiedDiffSha256: sha256Hex(rawUnifiedDiff)
  })
  const conflictWarnings = []
  const noChange = targetContent === patchSource.fixedSql
  const currentMatchesLintOriginal = targetContent === patchSource.originalSql

  if (!currentMatchesLintOriginal && !noChange) {
    conflictWarnings.push({
      code: 'TARGET_CONTENT_DRIFT',
      message: '目标文件当前内容与 lint 结果中的原始 SQL 不一致，已阻断自动写入。',
      nextAction: '请重新运行 SQL lint，或人工检查文件变化后再生成补丁计划。'
    })
  }
  if (confirm && confirm !== planHash) {
    conflictWarnings.push({
      code: 'CONFIRM_HASH_MISMATCH',
      message: '传入的确认 hash 与当前补丁计划不匹配，已阻断自动写入。',
      nextAction: '请重新 dry-run 并使用最新 planHash 确认。'
    })
  }

  const status = noChange
    ? 'NO_CHANGE'
    : conflictWarnings.length > 0
      ? 'CONFLICT'
      : 'READY'
  const applyCommand = status === 'READY'
    ? `node tools/dataspec-cli.mjs fixed-sql patch --lint-result ${formatCliArgument(lintResultCommandPath)} --target ${formatCliArgument(target.relativePath)} --apply --confirm ${planHash} --format json`
    : null

  return {
    kind: 'dataspec.fixed-sql.patch-plan',
    schemaVersion: 1,
    targetPath: target.relativePath,
    dryRunResult: {
      status,
      willWrite: false,
      confirmed: Boolean(confirm) && confirm === planHash,
      currentFileMatchesLintOriginal: currentMatchesLintOriginal,
      requiresConfirmation: status === 'READY'
    },
    unifiedDiff,
    conflictWarnings,
    planHash,
    currentFileSha256,
    lintOriginalSha256,
    fixedSqlSha256,
    applyCommand,
    rollbackHint: {
      targetPath: target.relativePath,
      originalSha256: currentFileSha256,
      hint: '如需回退，请从版本控制或 dry-run diff 恢复原文件内容。'
    },
    evidenceRef: `fixed-sql-patch:${planHash.slice(0, 12)}`,
    safety: {
      readOnly: false,
      writesProject: true,
      requiresDryRun: true,
      requiresExplicitConfirmation: true,
      requiresIdempotencyKey: false,
      sensitiveOutputPolicy: 'hash-and-redacted-diff-only'
    },
    nextActions: buildFixedSqlPatchNextActions(status, applyCommand)
  }
}

function normalizeFixedSqlPatchTarget(cwd, targetPath) {
  return normalizeFixedSqlPatchRelativePath(cwd, targetPath, 'target')
}

function normalizeFixedSqlPatchRelativePath(cwd, filePath, label) {
  const absolutePath = resolveFixedSqlPatchInputPath(filePath, cwd, label)
  const relativePath = path.relative(path.resolve(cwd), absolutePath)
  if (!relativePath) {
    throw new Error(`fixed-sql patch ${label} 必须指向当前工作目录内的文件`)
  }
  return {
    absolutePath,
    relativePath: toPosixPath(relativePath)
  }
}

function selectFixedSqlPatchSource(lintResult, { cwd, targetPath, targetContent }) {
  const root = unwrapFixedSqlPatchPayload(lintResult)
  const fileItems = Array.isArray(root?.files) ? root.files : []
  if (fileItems.length > 0) {
    return selectFixedSqlPatchSourceFromFiles(fileItems, { cwd, targetPath, targetContent })
  }
  const direct = fixedSqlPatchSourceFromObject(root, targetContent)
  if (direct) {
    return direct
  }
  const nested = fixedSqlPatchSourceFromObject(root?.lintResult, targetContent)
  if (nested) {
    return nested
  }

  throw new Error('fixed-sql patch 需要 lint-result 中包含 fixedSql；请先运行支持 fixedSql dry-run 的 lint 命令。')
}

function selectFixedSqlPatchSourceFromFiles(fileItems, { cwd, targetPath, targetContent }) {
  const matchedItems = fileItems.filter((item) => fixedSqlPatchPathMatches(item?.path, targetPath, cwd))
  if (matchedItems.length > 1) {
    throw new Error('fixed-sql patch 无法唯一定位目标文件的 fixedSql，请提供单文件 lint 结果或匹配的 files[].path')
  }
  if (matchedItems.length === 1) {
    const item = matchedItems[0]
    const source = fixedSqlPatchSourceFromObject(item.result ?? item, targetContent)
    if (source) {
      return source
    }
    throw new Error('fixed-sql patch 匹配的 files[] 目标文件缺少 fixedSql')
  }
  if (fileItems.some((item) => hasFixedSqlPatchCandidate(item?.result ?? item))) {
    throw new Error('fixed-sql patch lint-result files[].path 未匹配目标文件，请确认 --target 与 lint 结果来自同一 SQL 文件。')
  }
  throw new Error('fixed-sql patch 需要 lint-result 中包含 fixedSql；请先运行支持 fixedSql dry-run 的 lint 命令。')
}

function unwrapFixedSqlPatchPayload(payload) {
  let current = payload
  for (let depth = 0; depth < 3; depth += 1) {
    if (current?.data && typeof current.data === 'object' && !Array.isArray(current.data)) {
      current = current.data
      continue
    }
    return current
  }
  return current
}

function fixedSqlPatchSourceFromObject(value, targetContent) {
  if (!value || typeof value !== 'object' || typeof value.fixedSql !== 'string') {
    return null
  }
  return {
    originalSql: resolveFixedSqlPatchOriginalSql(value, targetContent),
    fixedSql: value.fixedSql
  }
}

function resolveFixedSqlPatchOriginalSql(value, targetContent) {
  if (typeof value.originalSql === 'string') {
    return value.originalSql
  }
  if (typeof value.sql === 'string') {
    return value.sql
  }
  const originalSha256 = ['originalSqlSha256', 'sqlSha256', 'sourceSqlSha256', 'currentFileSha256']
    .map((key) => value[key])
    .find((item) => typeof item === 'string' && /^[a-f0-9]{64}$/i.test(item))
  if (originalSha256) {
    if (sha256Hex(targetContent) === originalSha256.toLowerCase()) {
      return targetContent
    }
    throw new Error('fixed-sql patch lint-result 中的原始 SQL hash 与目标文件当前内容不匹配。')
  }
  throw new Error('fixed-sql patch 需要 lint-result 包含 originalSql 或 sql，或提供与目标内容匹配的 originalSqlSha256。')
}

function hasFixedSqlPatchCandidate(value) {
  return Boolean(value && typeof value === 'object' && typeof value.fixedSql === 'string')
}

function fixedSqlPatchPathMatches(candidatePath, targetPath, cwd) {
  if (!candidatePath) {
    return false
  }
  try {
    return path.resolve(cwd, candidatePath) === path.resolve(targetPath)
  } catch {
    return false
  }
}

function createFixedSqlPatchPlanHash(input) {
  return createHash('sha256')
    .update(JSON.stringify({
      schemaVersion: 1,
      targetPath: input.targetPath,
      currentFileSha256: input.currentFileSha256,
      lintOriginalSha256: input.lintOriginalSha256,
      fixedSqlSha256: input.fixedSqlSha256,
      unifiedDiffSha256: input.unifiedDiffSha256
    }))
    .digest('hex')
}

function buildUnifiedDiff(filePath, originalContent, fixedContent) {
  if (originalContent === fixedContent) {
    return ''
  }
  const originalLines = splitDiffLines(originalContent)
  const fixedLines = splitDiffLines(fixedContent)
  const lines = [
    `--- a/${filePath}`,
    `+++ b/${filePath}`,
    `@@ -1,${Math.max(originalLines.length, 1)} +1,${Math.max(fixedLines.length, 1)} @@`
  ]
  for (const line of originalLines) {
    lines.push(`-${line}`)
  }
  for (const line of fixedLines) {
    lines.push(`+${line}`)
  }
  return `${lines.join('\n')}\n`
}

function splitDiffLines(content) {
  const lines = String(content).split('\n')
  if (lines.length > 1 && lines.at(-1) === '') {
    lines.pop()
  }
  return lines
}

function buildFixedSqlPatchNextActions(status, applyCommand) {
  if (status === 'READY') {
  return [
    '人工审查 unifiedDiff，确认目标文件和 SQL 变更范围。',
    `确认后运行: ${applyCommand}`
  ]
  }
  if (status === 'NO_CHANGE') {
    return ['fixedSql 与目标文件当前内容一致，无需写入。']
  }
  return [
    '不要 apply 当前计划。',
    '重新运行 SQL lint 或手工处理冲突后再生成补丁计划。'
  ]
}

function formatCliArgument(value) {
  const text = String(value)
  return /^[A-Za-z0-9_./:<>{}-]+$/.test(text) ? text : JSON.stringify(text)
}

function resolveTaskCardOutputPath(outputPath, cwd) {
  try {
    return resolveOutputInsideCwd(outputPath, cwd)
  } catch (error) {
    if (/输出路径越界/.test(error.message)) {
      throw new Error(`输出路径必须位于当前工作目录: ${outputPath}`)
    }
    throw error
  }
}

function resolveTaskCardInputPath(inputPath, cwd) {
  if (!inputPath) {
    throw new Error('task-card 需要提供 --file <path>')
  }
  return resolveOutputInsideCwd(inputPath, cwd)
}

function parseTaskCardInputs(inputValues) {
  const result = {}
  for (const item of Array.isArray(inputValues) ? inputValues : inputValues ? [inputValues] : []) {
    const index = String(item).indexOf('=')
    if (index <= 0) {
      throw new Error(`task-card --input 需要使用 key=value: ${item}`)
    }
    const key = String(item).slice(0, index).trim()
    const value = String(item).slice(index + 1).trim()
    if (!key) {
      throw new Error(`task-card --input key 不能为空: ${item}`)
    }
    result[key] = value
  }
  return result
}

function ensureTrailingNewline(content) {
  return content.endsWith('\n') ? content : `${content}\n`
}

function buildLintFilesDeliveryPackage(lintOutput, projectId) {
  const items = (lintOutput.files ?? []).map((file) => toDeliveryPackageItem(file))
  const summary = buildDeliverySummary(items)
  return {
    packageVersion: 'ai-batch-delivery@1',
    batchId: buildLocalBatchId(projectId, items, summary),
    projectId,
    batchType: 'SQL_LINT',
    source: 'cli',
    status: 'SUCCESS',
    summary,
    items,
    issueSummary: buildDeliveryIssueSummary(items),
    fixedSqlSummary: buildDeliveryFixedSqlSummary(items),
    unmanagedHints: [],
    evidence: [
      { kind: 'batchType', name: '任务类型', value: 'SQL_LINT' },
      { kind: 'source', name: '任务来源', value: 'cli' },
      { kind: 'summary', name: 'SQL 文件数量', value: String(summary.totalItems) }
    ],
    nextActions: buildDeliveryNextActions(summary),
    createdAt: new Date().toISOString()
  }
}

function toDeliveryPackageItem(file) {
  const result = file.result ?? {}
  return {
    itemName: path.basename(file.path ?? ''),
    filePath: sanitizeSecretText(file.path),
    status: 'SUCCESS',
    errorCount: Number(result.errorCount ?? 0),
    warningCount: Number(result.warningCount ?? 0),
    suggestionCount: Number(result.suggestionCount ?? 0),
    suppressedCount: Number(result.suppressedCount ?? 0),
    fixedSqlAvailable: Boolean(result.fixedSql),
    fixedSql: sanitizeSecretText(result.fixedSql),
    fixedSqlDiff: sanitizeSecretText(result.fixedSqlDiff),
    issues: sanitizeSecretValue(result.issues ?? []),
    dialectDiagnostics: sanitizeSecretValue(result.dialectDiagnostics ?? []),
    sqlCheckRecordId: result.sqlCheckRecordId ?? null,
    errorMessage: sanitizeSecretText(result.errorMessage)
  }
}

function buildDeliverySummary(items) {
  const failedItems = items.filter((item) => item.status === 'FAILED').length
  return {
    totalItems: items.length,
    successItems: items.length - failedItems,
    failedItems,
    errorCount: items.reduce((sum, item) => sum + Number(item.errorCount ?? 0), 0),
    warningCount: items.reduce((sum, item) => sum + Number(item.warningCount ?? 0), 0),
    suggestionCount: items.reduce((sum, item) => sum + Number(item.suggestionCount ?? 0), 0),
    fixedSqlCount: items.filter((item) => item.fixedSqlAvailable).length
  }
}

function buildDeliveryIssueSummary(items) {
  const byRule = new Map()
  for (const item of items) {
    for (const issue of Array.isArray(item.issues) ? item.issues : []) {
      const key = issue.ruleCode || 'unknown'
      const current = byRule.get(key) ?? { ruleCode: issue.ruleCode, ruleName: issue.ruleName, count: 0 }
      current.count += 1
      byRule.set(key, current)
    }
  }
  return {
    errorCount: items.reduce((sum, item) => sum + Number(item.errorCount ?? 0), 0),
    warningCount: items.reduce((sum, item) => sum + Number(item.warningCount ?? 0), 0),
    suggestionCount: items.reduce((sum, item) => sum + Number(item.suggestionCount ?? 0), 0),
    byRule: [...byRule.values()].sort((left, right) => right.count - left.count)
  }
}

function buildDeliveryFixedSqlSummary(items) {
  return {
    availableCount: items.filter((item) => item.fixedSqlAvailable).length,
    changedCount: items.filter((item) => item.fixedSqlAvailable && item.fixedSqlDiff).length
  }
}

function buildDeliveryNextActions(summary) {
  const actions = []
  if (summary.failedItems > 0) {
    actions.push('查看失败项 errorMessage，修正输入后缩小范围重试')
  }
  if (summary.errorCount > 0) {
    actions.push('优先修复 ERROR 级 SQL 标准问题')
  }
  if (summary.fixedSqlCount > 0) {
    actions.push('人工确认 fixedSql 后再应用到业务仓库')
  }
  return actions.length > 0 ? actions : ['无需处理']
}

function buildLocalBatchId(projectId, items, summary) {
  const hash = createHash('sha256')
    .update(JSON.stringify({ projectId, files: items.map((item) => item.filePath), summary }))
    .digest('hex')
    .slice(0, 12)
  return `local-${hash}`
}

const QUERY_TOKEN_EVIDENCE_KINDS = new Set(['WORD', 'ACRONYM', 'NUMBER', 'UNIT', 'HAN'])
const QUERY_TOKEN_EVIDENCE_STATUSES = new Set(['RESOLVED', 'AMBIGUOUS', 'DISABLED', 'UNRESOLVED'])

function sanitizeSecretValue(value) {
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeSecretValue(item))
  }
  if (value && typeof value === 'object') {
    const queryTokenEvidence = isQueryTokenEvidence(value)
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key,
      isSensitiveSecretKey(key) && !(queryTokenEvidence && isQueryTokenEvidenceTextKey(key))
        ? '***'
        : sanitizeSecretValue(item)
    ]))
  }
  return value
}

function isQueryTokenEvidence(value) {
  return typeof value.token === 'string' &&
    typeof value.normalizedToken === 'string' &&
    QUERY_TOKEN_EVIDENCE_KINDS.has(value.tokenKind) &&
    QUERY_TOKEN_EVIDENCE_STATUSES.has(value.resolutionStatus)
}

function isQueryTokenEvidenceTextKey(key) {
  // token 在该契约中是命名证据而非凭据；只保留字段结构，字段值仍递归脱敏。
  return key === 'token' || key === 'normalizedToken'
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
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

function isSensitiveSecretKey(key) {
  const normalized = String(key ?? '').replace(/[^A-Za-z0-9]/g, '').toLowerCase()
  return [
    'password',
    'passwd',
    'pwd',
    'token',
    'apitoken',
    'dataspectoken',
    'apikey',
    'authorization',
    'secret',
    'clientsecret',
    'accesstoken',
    'refreshtoken',
    'plaintoken',
    'tokenhash',
    'jdbcurl',
    'connectionstring',
    'dsn'
  ].includes(normalized) ||
    normalized.endsWith('token') ||
    normalized.endsWith('secret') ||
    normalized.includes('apikey') ||
    normalized.includes('password') ||
    normalized.includes('authorization') ||
    normalized.includes('tokenhash') ||
    normalized.includes('connectionstring') ||
    normalized.includes('jdbcurl') ||
    normalized === 'dsn' ||
    normalized.endsWith('dsn')
}

function formatLintText(result) {
  const lines = [
    'DataSpec Lint',
    `ERROR ${Number(result.errorCount ?? 0)} / WARNING ${Number(result.warningCount ?? 0)} / SUGGESTION ${Number(result.suggestionCount ?? 0)}`
  ]
  const diagnostics = Array.isArray(result.dialectDiagnostics) ? result.dialectDiagnostics : []
  if (diagnostics.length > 0) {
    lines.push('', `Dialect: ${formatDialectSummary(diagnostics)}`)
    for (const diagnostic of diagnostics) {
      if (!['WARNING', 'PARTIAL', 'UNSUPPORTED'].includes(diagnostic.level ?? '')) {
        continue
      }
      lines.push(`- [${diagnostic.level}] ${diagnostic.code}: ${diagnostic.message}`)
      if (diagnostic.nextAction) {
        lines.push(`  next: ${diagnostic.nextAction}`)
      }
    }
  }
  const issues = Array.isArray(result.issues) ? result.issues : []
  if (issues.length > 0) {
    lines.push('', 'Issues:')
    for (const issue of issues.slice(0, 20)) {
      lines.push(formatIssueMarkdown(issue))
    }
    if (issues.length > 20) {
      lines.push(`- 其余 ${issues.length - 20} 个问题已省略，请使用 --format json 查看完整结果。`)
    }
  }
  if (result.fixedSql) {
    lines.push('', 'fixedSql: available')
  }
  lines.push('')
  return lines.join('\n')
}

function formatDialectSummary(diagnostics) {
  const first = diagnostics.find((item) => item.dialect)
  const dialect = first?.dialect === 'mysql'
    ? 'MySQL'
    : first?.dialect === 'postgresql'
      ? 'PostgreSQL'
      : 'unknown'
  const riskCount = diagnostics.filter((item) => ['WARNING', 'PARTIAL', 'UNSUPPORTED'].includes(item.level ?? '')).length
  return riskCount > 0 ? `${dialect} (${riskCount} compatibility notes)` : `${dialect}`
}

function formatOutputPath(filePath) {
  const relativePath = path.relative(process.cwd(), filePath)
  const outputPath = relativePath && !relativePath.startsWith('..') && !path.isAbsolute(relativePath)
    ? relativePath
    : filePath
  return outputPath.replaceAll(path.sep, '/')
}

function formatInputSourcePath(filePath, cwd) {
  const relativePath = path.relative(cwd, filePath)
  const sourcePath = relativePath && !relativePath.startsWith('..') && !path.isAbsolute(relativePath)
    ? relativePath
    : filePath
  return sourcePath.replaceAll(path.sep, '/')
}

async function upsertPullRequestComment({ repo, prNumber, token, githubApi, body, fetchFn }) {
  const repoPath = repo.split('/').map(encodeURIComponent).join('/')
  const listUrl = `${githubApi}/repos/${repoPath}/issues/${encodeURIComponent(prNumber)}/comments?per_page=100`
  const commentsResponse = await fetchFn(listUrl, {
    method: 'GET',
    headers: githubHeaders(token)
  })
  const comments = await readGithubJsonResponse(commentsResponse)
  const existing = Array.isArray(comments)
    ? comments.find((comment) => comment.body?.includes(DATASPEC_REVIEW_MARKER))
    : null
  if (existing?.id) {
    const response = await fetchFn(`${githubApi}/repos/${repoPath}/issues/comments/${encodeURIComponent(existing.id)}`, {
      method: 'PATCH',
      headers: githubHeaders(token),
      body: JSON.stringify({ body })
    })
    await readGithubJsonResponse(response)
    return 'updated'
  }

  const response = await fetchFn(`${githubApi}/repos/${repoPath}/issues/${encodeURIComponent(prNumber)}/comments`, {
    method: 'POST',
    headers: githubHeaders(token),
    body: JSON.stringify({ body })
  })
  await readGithubJsonResponse(response)
  return 'created'
}

async function publishInlineReviewComments({ repo, prNumber, token, githubApi, lintOutput, fetchFn }) {
  const repoPath = repo.split('/').map(encodeURIComponent).join('/')
  const pull = await fetchGithubJson(
    `${githubApi}/repos/${repoPath}/pulls/${encodeURIComponent(prNumber)}`,
    token,
    fetchFn
  )
  const commitId = pull?.head?.sha
  if (!commitId) {
    throw new Error('GitHub PR 响应缺少 head.sha；请检查 repo/pr 是否有效')
  }
  const prFiles = await fetchGithubJson(
    `${githubApi}/repos/${repoPath}/pulls/${encodeURIComponent(prNumber)}/files?per_page=100`,
    token,
    fetchFn
  )
  const existingComments = await fetchGithubJson(
    `${githubApi}/repos/${repoPath}/pulls/${encodeURIComponent(prNumber)}/comments?per_page=100`,
    token,
    fetchFn
  )
  const plan = buildInlineReviewPlan(lintOutput, Array.isArray(prFiles) ? prFiles : [], Array.isArray(existingComments) ? existingComments : [])
  const created = []
  for (const comment of plan.comments) {
    const response = await fetchFn(`${githubApi}/repos/${repoPath}/pulls/${encodeURIComponent(prNumber)}/comments`, {
      method: 'POST',
      headers: githubHeaders(token),
      body: JSON.stringify({
        body: comment.body,
        commit_id: commitId,
        path: comment.path,
        line: comment.line,
        side: 'RIGHT'
      })
    })
    await readGithubJsonResponse(response)
    created.push(comment)
  }
  return {
    ...plan,
    created,
    summary: {
      inlineCommentsCreated: created.length,
      inlineCommentsSkipped: plan.skipped.length,
      fallbackIssues: plan.fallbackIssues.length,
      fallbackReasons: summarizeFallbackReasons(plan.fallbackIssues)
    }
  }
}

async function fetchGithubJson(url, token, fetchFn) {
  const response = await fetchFn(url, {
    method: 'GET',
    headers: githubHeaders(token)
  })
  return await readGithubJsonResponse(response)
}

function githubHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    Accept: 'application/vnd.github+json',
    'Content-Type': 'application/json',
    'X-GitHub-Api-Version': '2022-11-28'
  }
}

async function readGithubJsonResponse(response) {
  if (!response.ok) {
    throw new Error(`GitHub 请求失败，HTTP ${response.status}；请检查 GitHub token、repo、pr 或权限`)
  }
  return await response.json()
}

function buildReviewMarkdown(lintOutput, inlineSummary = emptyInlineResult().summary) {
  const { summary, files } = lintOutput
  const status = summary.failedFiles > 0
    ? `发现 ${summary.failedFiles} 个 SQL 文件存在 ERROR`
    : '未发现 ERROR'
  const lines = [
    DATASPEC_REVIEW_MARKER,
    '## DataSpec SQL Review',
    '',
    `**状态**：${status}`,
    '',
    '| 指标 | 数量 |',
    '|---|---:|',
    `| SQL 文件 | ${summary.totalFiles} |`,
    `| ERROR 文件 | ${summary.failedFiles} |`,
    `| ERROR | ${summary.errorCount} |`,
    `| WARNING | ${summary.warningCount} |`,
    `| SUGGESTION | ${summary.suggestionCount} |`,
    `| Inline 评论 | ${inlineSummary.inlineCommentsCreated} |`,
    `| Inline 跳过 | ${inlineSummary.inlineCommentsSkipped} |`,
    `| Fallback 问题 | ${inlineSummary.fallbackIssues} |`,
    ''
  ]

  if (inlineSummary.fallbackReasons.length > 0) {
    lines.push('### Inline fallback', '')
    for (const reason of inlineSummary.fallbackReasons) {
      lines.push(`- ${reason.reason}: ${reason.count}`)
    }
    lines.push('')
  }

  if (files.length > 0) {
    lines.push('### 文件概览', '')
    lines.push('| 文件 | ERROR | WARNING | SUGGESTION |')
    lines.push('|---|---:|---:|---:|')
    for (const item of files) {
      const result = item.result ?? {}
      lines.push(`| \`${escapeTableCell(item.path)}\` | ${Number(result.errorCount ?? 0)} | ${Number(result.warningCount ?? 0)} | ${Number(result.suggestionCount ?? 0)} |`)
    }
    lines.push('')
  }

  const issueLines = buildIssueMarkdownLines(files)
  if (issueLines.length > 0) {
    lines.push('### 问题明细', '')
    lines.push(...issueLines)
  }

  lines.push('> 该评论由 DataSpec CLI 自动生成。')
  return lines.join('\n')
}

function buildIssueMarkdownLines(files) {
  const lines = []
  let issueCount = 0
  const maxIssues = 50
  for (const item of files) {
    const issues = item.result?.issues ?? []
    if (issues.length === 0) {
      continue
    }
    lines.push(`#### \`${item.path}\``)
    for (const issue of issues) {
      if (issueCount >= maxIssues) {
        lines.push(`- 其余问题已省略，请查看 CLI JSON 输出。`)
        return [...lines, '']
      }
      lines.push(formatIssueMarkdown(issue))
      issueCount += 1
    }
    lines.push('')
  }
  return lines
}

function hasReviewIssues(lintOutput) {
  return lintOutput.files.some((file) => (file.result?.issues ?? []).length > 0)
}

function emptyInlineResult() {
  return {
    comments: [],
    skipped: [],
    fallbackIssues: [],
    created: [],
    summary: {
      inlineCommentsCreated: 0,
      inlineCommentsSkipped: 0,
      fallbackIssues: 0,
      fallbackReasons: []
    }
  }
}

export function buildInlineReviewPlan(lintOutput, prFiles, existingComments = []) {
  const lineMap = buildPullRequestLineMap(prFiles)
  const existingMarkers = new Set((existingComments ?? [])
    .map((comment) => extractInlineMarker(comment.body))
    .filter(Boolean))
  const comments = []
  const skipped = []
  const fallbackIssues = []
  for (const file of lintOutput.files ?? []) {
    for (const issue of file.result?.issues ?? []) {
      const line = parseOptionalNumber(issue.line)
      if (line === null) {
        fallbackIssues.push(fallbackIssue(file.path, issue, 'issue_missing_line'))
        continue
      }
      const mapped = findMappedReviewLine(lineMap, file.path, line)
      if (!mapped) {
        fallbackIssues.push(fallbackIssue(file.path, issue, 'line_not_in_pr_diff'))
        continue
      }
      const marker = inlineMarker(mapped.path, mapped.line, issue.ruleCode)
      if (existingMarkers.has(marker)) {
        skipped.push({ path: mapped.path, line: mapped.line, ruleCode: issue.ruleCode, reason: 'duplicate_marker' })
        continue
      }
      comments.push({
        path: mapped.path,
        line: mapped.line,
        marker,
        issue,
        body: buildInlineCommentBody(marker, issue)
      })
    }
  }
  return {
    comments,
    skipped,
    fallbackIssues,
    summary: {
      inlineCommentsCreated: comments.length,
      inlineCommentsSkipped: skipped.length,
      fallbackIssues: fallbackIssues.length,
      fallbackReasons: summarizeFallbackReasons(fallbackIssues)
    }
  }
}

export function buildPullRequestLineMap(prFiles) {
  const mappings = []
  for (const file of prFiles ?? []) {
    const normalizedPath = normalizeReviewPath(file.filename)
    const patch = file.patch
    if (!normalizedPath || typeof patch !== 'string' || patch.trim() === '') {
      continue
    }
    for (const line of parsePatchNewLines(patch)) {
      mappings.push({ path: normalizedPath, line })
    }
  }
  return mappings
}

function parsePatchNewLines(patch) {
  const lines = []
  let newLine = null
  for (const rawLine of patch.split(/\r?\n/)) {
    const header = /^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/.exec(rawLine)
    if (header) {
      newLine = Number(header[1])
      continue
    }
    if (newLine === null) {
      continue
    }
    if (rawLine.startsWith('+') && !rawLine.startsWith('+++')) {
      lines.push(newLine)
      newLine += 1
      continue
    }
    if (rawLine.startsWith('-') && !rawLine.startsWith('---')) {
      continue
    }
    if (rawLine.startsWith('\\')) {
      continue
    }
    newLine += 1
  }
  return lines
}

function findMappedReviewLine(lineMap, filePath, line) {
  const normalizedFile = normalizeReviewPath(filePath)
  return lineMap.find((item) => item.line === line && pathsMatch(normalizedFile, item.path)) ?? null
}

function pathsMatch(left, right) {
  return left === right || left.endsWith(`/${right}`) || right.endsWith(`/${left}`)
}

function fallbackIssue(filePath, issue, reason) {
  return {
    path: normalizeReviewPath(filePath),
    line: issue.line ?? null,
    ruleCode: issue.ruleCode ?? 'unknown_rule',
    reason
  }
}

function summarizeFallbackReasons(fallbackIssues) {
  const counts = new Map()
  for (const issue of fallbackIssues) {
    counts.set(issue.reason, (counts.get(issue.reason) ?? 0) + 1)
  }
  return [...counts.entries()].map(([reason, count]) => ({ reason, count }))
}

function inlineMarker(pathValue, line, ruleCode) {
  return `${DATASPEC_INLINE_REVIEW_PREFIX}:${encodeURIComponent(pathValue)}:${line}:${encodeURIComponent(ruleCode ?? 'unknown_rule')}`
}

function extractInlineMarker(body) {
  const match = /<!--\s*(dataspec-inline-review:[^>]+)\s*-->/.exec(body ?? '')
  return match?.[1]?.trim() ?? null
}

function buildInlineCommentBody(marker, issue) {
  return [
    `<!-- ${marker} -->`,
    formatIssueMarkdown(issue)
  ].join('\n')
}

function normalizeReviewPath(value) {
  return String(value ?? '').replaceAll('\\', '/').replace(/^\.?\//, '')
}

function formatIssueMarkdown(issue) {
  const severity = issue.severity ?? 'UNKNOWN'
  const ruleCode = issue.ruleCode ?? 'unknown_rule'
  const message = issue.message ?? ''
  const location = formatIssueLocation(issue)
  const lines = [`- **${severity}** \`${ruleCode}\`${location ? ` (${location})` : ''}: ${message}`]
  if (issue.suggestion) {
    lines.push(`  - 建议：${issue.suggestion}`)
  }
  if (issue.replacement) {
    lines.push(`  - 替换：\`${issue.replacement}\``)
  }
  return lines.join('\n')
}

function formatIssueLocation(issue) {
  const parts = []
  const range = formatIssueRange(issue)
  if (range) {
    parts.push(range)
  }
  if (issue.tableName) {
    parts.push(`表 \`${issue.tableName}\``)
  }
  if (issue.columnName) {
    parts.push(`字段 \`${issue.columnName}\``)
  }
  return parts.join(' / ')
}

function formatIssueRange(issue) {
  const startLine = parseOptionalNumber(issue.line)
  if (startLine === null) {
    return ''
  }
  const startColumn = parseOptionalNumber(issue.column) ?? 1
  const endLine = parseOptionalNumber(issue.lineEnd) ?? startLine
  const endColumn = parseOptionalNumber(issue.columnEnd) ?? startColumn
  return `行 ${startLine}:${startColumn}-${endLine}:${endColumn}`
}

function parseOptionalNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function escapeTableCell(value) {
  return String(value ?? '').replaceAll('|', '\\|').replaceAll('\n', ' ')
}

function normalizeServer(server = DEFAULT_SERVER) {
  return server.replace(/\/+$/, '')
}

function normalizeAiOutputPostCheckContentType(value) {
  const normalized = String(value ?? '').trim().toUpperCase()
  return normalized === 'PLAIN_TEXT' ? 'TEXT' : normalized
}

function resolveDataSpecToken(options, config) {
  return options['dataspec-token'] ?? process.env.DATASPEC_TOKEN ?? config.apiToken
}

function resolveIdempotencyKey(options) {
  const value = options['idempotency-key'] ?? options.idempotencyKey ?? process.env.DATASPEC_IDEMPOTENCY_KEY
  if (value === undefined || value === null || String(value).trim() === '') {
    return null
  }
  return String(value).trim()
}

function dataSpecHeaders(apiToken, headers = {}, idempotencyKey = null) {
  const result = {
    ...headers
  }
  if (apiToken) {
    result.Authorization = `Bearer ${apiToken}`
  }
  if (idempotencyKey) {
    result['Idempotency-Key'] = idempotencyKey
  }
  return result
}

function scopedIdempotencyKey(idempotencyKey, scope) {
  if (!idempotencyKey) {
    return null
  }
  const normalized = `${idempotencyKey}:${createHash('sha256').update(String(scope)).digest('hex').slice(0, 16)}`
  if (normalized.length <= 128) {
    return normalized
  }
  return `${idempotencyKey.slice(0, 96)}:${createHash('sha256').update(normalized).digest('hex').slice(0, 24)}`
}

function cliCwd(io) {
  return typeof io.cwd === 'function' ? io.cwd() : io.cwd ?? process.cwd()
}

async function readJsonResponse(response) {
  const payload = await readResponseJson(response)
  if (!response.ok) {
    throw toDataSpecCliError(payload, response.status)
  }
  if (payload?.code && payload.code !== 200) {
    throw toDataSpecCliError(payload, response.status)
  }
  return payload
}

function unwrapResponse(payload) {
  return payload?.data ?? payload
}

async function readResponseJson(response) {
  try {
    return await response.json()
  } catch {
    return null
  }
}

function toDataSpecCliError(payload, httpStatus) {
  const message = payload?.message || `DataSpec 请求失败，HTTP ${httpStatus}`
  return new DataSpecCliError(message, normalizeDataSpecDiagnostic(payload?.error, httpStatus, message))
}

function normalizeDataSpecDiagnostic(error, httpStatus, message) {
  if (error && typeof error === 'object') {
    const diagnostic = {
      code: String(error.code ?? 'DATASPEC_ERROR'),
      category: String(error.category ?? 'DATASPEC'),
      retryable: Boolean(error.retryable),
      suggestedAction: String(error.suggestedAction ?? '查看 DataSpec 响应 message 并按提示修正请求。'),
      docsRef: String(error.docsRef ?? 'README.md#验证'),
      httpStatus
    }
    appendDiagnosticExtras(diagnostic, error)
    return diagnostic
  }
  return fallbackDataSpecDiagnostic(httpStatus, message)
}

function appendDiagnosticExtras(diagnostic, error) {
  if (Array.isArray(error.missing)) {
    diagnostic.missing = error.missing.map((item) => String(item))
  }
  if (typeof error.operation === 'string' && error.operation.trim()) {
    diagnostic.operation = error.operation
  }
  if (typeof error.capabilityId === 'string' && error.capabilityId.trim()) {
    diagnostic.capabilityId = error.capabilityId
  }
  if (error.safety && typeof error.safety === 'object' && !Array.isArray(error.safety)) {
    diagnostic.safety = error.safety
  }
  if (Array.isArray(error.nextActions)) {
    diagnostic.nextActions = error.nextActions.map((item) => String(item))
  }
}

function fallbackDataSpecDiagnostic(httpStatus, message) {
  if (httpStatus === 401) {
    return {
      code: 'AUTH_TOKEN_MISSING_OR_INVALID',
      category: 'AUTH',
      retryable: true,
      suggestedAction: '提供有效的 API Token；CLI/MCP 可设置 DATASPEC_TOKEN 或 --dataspec-token。',
      docsRef: 'README.md#安全基线',
      httpStatus
    }
  }
  if (httpStatus === 403) {
    return {
      code: 'PROJECT_ACCESS_DENIED',
      category: 'AUTH',
      retryable: false,
      suggestedAction: '切换到 token 授权的项目，或使用具备该项目权限的 API Token 后重试。',
      docsRef: 'README.md#安全基线',
      httpStatus
    }
  }
  return {
    code: httpStatus >= 500 ? 'INTERNAL_ERROR' : 'DATASPEC_REQUEST_FAILED',
    category: httpStatus >= 500 ? 'SERVER' : 'DATASPEC',
    retryable: httpStatus >= 500,
    suggestedAction: message || '查看 DataSpec 响应 message 并按提示修正请求。',
    docsRef: 'README.md#验证',
    httpStatus
  }
}

function formatCliError(error) {
  const lines = [`错误: ${sanitizeSecretText(error.message)}`]
  if (error instanceof DataSpecCliError && error.diagnostic) {
    lines.push(`DataSpecError: ${JSON.stringify(sanitizeSecretValue(error.diagnostic))}`)
  }
  return `${lines.join('\n')}\n`
}

function helpText() {
  return `DataSpec CLI

Usage:
  node tools/dataspec-cli.mjs lint <path|-> [--project <id>] [--profile <id>|--task-type <type>] --format text|json [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs lint-debug <path|-> [--project <id>] [--profile <id>|--task-type <type>] [--fix-mode GENERATE|DRY_RUN|DISABLED] [--max-risk LOW|MEDIUM|HIGH] [--include-explanations true|false] [--enable-rule <code>] [--disable-rule <code>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs lint-files [path...] [--project <id>] [--profile <id>|--task-type <type>] --format json [--delivery-package <json>] [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs fixed-sql patch --lint-result <json> --target <file.sql> [--apply --confirm <planHash>] --format json
  node tools/dataspec-cli.mjs changed [--project <id>] [--profile <id>|--task-type <type>] [--format text|json] [--server <url>]
  node tools/dataspec-cli.mjs lint-changed [--project <id>] [--profile <id>|--task-type <type>] --format text|json [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs install-hook [--hook pre-commit] [--with-vscode] [--format text|json]
  node tools/dataspec-cli.mjs index-refs --field <name> [--alias <name|field=alias> ...] [--path <file|dir> ...] [--format text|json]
  node tools/dataspec-cli.mjs code-patch plan --field <name> [--to-field <new>] [--from-type <type>] [--to-type <type>] [--enum-change <old=new> ...] [--alias <alias> ...] [--path <file|dir> ...] [--format json|markdown]
  node tools/dataspec-cli.mjs review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token> [--format text|json] [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs export-context [--project <id>] [--profile <id>|--task-type <type>] [--output <zip>] [--cache] [--cache-ttl-days <days>] [--scope all|field|domain|tag|table|changed] [--query <text>] [--status <status>] [--limit <n>] [--snapshot-id <id>|--snapshot-version <version>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs context-budget plan [--project <id>] --token-budget <n> [--profile <id>|--task-type <type>] [--scope all|field|domain|tag|table|changed] [--query <text>] [--status <status>] [--limit <n>] [--target-table <name>] [--target-file <path>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs context-quality check [--context-dir <dir>|--context-zip <zip>|--budget-plan <json>] [--format text|json]
  node tools/dataspec-cli.mjs suggest-field <query> [--project <id>] --format json [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs search-fields [query] [--project <id>] --format json [--category <name>] [--tag <tag>] [--status <status>] [--sensitive true|false] [--source-batch <id>] [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs search-fields [--project <id>] --format json (--dsl <json>|--dsl-file <path>|--stdin) [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs ref resolve --project <id> --type <FIELD|ENUM|RULE|SNAPSHOT> --ref <value> [--ref <value> ...] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs ai-output check --project <id> --type <SQL|DDL|MARKDOWN|JSON|TEXT> (--file <path>|--stdin) [--snapshot-ref <ref>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs generate-ddl [--project <id>] --template <id> --table <name> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs table-standards list --project <id> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs table-standards show --project <id> (--template <id>|--business-object <key>) --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs field-knowledge list --project <id> [--query <text>] [--status <status>] [--field-id <id>] [--limit <n>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs field-knowledge show --project <id> <fieldId> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs field-semantics list --project <id> [--field-id <id>] [--rule-type <type>] [--query <text>] [--limit <n>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs field-semantics show <id> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs metric-definitions list --project <id> [--query <text>] [--status <status>] [--field-id <id>] [--metric-key <key>] [--limit <n>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs metric-definitions show <id> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs synthetic-examples generate [--project <id>] --scenario <user|order|payment|audit> [--max-cases <n>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs test-data generate [--project <id>] [--field <name> ...] [--object-scenario <name>] [--max-fields <n>] [--cases-per-field <n>] [--seed-row-count <n>] [--dialect <name>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs consumer-compat check [--fixture <path>] [--format text|json]
  node tools/dataspec-cli.mjs contract-import preview [--project <id>] --source-kind <openapi|json-schema|protobuf> --input <path> [--max-candidates <n>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs schema-plan [--project <id>] --database-type <postgresql|mysql> --host <host> [--port <n>] --database <name> [--schema <schema>] --username <user> [--password-env <env>|--password <value>] --table <name> [--table <name> ...] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs comment-plan preview [--project <id>] --database-type <postgresql|mysql> --host <host> [--port <n>] --database <name> [--schema <schema>] --username <user> [--password-env <env>|--password <value>] --table <name> [--table <name> ...] [--metadata-cache-mode AUTO|REFRESH|BYPASS] --format text|json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs init --project <id> [--server <url>] [--default-path <path> ...] [--with-agents] [--force] [--format text|json]
  node tools/dataspec-cli.mjs bootstrap [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs doctor [--project <id>] [--profile <id>|--task-type <type>] [--format text|json] [--server <url>] [--dataspec-token <token>] [--check-openapi]
  node tools/dataspec-cli.mjs compat check [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs evidence export --source-type <AI_JOB|SQL_CHECK|COVERAGE_REPORT|AI_BATCH_RUN|AI_TASK_RUN> [--source-id <id>] [--payload <json>] [--format json|zip] [--output <path>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs task list [--project <id>] [--status <status>] [--task-type <type>] [--current <n>] [--size <n>] [--format json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs task failures [--project <id>] [--limit <n>] [--format json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs task show <id> [--project <id>] [--format json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs quality-gate check [--project <id>] [--format json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs contract list [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs contract show <contractId> [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs contract check [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs capability list [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs capability show <capabilityId> [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs capability check [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs profile list [--project <id>] [--profile <id>|--task-type <type>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs profile show <id|taskType> [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs workflow list [--format text|json]
  node tools/dataspec-cli.mjs workflow show <id> [--format text|json]
  node tools/dataspec-cli.mjs task-card create --workflow <id> --goal <text> [--project <id>] [--input key=value ...] [--format json|markdown] [--output <path>]
  node tools/dataspec-cli.mjs task-card show --file <path> [--format json|markdown]
  node tools/dataspec-cli.mjs task-card update --file <path> --step <id> --status <status> [--artifact <path>] [--notes <text>] [--format json|markdown]

Options:
  --project 可由 .dataspec/config.json 的 projectId 提供
  --server  可由 .dataspec/config.json 的 server 提供
  --profile/--task-type 可由 .dataspec/config.json 的 aiProfile/taskType 提供，显式参数优先
  --dataspec-token 可由 .dataspec/config.json 的 apiToken 或 DATASPEC_TOKEN 环境变量提供
  --idempotency-key 可由 DATASPEC_IDEMPOTENCY_KEY 兜底，写入型命令会作为 Idempotency-Key header 传给后端
  lint-debug 读取 /api/lint/debug 的只读规则 trace；成功请求始终返回 0，适合 AI 排查命中原因和参数快照
  lint-files 未传 path 时可使用 .dataspec/config.json 的 defaultPaths
  lint-files 可通过 --delivery-package 或 --batch-package 写出 AI 批量任务交付包，stdout JSON 保持原结构
  fixed-sql patch 默认只输出补丁计划；写入目标 SQL 文件必须显式传 --apply --confirm <planHash>
  changed 读取 git 变更并按 defaultPaths 输出文件清单、SQL 子集、最小 Context 建议和恢复诊断，不调用服务端
  lint-changed 只对 changed 发现的 SQL 文件调用 lint；json 适合 AI 读取，text 输出 file:line:column 行格式供 IDE Problem Matcher 跳转；无 SQL 变更时返回诊断且不调用服务端
  install-hook 显式安装 DataSpec 管理的本地 pre-commit hook；--with-vscode 会生成 VS Code task/problem matcher；不会覆盖非 DataSpec 管理的用户文件
  index-refs 只读扫描 defaultPaths 或 --path 内字段引用，输出重命名风险；多字段扫描时 --alias 使用 field=alias 明确归属；不会调用服务端或修改业务代码
  code-patch plan 基于字段引用索引生成字段变更 Patch Plan；默认 dry-run，只输出候选修改、人工步骤、验证命令和回滚提示，不写业务仓库
  export-context 默认导出完整包；传 --profile 或配置 aiProfile 时可让服务端 profile 提供上下文默认值；传 --scope/--query/--status/--limit 时显式裁剪优先；传 --snapshot-id/--snapshot-version 可按历史标准快照导出
  context-budget plan 是导出前只读预算预检；只调用后端 planner，不下载、不缓存、不写入 AI Context 文件
  context-quality check 是本地只读质量检查；读取已导出的目录、zip 或预算 plan JSON，不调用后端、不写缓存、不修改项目状态
  search-fields 返回字段标准检索 JSON，适合 AI 在建表或修 SQL 前选择相关标准字段；传 --dsl/--dsl-file/--stdin 时使用只读 Standard Query DSL 且不与 legacy 筛选参数混用
  ref resolve 只读解析字段、枚举、规则或快照引用，返回 stableRef/canonicalRef、生命周期状态和替代引用建议
  ai-output check 只读校验 AI 产物中的标准引用；PASS 返回 0，WARN/FAIL 返回 1，参数、配置或 API 错误返回 2
  table-standards 只读读取业务对象、模板结构标准、关系摘要、安全 metadata 和 nextActions；show 需在 --template 与 --business-object 间二选一
  field-knowledge、field-semantics 和 metric-definitions 只读读取字段知识卡、字段语义规则和指标口径；输出会再次脱敏，不执行计算、不写项目状态
  synthetic-examples generate 只读生成合成标准样例包，可作为 fixture、Prompt 评测或人工审核草案；不会写入项目标准或调用外部 LLM
  test-data generate 只读调用标准测试数据包 API，生成 valid/invalid/boundary、mock、CSV 和 SQL seed 草稿；不会写业务仓库、读取真实业务行或调用外部 LLM
  consumer-compat check 本地只读检查 DataSpec 自有消费端 golden payload 与 breaking rules，不需要服务端或真实凭据
  contract-import preview 只读读取本地 OpenAPI/JSON Schema/Protobuf 契约并生成候选预览；不会自动写入标准字段或候选 Inbox
  schema-plan 只生成数据库 schema change plan 预览，不执行迁移；推荐使用 --password-env 读取数据库密码
  comment-plan preview 只生成数据库 COMMENT 回写计划预览，不执行 SQL；text 输出仅供阅读，json 输出保留稳定字段
  init 默认不覆盖已有文件，传 --force 才覆盖 DataSpec 管理文件；不会写入明文 API token
  bootstrap 是 AI 新会话第一跳；服务可达时读取后端启动包，服务不可达时仍输出本地 BLOCKED JSON 和 nextActions
  doctor 默认做轻量 OpenAPI 状态和 AI Context 缓存检查；传 --check-openapi 时执行完整 schema 漂移检查
  compat check 读取 /api/capabilities/version；兼容返回 0，不兼容返回 1，服务不可达或响应错误返回 2
  evidence export 生成只读 AI 执行证据包；zip 输出必须显式指定 --output，证据包不是审批、审计或写入权限
  contract 用于读取和检查 AI 可消费输出契约 registry，不代表权限或发布审批
  capability 用于读取和检查 AI 可用能力清单，只描述入口和前置检查，不会自动执行能力
  workflow 只输出任务计划和命令建议，不会自动执行步骤或调用外部 LLM
  task-card 从 workflow 生成或更新本地 AI 任务卡文件，只描述进度、下一步、产物和验证命令，不会执行 workflow 或写入远端
`
}

function processIo() {
  return {
    cwd: () => process.cwd(),
    writeOut(text) {
      process.stdout.write(text)
    },
    writeErr(text) {
      process.stderr.write(text)
    },
    async readStdin() {
      const chunks = []
      for await (const chunk of process.stdin) {
        chunks.push(Buffer.from(chunk))
      }
      return Buffer.concat(chunks).toString('utf8')
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = await runCli(process.argv.slice(2))
}
