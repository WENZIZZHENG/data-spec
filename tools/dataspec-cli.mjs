#!/usr/bin/env node

import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const DEFAULT_SERVER = 'http://localhost:8090'
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
    if (command === 'export-context') {
      return await runExportContext(rest, io, fetchFn)
    }
    if (command === 'suggest-field') {
      return await runSuggestField(rest, io, fetchFn)
    }
    if (command === 'generate-ddl') {
      return await runGenerateDdl(rest, io, fetchFn)
    }
    throw new Error(`未知命令: ${command}\n\n${helpText()}`)
  } catch (error) {
    io.writeErr(`错误: ${error.message}\n`)
    return 2
  }
}

async function runLint(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'format', 'server'])
  const sqlPath = positional[0]
  if (!sqlPath) {
    throw new Error('lint 需要提供 SQL 文件路径或 -')
  }
  if (positional.length > 1) {
    throw new Error(`lint 只接受一个 SQL 输入路径，收到: ${positional.slice(1).join(', ')}`)
  }
  const projectId = parseProjectId(options.project)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const server = normalizeServer(options.server)
  const sql = sqlPath === '-' ? await io.readStdin() : await readFile(sqlPath, 'utf8')

  const response = await fetchFn(`${server}/api/lint`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql, projectId })
  })
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return Number(result.errorCount ?? 0) > 0 ? 1 : 0
}

async function runLintFiles(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'format', 'server'])
  if (positional.length === 0) {
    throw new Error('lint-files 需要提供至少一个 SQL 文件或目录路径')
  }
  const projectId = parseProjectId(options.project)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const server = normalizeServer(options.server)
  const files = await collectSqlFiles(positional)
  const results = []

  for (const filePath of files) {
    const sql = await readFile(filePath, 'utf8')
    const response = await fetchFn(`${server}/api/lint`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sql, projectId })
    })
    const payload = await readJsonResponse(response)
    results.push({
      path: formatOutputPath(filePath),
      result: unwrapResponse(payload)
    })
  }

  const summary = summarizeLintResults(results)
  io.writeOut(`${JSON.stringify({ summary, files: results }, null, 2)}\n`)
  return summary.failedFiles > 0 ? 1 : 0
}

async function runExportContext(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'output', 'server'])
  if (positional.length > 0) {
    throw new Error(`export-context 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project)
  const output = options.output
  if (!output) {
    throw new Error('export-context 需要提供 --output <zip>')
  }
  const server = normalizeServer(options.server)
  const url = `${server}/api/ai-context/package/download?projectId=${encodeURIComponent(projectId)}`
  const response = await fetchFn(url)
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
  const { positional, options } = parseArgs(args, ['project', 'format', 'server', 'limit'])
  const query = positional[0]
  if (!query) {
    throw new Error('suggest-field 需要提供业务字段描述')
  }
  if (positional.length > 1) {
    throw new Error(`suggest-field 只接受一个描述参数，收到: ${positional.slice(1).join(', ')}`)
  }
  const projectId = parseProjectId(options.project)
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const limit = parseLimit(options.limit)
  const server = normalizeServer(options.server)
  const url = `${server}/api/fields/suggest?projectId=${encodeURIComponent(projectId)}&query=${encodeURIComponent(query)}&limit=${encodeURIComponent(limit)}`
  const response = await fetchFn(url)
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

async function runGenerateDdl(args, io, fetchFn) {
  const { positional, options } = parseArgs(args, ['project', 'template', 'table', 'format', 'server'])
  if (positional.length > 0) {
    throw new Error(`generate-ddl 不接受位置参数: ${positional.join(', ')}`)
  }
  const projectId = parseProjectId(options.project)
  const templateId = parsePositiveInteger(options.template, 'template id')
  const tableName = options.table
  if (!tableName) {
    throw new Error('generate-ddl 需要提供 --table <name>')
  }
  const format = options.format ?? 'json'
  if (format !== 'json') {
    throw new Error('当前仅支持 --format json')
  }
  const server = normalizeServer(options.server)
  const url = `${server}/api/generator/ddl/preview?projectId=${encodeURIComponent(projectId)}&templateId=${encodeURIComponent(templateId)}&tableName=${encodeURIComponent(tableName)}`
  const response = await fetchFn(url)
  const payload = await readJsonResponse(response)
  const result = unwrapResponse(payload)
  io.writeOut(`${JSON.stringify(result, null, 2)}\n`)
  return 0
}

function parseArgs(args, allowedOptions) {
  const positional = []
  const options = {}
  const allowedOptionSet = new Set(allowedOptions)
  for (let i = 0; i < args.length; i += 1) {
    const token = args[i]
    if (!token.startsWith('--')) {
      positional.push(token)
      continue
    }
    const name = token.slice(2)
    if (!allowedOptionSet.has(name)) {
      throw new Error(`未知参数: ${token}`)
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

function parseProjectId(value) {
  if (!value) {
    throw new Error('需要提供 --project <id>')
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

function normalizeServer(server = DEFAULT_SERVER) {
  return server.replace(/\/+$/, '')
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
  node tools/dataspec-cli.mjs lint <path|-> --project <id> --format json [--server <url>]
  node tools/dataspec-cli.mjs lint-files <path...> --project <id> --format json [--server <url>]
  node tools/dataspec-cli.mjs export-context --project <id> --output <zip> [--server <url>]
  node tools/dataspec-cli.mjs suggest-field <query> --project <id> --format json [--limit <n>] [--server <url>]
  node tools/dataspec-cli.mjs generate-ddl --project <id> --template <id> --table <name> --format json [--server <url>]
`
}

function processIo() {
  return {
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
