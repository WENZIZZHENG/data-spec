#!/usr/bin/env node

import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
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
  const { positional, options } = parseArgs(args, ['project', 'format', 'server', 'dataspec-token'])
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
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const sql = sqlPath === '-' ? await io.readStdin() : await readFile(sqlPath, 'utf8')

  const response = await fetchFn(`${server}/api/lint`, {
    method: 'POST',
    headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ sql, projectId })
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
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
  const { positional, options } = parseArgs(args, ['project', 'format', 'server', 'dataspec-token'])
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
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const output = await lintSqlFiles(inputPaths, projectId, server, fetchFn, apiToken)
  io.writeOut(`${JSON.stringify(output, null, 2)}\n`)
  return output.summary.failedFiles > 0 ? 1 : 0
}

async function runReviewPr(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'repo', 'pr', 'token', 'server', 'github-api', 'dataspec-token', 'format'])
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
  const lintOutput = await lintSqlFiles(positional, projectId, server, fetchFn, apiToken)
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
    'snapshotVersion'
  ])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length > 0) {
    throw new Error(`export-context 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project ?? config.projectId)
  const output = options.output
  if (!output) {
    throw new Error('export-context 需要提供 --output <zip>')
  }
  const server = normalizeServer(options.server ?? config.server)
  const apiToken = resolveDataSpecToken(options, config)
  const url = buildAiContextPackageUrl(server, projectId, options)
  const response = await fetchFn(url, { headers: dataSpecHeaders(apiToken) })
  if (!response.ok) {
    throw new Error(`导出 AI Context 失败，HTTP ${response.status}`)
  }
  const bytes = Buffer.from(await response.arrayBuffer())
  await mkdir(path.dirname(output), { recursive: true })
  await writeFile(output, bytes)
  io.writeOut(`已导出 ${output}\n`)
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
    ['project', 'format', 'server', 'dataspec-token'],
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
  checks.push(await checkOpenapiStatus({
    server,
    apiDocsReachable: apiDocsResult.check.status === 'pass',
    checkDrift: Boolean(options['check-openapi']),
    runDriftCheck: io.checkOpenapiDrift
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
- 需要完整上下文时，运行 \`export-context\` 并让 AI 读取导出的 \`.dataspec/\` 内容；单个建表或修 SQL 任务可加 \`--scope field --query <关键词>\` 导出按需包。
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
- 不确定字段命名时，先用 \`suggest-field\` 或导出的 AI Context 查找标准字段。`
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

async function lintSqlFiles(paths, projectId, server, fetchFn, apiToken) {
  const files = await collectSqlFiles(paths)
  const results = []

  for (const filePath of files) {
    const sql = await readFile(filePath, 'utf8')
    const response = await fetchFn(`${server}/api/lint`, {
      method: 'POST',
      headers: dataSpecHeaders(apiToken, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ sql, projectId })
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

function dataSpecHeaders(apiToken, headers = {}) {
  if (!apiToken) {
    return headers
  }
  return {
    ...headers,
    Authorization: `Bearer ${apiToken}`
  }
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
  node tools/dataspec-cli.mjs lint <path|-> [--project <id>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs lint-files [path...] [--project <id>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token> [--format text|json] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs export-context [--project <id>] --output <zip> [--scope all|field|domain|tag|table|changed] [--query <text>] [--status <status>] [--limit <n>] [--snapshot-id <id>|--snapshot-version <version>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs suggest-field <query> [--project <id>] --format json [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs search-fields [query] [--project <id>] --format json [--category <name>] [--tag <tag>] [--status <status>] [--sensitive true|false] [--source-batch <id>] [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs generate-ddl [--project <id>] --template <id> --table <name> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs init --project <id> [--server <url>] [--default-path <path> ...] [--with-agents] [--force] [--format text|json]
  node tools/dataspec-cli.mjs doctor [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>] [--check-openapi]
  node tools/dataspec-cli.mjs workflow list [--format text|json]
  node tools/dataspec-cli.mjs workflow show <id> [--format text|json]

Options:
  --project 可由 .dataspec/config.json 的 projectId 提供
  --server  可由 .dataspec/config.json 的 server 提供
  --dataspec-token 可由 .dataspec/config.json 的 apiToken 或 DATASPEC_TOKEN 环境变量提供
  lint-files 未传 path 时可使用 .dataspec/config.json 的 defaultPaths
  export-context 默认导出完整包；传 --scope/--query/--status/--limit 时导出按需包；传 --snapshot-id/--snapshot-version 可按历史标准快照导出
  search-fields 返回字段标准检索 JSON，适合 AI 在建表或修 SQL 前选择相关标准字段
  init 默认不覆盖已有文件，传 --force 才覆盖 DataSpec 管理文件；不会写入明文 API token
  doctor 默认做轻量 OpenAPI 状态检查；传 --check-openapi 时执行完整 schema 漂移检查
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
