#!/usr/bin/env node
import { pathToFileURL } from 'node:url'

const DEFAULT_SERVER = 'http://localhost:8090'
const DEFAULT_WEB = 'http://localhost:5173'
const DEFAULT_TIMEOUT_MS = 120_000
const DEFAULT_INTERVAL_MS = 2_000
const DEMO_FALLBACK_SQL = `
CREATE TABLE UserOrder (
  id bigint PRIMARY KEY,
  userName varchar(64),
  amount decimal(10, 2)
);
`.trim()

export function parseArgs(argv = [], env = process.env) {
  const options = {
    server: normalizeBaseUrl(env.DATASPEC_SERVER || DEFAULT_SERVER),
    web: normalizeBaseUrl(env.DATASPEC_WEB || DEFAULT_WEB),
    timeoutMs: parsePositiveInt(env.DATASPEC_SMOKE_TIMEOUT_MS, DEFAULT_TIMEOUT_MS),
    intervalMs: parsePositiveInt(env.DATASPEC_SMOKE_INTERVAL_MS, DEFAULT_INTERVAL_MS),
    format: env.DATASPEC_SMOKE_FORMAT === 'json' ? 'json' : 'text',
    token: env.DATASPEC_TOKEN || '',
    skipDemo: false,
    help: false
  }

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--help' || arg === '-h') {
      options.help = true
      continue
    }
    if (arg === '--json') {
      options.format = 'json'
      continue
    }
    if (arg === '--skip-demo') {
      options.skipDemo = true
      continue
    }
    if (arg === '--server') {
      options.server = normalizeBaseUrl(readValue(argv, i, arg))
      i += 1
      continue
    }
    if (arg.startsWith('--server=')) {
      options.server = normalizeBaseUrl(arg.slice('--server='.length))
      continue
    }
    if (arg === '--web') {
      options.web = normalizeBaseUrl(readValue(argv, i, arg))
      i += 1
      continue
    }
    if (arg.startsWith('--web=')) {
      options.web = normalizeBaseUrl(arg.slice('--web='.length))
      continue
    }
    if (arg === '--timeout-ms') {
      options.timeoutMs = parsePositiveInt(readValue(argv, i, arg), DEFAULT_TIMEOUT_MS)
      i += 1
      continue
    }
    if (arg.startsWith('--timeout-ms=')) {
      options.timeoutMs = parsePositiveInt(readInlineValue(arg, '--timeout-ms=', '--timeout-ms'), DEFAULT_TIMEOUT_MS)
      continue
    }
    if (arg === '--interval-ms') {
      options.intervalMs = parsePositiveInt(readValue(argv, i, arg), DEFAULT_INTERVAL_MS)
      i += 1
      continue
    }
    if (arg.startsWith('--interval-ms=')) {
      options.intervalMs = parsePositiveInt(readInlineValue(arg, '--interval-ms=', '--interval-ms'), DEFAULT_INTERVAL_MS)
      continue
    }
    if (arg === '--format') {
      options.format = normalizeFormat(readValue(argv, i, arg))
      i += 1
      continue
    }
    if (arg.startsWith('--format=')) {
      options.format = normalizeFormat(arg.slice('--format='.length))
      continue
    }
    if (arg === '--token') {
      options.token = readValue(argv, i, arg)
      i += 1
      continue
    }
    if (arg.startsWith('--token=')) {
      options.token = arg.slice('--token='.length)
      continue
    }
    throw new Error(`Unknown option: ${arg}`)
  }

  return options
}

