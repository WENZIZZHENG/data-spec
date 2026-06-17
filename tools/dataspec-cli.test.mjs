import assert from 'node:assert/strict'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
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
