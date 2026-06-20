#!/usr/bin/env node

import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { loadDataSpecConfig, resolveDefaultPaths } from './dataspec-config.mjs'

const DEFAULT_SERVER = 'http://localhost:8090'
const DEFAULT_GITHUB_API = 'https://api.github.com'
const DATASPEC_REVIEW_MARKER = '<!-- dataspec-sql-review -->'
const TOOLS_DIR = path.dirname(fileURLToPath(import.meta.url))
const REPO_ROOT = path.dirname(TOOLS_DIR)
const DATASPEC_WEB_DIR = path.join(REPO_ROOT, 'dataspec-web')
const OPENAPI_SCHEMA_PATH = path.join(DATASPEC_WEB_DIR, 'src', 'api', 'schema.ts')
const SKIPPED_SCAN_DIRECTORIES = new Set([
  '.git',
  '.idea',
  '.vscode',
  'build',
  'dist',
  'node_modules',
  'target'
])

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
    if (command === 'generate-ddl') {
      return await runGenerateDdl(rest, io, fetchFn)
    }
    if (command === 'doctor') {
      return await runDoctor(rest, io, fetchFn)
    }
    throw new Error(`未知命令: ${command}\n\n${helpText()}`)
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
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
  const { positional, options } = parseArgs(args, ['project', 'repo', 'pr', 'token', 'server', 'github-api', 'dataspec-token'])
  const config = loadDataSpecConfig(cliCwd(io))
  if (positional.length === 0) {
    throw new Error('review-pr 需要提供至少一个 SQL 文件或目录路径')
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
  const body = buildReviewMarkdown(lintOutput)
  const action = await upsertPullRequestComment({
    repo,
    prNumber,
    token,
    githubApi,
    body,
    fetchFn
  })
  io.writeOut(`已${action === 'updated' ? '更新' : '创建'} DataSpec Review 评论\n`)
  return lintOutput.summary.failedFiles > 0 ? 1 : 0
}

async function runExportContext(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'output', 'server', 'dataspec-token'])
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
  const url = `${server}/api/ai-context/package/download?projectId=${encodeURIComponent(projectId)}`
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

  const result = {
    ok: checks.every((check) => check.status !== 'fail'),
    server,
    projectId: projectId ?? null,
    configPath: config.configPath,
    checks
  }
  if (format === 'json') {
    io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  } else {
    io.writeOut(formatDoctorText(result))
  }
  return result.ok ? 0 : 1
}

function parseArgs(args, allowedOptions, flagOptions = []) {
  const positional = []
  const options = {}
  const allowedOptionSet = new Set(allowedOptions)
  const flagOptionSet = new Set(flagOptions)
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
    options[name] = value
    i += 1
  }
  return { positional, options }
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
    throw new Error(`GitHub 请求失败，HTTP ${response.status}`)
  }
  return await response.json()
}

function buildReviewMarkdown(lintOutput) {
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
    ''
  ]

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
  if (issue.tableName) {
    parts.push(`表 \`${issue.tableName}\``)
  }
  if (issue.columnName) {
    parts.push(`字段 \`${issue.columnName}\``)
  }
  return parts.join(' / ')
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
  if (!response.ok) {
    throw new Error(`DataSpec 请求失败，HTTP ${response.status}`)
  }
  const payload = await response.json()
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload.message || `DataSpec 返回错误 code=${payload.code}`)
  }
  return payload
}

function unwrapResponse(payload) {
  return payload?.data ?? payload
}

function helpText() {
  return `DataSpec CLI

Usage:
  node tools/dataspec-cli.mjs lint <path|-> [--project <id>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs lint-files [path...] [--project <id>] --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs review-pr <path...> --project <id> --repo <owner/name> --pr <number> --token <token> [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs export-context [--project <id>] --output <zip> [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs suggest-field <query> [--project <id>] --format json [--limit <n>] [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs generate-ddl [--project <id>] --template <id> --table <name> --format json [--server <url>] [--dataspec-token <token>]
  node tools/dataspec-cli.mjs doctor [--project <id>] [--format text|json] [--server <url>] [--dataspec-token <token>] [--check-openapi]

Options:
  --project 可由 .dataspec/config.json 的 projectId 提供
  --server  可由 .dataspec/config.json 的 server 提供
  --dataspec-token 可由 .dataspec/config.json 的 apiToken 或 DATASPEC_TOKEN 环境变量提供
  lint-files 未传 path 时可使用 .dataspec/config.json 的 defaultPaths
  doctor 默认做轻量 OpenAPI 状态检查；传 --check-openapi 时执行完整 schema 漂移检查
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
