#!/usr/bin/env node

import { mkdir, readdir, readFile, rm, stat, writeFile } from 'node:fs/promises'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { inflateRawSync } from 'node:zlib'
import { loadDataSpecConfig, resolveDefaultPaths } from './dataspec-config.mjs'
import {
  formatWorkflowListText,
  formatWorkflowRecipeText,
  getWorkflowRecipe,
  supportedWorkflowRecipeIds,
  workflowCatalogPayload
} from './dataspec-workflows.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
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
const CONTEXT_CACHE_DIR = path.join('.dataspec', 'context')
const CACHE_METADATA_FILE = 'cache-metadata.json'
const DEFAULT_CONTEXT_CACHE_TTL_DAYS = 7
const MS_PER_DAY = 24 * 60 * 60 * 1000
const ZIP_LOCAL_FILE_HEADER = 0x04034b50
const ZIP_CENTRAL_DIRECTORY_HEADER = 0x02014b50
const ZIP_END_OF_CENTRAL_DIRECTORY = 0x06054b50
const ZIP_METHOD_STORED = 0
const ZIP_METHOD_DEFLATED = 8
const ZIP_FLAG_ENCRYPTED = 0x0001
const SKIPPED_SCAN_DIRECTORIES = new Set([
  '.git',
  '.idea',
  '.vscode',
  'build',
  'dist',
  'node_modules',
  'target'
])

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
    if (command === 'lint-files') {
      return await runLintFiles(rest, io, fetchFn)
    }
    if (command === 'review-pr') {
      return await runReviewPr(rest, io, fetchFn)
    }
    if (command === 'export-context') {
      return await runExportContext(rest, io, fetchFn)
    }
    if (command === 'suggest-field') {
      return await runSuggestField(rest, io, fetchFn)
    }
    if (command === 'search-fields') {
      return await runSearchFields(rest, io, fetchFn)
    }
    if (command === 'generate-ddl') {
      return await runGenerateDdl(rest, io, fetchFn)
    }
    if (command === 'init') {
      return await runInit(rest, io, fetchFn)
    }
    if (command === 'doctor') {
      return await runDoctor(rest, io, fetchFn)
    }
    if (command === 'evidence') {
      return await runEvidence(rest, io, fetchFn)
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
    if (command === 'workflow' || command === 'workflows') {
      return runWorkflow(rest, io)
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
    'format',
    'server',
    'limit',
    'dataspec-token'
  ])
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
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
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
    throw new Error('evidence export 需要提供 --source-type <AI_JOB|SQL_CHECK|COVERAGE_REPORT|AI_BATCH_RUN>')
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
  'search-fields',
  'suggest-fields',
  'generate-ddl',
  'reverse-import',
  'coverage-report',
  'schema-registry',
  'export-evidence-package',
  'workflow-recipes',
  'ai-task-profiles',
  'domain-starter-kits'
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
node tools/dataspec-cli.mjs doctor --format json
node tools/dataspec-cli.mjs lint-files --format json
node tools/dataspec-cli.mjs export-context --output dataspec-ai-context.zip
node tools/dataspec-cli.mjs export-context --cache
node tools/dataspec-cli.mjs export-context --scope field --query 用户手机号 --output dataspec-ai-context.zip
\`\`\`

## Token

不要把明文 API token 写入可提交文件。需要安全模式访问时，优先在本机或 CI 中设置：

\`\`\`bash
export DATASPEC_TOKEN=ds_xxx
\`\`\`

也可以在单次命令中使用 \`--dataspec-token <token>\`。

## 给 AI agent 的约定

- 修改 SQL、migration 或 ORM entity 前，先运行 \`doctor\` 确认 DataSpec 可用。
- 未显式传路径时，\`lint-files\` 会读取 \`.dataspec/config.json\` 的 \`defaultPaths\`。
- 需要完整上下文时，运行 \`export-context --cache\` 并让 AI 读取 \`.dataspec/context/\`；单个建表或修 SQL 任务可加 \`--scope field --query <关键词>\` 导出按需包。
`
}

