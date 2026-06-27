import assert from 'node:assert/strict'
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { buildInlineReviewPlan, buildPullRequestLineMap, runCli } from './dataspec-cli.mjs'

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

test('lint uses local config defaults when project and server are omitted', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, server: 'http://dataspec.local/', apiToken: 'ds_config_token' }),
      'utf8'
    )
    const sqlPath = path.join(dir, 'good.sql')
    await writeFile(sqlPath, 'CREATE TABLE users (id bigserial);', 'utf8')
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
        })
      }
    }
    const io = createIo('', dir)

    const code = await runCli(['lint', sqlPath, '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/lint')
    assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_config_token')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      sql: 'CREATE TABLE users (id bigserial);',
      projectId: 7
    })
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('explicit lint options override local config defaults', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, server: 'http://dataspec.local', apiToken: 'ds_config_token' }),
      'utf8'
    )
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] }
        })
      }
    }
    const io = createIo('CREATE TABLE users (id bigserial);', dir)

    const code = await runCli([
      'lint',
      '-',
      '--project',
      '8',
      '--format',
      'json',
      '--server',
      'http://override.local/',
      '--dataspec-token',
      'ds_arg_token'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://override.local/api/lint')
    assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_arg_token')
    assert.equal(JSON.parse(calls[0].options.body).projectId, 8)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
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
            issues: hasError
              ? [{
                  ruleCode: 'table_naming_snake_case',
                  line: 1,
                  column: 14,
                  lineEnd: 1,
                  columnEnd: 23,
                  sourceStart: 13,
                  sourceEnd: 22,
                  locationKind: 'table'
                }]
              : []
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
    const badFile = output.files.find((item) => path.basename(item.path) === 'bad.sql')
    assert.equal(badFile.result.issues[0].line, 1)
    assert.equal(badFile.result.issues[0].columnEnd, 23)
    assert.equal(badFile.result.issues[0].locationKind, 'table')
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

test('lint-files uses default paths from local config when paths are omitted', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local',
        defaultPaths: ['sql']
      }),
      'utf8'
    )
    await writeFile(path.join(dir, 'sql', 'good.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { errorCount: 0, warningCount: 1, suggestionCount: 0, issues: [] }
        })
      }
    }
    const io = createIo('', dir)

    const code = await runCli(['lint-files', '--format', 'json'], io, fetchFn)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, 'http://dataspec.local/api/lint')
    assert.equal(JSON.parse(calls[0].options.body).projectId, 7)
    assert.equal(output.summary.totalFiles, 1)
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
                  line: 1,
                  column: 14,
                  lineEnd: 1,
                  columnEnd: 23,
                  sourceStart: 13,
                  sourceEnd: 22,
                  locationKind: 'table',
                  suggestion: '改为 user_order',
                  replacement: 'user_order'
                },
                {
                  severity: 'WARNING',
                  ruleCode: 'comment_missing',
                  message: '表缺少注释',
                  tableName: 'UserOrder',
                  line: null,
                  column: null
                }
              ]
            }
          })
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/issues/42/comments?per_page=100') {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42') {
        return { ok: true, status: 200, json: async () => ({ head: { sha: 'abc123' } }) }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/files?per_page=100') {
        return {
          ok: true,
          status: 200,
          json: async () => [{
            filename: 'bad.sql',
            patch: '@@ -0,0 +1,1 @@\n+CREATE TABLE UserOrder (id bigint);'
          }]
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/comments?per_page=100') {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/comments' && options.method === 'POST') {
        return { ok: true, status: 201, json: async () => ({ id: 199 }) }
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
    const inlineCall = calls.find((call) => call.url.endsWith('/pulls/42/comments') && call.options.method === 'POST')
    const commentBody = JSON.parse(postCall.options.body).body
    const inlineBody = JSON.parse(inlineCall.options.body)
    assert.equal(code, 1)
    assert.equal(postCall.options.headers.Authorization, 'Bearer ghs_test')
    assert.equal(inlineBody.commit_id, 'abc123')
    assert.equal(inlineBody.path, 'bad.sql')
    assert.equal(inlineBody.line, 1)
    assert.match(inlineBody.body, /dataspec-inline-review/)
    assert.match(inlineBody.body, /table_naming_snake_case/)
    assert.match(commentBody, /<!-- dataspec-sql-review -->/)
    assert.match(commentBody, /bad\.sql/)
    assert.match(commentBody, /table_naming_snake_case/)
    assert.match(commentBody, /行 1:14-1:23/)
    assert.doesNotMatch(commentBody, /行 0:0-0:0/)
    assert.match(commentBody, /user_order/)
    assert.match(commentBody, /Fallback 问题 \| 1/)
    assert.match(io.stdout, /已创建 DataSpec Review 评论；inline 创建 1，跳过 0，fallback 1/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('review-pr prints json summary when requested', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await writeFile(path.join(dir, 'bad.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const fetchFn = async (url, options = {}) => {
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
              issues: [{
                severity: 'ERROR',
                ruleCode: 'table_naming_snake_case',
                message: '表名必须使用 snake_case',
                line: 1,
                column: 14
              }]
            }
          })
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42') {
        return { ok: true, status: 200, json: async () => ({ head: { sha: 'abc123' } }) }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/files?per_page=100') {
        return {
          ok: true,
          status: 200,
          json: async () => [{
            filename: 'bad.sql',
            patch: '@@ -0,0 +1,1 @@\n+CREATE TABLE UserOrder (id bigint);'
          }]
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/comments?per_page=100') {
        return { ok: true, status: 200, json: async () => [] }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42/comments' && options.method === 'POST') {
        return { ok: true, status: 201, json: async () => ({ id: 199 }) }
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
      'http://dataspec.local',
      '--format',
      'json'
    ], io, fetchFn)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 1)
    assert.equal(output.reviewCommentAction, 'created')
    assert.equal(output.summary.failedFiles, 1)
    assert.equal(output.inline.inlineCommentsCreated, 1)
    assert.equal(output.inline.inlineCommentsSkipped, 0)
    assert.equal(output.inline.fallbackIssues, 0)
    assert.equal(output.files[0].path.endsWith('bad.sql'), true)
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

test('review-pr reports token and permission diagnosis when GitHub rejects inline setup', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await writeFile(path.join(dir, 'bad.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const fetchFn = async (url) => {
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
              issues: [{ severity: 'ERROR', ruleCode: 'table_naming_snake_case', line: 1 }]
            }
          })
        }
      }
      if (url === 'https://api.github.com/repos/acme/app/pulls/42') {
        return { ok: false, status: 403, json: async () => ({ message: 'forbidden' }) }
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

    assert.equal(code, 2)
    assert.match(io.stderr, /GitHub 请求失败，HTTP 403/)
    assert.match(io.stderr, /token、repo、pr 或权限/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('buildPullRequestLineMap parses new diff lines and normalizes paths', () => {
  const mapping = buildPullRequestLineMap([
    {
      filename: 'db\\migrations\\bad.sql',
      patch: [
        '@@ -1,2 +10,4 @@',
        ' context',
        '+CREATE TABLE UserOrder (id bigint);',
        '-old line',
        '+COMMENT ON TABLE UserOrder IS \'订单\';'
      ].join('\n')
    },
    {
      filename: 'db/large.sql',
      patch: undefined
    }
  ])

  assert.deepEqual(mapping, [
    { path: 'db/migrations/bad.sql', line: 11 },
    { path: 'db/migrations/bad.sql', line: 12 }
  ])
})

test('buildInlineReviewPlan creates, skips, and falls back by diff mapping', () => {
  const lintOutput = {
    files: [
      {
        path: 'C:\\repo\\db\\migrations\\bad.sql',
        result: {
          issues: [
            {
              severity: 'ERROR',
              ruleCode: 'table_naming_snake_case',
              message: '表名必须使用 snake_case',
              line: 11,
              column: 1
            },
            {
              severity: 'WARNING',
              ruleCode: 'comment_missing',
              message: '缺少注释',
              line: 99
            },
            {
              severity: 'SUGGESTION',
              ruleCode: 'recommended_field_name',
              message: '推荐字段名',
              line: null
            }
          ]
        }
      }
    ]
  }
  const prFiles = [{
    filename: 'db/migrations/bad.sql',
    patch: '@@ -1,1 +11,1 @@\n+CREATE TABLE UserOrder (id bigint);'
  }]

  const createdPlan = buildInlineReviewPlan(lintOutput, prFiles, [])
  assert.equal(createdPlan.comments.length, 1)
  assert.equal(createdPlan.comments[0].path, 'db/migrations/bad.sql')
  assert.equal(createdPlan.comments[0].line, 11)
  assert.equal(createdPlan.fallbackIssues.length, 2)
  assert.deepEqual(createdPlan.summary.fallbackReasons.map((item) => item.reason).sort(), [
    'issue_missing_line',
    'line_not_in_pr_diff'
  ])

  const skippedPlan = buildInlineReviewPlan(lintOutput, prFiles, [
    { body: '<!-- dataspec-inline-review:db%2Fmigrations%2Fbad.sql:11:table_naming_snake_case -->' }
  ])
  assert.equal(skippedPlan.comments.length, 0)
  assert.equal(skippedPlan.skipped.length, 1)
  assert.equal(skippedPlan.summary.inlineCommentsSkipped, 1)
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

test('export-context passes scoped AI context options', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const outputPath = path.join(dir, 'dataspec-ai-context.zip')
    const fetchFn = async (url) => {
      assert.equal(
        url,
        'http://localhost:8090/api/ai-context/package/download?projectId=9&scope=field&query=%E7%94%A8%E6%88%B7%E6%89%8B%E6%9C%BA&status=enabled&limit=20'
      )
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => Uint8Array.from([5, 6]).buffer
      }
    }
    const io = createIo()

    const code = await runCli([
      'export-context',
      '--project',
      '9',
      '--scope',
      'field',
      '--query',
      '用户手机',
      '--status',
      'enabled',
      '--limit',
      '20',
      '--output',
      outputPath
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.deepEqual([...await readFile(outputPath)], [5, 6])
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

test('cli ai contract keeps stable json fields while allowing additive fields', async () => {
  const io = createIo('CREATE TABLE UserOrder (id bigint);')
  const fetchFn = async (url, options) => {
    if (url.endsWith('/api/lint')) {
      assert.equal(JSON.parse(options.body).projectId, 7)
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            errorCount: 1,
            warningCount: 0,
            suggestionCount: 0,
            suppressedCount: 0,
            fixedSql: 'CREATE TABLE user_order (id bigint);',
            issues: [
              {
                severity: 'ERROR',
                ruleCode: 'table_naming_snake_case',
                message: '表名不符合 snake_case',
                tableName: 'UserOrder',
                replacement: 'user_order',
                line: 1,
                column: 14,
                locationKind: 'table',
                futureField: 'compatible-addition'
              }
            ],
            futureResultField: 'compatible-addition'
          }
        })
      }
    }
    throw new Error(`unexpected fetch: ${url}`)
  }

  const code = await runCli(['lint', '-', '--project', '7', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.errorCount, 1)
  assert.equal(output.suppressedCount, 0)
  assert.equal(output.fixedSql, 'CREATE TABLE user_order (id bigint);')
  assert.equal(output.issues[0].severity, 'ERROR')
  assert.equal(output.issues[0].ruleCode, 'table_naming_snake_case')
  assert.equal(output.issues[0].replacement, 'user_order')
  assert.equal(output.issues[0].locationKind, 'table')
  assert.equal(output.futureResultField, 'compatible-addition')
})

test('doctor prints json checks from local config and returns 0 when ready', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await writeFile(path.join(dir, 'sql', 'good.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local/',
        apiToken: 'ds_config_token',
        defaultPaths: ['sql']
      }),
      'utf8'
    )
    const calls = []
    const fetchFn = async (url, options = {}) => {
      calls.push({ url, options })
      if (url === 'http://dataspec.local/api-docs') {
        return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
      }
      if (url === 'http://dataspec.local/api/auth/me') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: { operatorName: 'alice', allProjects: false, projectIds: [7] }
          })
        }
      }
      if (url === 'http://dataspec.local/api/projects/7') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: { id: 7, name: '演示项目' }
          })
        }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo('', dir)

    const code = await runCli(['doctor', '--format', 'json'], io, fetchFn)

    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.equal(output.ok, true)
    assert.equal(output.server, 'http://dataspec.local')
    assert.equal(output.projectId, 7)
    assert.deepEqual(output.checks.map((check) => check.name), [
      'config',
      'server',
      'auth',
      'project',
      'defaultPaths',
      'openapi'
    ])
    assert.equal(output.checks.every((check) => check.status === 'pass'), true)
    assert.equal(calls[0].url, 'http://dataspec.local/api-docs')
    assert.equal(calls[1].options.headers.Authorization, 'Bearer ds_config_token')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('doctor returns 1 and reports failed checks when server is unreachable', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('connect ECONNREFUSED')
  }

  const code = await runCli([
    'doctor',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.ok, false)
  assert.equal(output.checks.some((check) => check.name === 'server' && check.status === 'fail'), true)
  assert.match(output.checks.find((check) => check.name === 'server').message, /connect ECONNREFUSED/)
  assert.equal(io.stderr, '')
})

