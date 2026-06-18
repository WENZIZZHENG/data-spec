import assert from 'node:assert/strict'
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { runCli } from './dataspec-cli.mjs'

test('lint reads sql file, posts to server, prints json, and returns 1 for errors', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const sqlPath = path.join(dir, 'bad.sql')
    await writeFile(sqlPath, 'CREATE TABLE UserOrder (userId bigint);', 'utf8')
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            errorCount: 1,
            warningCount: 0,
            suggestionCount: 0,
            issues: [{ ruleCode: 'table_naming_snake_case' }]
          }
        })
      }
    }
    const io = createIo()

    const code = await runCli([
      'lint',
      sqlPath,
      '--project',
      '7',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 1)
    assert.equal(calls[0].url, 'http://dataspec.local/api/lint')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      sql: 'CREATE TABLE UserOrder (userId bigint);',
      projectId: 7
    })
    assert.equal(JSON.parse(io.stdout).errorCount, 1)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint returns 0 when server reports no errors', async () => {
  const io = createIo('CREATE TABLE users (id bigserial);')
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: { errorCount: 0, warningCount: 1, suggestionCount: 0, issues: [] }
    })
  })

  const code = await runCli(['lint', '-', '--project', '1', '--format', 'json'], io, fetchFn)

  assert.equal(code, 0)
  assert.equal(JSON.parse(io.stdout).warningCount, 1)
})

