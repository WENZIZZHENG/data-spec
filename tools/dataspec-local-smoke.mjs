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
      options.timeoutMs = parsePositiveInt(arg.slice('--timeout-ms='.length), DEFAULT_TIMEOUT_MS)
      continue
    }
    if (arg === '--interval-ms') {
      options.intervalMs = parsePositiveInt(readValue(argv, i, arg), DEFAULT_INTERVAL_MS)
      i += 1
      continue
    }
    if (arg.startsWith('--interval-ms=')) {
      options.intervalMs = parsePositiveInt(arg.slice('--interval-ms='.length), DEFAULT_INTERVAL_MS)
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
      token: options.token
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
      token: options.token
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
  const attempts = Math.max(1, Math.ceil(options.timeoutMs / options.intervalMs))
  let lastError = null
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const response = await fetchFn(url, requestOptions({ token: options.token }))
      if (response.ok) {
        return response
      }
      lastError = new Error(`HTTP ${response.status} from ${safeServiceUrl(url)}`)
    } catch (error) {
      lastError = error
    }
    if (attempt < attempts) {
      await options.sleepFn(options.intervalMs)
    }
  }
  throw new Error(`Timed out waiting for ${safeServiceUrl(url)}: ${redactText(lastError?.message)}`)
}

async function requestJson(fetchFn, url, options = {}) {
  const response = await fetchFn(url, requestOptions(options))
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} from ${safeServiceUrl(url)}: ${await responseText(response)}`)
  }
  const json = await response.json()
  if (json && typeof json.code === 'number' && json.code >= 400) {
    throw new Error(json.message || `DataSpec API returned code ${json.code}`)
  }
  return json
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
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
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
  if (!value || value.startsWith('--')) {
    throw new Error(`${option} requires a value`)
  }
  return value
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