function renderAgentsFragment({ projectId, server, defaultPaths }) {
  return `# DataSpec 数据库规范

当前仓库已接入 DataSpec project ${projectId}（${server}）。

在创建或修改数据库 schema、SQL migration、ORM entity 或数据字典前：

- 先运行 \`node tools/dataspec-cli.mjs doctor --format json\`。
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

function parsePositiveInteger(value, label) {
  const parsed = Number(value)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`无效 ${label}: ${value}`)
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

function sanitizeSecretValue(value) {
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeSecretValue(item))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key,
      isSensitiveSecretKey(key) ? '***' : sanitizeSecretValue(item)
    ]))
  }
  return value
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
  }
  return String(value)
    .replace(/jdbc:[^\s"'<>]+/gi, 'jdbc:***')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*)(?!\s*['"]?bearer\s+)(['"]?)[^,;}&\r\n]+\2/gi, '$1$2***$2')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(/((?:"|')?\b(?:password|passwd|pwd|token|api[_-]?token|dataspec[_-]?token|api[_-]?key|secret|client[_-]?secret|access[_-]?token|refresh[_-]?token|plain[_-]?token|token[_-]?hash|jdbc[_-]?url|connection[_-]?string)\b(?:"|')?\s*[:=]\s*)(['"]?)[^\s"',;}&]+\2/gi, '$1$2***$2')
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
    'connectionstring'
  ].includes(normalized) ||
    normalized.endsWith('token') ||
    normalized.endsWith('secret') ||
    normalized.includes('apikey') ||
    normalized.includes('password') ||
    normalized.includes('authorization') ||
    normalized.includes('tokenhash') ||
    normalized.includes('connectionstring') ||
    normalized.includes('jdbcurl')
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
    return {
      code: String(error.code ?? 'DATASPEC_ERROR'),
      category: String(error.category ?? 'DATASPEC'),
      retryable: Boolean(error.retryable),
      suggestedAction: String(error.suggestedAction ?? '查看 DataSpec 响应 message 并按提示修正请求。'),
      docsRef: String(error.docsRef ?? 'README.md#验证'),
      httpStatus
    }
  }
  return fallbackDataSpecDiagnostic(httpStatus, message)
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
  const lines = [`错误: ${error.message}`]
  if (error instanceof DataSpecCliError && error.diagnostic) {
    lines.push(`DataSpecError: ${JSON.stringify(error.diagnostic)}`)
  }
  return `${lines.join('\n')}\n`
}

function helpText() {
  return `DataSpec CLI

Usage:
  node tools/dataspec-cli.mjs lint <path|-> [--project <id>] [--profile <id>|--task-type <type>] --format text|json [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs lint-files [path...] [--project <id>] [--profile <id>|--task-type <type>] --format json [--delivery-package <json>] [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token> [--format text|json] [--server <url>] [--dataspec-token <token>] [--idempotency-key <key>]
  node tools/dataspec-cli.mjs export-context [--project <id>] [--profile <id>|--task-type <type>] [--output <zip>] [--cache] [--cache-ttl-days <days>] [--scope all|field|domain|tag|table|changed] [--query <text>] [--status <status>] [--limit <n>] [--snapshot-id <id>|--snapshot-version <version>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs suggest-field <query> [--project <id>] --format json [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs search-fields [query] [--project <id>] --format json [--category <name>] [--tag <tag>] [--status <status>] [--sensitive true|false] [--source-batch <id>] [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs generate-ddl [--project <id>] --template <id> --table <name> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs init --project <id> [--server <url>] [--default-path <path> ...] [--with-agents] [--force] [--format text|json]
  node tools/dataspec-cli.mjs doctor [--project <id>] [--profile <id>|--task-type <type>] [--format text|json] [--server <url>] [--dataspec-token <token>] [--check-openapi]
  node tools/dataspec-cli.mjs evidence export --source-type <AI_JOB|SQL_CHECK|COVERAGE_REPORT|AI_BATCH_RUN> [--source-id <id>] [--payload <json>] [--format json|zip] [--output <path>] [--server <url>] [--dataspec-token <token>]
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

Options:
  --project 可由 .dataspec/config.json 的 projectId 提供
  --server  可由 .dataspec/config.json 的 server 提供
  --profile/--task-type 可由 .dataspec/config.json 的 aiProfile/taskType 提供，显式参数优先
  --dataspec-token 可由 .dataspec/config.json 的 apiToken 或 DATASPEC_TOKEN 环境变量提供
  --idempotency-key 可由 DATASPEC_IDEMPOTENCY_KEY 兜底，写入型命令会作为 Idempotency-Key header 传给后端
  lint-files 未传 path 时可使用 .dataspec/config.json 的 defaultPaths
  lint-files 可通过 --delivery-package 或 --batch-package 写出 AI 批量任务交付包，stdout JSON 保持原结构
  export-context 默认导出完整包；传 --profile 或配置 aiProfile 时可让服务端 profile 提供上下文默认值；传 --scope/--query/--status/--limit 时显式裁剪优先；传 --snapshot-id/--snapshot-version 可按历史标准快照导出
  search-fields 返回字段标准检索 JSON，适合 AI 在建表或修 SQL 前选择相关标准字段
  init 默认不覆盖已有文件，传 --force 才覆盖 DataSpec 管理文件；不会写入明文 API token
  doctor 默认做轻量 OpenAPI 状态和 AI Context 缓存检查；传 --check-openapi 时执行完整 schema 漂移检查
  evidence export 生成只读 AI 执行证据包；zip 输出必须显式指定 --output，证据包不是审批、审计或写入权限
  contract 用于读取和检查 AI 可消费输出契约 registry，不代表权限或发布审批
  capability 用于读取和检查 AI 可用能力清单，只描述入口和前置检查，不会自动执行能力
  workflow 只输出任务计划和命令建议，不会自动执行步骤或调用外部 LLM
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