export async function runSmoke(options, deps = {}) {
  const fetchFn = deps.fetchFn || globalThis.fetch
  if (typeof fetchFn !== 'function') {
    throw new Error('This command requires Node.js 18+ with global fetch support.')
  }
  const sleepFn = deps.sleepFn || sleep
  const checks = []
  let projectId = null
  let demoResult = null

  const fail = (name, error, nextAction) => {
    checks.push({
      name,
      status: 'fail',
      message: redactText(error?.message || error, options.token),
      nextAction
    })
    return buildResult(false, options, checks, projectId)
  }

  try {
    await waitForHttp(fetchFn, joinUrl(options.web, '/'), {
      timeoutMs: options.timeoutMs,
      intervalMs: options.intervalMs,
      sleepFn
    })
    checks.push({
      name: 'web',
      status: 'pass',
      message: `Frontend is reachable at ${safeServiceUrl(options.web)}`
    })
  } catch (error) {
    return fail('web', error, 'Run docker compose -f docker-compose.local.yml up web or pnpm dev.')
  }

  try {
    await waitForHttp(fetchFn, joinUrl(options.server, '/api-docs'), {
      timeoutMs: options.timeoutMs,
      intervalMs: options.intervalMs,
      sleepFn,
      token: options.token
    })
    checks.push({
      name: 'api-docs',
      status: 'pass',
      message: `API docs are reachable at ${safeServiceUrl(options.server)}/api-docs`
    })
  } catch (error) {
    return fail('api-docs', error, 'Run docker compose -f docker-compose.local.yml up server or mvn spring-boot:run.')
  }

  if (options.skipDemo) {
    checks.push({
      name: 'demo-project',
      status: 'skip',
      message: 'Skipped demo project creation by request.'
    })
    return buildResult(true, options, checks, projectId)
  }

  try {
    const response = await requestJson(fetchFn, joinUrl(options.server, '/api/projects/demo'), {
      method: 'POST',
      token: options.token,
      timeoutMs: options.timeoutMs
    })
    demoResult = unwrapData(response)
    projectId = demoResult?.project?.id || demoResult?.projectId || null
    if (!projectId) {
      throw new Error('Demo project response did not include project.id.')
    }
    checks.push({
      name: 'demo-project',
      status: 'pass',
      message: `${demoResult.created ? 'Created' : 'Reused'} demo project ${projectId}`
    })
  } catch (error) {
    return fail('demo-project', error, 'Check DATASPEC_TOKEN when security is enabled, then rerun the smoke command.')
  }

  try {
    await requestJson(fetchFn, joinUrl(options.server, `/api/dashboard/summary?projectId=${encodeURIComponent(projectId)}`), {
      token: options.token,
      timeoutMs: options.timeoutMs
    })
    checks.push({
      name: 'dashboard-summary',
      status: 'pass',
      message: `Dashboard summary loaded for project ${projectId}`
    })
  } catch (error) {
    return fail('dashboard-summary', error, 'Check backend logs and verify Flyway migrations completed.')
  }

  try {
    const lintSql = demoResult?.badExampleSql || DEMO_FALLBACK_SQL
    const lintResponse = await requestJson(fetchFn, joinUrl(options.server, '/api/lint'), {
      method: 'POST',
      token: options.token,
      timeoutMs: options.timeoutMs,
      body: { projectId, sql: lintSql }
    })
    const lintResult = unwrapData(lintResponse)
    const issueCount = Number(lintResult?.errorCount || 0)
      + Number(lintResult?.warningCount || 0)
      + Number(lintResult?.suggestionCount || 0)
    checks.push({
      name: 'sql-lint',
      status: 'pass',
      message: `SQL lint ran for project ${projectId} with ${issueCount} active issues`
    })
  } catch (error) {
    return fail('sql-lint', error, 'Open SQL lint page or backend logs to inspect parser and rule failures.')
  }

  return buildResult(true, options, checks, projectId)
}

export function formatResult(result, format = 'text') {
  if (format === 'json') {
    return JSON.stringify(result, null, 2)
  }
  const lines = [
    `DataSpec local smoke: ${result.ok ? 'PASS' : 'FAIL'}`,
    `Server: ${result.server}`,
    `Web: ${result.web}`
  ]
  if (result.projectId) {
    lines.push(`Project: ${result.projectId}`)
  }
  for (const check of result.checks) {
    lines.push(`- ${check.status.toUpperCase()} ${check.name}: ${check.message}`)
    if (check.nextAction) {
      lines.push(`  next: ${check.nextAction}`)
    }
  }
  return lines.join('\n')
}