test('lint-files scans sql files, aggregates json, and returns 1 for error files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const nestedDir = path.join(dir, 'migrations')
    await mkdir(nestedDir)
    await writeFile(path.join(dir, 'README.md'), '# ignored', 'utf8')
    await writeFile(path.join(dir, 'bad.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    await writeFile(path.join(nestedDir, 'good.sql'), 'CREATE TABLE user_order (id bigint);', 'utf8')

    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      const sql = JSON.parse(options.body).sql
      const hasError = sql.includes('UserOrder')
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            errorCount: hasError ? 1 : 0,
            warningCount: hasError ? 0 : 1,
            suggestionCount: 0,
            issues: hasError ? [{ ruleCode: 'table_naming_snake_case' }] : []
          }
        })
      }
    }
    const io = createIo()

    const code = await runCli([
      'lint-files',
      dir,
      '--project',
      '7',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 1)
    assert.equal(calls.length, 2)
    assert.deepEqual(output.summary, {
      totalFiles: 2,
      failedFiles: 1,
      errorCount: 1,
      warningCount: 1,
      suggestionCount: 0
    })
    assert.deepEqual(output.files.map((item) => path.basename(item.path)).sort(), ['bad.sql', 'good.sql'])
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-files returns 0 when no sql file has errors', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const sqlPath = path.join(dir, 'good.sql')
    await writeFile(sqlPath, 'CREATE TABLE user_order (id bigint);', 'utf8')
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: { errorCount: 0, warningCount: 0, suggestionCount: 1, issues: [] }
      })
    })
    const io = createIo()

    const code = await runCli(['lint-files', sqlPath, '--project', '1', '--format', 'json'], io, fetchFn)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.equal(output.summary.totalFiles, 1)
    assert.equal(output.summary.suggestionCount, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('review-pr posts markdown comment and returns 1 when lint has errors', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await writeFile(path.join(dir, 'bad.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const calls = []
    const fetchFn = async (url, options = {}) => {
      calls.push({ url, options })
      if (url === 'http://dataspec.local/api/lint') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: {
              errorCount: 1,
              warningCount: 0,
              suggestionCount: 0,
              issues: [
                {
                  severity: 'ERROR',
                  ruleCode: 'table_naming_snake_case',
                  message: '表名必须使用 snake_case',
                  tableName: 'UserOrder',
                  suggestion: '改为 user_order',
                  replacement: 'user_order'
                }
              ]
            }
          })
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/issues/42/comments?per_page=100') {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (url === 'https://api.github.com/repos/acme/app/issues/42/comments' && options.method === 'POST') {
        return { ok: true, status: 201, json: async () => ({ id: 99 }) }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo()

    const code = await runCli([
      'review-pr',
      dir,
      '--project',
      '7',
      '--repo',
      'acme/app',
      '--pr',
      '42',
      '--token',
      'ghs_test',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    const postCall = calls.find((call) => call.url.endsWith('/issues/42/comments') && call.options.method === 'POST')
    const commentBody = JSON.parse(postCall.options.body).body
    assert.equal(code, 1)
    assert.equal(postCall.options.headers.Authorization, 'Bearer ghs_test')
    assert.match(commentBody, /<!-- dataspec-sql-review -->/)
    assert.match(commentBody, /bad\.sql/)
    assert.match(commentBody, /table_naming_snake_case/)
    assert.match(commentBody, /user_order/)
    assert.match(io.stdout, /已创建 DataSpec Review 评论/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('review-pr updates existing markdown comment and returns 0 when lint passes', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await writeFile(path.join(dir, 'good.sql'), 'CREATE TABLE user_order (id bigint);', 'utf8')
    const calls = []
    const fetchFn = async (url, options = {}) => {
      calls.push({ url, options })
      if (url === 'http://localhost:8090/api/lint') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
          })
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/issues/9/comments?per_page=100') {
        return {
          ok: true,
          status: 200,
          json: async () => [{ id: 123, body: 'old\n<!-- dataspec-sql-review -->' }]
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/issues/comments/123' && options.method === 'PATCH') {
        return { ok: true, status: 200, json: async () => ({ id: 123 }) }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo()

    const code = await runCli([
      'review-pr',
      dir,
      '--project',
      '1',
      '--repo',
      'acme/app',
      '--pr',
      '9',
      '--token',
      'ghs_test'
    ], io, fetchFn)

    const patchCall = calls.find((call) => call.options.method === 'PATCH')
    const commentBody = JSON.parse(patchCall.options.body).body
    assert.equal(code, 0)
    assert.match(commentBody, /未发现 ERROR/)
    assert.match(io.stdout, /已更新 DataSpec Review 评论/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context downloads zip bytes to output path', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const outputPath = path.join(dir, 'dataspec-ai-context.zip')
    const fetchFn = async (url) => {
      assert.equal(url, 'http://localhost:8090/api/ai-context/package/download?projectId=9')
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => Uint8Array.from([1, 2, 3, 4]).buffer
      }
    }
    const io = createIo()

    const code = await runCli(['export-context', '--project', '9', '--output', outputPath], io, fetchFn)

    assert.equal(code, 0)
    assert.deepEqual([...await readFile(outputPath)], [1, 2, 3, 4])
    assert.match(io.stdout, /已导出/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('suggest-field calls field suggestion api and prints json', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: [
          {
            recommendedName: 'mobile_no',
            score: 92,
            matchReason: '显示名匹配',
            existing: true
          }
        ]
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'suggest-field',
    '用户手机号',
    '--project',
    '7',
    '--format',
    'json',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.equal(
    calls[0],
    'http://dataspec.local/api/fields/suggest?projectId=7&query=%E7%94%A8%E6%88%B7%E6%89%8B%E6%9C%BA%E5%8F%B7&limit=5'
  )
  assert.equal(JSON.parse(io.stdout)[0].recommendedName, 'mobile_no')
  assert.equal(io.stderr, '')
})

test('generate-ddl calls ddl preview api and prints json', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          ddl: 'CREATE TABLE user_order (id bigserial);',
          lintResult: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'generate-ddl',
    '--project',
    '7',
    '--template',
    '10',
    '--table',
    'user_order',
    '--format',
    'json',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.equal(
    calls[0],
    'http://dataspec.local/api/generator/ddl/preview?projectId=7&templateId=10&tableName=user_order'
  )
  assert.equal(JSON.parse(io.stdout).ddl, 'CREATE TABLE user_order (id bigserial);')
  assert.equal(io.stderr, '')
})

test('invalid arguments print stderr and return 2', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['lint', '-', '--project', '1', '--formta', 'json'], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /未知参数: --formta/)
  assert.equal(io.stdout, '')
})

function createIo(stdin = '') {
  return {
    stdin,
    stdout: '',
    stderr: '',
    writeOut(text) {
      this.stdout += text
    },
    writeErr(text) {
      this.stderr += text
    },
    async readStdin() {
      return this.stdin
    }
  }
}