test('doctor check-openapi reports schema drift failure', async () => {
  const fetchFn = async (url) => {
    if (url === 'http://dataspec.local/api-docs') {
      return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
    }
    if (url === 'http://dataspec.local/api/projects/7') {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { id: 7, name: '演示项目' }
        })
      }
    }
    throw new Error(`unexpected fetch: ${url}`)
  }
  const io = createIo()
  io.checkOpenapiDrift = async (server) => {
    assert.equal(server, 'http://dataspec.local')
    return { ok: false, message: 'OpenAPI schema.ts 已过期: src/api/schema.ts' }
  }

  const code = await runCli([
    'doctor',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json',
    '--check-openapi'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const openapiCheck = output.checks.find((check) => check.name === 'openapi')
  assert.equal(code, 1)
  assert.equal(openapiCheck.status, 'fail')
  assert.match(openapiCheck.message, /schema\.ts 已过期/)
  assert.equal(io.stderr, '')
})

test('doctor invalid format prints stderr and returns 2', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['doctor', '--format', 'xml'], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /doctor 当前仅支持 --format text 或 json/)
  assert.equal(io.stdout, '')
})

test('init creates dataspec files and prints json doctor result', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await mkdir(path.join(dir, 'db', 'migrations'), { recursive: true })
    const fetchFn = createReadyDoctorFetch('http://dataspec.local', 7)
    const io = createIo('', dir)

    const code = await runCli([
      'init',
      '--project',
      '7',
      '--server',
      'http://dataspec.local/',
      '--default-path',
      'sql',
      '--default-path',
      'db/migrations',
      '--format',
      'json'
    ], io, fetchFn)

    const config = JSON.parse(await readFile(path.join(dir, '.dataspec', 'config.json'), 'utf8'))
    const readme = await readFile(path.join(dir, '.dataspec', 'README.md'), 'utf8')
    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.deepEqual(config, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql', 'db/migrations']
    })
    assert.equal('apiToken' in config, false)
    assert.match(readme, /DATASPEC_TOKEN/)
    assert.equal(output.ok, true)
    assert.equal(output.configPath, path.join(dir, '.dataspec', 'config.json'))
    assert.deepEqual(output.writtenFiles.sort(), [
      path.join(dir, '.dataspec', 'README.md'),
      path.join(dir, '.dataspec', 'config.json')
    ].sort())
    assert.deepEqual(output.skippedFiles, [])
    assert.equal(output.doctor.projectId, 7)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('init skips existing files by default and force overwrites managed files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    const configPath = path.join(dir, '.dataspec', 'config.json')
    const readmePath = path.join(dir, '.dataspec', 'README.md')
    await writeFile(configPath, JSON.stringify({ projectId: 7, server: 'http://old.local', defaultPaths: ['sql'] }), 'utf8')
    await writeFile(readmePath, 'custom readme', 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'init',
      '--project',
      '8',
      '--server',
      'http://new.local',
      '--format',
      'json'
    ], io, createReadyDoctorFetch('http://new.local', 8))

    const output = JSON.parse(io.stdout)
    assert.equal(code, 0)
    assert.deepEqual(JSON.parse(await readFile(configPath, 'utf8')), {
      projectId: 7,
      server: 'http://old.local',
      defaultPaths: ['sql']
    })
    assert.equal(await readFile(readmePath, 'utf8'), 'custom readme')
    assert.deepEqual(output.writtenFiles, [])
    assert.deepEqual(output.skippedFiles.sort(), [configPath, readmePath].sort())

    const forceIo = createIo('', dir)
    const forceCode = await runCli([
      'init',
      '--project',
      '8',
      '--server',
      'http://new.local',
      '--default-path',
      'sql',
      '--force',
      '--format',
      'json'
    ], forceIo, createReadyDoctorFetch('http://new.local', 8))

    assert.equal(forceCode, 0)
    assert.deepEqual(JSON.parse(await readFile(configPath, 'utf8')), {
      projectId: 8,
      server: 'http://new.local',
      defaultPaths: ['sql']
    })
    assert.match(await readFile(readmePath, 'utf8'), /DataSpec 初始化/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('init with agents writes marker and replaces only with force', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    const agentsPath = path.join(dir, 'AGENTS.md')
    const firstIo = createIo('', dir)

    const firstCode = await runCli([
      'init',
      '--project',
      '7',
      '--server',
      'http://dataspec.local',
      '--default-path',
      'sql',
      '--with-agents',
      '--format',
      'json'
    ], firstIo, createReadyDoctorFetch('http://dataspec.local', 7))

    const firstAgents = await readFile(agentsPath, 'utf8')
    assert.equal(firstCode, 0)
    assert.match(firstAgents, /dataspec-agents:start/)
    assert.match(firstAgents, /project 7/)

    const secondIo = createIo('', dir)
    await runCli([
      'init',
      '--project',
      '8',
      '--server',
      'http://dataspec.local',
      '--with-agents',
      '--format',
      'json'
    ], secondIo, createReadyDoctorFetch('http://dataspec.local', 8))
    const secondAgents = await readFile(agentsPath, 'utf8')
    assert.match(secondAgents, /project 7/)
    assert.doesNotMatch(secondAgents, /project 8/)

    const forceIo = createIo('', dir)
    await runCli([
      'init',
      '--project',
      '8',
      '--server',
      'http://dataspec.local',
      '--default-path',
      'sql',
      '--with-agents',
      '--force',
      '--format',
      'json'
    ], forceIo, createReadyDoctorFetch('http://dataspec.local', 8))
    const forcedAgents = await readFile(agentsPath, 'utf8')
    assert.match(forcedAgents, /project 8/)
    assert.equal((forcedAgents.match(/dataspec-agents:start/g) ?? []).length, 1)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('init never writes dataspec token to generated files or output', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    const secretToken = 'ds_secret_token_should_not_be_written'
    const io = createIo('', dir)

    const code = await runCli([
      'init',
      '--project',
      '7',
      '--server',
      'http://dataspec.local',
      '--default-path',
      'sql',
      '--with-agents',
      '--dataspec-token',
      secretToken,
      '--format',
      'json'
    ], io, createReadyDoctorFetch('http://dataspec.local', 7))

    const generatedText = [
      await readFile(path.join(dir, '.dataspec', 'config.json'), 'utf8'),
      await readFile(path.join(dir, '.dataspec', 'README.md'), 'utf8'),
      await readFile(path.join(dir, 'AGENTS.md'), 'utf8'),
      io.stdout,
      io.stderr
    ].join('\n')
    assert.equal(code, 0)
    assert.doesNotMatch(generatedText, new RegExp(secretToken))
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('init rejects incomplete agents marker', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await writeFile(path.join(dir, 'AGENTS.md'), '<!-- dataspec-agents:start -->\n旧片段', 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'init',
      '--project',
      '7',
      '--server',
      'http://dataspec.local',
      '--default-path',
      'sql',
      '--with-agents',
      '--format',
      'json'
    ], io, createReadyDoctorFetch('http://dataspec.local', 7))

    assert.equal(code, 2)
    assert.match(io.stderr, /marker 不完整/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('init rejects repeated single-value options with readable error', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'init',
      '--project',
      '7',
      '--project',
      '8'
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.match(io.stderr, /参数不可重复: --project/)
    assert.equal(io.stdout, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('workflow list prints machine-readable recipe summaries without calling server', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'list', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.kind, 'dataspec-workflow-recipes')
  assert.deepEqual(output.recipes.map((recipe) => recipe.id), [
    'create-table',
    'review-pr-sql',
    'reverse-import-standards',
    'export-min-context'
  ])
  assert.equal(output.recipes[0].requiredInputs[0].name, 'projectId')
  assert.equal(io.stderr, '')
})

test('workflow show prints complete recipe with commands and recovery guidance', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'show', 'create-table', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.recipe.id, 'create-table')
  assert.ok(output.recipe.prechecks.length > 0)
  assert.ok(output.recipe.steps.some((step) => step.command.includes('export-context')))
  assert.ok(output.recipe.expectedArtifacts.length > 0)
  assert.ok(output.recipe.failureHandling.length > 0)
  assert.ok(output.recipe.nextActions.length > 0)
  assert.equal(output.recipe.sideEffectPolicy, 'plan-only')
})

test('workflow recipes do not suggest printing secret tokens', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'show', 'review-pr-sql', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const commands = [
    ...output.recipe.prechecks.map((precheck) => precheck.command),
    ...output.recipe.steps.map((step) => step.command)
  ].join('\n')
  assert.equal(code, 0)
  assert.doesNotMatch(commands, /echo "\$GITHUB_TOKEN"/)
  assert.match(commands, /process\.env\.GITHUB_TOKEN/)
})

test('workflow show rejects unknown recipe with supported ids', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'show', 'unknown-recipe', '--format', 'json'], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /未知 workflow recipe: unknown-recipe/)
  assert.match(io.stderr, /create-table/)
  assert.match(io.stderr, /export-min-context/)
  assert.equal(io.stdout, '')
})

test('init requires project id when config is absent', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-init-'))
  try {
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['init'], io, fetchFn)

    assert.equal(code, 2)
    assert.match(io.stderr, /需要提供 --project <id>/)
    assert.equal(io.stdout, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
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

function createReadyDoctorFetch(server, projectId) {
  return async (url) => {
    if (url === `${server}/api-docs`) {
      return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
    }
    if (url === `${server}/api/auth/me`) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { operatorName: 'cli-test' }
        })
      }
    }
    if (url === `${server}/api/projects/${projectId}`) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: { id: projectId, name: `project ${projectId}` }
        })
      }
    }
    throw new Error(`unexpected fetch: ${url}`)
  }
}

function createIo(stdin = '', cwd = process.cwd()) {
  return {
    stdin,
    cwd,
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