export function redactText(value, token = '') {
  let text = String(value || '')
  if (token) {
    text = text.split(token).join('<token>')
  }
  return text
    .replace(/Bearer\s+[A-Za-z0-9._~+/=-]+/gi, 'Bearer <redacted>')
    .replace(/password=([^&\s'"]+)/gi, 'password=<redacted>')
    .replace(/jdbc:[^\s'"]+/gi, '<jdbc-url-redacted>')
}

export function helpText() {
  return `
Usage: node tools/dataspec-local-smoke.mjs [options]

Options:
  --server <url>       DataSpec backend URL (default: ${DEFAULT_SERVER})
  --web <url>          DataSpec frontend URL (default: ${DEFAULT_WEB})
  --timeout-ms <ms>    Wait timeout per service (default: ${DEFAULT_TIMEOUT_MS})
  --interval-ms <ms>   Wait polling interval (default: ${DEFAULT_INTERVAL_MS})
  --token <token>      API token for secured local services
  --skip-demo          Only check web and API docs
  --format text|json   Output format
  --json               Alias for --format json
  --help               Show this help
`.trim()
}

function buildResult(ok, options, checks, projectId) {
  return {
    ok,
    server: safeServiceUrl(options.server),
    web: safeServiceUrl(options.web),
    projectId: projectId || undefined,
    checks
  }
}

async function waitForHttp(fetchFn, url, options) {
  const deadline = Date.now() + options.timeoutMs
  let lastError = null
  while (Date.now() < deadline) {
    const remainingMs = Math.max(1, deadline - Date.now())
    try {
      const response = await fetchWithDeadline(
        fetchFn,
        url,
        { token: options.token },
        Math.min(options.intervalMs, remainingMs)
      )
      if (response.ok) {
        return response
      }
      lastError = new Error(`HTTP ${response.status} from ${safeServiceUrl(url)}`)
    } catch (error) {
      lastError = error
    }
    const sleepMs = Math.min(options.intervalMs, Math.max(0, deadline - Date.now()))
    if (sleepMs > 0) {
      await options.sleepFn(sleepMs)
    }
  }
  throw new Error(`Timed out waiting for ${safeServiceUrl(url)}: ${redactText(lastError?.message)}`)
}

async function requestJson(fetchFn, url, options = {}) {
  return withDeadline(url, options.timeoutMs || DEFAULT_TIMEOUT_MS, async (signal) => {
    const response = await fetchFn(url, requestOptions({ ...options, signal }))
    if (!response.ok) {
      throw new Error(`HTTP ${response.status} from ${safeServiceUrl(url)}: ${await responseText(response)}`)
    }
    const json = await response.json()
    if (json && typeof json.code === 'number' && json.code >= 400) {
      throw new Error(json.message || `DataSpec API returned code ${json.code}`)
    }
    return json
  })
}

async function fetchWithDeadline(fetchFn, url, options, timeoutMs) {
  return withDeadline(
    url,
    timeoutMs,
    (signal) => fetchFn(url, requestOptions({ ...options, signal }))
  )
}

/**
 * 同时使用 Promise deadline 和 AbortController：前者保证忽略 signal 的调用也会返回，
 * 后者负责释放原生 fetch 持有的 socket，避免超时后连接继续占用资源。
 */
async function withDeadline(url, timeoutMs, operation) {
  const controller = new AbortController()
  let timeoutId
  const timeoutPromise = new Promise((resolve, reject) => {
    timeoutId = setTimeout(() => {
      reject(new Error(`Timed out requesting ${safeServiceUrl(url)} after ${timeoutMs}ms`))
      controller.abort()
    }, timeoutMs)
  })
  try {
    return await Promise.race([operation(controller.signal), timeoutPromise])
  } finally {
    clearTimeout(timeoutId)
  }
}

function requestOptions(options = {}) {
  const headers = {
    ...(options.token ? { Authorization: `Bearer ${options.token}` } : {})
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  return {
    method: options.method || 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal
  }
}

function unwrapData(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response
}

async function responseText(response) {
  try {
    return redactText(await response.text())
  } catch {
    return ''
  }
}

function normalizeBaseUrl(value) {
  const raw = String(value || '').trim()
  if (!raw) {
    throw new Error('URL value cannot be empty.')
  }
  return raw.replace(/\/+$/, '')
}

function safeServiceUrl(value) {
  try {
    const url = new URL(value)
    url.username = ''
    url.password = ''
    return url.toString().replace(/\/+$/, '')
  } catch {
    return redactText(value)
  }
}

function joinUrl(base, path) {
  return `${normalizeBaseUrl(base)}${path.startsWith('/') ? path : `/${path}`}`
}

function parsePositiveInt(value, fallback) {
  const parsed = Number.parseInt(String(value || ''), 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback
}

function normalizeFormat(value) {
  if (value !== 'text' && value !== 'json') {
    throw new Error(`Unsupported format: ${value}`)
  }
  return value
}

function readValue(argv, index, option) {
  const value = argv[index + 1]
  if (!isOptionValue(value)) {
    throw new Error(`${option} requires a value`)
  }
  return value
}

function readInlineValue(arg, prefix, option) {
  const value = arg.slice(prefix.length)
  if (!isOptionValue(value)) {
    throw new Error(`${option} requires a value`)
  }
  return value
}

function isOptionValue(value) {
  return typeof value === 'string' && value.length > 0 && !value.startsWith('-')
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function main() {
  let options
  try {
    options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log(helpText())
      return
    }
    const result = await runSmoke(options)
    console.log(formatResult(result, options.format))
    process.exitCode = result.ok ? 0 : 1
  } catch (error) {
    const message = redactText(error?.message || error, options?.token)
    console.error(message)
    process.exitCode = 2
  }
}

if (import.meta.url === pathToFileURL(process.argv[1] || '').href) {
  await main()
}
