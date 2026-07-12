import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  formatResult,
  parseArgs,
  redactText,
  runSmoke
} from './dataspec-local-smoke.mjs'

function readRepoFile(relativePath) {
  return readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')
}

function readResolvedComposeConfig(t, env = {}) {
  try {
    return JSON.parse(execFileSync(
      'docker',
      ['compose', '-f', 'docker-compose.local.yml', 'config', '--format', 'json'],
      {
        cwd: fileURLToPath(new URL('../', import.meta.url)),
        encoding: 'utf8',
        env: { ...process.env, ...env },
        windowsHide: true
      }
    ))
  } catch (error) {
    if (error?.code === 'ENOENT') {
      t.skip('Docker Compose CLI is unavailable in this environment.')
      return null
    }
    throw error
  }
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

test('runSmoke bounds a hanging service readiness request', { timeout: 500 }, async () => {
  const result = await runSmoke(parseArgs(['--timeout-ms', '20', '--interval-ms', '10'], {}), {
    fetchFn: async () => new Promise(() => {}),
    sleepFn: async () => {}
  })

  assert.equal(result.ok, false)
  assert.equal(result.checks.at(-1).name, 'web')
  assert.match(result.checks.at(-1).message, /Timed out/)
})

test('runSmoke bounds a hanging demo API request', { timeout: 500 }, async () => {
  const fetchFn = async (url) => {
    if (url === 'http://localhost:5173/' || url === 'http://localhost:8090/api-docs') {
      return jsonResponse({ ok: true })
    }
    return new Promise(() => {})
  }

  const result = await runSmoke(parseArgs(['--timeout-ms', '20'], {}), {
    fetchFn,
    sleepFn: async () => {}
  })

  assert.equal(result.ok, false)
  assert.equal(result.checks.at(-1).name, 'demo-project')
  assert.match(result.checks.at(-1).message, /Timed out/)
})

test('runSmoke aborts the underlying fetch when a request times out', { timeout: 500 }, async () => {
  let abortCount = 0
  const result = await runSmoke(parseArgs(['--timeout-ms', '20'], {}), {
    fetchFn: async (url, options) => new Promise(() => {
      options.signal.addEventListener('abort', () => {
        abortCount += 1
      }, { once: true })
    }),
    sleepFn: async () => {}
  })

  assert.equal(result.ok, false)
  assert.equal(abortCount, 1)
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
  const packageJson = JSON.parse(readRepoFile('dataspec-web/package.json'))
  const viteConfig = readRepoFile('dataspec-web/vite.config.ts')

  for (const snippet of [
    'postgres:',
    'server:',
    'web:',
    '${DATASPEC_BIND_HOST:-127.0.0.1}:${DATASPEC_DB_PORT:-15432}:5432',
    '${DATASPEC_BIND_HOST:-127.0.0.1}:${DATASPEC_SERVER_PORT:-8090}:8090',
    '${DATASPEC_BIND_HOST:-127.0.0.1}:${DATASPEC_WEB_PORT:-5173}:5173',
    'SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DATASPEC_DB_NAME:-dataspec}',
    'VITE_PROXY_TARGET: http://server:8090',
    'image: node:22.23.1-bookworm',
    'curl --fail --silent --show-error http://localhost:8090/api-docs',
    'curl --fail --silent --show-error http://localhost:5173/',
    'condition: service_healthy',
    'start_period: 6m',
    'exec node node_modules/vite/bin/vite.js --host 0.0.0.0 --port 5173',
    'dataspec-maven-cache:',
    'dataspec-pnpm-store:',
    'dataspec-web-node-modules:'
  ]) {
    assert.ok(compose.includes(snippet), `compose should include ${snippet}`)
  }

  assert.equal(packageJson.packageManager, 'pnpm@11.12.0')
  assert.equal(packageJson.engines?.node, '>=22.12.0 <23')
  assert.ok(viteConfig.includes("process.env.VITE_PROXY_TARGET || 'http://localhost:8090'"))
  assert.ok(viteConfig.includes('target: apiProxyTarget'))
})

test('resolved compose config enforces service health and explicit shared-network security', (t) => {
  const compose = readRepoFile('docker-compose.local.yml')
  const config = readResolvedComposeConfig(t, {
    DATASPEC_BIND_HOST: '0.0.0.0',
    DATASPEC_SECURITY_ENABLED: 'true'
  })
  if (!config) {
    return
  }

  assert.doesNotMatch(compose, /^name:/m)
  assert.equal(config.services.server.environment.DATASPEC_SECURITY_ENABLED, 'true')
  assert.deepEqual(
    ['postgres', 'server', 'web'].map((service) => config.services[service].ports[0].host_ip),
    ['0.0.0.0', '0.0.0.0', '0.0.0.0']
  )
  assert.equal(config.services.server.depends_on.postgres.condition, 'service_healthy')
  assert.equal(config.services.web.depends_on.server.condition, 'service_healthy')
  assert.match(config.services.server.healthcheck.test.join(' '), /localhost:8090\/api-docs/)
  assert.match(config.services.web.healthcheck.test.join(' '), /localhost:5173\//)
  assert.match(config.services.web.command.at(-1), /exec node node_modules\/vite\/bin\/vite\.js/)
})

test('frontend lockfile rejects npmmirror tarball URLs', () => {
  const lockfile = readRepoFile('dataspec-web/pnpm-lock.yaml')
  const mirrorTarballs = lockfile.match(
    /tarball:\s*https:\/\/registry\.npmmirror\.com\/[^,}\s]+/g
  ) || []

  assert.deepEqual(
    mirrorTarballs,
    [],
    'shared lockfile must not contain npmmirror tarball URLs rejected by the Docker toolchain'
  )
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
