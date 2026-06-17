#!/usr/bin/env node

import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const DEFAULT_SERVER = 'http://localhost:8090'

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
    if (command === 'export-context') {
      return await runExportContext(rest, io, fetchFn)
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
  const projectId = Number(value)
  if (!Number.isInteger(projectId) || projectId <= 0) {
    throw new Error(`无效 project id: ${value}`)
  }
  return projectId
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
  node tools/dataspec-cli.mjs export-context --project <id> --output <zip> [--server <url>]
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
