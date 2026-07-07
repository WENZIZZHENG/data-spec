import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import {
  formatResult,
  parseArgs,
  redactText,
  runSmoke
} from './dataspec-local-smoke.mjs'

function readRepoFile(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

test('parseArgs resolves env defaults and explicit options', () => {
  const options = parseArgs([
    '--server', 'http://127.0.0.1:18090/',
    '--web=http://127.0.0.1:15173/',
    '--timeout-ms', '5000',
    '--interval-ms=50',
    '--format', 'json',
    '--token', 'ds_secret',
    '--skip-demo'
  ], {})

  assert.equal(options.server, 'http://127.0.0.1:18090')
  assert.equal(options.web, 'http://127.0.0.1:15173')
  assert.equal(options.timeoutMs, 5000)
  assert.equal(options.intervalMs, 50)
  assert.equal(options.format, 'json')
  assert.equal(options.token, 'ds_secret')
  assert.equal(options.skipDemo, true)
})

test('parseArgs rejects option-like values before numeric fallback', () => {
  assert.throws(
    () => parseArgs(['--timeout-ms', '-h'], {}),
    /--timeout-ms requires a value/
  )
  assert.throws(
    () => parseArgs(['--timeout-ms=-h'], {}),
    /--timeout-ms requires a value/
  )
  assert.throws(
    () => parseArgs(['--interval-ms', '-h'], {}),
    /--interval-ms requires a value/
  )
  assert.throws(
    () => parseArgs(['--interval-ms=-h'], {}),
    /--interval-ms requires a value/
  )
})

test('parseArgs keeps explicit equals values that start with hyphen', () => {
  const options = parseArgs(['--token=-abc'], {})

  assert.equal(options.token, '-abc')
})

test('runSmoke emits stable JSON result for a healthy stack', async () => {
  const seen = []
  const fetchFn = async (url, options = {}) => {
    seen.push({ url, options })
    if (url === 'http://localhost:5173/') {
      return jsonResponse({ ok: true })
    }
    if (url === 'http://localhost:8090/api-docs') {
      return jsonResponse({ openapi: '3.0.1' })
    }
    if (url === 'http://localhost:8090/api/projects/demo') {
      assert.equal(options.method, 'POST')
      return jsonResponse({
        code: 200,
        data: {
          created: true,
          project: { id: 12 },
          badExampleSql: 'CREATE TABLE UserOrder (id bigint);'
        }
      })
    }
    if (url === 'http://localhost:8090/api/dashboard/summary?projectId=12') {
      return jsonResponse({ code: 200, data: { fieldCount: 8 } })
    }
    if (url === 'http://localhost:8090/api/lint') {
      assert.equal(options.method, 'POST')
      assert.match(options.body, /UserOrder/)
      return jsonResponse({ code: 200, data: { errorCount: 1, warningCount: 0, suggestionCount: 0 } })
    }
    throw new Error(`Unexpected URL ${url}`)
  }

  const result = await runSmoke(parseArgs(['--json'], {}), {
    fetchFn,
    sleepFn: async () => {}
  })
  const json = JSON.parse(formatResult(result, 'json'))

  assert.equal(json.ok, true)
  assert.equal(json.projectId, 12)
  assert.deepEqual(json.checks.map((check) => check.name), [
    'web',
    'api-docs',
    'demo-project',
    'dashboard-summary',
    'sql-lint'
  ])
  assert.ok(seen.some((entry) => entry.url.endsWith('/api/lint')))
})

test('runSmoke reports unavailable services with redacted details', async () => {
  const secret = 'ds_secret'
  const result = await runSmoke(parseArgs(['--token', secret, '--timeout-ms', '1'], {}), {
    fetchFn: async (url) => {
      if (url === 'http://localhost:5173/') {
        return jsonResponse({ ok: true })
      }
      throw new Error(`Bearer ${secret} password=top-secret jdbc:postgresql://localhost:5432/dataspec`)
    },
    sleepFn: async () => {}
  })
  const text = formatResult(result, 'text')

  assert.equal(result.ok, false)
  assert.equal(result.checks.at(-1).name, 'api-docs')
  assert.doesNotMatch(text, /ds_secret|top-secret|jdbc:postgresql/)
  assert.match(text, /Bearer <redacted>/)
  assert.match(text, /password=<redacted>/)
  assert.match(text, /<jdbc-url-redacted>/)
})

test('redactText removes common secret shapes', () => {
  const text = redactText(
    'Bearer abc.def password=s3cr3t jdbc:postgresql://localhost:5432/demo token ds_secret',
    'ds_secret'
  )

  assert.equal(text.includes('abc.def'), false)
  assert.equal(text.includes('s3cr3t'), false)
  assert.equal(text.includes('jdbc:postgresql'), false)
  assert.equal(text.includes('ds_secret'), false)
})

test('local compose and Vite proxy keep startup contract wired', () => {
  const compose = readRepoFile('docker-compose.local.yml')
  const viteConfig = readRepoFile('dataspec-web/vite.config.ts')

  for (const snippet of [
    'postgres:',
    'server:',
    'web:',
    '${DATASPEC_DB_PORT:-5432}:5432',
    '${DATASPEC_SERVER_PORT:-8090}:8090',
    '${DATASPEC_WEB_PORT:-5173}:5173',
    'SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DATASPEC_DB_NAME:-dataspec}',
    'VITE_PROXY_TARGET: http://server:8090',
    'dataspec-maven-cache:',
    'dataspec-pnpm-store:',
    'dataspec-web-node-modules:'
  ]) {
    assert.ok(compose.includes(snippet), `compose should include ${snippet}`)
  }

  assert.ok(viteConfig.includes("process.env.VITE_PROXY_TARGET || 'http://localhost:8090'"))
  assert.ok(viteConfig.includes('target: apiProxyTarget'))
})

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    async json() {
      return body
    },
    async text() {
      return JSON.stringify(body)
    }
  }
}
