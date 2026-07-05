import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { promisify } from 'node:util'
import { buildInlineReviewPlan, buildPullRequestLineMap, runCli } from './dataspec-cli.mjs'

const execFileAsync = promisify(execFile)

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

test('lint forwards idempotency key option as header', async () => {
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
  const io = createIo('CREATE TABLE users (id bigint);')

  const code = await runCli([
    'lint',
    '-',
    '--project',
    '1',
    '--format',
    'json',
    '--idempotency-key',
    'retry-lint-1'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.equal(calls[0].options.headers['Idempotency-Key'], 'retry-lint-1')
})

test('lint uses DATASPEC_IDEMPOTENCY_KEY when option is omitted', async () => {
  const previous = process.env.DATASPEC_IDEMPOTENCY_KEY
  process.env.DATASPEC_IDEMPOTENCY_KEY = 'env-retry-1'
  try {
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
    const io = createIo('CREATE TABLE users (id bigint);')

    const code = await runCli(['lint', '-', '--project', '1', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].options.headers['Idempotency-Key'], 'env-retry-1')
  } finally {
    if (previous === undefined) {
      delete process.env.DATASPEC_IDEMPOTENCY_KEY
    } else {
      process.env.DATASPEC_IDEMPOTENCY_KEY = previous
    }
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

test('lint uses configured aiProfile and lets explicit task type override config', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-profile-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local',
        aiProfile: 'sql-fix'
      }),
      'utf8'
    )
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({ code: 200, data: { errorCount: 0, warningCount: 0, suggestionCount: 0, issues: [] } })
      }
    }
    const io = createIo('CREATE TABLE users (id bigint);', dir)

    const code = await runCli(['lint', '-', '--task-type', 'PR_REVIEW', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      sql: 'CREATE TABLE users (id bigint);',
      projectId: 7,
      taskType: 'PR_REVIEW'
    })
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint text output prints dialect diagnostics summary', async () => {
  const io = createIo('CREATE TABLE `user_order` (id bigint);')
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        errorCount: 1,
        warningCount: 0,
        suggestionCount: 0,
        fixedSql: 'CREATE TABLE user_order (id bigint);',
        dialectDiagnostics: [
          {
            dialect: 'mysql',
            capability: 'DIALECT_DETECTION',
            level: 'INFO',
            code: 'MYSQL_DIALECT_INFERRED',
            message: '检测到 MySQL DDL 特征',
            nextAction: '继续检查'
          },
          {
            dialect: 'mysql',
            capability: 'FIXED_SQL',
            level: 'WARNING',
            code: 'MYSQL_FIXED_SQL_REVIEW_REQUIRED',
            message: 'fixedSql 需要人工复核',
            nextAction: '不要直接覆盖 MySQL 迁移文件'
          }
        ],
        issues: [{ severity: 'ERROR', ruleCode: 'table_naming_snake_case', message: '表名需使用 snake_case' }]
      }
    })
  })

  const code = await runCli(['lint', '-', '--project', '1', '--format', 'text'], io, fetchFn)

  assert.equal(code, 1)
  assert.match(io.stdout, /Dialect: MySQL \(1 compatibility notes\)/)
  assert.match(io.stdout, /MYSQL_FIXED_SQL_REVIEW_REQUIRED/)
  assert.match(io.stdout, /fixedSql: available/)
})

test('lint prints machine-readable DataSpecError when api returns diagnostic', async () => {
  const io = createIo('CREATE TABLE users (id bigserial);')
  const fetchFn = async () => ({
    ok: false,
    status: 400,
    json: async () => ({
      code: 400,
      message: 'projectId 参数无效: abc',
      error: {
        code: 'PROJECT_ID_INVALID',
        category: 'VALIDATION',
        retryable: true,
        suggestedAction: '提供有效 projectId；不确定时先运行 dataspec doctor --format json 查看当前项目状态。',
        docsRef: 'README.md#验证'
      }
    })
  })

  const code = await runCli(['lint', '-', '--project', '1', '--format', 'json'], io, fetchFn)
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.match(io.stderr, /错误: projectId 参数无效: abc/)
  assert.equal(diagnostic.code, 'PROJECT_ID_INVALID')
  assert.equal(diagnostic.category, 'VALIDATION')
  assert.equal(diagnostic.retryable, true)
  assert.equal(diagnostic.httpStatus, 400)
  assert.equal(io.stdout, '')
})

test('search-fields passes filters and prints stable json', async () => {
  const calls = []
  const fetchFn = async (url, options) => {
    calls.push({ url, options })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          projectId: 7,
          query: '手机号',
          summary: { matchedCount: 1, returnedCount: 1, appliedFilters: { category: 'contact' } },
          items: [{ field: { name: 'mobile_no' }, score: 98, matchReasons: ['别名匹配'] }],
          nextActions: ['优先查看首个高分字段']
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'search-fields',
    '手机号',
    '--project',
    '7',
    '--category',
    'contact',
    '--tag',
    'pii',
    '--sensitive',
    'true',
    '--source-batch',
    '3',
    '--limit',
    '10',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const url = new URL(calls[0].url)
  assert.equal(code, 0)
  assert.equal(url.pathname, '/api/fields/search')
  assert.equal(url.searchParams.get('projectId'), '7')
  assert.equal(url.searchParams.get('query'), '手机号')
  assert.equal(url.searchParams.get('category'), 'contact')
  assert.equal(url.searchParams.get('tag'), 'pii')
  assert.equal(url.searchParams.get('sensitive'), 'true')
  assert.equal(url.searchParams.get('sourceBatchId'), '3')
  assert.equal(url.searchParams.get('limit'), '10')
  assert.equal(JSON.parse(io.stdout).items[0].field.name, 'mobile_no')
  assert.equal(io.stderr, '')
})

test('search-fields prints DataSpecError when api returns diagnostic', async () => {
  const io = createIo()
  const fetchFn = async () => ({
    ok: false,
    status: 400,
    json: async () => ({
      code: 400,
      message: '字段检索需要 query 或至少一个过滤条件',
      error: {
        code: 'VALIDATION_FAILED',
        category: 'VALIDATION',
        retryable: true,
        suggestedAction: '补充 query 或过滤条件。',
        docsRef: 'README.md#字段标准检索'
      }
    })
  })

  const code = await runCli(['search-fields', '--project', '7', '--format', 'json'], io, fetchFn)
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.match(io.stderr, /字段检索需要/)
  assert.equal(diagnostic.code, 'VALIDATION_FAILED')
  assert.equal(io.stdout, '')
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

test('lint-files writes AI batch delivery package without changing json stdout', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const sqlPath = path.join(dir, 'bad.sql')
    const packagePath = path.join(dir, 'out', 'ai-batch.json')
    await writeFile(sqlPath, 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          errorCount: 1,
          warningCount: 0,
          suggestionCount: 0,
          fixedSql: "CREATE TABLE user_order (id bigint); -- password='quoted-secret' token=ds_secret jdbc:postgresql://localhost/db",
          fixedSqlDiff: '--- before\n+++ after\n',
          issues: [{ ruleCode: 'table_naming_snake_case', ruleName: '表名 snake_case', message: 'Bearer abc' }],
          dialectDiagnostics: [{
            code: 'SECRET',
            params: {
              tokenHash: 'hash_raw',
              dbPassword: 'nested-secret',
              githubApiKey: 'api_raw',
              connectionString: 'jdbc:mysql://localhost/app'
            }
          }],
          errorMessage: 'Authorization: Bearer raw.jwt.token'
        }
      })
    })
    const io = createIo()

    const code = await runCli([
      'lint-files',
      sqlPath,
      '--project',
      '7',
      '--format',
      'json',
      '--delivery-package',
      packagePath
    ], io, fetchFn)

    const stdout = JSON.parse(io.stdout)
    const pkg = JSON.parse(await readFile(packagePath, 'utf8'))
    assert.equal(code, 1)
    assert.deepEqual(Object.keys(stdout).sort(), ['files', 'summary'])
    assert.equal(pkg.packageVersion, 'ai-batch-delivery@1')
    assert.equal(pkg.projectId, 7)
    assert.equal(pkg.batchType, 'SQL_LINT')
    assert.equal(pkg.source, 'cli')
    assert.equal(pkg.summary.totalItems, 1)
    assert.equal(pkg.issueSummary.errorCount, 1)
    assert.equal(pkg.items[0].fixedSqlAvailable, true)
    assert.doesNotMatch(JSON.stringify(pkg), /quoted-secret|ds_secret|Bearer abc|raw\.jwt\.token|hash_raw|nested-secret|api_raw|jdbc:postgresql:\/\/localhost\/db|jdbc:mysql:\/\/localhost\/app/)
    assert.match(JSON.stringify(pkg), /Authorization: Bearer \*\*\*/)
    assert.match(JSON.stringify(pkg), /"\*\*\*"/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-files rejects duplicate delivery package options before server calls', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const sqlPath = path.join(dir, 'bad.sql')
    await writeFile(sqlPath, 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    let called = false
    const fetchFn = async () => {
      called = true
      throw new Error('fetch should not be called')
    }
    const io = createIo()

    const code = await runCli([
      'lint-files',
      sqlPath,
      '--project',
      '7',
      '--delivery-package',
      path.join(dir, 'ai-batch.json'),
      '--batch-package',
      path.join(dir, 'batch.json')
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.match(io.stderr, /请只使用 --delivery-package 或 --batch-package 之一/)
    assert.equal(called, false)
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

test('changed discovers configured git changes and recommends minimal context', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-changed-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql', 'models']
    })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await mkdir(path.join(dir, 'models'), { recursive: true })
    await mkdir(path.join(dir, 'docs'), { recursive: true })
    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const user = {}\n', 'utf8')
    await writeFile(path.join(dir, 'docs', 'old.sql'), 'CREATE TABLE docs_old (id bigint);', 'utf8')
    await git(dir, ['add', '.'])
    await git(dir, ['commit', '-m', 'baseline'])

    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'sql', 'new_order.sql'), 'CREATE TABLE order_items (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const userOrder = {}\n', 'utf8')
    await writeFile(path.join(dir, 'docs', 'old.sql'), 'CREATE TABLE docs_changed (id bigint);', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['changed', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec.changed-workflow')
    assert.equal(output.config.rootDir, '.')
    assert.equal(path.isAbsolute(output.config.configPath), false)
    assert.equal(output.contextRecommendation.scope, 'changed')
    assert.match(output.contextRecommendation.command, /export-context .*--scope changed/)
    assert.deepEqual(output.files.sql.map((item) => item.path).sort(), ['sql/existing.sql', 'sql/new_order.sql'])
    assert.equal('absolutePath' in output.files.sql[0], false)
    assert.ok(output.files.other.some((item) => item.path === 'models/user-model.ts'))
    assert.equal(output.summary.sqlFiles, 2)
    assert.equal(output.summary.ignoredFiles, 1)
    assert.ok(output.nextActions.some((action) => action.includes('lint-changed')))
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-changed lints only changed sql files and keeps context recommendation', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-lint-changed-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql', 'models']
    })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await mkdir(path.join(dir, 'models'), { recursive: true })
    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const user = {}\n', 'utf8')
    await git(dir, ['add', '.'])
    await git(dir, ['commit', '-m', 'baseline'])

    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'sql', 'new_order.sql'), 'CREATE TABLE order_items (id bigint);', 'utf8')
    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const userOrder = {}\n', 'utf8')
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
    const io = createIo('', dir)

    const code = await runCli(['lint-changed', '--format', 'json'], io, fetchFn)

    assert.equal(code, 1)
    const output = JSON.parse(io.stdout)
    assert.equal(calls.length, 2)
    assert.equal(calls.every((call) => call.url === 'http://dataspec.local/api/lint'), true)
    assert.equal(calls.some((call) => JSON.parse(call.options.body).sql.includes('userOrder')), false)
    assert.deepEqual(output.changed.files.sql.map((item) => item.path).sort(), ['sql/existing.sql', 'sql/new_order.sql'])
    assert.equal('absolutePath' in output.changed.files.sql[0], false)
    assert.equal(output.lint.summary.totalFiles, 2)
    assert.equal(output.lint.summary.failedFiles, 1)
    assert.deepEqual(output.lint.files.map((item) => item.path).sort(), ['sql/existing.sql', 'sql/new_order.sql'])
    assert.equal(output.contextRecommendation.scope, 'changed')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-changed reports recoverable diagnostic when there are no changed sql files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-lint-changed-no-sql-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['models']
    })
    await mkdir(path.join(dir, 'models'), { recursive: true })
    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const user = {}\n', 'utf8')
    await git(dir, ['add', '.'])
    await git(dir, ['commit', '-m', 'baseline'])

    await writeFile(path.join(dir, 'models', 'user-model.ts'), 'export const userOrder = {}\n', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['lint-changed', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.diagnostics[0].code, 'NO_CHANGED_SQL_FILES')
    assert.equal(output.lint.summary.totalFiles, 0)
    assert.ok(output.nextActions.some((action) => action.includes('changed')))
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('changed reports recoverable diagnostic outside git repository', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-no-git-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: ['sql'] }),
      'utf8'
    )
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['changed', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.summary.totalFiles, 0)
    assert.equal(output.diagnostics[0].code, 'NO_GIT_REPOSITORY')
    assert.match(output.nextActions.join('\n'), /git init|在 git 仓库/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('changed avoids full scan when default paths are missing or no changes exist', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-no-default-paths-'))
  try {
    await initGitRepo(dir, { projectId: 7, defaultPaths: [] })
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const missingPathsCode = await runCli(['changed', '--format', 'json'], io, fetchFn)

    assert.equal(missingPathsCode, 0)
    const missingPathsOutput = JSON.parse(io.stdout)
    assert.equal(missingPathsOutput.diagnostics[0].code, 'DATASPEC_DEFAULT_PATHS_MISSING')
    assert.match(missingPathsOutput.nextActions.join('\n'), /defaultPaths|dataspec init/)

    io.stdout = ''
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: ['sql'] }),
      'utf8'
    )
    await git(dir, ['add', '.dataspec/config.json'])
    await git(dir, ['commit', '-m', 'configure paths'])

    const noChangesCode = await runCli(['changed', '--format', 'json'], io, fetchFn)

    assert.equal(noChangesCode, 0)
    const noChangesOutput = JSON.parse(io.stdout)
    assert.equal(noChangesOutput.summary.totalFiles, 0)
    assert.equal(noChangesOutput.diagnostics[0].code, 'NO_CHANGED_FILES')
    assert.equal(io.stderr, '')
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
    await assert.rejects(readFile(path.join(dir, '.dataspec', 'context', 'cache-metadata.json')), /ENOENT/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context cache writes AI context files and redacted metadata', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-cache-'))
  try {
    const zip = makeZip({
      '.dataspec/manifest.json': JSON.stringify({
        schemaVersion: 1,
        kind: 'dataspec-ai-context',
        projectId: 9,
        generatedAt: '2026-06-28T01:00:00Z',
        standard: { specVersion: 'v1', specHash: 'hash-1', source: 'current' }
      }),
      '.dataspec/capabilities.json': '{"kind":"dataspec-ai-capability-catalog","capabilities":[]}',
      '.dataspec/field-catalog.json': '{"fields":[]}',
      '.dataspec/rules.yaml': 'rules: []\n',
      'AGENTS.md.fragment': 'Read DataSpec context'
    })
    const fetchFn = async (url) => {
      assert.match(url, /query=token%3Dabc/)
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength)
      }
    }
    const io = createIo('', dir)

    const code = await runCli([
      'export-context',
      '--project',
      '9',
      '--server',
      'http://token:secret@dataspec.local',
      '--scope',
      'field',
      '--query',
      'token=abc Bearer abc jdbc:postgresql://localhost/db',
      '--cache',
      '--cache-ttl-days',
      '3'
    ], io, fetchFn)

    const metadataText = await readFile(path.join(dir, '.dataspec', 'context', 'cache-metadata.json'), 'utf8')
    const metadata = JSON.parse(metadataText)
    assert.equal(code, 0)
    assert.equal(await readFile(path.join(dir, '.dataspec', 'context', 'field-catalog.json'), 'utf8'), '{"fields":[]}')
    assert.equal(await readFile(path.join(dir, '.dataspec', 'context', 'AGENTS.md.fragment'), 'utf8'), 'Read DataSpec context')
    assert.equal(metadata.projectId, 9)
    assert.equal(
      await readFile(path.join(dir, '.dataspec', 'context', 'capabilities.json'), 'utf8'),
      '{"kind":"dataspec-ai-capability-catalog","capabilities":[]}'
    )
    assert.equal(metadata.standard.specHash, 'hash-1')
    assert.equal(metadata.ttlDays, 3)
    assert.equal(metadata.server, 'http://dataspec.local')
    assert.match(metadataText, /token=\*\*\*/)
    assert.doesNotMatch(metadataText, /token=abc|Bearer abc|jdbc:postgresql|token:secret/)
    assert.match(io.stdout, /已缓存 AI Context/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context can write zip output and cache together', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-cache-'))
  try {
    const outputPath = path.join(dir, 'context.zip')
    const zip = makeZip({
      '.dataspec/manifest.json': JSON.stringify({
        standard: { specVersion: 'v1', specHash: 'hash-1', source: 'current' }
      })
    })
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength)
    })
    const io = createIo('', dir)

    const code = await runCli(['export-context', '--project', '9', '--output', outputPath, '--cache'], io, fetchFn)

    assert.equal(code, 0)
    assert.equal((await readFile(outputPath)).length, zip.length)
    assert.equal(JSON.parse(await readFile(path.join(dir, '.dataspec', 'context', 'cache-metadata.json'), 'utf8')).contentHash.length, 64)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context cache rejects unsafe zip paths', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-cache-'))
  try {
    const contextDir = path.join(dir, '.dataspec', 'context')
    const oldMetadataPath = path.join(contextDir, 'cache-metadata.json')
    await mkdir(contextDir, { recursive: true })
    await writeFile(oldMetadataPath, '{"kind":"old-cache"}\n', 'utf8')
    const zip = makeZip({
      '.dataspec/manifest.json': JSON.stringify({ standard: { specHash: 'new' } }),
      '../evil.txt': 'bad'
    })
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength)
    })
    const io = createIo('', dir)

    const code = await runCli(['export-context', '--project', '9', '--cache'], io, fetchFn)

    assert.equal(code, 2)
    assert.match(io.stderr, /越界路径/)
    await assert.rejects(readFile(path.join(dir, 'evil.txt')), /ENOENT/)
    assert.equal(await readFile(oldMetadataPath, 'utf8'), '{"kind":"old-cache"}\n')
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

test('export-context forwards profile defaults when no explicit scope is provided', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const outputPath = path.join(dir, 'dataspec-ai-context.zip')
    const fetchFn = async (url) => {
      assert.equal(
        url,
        'http://localhost:8090/api/ai-context/package/download?projectId=9&profileId=minimal-context'
      )
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => Uint8Array.from([9, 9]).buffer
      }
    }
    const io = createIo()

    const code = await runCli([
      'export-context',
      '--project',
      '9',
      '--profile',
      'minimal-context',
      '--output',
      outputPath
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.deepEqual([...await readFile(outputPath)], [9, 9])
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('profile list prints machine-readable profile catalog', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          projectId: 7,
          defaultProfileId: 'create-table',
          selectedProfileId: 'sql-fix',
          profiles: [
            {
              profileId: 'sql-fix',
              taskType: 'SQL_FIX',
              contextScope: { scope: 'field', status: 'enabled', limit: 60 },
              fixedSqlPolicy: { mode: 'DRY_RUN', maxRiskLevel: 'LOW' },
              outputFormat: { format: 'json+diff' },
              recommendedCommands: ['dataspec lint <file.sql> --profile sql-fix --format json']
            }
          ],
          diagnostics: [],
          supportedTaskTypes: ['SQL_FIX']
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'profile',
    'list',
    '--project',
    '7',
    '--profile',
    'sql-fix',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0], 'http://dataspec.local/api/ai-profiles?projectId=7&profile=sql-fix')
  assert.equal(output.profiles[0].profileId, 'sql-fix')
  assert.equal(output.profiles[0].fixedSqlPolicy.mode, 'DRY_RUN')
})

test('profile show returns 2 for unknown profile with supported values', async () => {
  const fetchFn = async (url) => {
    assert.equal(url, 'http://dataspec.local/api/ai-profiles/missing?projectId=7')
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          requestedProfile: 'missing',
          profile: null,
          supportedProfileIds: ['create-table', 'sql-fix'],
          supportedTaskTypes: ['CREATE_TABLE', 'SQL_FIX'],
          diagnostics: [{ code: 'UNKNOWN_AI_PROFILE', status: 'fail' }]
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'profile',
    'show',
    'missing',
    '--project',
    '7',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /未知 AI profile/)
  assert.match(io.stderr, /create-table/)
})

test('contract list prints machine-readable registry catalog', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: contractCatalogFixture()
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'contract',
    'list',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0], 'http://dataspec.local/api/contracts')
  assert.equal(output.kind, 'dataspec-schema-registry')
  assert.equal(output.contracts[0].contractId, 'field')
  assert.equal(output.compatibilityPolicy.level, 'stable-ai-contract')
})

test('contract show prints contract detail', async () => {
  const fetchFn = async (url) => {
    assert.equal(url, 'http://dataspec.local/api/contracts/lint-result')
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          contractId: 'lint-result',
          displayName: 'SQL 校验结果',
          schemaVersion: '1.0',
          jsonSchemaRef: 'dataspec://contracts/lint-result@1.0',
          jsonSchema: { type: 'object', properties: { fixedSql: { type: 'string' } } },
          stableFields: ['issues[]', 'fixedSql', 'dialectDiagnostics[]'],
          deprecatedFields: [],
          compatibility: { breakingChangePolicy: '更新 schemaVersion' },
          docsRef: 'docs/ai-contracts.md#lint-result',
          examples: [{ errorCount: 1 }]
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'contracts',
    'show',
    'lint-result',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.contractId, 'lint-result')
  assert.deepEqual(output.deprecatedFields, [])
  assert.equal(output.jsonSchema.properties.fixedSql.type, 'string')
})

test('contract check reports missing registry invariants', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        ...contractCatalogFixture(),
        contracts: contractCatalogFixture().contracts.filter((item) => item.contractId !== 'lint-result')
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'contract',
    'check',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 2)
  assert.equal(output.ok, false)
  assert.match(JSON.stringify(output.diagnostics), /lint-result/)
})

test('capability list prints machine-readable catalog with project diagnostics', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: capabilityCatalogFixture()
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'capability',
    'list',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0], 'http://dataspec.local/api/capabilities?projectId=7')
  assert.equal(output.kind, 'dataspec-ai-capability-catalog')
  assert.equal(output.projectId, 7)
  assert.ok(output.capabilities.some((capability) => capability.id === 'session-bootstrap'))
  assert.ok(output.capabilities.some((capability) => capability.id === 'capability-catalog'))
})

test('capability show prints capability detail', async () => {
  const fetchFn = async (url) => {
    assert.equal(url, 'http://dataspec.local/api/capabilities/lint-sql?projectId=7')
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: capabilityCatalogFixture().capabilities.find((item) => item.id === 'lint-sql')
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'capabilities',
    'show',
    'lint-sql',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.id, 'lint-sql')
  assert.equal(output.writeRisk, 'WRITES_DATASPEC_RECORD')
  assert.match(output.cliCommands[0], /dataspec lint/)
})

test('capability show normalizes unknown id as parameter diagnostic', async () => {
  const fetchFn = async () => ({
    ok: false,
    status: 404,
    json: async () => ({
      code: 404,
      message: '未知 DataSpec capability: missing。请先读取 /api/capabilities 获取可用能力。'
    })
  })
  const io = createIo()

  const code = await runCli([
    'capability',
    'show',
    'missing',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.equal(diagnostic.code, 'CAPABILITY_NOT_FOUND')
  assert.equal(diagnostic.category, 'PARAMETER')
  assert.equal(diagnostic.retryable, false)
  assert.match(diagnostic.suggestedAction, /capability list/)
})

test('capability check reports missing core capability ids', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        ...capabilityCatalogFixture(),
        capabilities: capabilityCatalogFixture().capabilities.filter((item) => item.id !== 'lint-sql')
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'capability',
    'check',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 2)
  assert.equal(output.ok, false)
  assert.match(JSON.stringify(output.diagnostics), /lint-sql/)
})

test('capability list prints DataSpecError when server is unavailable', async () => {
  const fetchFn = async () => {
    throw new Error('ECONNREFUSED')
  }
  const io = createIo()

  const code = await runCli(['capability', 'list', '--server', 'http://dataspec.local', '--format', 'json'], io, fetchFn)
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.equal(diagnostic.code, 'DATASPEC_SERVER_UNAVAILABLE')
  assert.match(diagnostic.suggestedAction, /doctor/)
})

test('bootstrap prints server session package and hides token value', async () => {
  const calls = []
  const fetchFn = async (url, options = {}) => {
    calls.push({ url, options })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: sessionBootstrapFixture()
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'bootstrap',
    '--project',
    '7',
    '--server',
    'http://dataspec.local/',
    '--dataspec-token',
    'ds_secret_token',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0].url, 'http://dataspec.local/api/bootstrap/session?projectId=7&server=http%3A%2F%2Fdataspec.local')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_secret_token')
  assert.equal(output.kind, 'dataspec-ai-session-bootstrap')
  assert.equal(output.status, 'READY')
  assert.equal(output.projectId, 7)
  assert.match(output.recommendedCommands.join('\n'), /dataspec lint/)
  assert.doesNotMatch(io.stdout, /ds_secret_token|Authorization|Bearer/)
  assert.equal(io.stderr, '')
})

test('bootstrap returns structured fallback when server is unavailable', async () => {
  const fetchFn = async () => {
    throw new Error('ECONNREFUSED')
  }
  const io = createIo()

  const code = await runCli([
    'bootstrap',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--dataspec-token',
    'ds_secret_token',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.kind, 'dataspec-ai-session-bootstrap')
  assert.equal(output.status, 'BLOCKED')
  assert.equal(output.server, 'http://dataspec.local')
  assert.equal(output.projectId, 7)
  assert.equal(output.authMode, 'TOKEN_PRESENT')
  assert.equal(output.nextActions[0].code, 'RUN_DOCTOR')
  assert.match(output.nextActions.map((action) => action.command).join('\n'), /doctor/)
  assert.doesNotMatch(io.stdout, /ds_secret_token|Authorization|Bearer/)
  assert.equal(io.stderr, '')
})

test('bootstrap returns auth next action when server rejects token', async () => {
  const fetchFn = async () => ({
    ok: false,
    status: 401,
    json: async () => ({
      code: 401,
      message: 'API Token 无效',
      error: {
        code: 'AUTH_TOKEN_MISSING_OR_INVALID',
        category: 'AUTH',
        retryable: true,
        suggestedAction: '重新设置 DATASPEC_TOKEN 或 --dataspec-token。',
        docsRef: 'README.md#安全基线'
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'bootstrap',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--dataspec-token',
    'expired_token',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.status, 'BLOCKED')
  assert.equal(output.checks.find((check) => check.name === 'server').status, 'pass')
  assert.equal(output.checks.find((check) => check.name === 'auth').status, 'fail')
  assert.equal(output.nextActions[0].code, 'AUTH_TOKEN_MISSING_OR_INVALID')
  assert.doesNotMatch(output.nextActions.map((action) => action.code).join('\n'), /START_DATASPEC_SERVER/)
  assert.doesNotMatch(io.stdout, /expired_token|Authorization|Bearer/)
  assert.equal(io.stderr, '')
})

test('bootstrap fallback redacts server userinfo in commands', async () => {
  const fetchFn = async () => {
    throw new Error('ECONNREFUSED')
  }
  const io = createIo()

  const code = await runCli([
    'bootstrap',
    '--project',
    '7',
    '--server',
    'http://user:p@ss@dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.server, 'http://dataspec.local')
  assert.match(output.recommendedCommands.join('\n'), /--server http:\/\/dataspec\.local/)
  assert.doesNotMatch(io.stdout, /user:p|p@ss|user%3A|p%40ss|http:\/\/user/)
  assert.equal(io.stderr, '')
})

test('bootstrap text output summarizes status and next commands', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        ...sessionBootstrapFixture(),
        status: 'DEGRADED',
        checks: [{ name: 'standard', status: 'warn', message: '当前项目还没有版本化标准快照' }]
      }
    })
  })
  const io = createIo()

  const code = await runCli(['bootstrap', '--project', '7', '--format', 'text'], io, fetchFn)

  assert.equal(code, 1)
  assert.match(io.stdout, /AI Session Bootstrap/)
  assert.match(io.stdout, /DEGRADED/)
  assert.match(io.stdout, /dataspec doctor/)
  assert.match(io.stdout, /当前项目还没有版本化标准快照/)
})

test('task list prints paginated task run json with filters', async () => {
  const calls = []
  const fetchFn = async (url, init) => {
    calls.push({ url, init })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          records: [taskRunListItemFixture()],
          total: 1,
          current: 2,
          size: 5
        }
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'task',
    'list',
    '--project',
    '7',
    '--status',
    'FAILED',
    '--task-type',
    'SQL_LINT',
    '--current',
    '2',
    '--size',
    '5',
    '--server',
    'http://dataspec.local',
    '--dataspec-token',
    'ds_cli_token',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const url = new URL(calls[0].url)
  assert.equal(code, 0)
  assert.equal(url.pathname, '/api/ai-task-runs')
  assert.equal(url.searchParams.get('projectId'), '7')
  assert.equal(url.searchParams.get('status'), 'FAILED')
  assert.equal(url.searchParams.get('taskType'), 'SQL_LINT')
  assert.equal(url.searchParams.get('current'), '2')
  assert.equal(url.searchParams.get('size'), '5')
  assert.equal(calls[0].init.headers.Authorization, 'Bearer ds_cli_token')
  assert.equal(output.records[0].resumeCommand, 'node tools/dataspec-cli.mjs task show 91 --project 7 --format json')
  assert.equal(io.stderr, '')
})

test('task failures prints recent failed task runs', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: [
          taskRunListItemFixture({
            id: 92,
            status: 'PARTIAL_FAILED',
            retryable: true,
            nextAction: '复制 resumeCommand 后只重试失败 SQL 文件。'
          })
        ]
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'tasks',
    'failures',
    '--project',
    '7',
    '--limit',
    '3',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0], 'http://dataspec.local/api/ai-task-runs/recent-failures?projectId=7&limit=3')
  assert.equal(output[0].id, 92)
  assert.equal(output[0].retryable, true)
  assert.match(output[0].nextAction, /resumeCommand/)
})

test('task show prints task run detail', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: taskRunDetailFixture()
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'task',
    'show',
    '91',
    '--project',
    '7',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0], 'http://dataspec.local/api/ai-task-runs/91?projectId=7')
  assert.equal(output.id, 91)
  assert.equal(output.stepStatus[0].step, 'lint-items')
  assert.equal(output.partialArtifacts[0].type, 'sql-lint-result')
  assert.equal(output.partialArtifacts[0].ref, 'sql-lint-result:good.sql')
  assert.equal(output.resumeCommand, 'node tools/dataspec-cli.mjs task show 91 --project 7 --format json')
})

test('task command prints DataSpecError when server is unavailable', async () => {
  const io = createIo()
  const code = await runCli([
    'task',
    'failures',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, async () => {
    throw new Error('ECONNREFUSED')
  })
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.equal(diagnostic.code, 'DATASPEC_SERVER_UNAVAILABLE')
  assert.equal(diagnostic.retryable, true)
  assert.doesNotMatch(io.stderr, /Authorization|ds_cli_token|password=/)
})

test('quality-gate check prints pass result and exits zero', async () => {
  const calls = []
  const fetchFn = async (url, options = {}) => {
    calls.push({ url, options })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: qualityGateResultFixture({ status: 'PASS' })
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'quality-gate',
    'check',
    '--project',
    '7',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(calls[0].url, 'http://dataspec.local/api/quality-gate/evaluate')
  assert.equal(calls[0].options.method, 'POST')
  assert.equal(JSON.parse(calls[0].options.body).projectId, 7)
  assert.equal(output.status, 'PASS')
})

test('quality-gate check exits one when gate fails', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: qualityGateResultFixture({
        status: 'FAIL',
        failedChecks: [
          {
            code: 'coverage_rate',
            status: 'FAIL',
            nextAction: '处理未纳管字段并重新生成覆盖率报告'
          }
        ]
      })
    })
  })
  const io = createIo()

  const code = await runCli([
    'quality',
    'check',
    '--project',
    '7',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.status, 'FAIL')
  assert.equal(output.failedChecks[0].code, 'coverage_rate')
})

test('quality-gate check prints DataSpecError when server is unavailable', async () => {
  const io = createIo()
  const code = await runCli([
    'quality-gate',
    'check',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, async () => {
    throw new Error('ECONNREFUSED password=p@ss Authorization: Bearer ds_cli_token')
  })
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.equal(diagnostic.code, 'DATASPEC_SERVER_UNAVAILABLE')
  assert.equal(diagnostic.retryable, true)
  assert.doesNotMatch(io.stderr, /ds_cli_token|p@ss/)
})

test('quality-gate check redacts secrets in argument errors', async () => {
  const io = createIo()
  const code = await runCli([
    'quality-gate',
    'check',
    'password=p@ss',
    'Authorization: Bearer ds_cli_token',
    'jdbc:postgresql://localhost/db',
    '--project',
    '7',
    '--format',
    'json'
  ], io, async () => {
    throw new Error('fetch should not be called')
  })

  assert.equal(code, 2)
  assert.match(io.stderr, /password=\*\*\*/)
  assert.match(io.stderr, /Authorization: Bearer \*\*\*/)
  assert.match(io.stderr, /jdbc:\*\*\*/)
  assert.doesNotMatch(io.stderr, /p@ss|ds_cli_token|jdbc:postgresql:\/\/localhost\/db/)
})

test('evidence export prints machine-readable package json', async () => {
  const calls = []
  const fetchFn = async (url, init) => {
    calls.push({ url, init })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: evidencePackageFixture()
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'evidence',
    'export',
    '--source-type',
    'SQL_CHECK',
    '--source-id',
    '11',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const body = JSON.parse(calls[0].init.body)
  assert.equal(code, 0)
  assert.equal(calls[0].url, 'http://dataspec.local/api/evidence-packages')
  assert.equal(body.sourceType, 'SQL_CHECK')
  assert.equal(body.sourceId, 11)
  assert.equal(output.kind, 'dataspec-ai-evidence-package')
  assert.equal(output.source.sourceType, 'SQL_CHECK')
  assert.equal(output.validationSummary.errorCount, 1)
})

test('evidence export writes zip inside cwd', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const zipBytes = Buffer.from('PK evidence')
    const fetchFn = async (url, init) => {
      assert.equal(url, 'http://dataspec.local/api/evidence-packages/download')
      assert.equal(JSON.parse(init.body).sourceType, 'AI_BATCH_RUN')
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => zipBytes
      }
    }
    const io = createIo('', dir)

    const code = await runCli([
      'evidence',
      'export',
      '--source-type',
      'AI_BATCH_RUN',
      '--source-id',
      '31',
      '--server',
      'http://dataspec.local',
      '--format',
      'zip',
      '--output',
      'out/evidence.zip'
    ], io, fetchFn)

    const written = await readFile(path.join(dir, 'out', 'evidence.zip'))
    assert.equal(code, 0)
    assert.deepEqual(written, zipBytes)
    assert.match(io.stdout, /evidence package/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('evidence export rejects unsafe output path', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const io = createIo('', dir)
    const code = await runCli([
      'evidence',
      'export',
      '--source-type',
      'SQL_CHECK',
      '--source-id',
      '11',
      '--format',
      'json',
      '--output',
      '..\\evidence.json'
    ], io, async () => {
      throw new Error('should not call server')
    })

    assert.equal(code, 2)
    assert.match(io.stderr, /输出路径越界/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context passes snapshot options without secrets', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-'))
  try {
    const outputPath = path.join(dir, 'dataspec-ai-context.zip')
    const fetchFn = async (url, init) => {
      assert.equal(
        url,
        'http://localhost:8090/api/ai-context/package/download?projectId=9&snapshotId=42&snapshotVersion=v-history'
      )
      assert.equal(init.headers.Authorization, 'Bearer test-token')
      assert.equal(url.includes('test-token'), false)
      return {
        ok: true,
        status: 200,
        arrayBuffer: async () => Uint8Array.from([7, 8]).buffer
      }
    }
    const io = createIo()

    const code = await runCli([
      'export-context',
      '--project',
      '9',
      '--snapshot-id',
      '42',
      '--snapshot-version',
      'v-history',
      '--dataspec-token',
      'test-token',
      '--output',
      outputPath
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.deepEqual([...await readFile(outputPath)], [7, 8])
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('export-context rejects invalid snapshot id before calling server', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli([
    'export-context',
    '--project',
    '9',
    '--snapshot-id',
    'bad',
    '--output',
    'dataspec-ai-context.zip'
  ], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /无效 snapshot id: bad/)
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
    await mkdir(path.join(dir, '.dataspec', 'context'), { recursive: true })
    await writeFile(path.join(dir, 'sql', 'good.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    await writeFile(
      path.join(dir, '.dataspec', 'context', 'cache-metadata.json'),
      JSON.stringify({
        projectId: 7,
        exportedAt: '2026-06-28T01:00:00Z',
        expiresAt: '2999-01-01T00:00:00Z',
        standard: { specVersion: 'v1', specHash: 'hash-1', source: 'current' }
      }),
      'utf8'
    )
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
      if (url === 'http://dataspec.local/api/ai-context/package/download?projectId=7') {
        const zip = makeZip({
          '.dataspec/manifest.json': JSON.stringify({
            standard: { specVersion: 'v1', specHash: 'hash-1', source: 'current' }
          })
        })
        return {
          ok: true,
          status: 200,
          arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength)
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
      'ai-profile',
      'openapi',
      'context-cache'
    ])
    assert.equal(output.checks.every((check) => check.status === 'pass'), true)
    assert.equal(calls[0].url, 'http://dataspec.local/api-docs')
    assert.equal(calls[1].options.headers.Authorization, 'Bearer ds_config_token')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('doctor validates configured ai profile through remote profile api', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-profile-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local',
        aiProfile: 'sql-fix'
      }),
      'utf8'
    )
    const fetchFn = async (url) => {
      if (url === 'http://dataspec.local/api-docs') {
        return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
      }
      if (url === 'http://dataspec.local/api/projects/7') {
        return { ok: true, status: 200, json: async () => ({ code: 200, data: { id: 7, name: '演示项目' } }) }
      }
      if (url === 'http://dataspec.local/api/ai-profiles/sql-fix?projectId=7') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: {
              profile: {
                profileId: 'sql-fix',
                taskType: 'SQL_FIX',
                recommendedCommands: ['dataspec lint <file.sql> --profile sql-fix --format json']
              },
              diagnostics: [{ code: 'PROFILE_READY', status: 'pass' }]
            }
          })
        }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo('', dir)

    const code = await runCli(['doctor', '--format', 'json'], io, fetchFn)

    const output = JSON.parse(io.stdout)
    const profileCheck = output.checks.find((check) => check.name === 'ai-profile')
    assert.equal(code, 0)
    assert.equal(profileCheck.status, 'pass')
    assert.equal(profileCheck.details.profileId, 'sql-fix')
    assert.equal(profileCheck.details.taskType, 'SQL_FIX')
    assert.match(profileCheck.details.recommendedCommand, /dataspec lint/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('doctor reports unknown configured ai profile with supported values', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-profile-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local',
        aiProfile: 'missing'
      }),
      'utf8'
    )
    const fetchFn = async (url) => {
      if (url === 'http://dataspec.local/api-docs') {
        return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
      }
      if (url === 'http://dataspec.local/api/projects/7') {
        return { ok: true, status: 200, json: async () => ({ code: 200, data: { id: 7, name: '演示项目' } }) }
      }
      if (url === 'http://dataspec.local/api/ai-profiles/missing?projectId=7') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: {
              profile: null,
              supportedProfileIds: ['create-table', 'sql-fix'],
              supportedTaskTypes: ['CREATE_TABLE', 'SQL_FIX']
            }
          })
        }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo('', dir)

    const code = await runCli(['doctor', '--format', 'json'], io, fetchFn)

    const output = JSON.parse(io.stdout)
    const profileCheck = output.checks.find((check) => check.name === 'ai-profile')
    assert.equal(code, 1)
    assert.equal(profileCheck.status, 'fail')
    assert.deepEqual(profileCheck.details.supportedProfileIds, ['create-table', 'sql-fix'])
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

test('doctor reports missing context cache as warning', async () => {
  const fetchFn = createReadyDoctorFetch('http://dataspec.local', 7)
  const io = createIo()

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
  const cacheCheck = output.checks.find((check) => check.name === 'context-cache')
  assert.equal(code, 0)
  assert.equal(cacheCheck.status, 'warn')
  assert.equal(cacheCheck.details.cacheStatus, 'missing')
  assert.match(cacheCheck.message, /export-context --cache/)
})

test('doctor reports stale cache while offline', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-cache-'))
  try {
    await mkdir(path.join(dir, '.dataspec', 'context'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'context', 'cache-metadata.json'),
      JSON.stringify({
        projectId: 7,
        exportedAt: '2020-01-01T00:00:00Z',
        expiresAt: '2020-01-02T00:00:00Z',
        standard: { specVersion: 'v-old', specHash: 'hash-old', source: 'current' }
      }),
      'utf8'
    )
    const io = createIo('', dir)
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
    const cacheCheck = output.checks.find((check) => check.name === 'context-cache')
    assert.equal(code, 1)
    assert.equal(cacheCheck.status, 'warn')
    assert.equal(cacheCheck.details.cacheStatus, 'stale')
    assert.match(cacheCheck.message, /服务不可用/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('doctor reports remote standard difference from context cache', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-cache-'))
  try {
    await mkdir(path.join(dir, '.dataspec', 'context'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'context', 'cache-metadata.json'),
      JSON.stringify({
        projectId: 7,
        exportedAt: '2026-06-28T00:00:00Z',
        expiresAt: '2999-01-01T00:00:00Z',
        standard: { specVersion: 'v1', specHash: 'hash-old', source: 'current' }
      }),
      'utf8'
    )
    const fetchFn = async (url) => {
      if (url === 'http://dataspec.local/api-docs') {
        return { ok: true, status: 200, json: async () => ({ openapi: '3.0.1' }) }
      }
      if (url === 'http://dataspec.local/api/projects/7') {
        return {
          ok: true,
          status: 200,
          json: async () => ({ code: 200, data: { id: 7, name: '演示项目' } })
        }
      }
      if (url === 'http://dataspec.local/api/ai-context/package/download?projectId=7') {
        const zip = makeZip({
          '.dataspec/manifest.json': JSON.stringify({
            standard: { specVersion: 'v2', specHash: 'hash-new', source: 'current' }
          })
        })
        return {
          ok: true,
          status: 200,
          arrayBuffer: async () => zip.buffer.slice(zip.byteOffset, zip.byteOffset + zip.byteLength)
        }
      }
      throw new Error(`unexpected fetch: ${url}`)
    }
    const io = createIo('', dir)

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
    const cacheCheck = output.checks.find((check) => check.name === 'context-cache')
    assert.equal(code, 1)
    assert.equal(cacheCheck.status, 'fail')
    assert.equal(cacheCheck.details.cacheStatus, 'remote-different')
    assert.equal(cacheCheck.details.remoteStandard.specHash, 'hash-new')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
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
    assert.match(firstAgents, /dataspec-verify-advisor\.mjs --changed --format json/)

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
    assert.match(forcedAgents, /dataspec-verify-advisor\.mjs --changed --format json/)
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

test('task-card create prints stable json without calling server', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli([
    'task-card',
    'create',
    '--workflow',
    'create-table',
    '--goal',
    '创建订单表',
    '--project',
    '7',
    '--input',
    'businessDescription=订单表',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.kind, 'dataspec-ai-task-card')
  assert.equal(output.workflowId, 'create-table')
  assert.equal(output.projectId, 7)
  assert.equal(output.status, 'PLANNED')
  assert.ok(output.currentStep)
  assert.equal(io.stderr, '')
})

test('task-card create writes markdown inside cwd and redacts secrets', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-task-card-'))
  try {
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'task-card',
      'create',
      '--workflow',
      'review-pr-sql',
      '--goal',
      '检查 PR SQL',
      '--project',
      '7',
      '--input',
      'repo=acme/app',
      '--input',
      'pr=12',
      '--input',
      'GITHUB_TOKEN=ghp_secret_token',
      '--format',
      'markdown',
      '--output',
      'task-card.md'
    ], io, fetchFn)

    const content = await readFile(path.join(dir, 'task-card.md'), 'utf8')
    assert.equal(code, 0)
    assert.equal(io.stdout, '')
    assert.match(content, /DataSpec AI Task Card/)
    assert.match(content, /检查 PR SQL/)
    assert.doesNotMatch(content, /ghp_secret_token|Authorization|Bearer/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('task-card show and update local json without executing workflow', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-task-card-'))
  try {
    const filePath = path.join(dir, 'task-card.json')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const createCode = await runCli([
      'task-card',
      'create',
      '--workflow',
      'export-min-context',
      '--goal',
      '导出订单上下文',
      '--project',
      '7',
      '--input',
      'scope=field',
      '--output',
      'task-card.json'
    ], io, fetchFn)
    assert.equal(createCode, 0)

    const updateIo = createIo('', dir)
    const updateCode = await runCli([
      'task-card',
      'update',
      '--file',
      'task-card.json',
      '--step',
      'precheck-1',
      '--status',
      'DONE',
      '--artifact',
      'dataspec-ai-context.zip'
    ], updateIo, fetchFn)
    const updated = JSON.parse(await readFile(filePath, 'utf8'))

    const showIo = createIo('', dir)
    const showCode = await runCli(['task-card', 'show', '--file', 'task-card.json', '--format', 'markdown'], showIo, fetchFn)

    assert.equal(updateCode, 0)
    assert.equal(showCode, 0)
    assert.equal(updated.steps[0].status, 'DONE')
    assert.equal(updated.artifacts[0].path, 'dataspec-ai-context.zip')
    assert.match(showIo.stdout, /Validation Commands/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('task-card rejects unknown workflow and unsafe output path', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-task-card-'))
  try {
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }
    const unknownIo = createIo('', dir)
    const unknownCode = await runCli([
      'task-card',
      'create',
      '--workflow',
      'missing',
      '--goal',
      'x',
      '--format',
      'json'
    ], unknownIo, fetchFn)
    const unsafeIo = createIo('', dir)
    const unsafeCode = await runCli([
      'task-card',
      'create',
      '--workflow',
      'create-table',
      '--goal',
      'x',
      '--project',
      '7',
      '--input',
      'businessDescription=订单',
      '--output',
      '..\\outside.json'
    ], unsafeIo, fetchFn)
    const createIoForUpdate = createIo('', dir)
    const createCodeForUpdate = await runCli([
      'task-card',
      'create',
      '--workflow',
      'export-min-context',
      '--goal',
      '导出上下文',
      '--project',
      '7',
      '--input',
      'scope=field',
      '--output',
      'task-card.json'
    ], createIoForUpdate, fetchFn)
    const invalidStatusIo = createIo('', dir)
    const invalidStatusCode = await runCli([
      'task-card',
      'update',
      '--file',
      'task-card.json',
      '--step',
      'precheck-1',
      '--status',
      'INVALID'
    ], invalidStatusIo, fetchFn)
    const missingStepIo = createIo('', dir)
    const missingStepCode = await runCli([
      'task-card',
      'update',
      '--file',
      'task-card.json',
      '--step',
      'missing-step',
      '--status',
      'DONE'
    ], missingStepIo, fetchFn)
    await writeFile(path.join(dir, 'broken-task-card.json'), '{not-json', 'utf8')
    const brokenIo = createIo('', dir)
    const brokenCode = await runCli([
      'task-card',
      'show',
      '--file',
      'broken-task-card.json'
    ], brokenIo, fetchFn)

    assert.equal(unknownCode, 2)
    assert.match(unknownIo.stderr, /未知 workflow recipe: missing/)
    assert.equal(unsafeCode, 2)
    assert.match(unsafeIo.stderr, /输出路径必须位于当前工作目录/)
    assert.equal(createCodeForUpdate, 0)
    assert.equal(invalidStatusCode, 2)
    assert.match(invalidStatusIo.stderr, /无效 task card step status: INVALID/)
    assert.equal(missingStepCode, 2)
    assert.match(missingStepIo.stderr, /未知 task card step: missing-step/)
    assert.equal(brokenCode, 2)
    assert.match(brokenIo.stderr, /Expected property name|Unexpected token|JSON/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
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

function makeZip(entries) {
  const localParts = []
  const centralParts = []
  let offset = 0
  for (const [name, content] of Object.entries(entries)) {
    const nameBuffer = Buffer.from(name, 'utf8')
    const contentBuffer = Buffer.from(content)
    const local = Buffer.alloc(30 + nameBuffer.length)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0, 6)
    local.writeUInt16LE(0, 8)
    local.writeUInt32LE(0, 10)
    local.writeUInt32LE(0, 14)
    local.writeUInt32LE(contentBuffer.length, 18)
    local.writeUInt32LE(contentBuffer.length, 22)
    local.writeUInt16LE(nameBuffer.length, 26)
    nameBuffer.copy(local, 30)
    localParts.push(local, contentBuffer)

    const central = Buffer.alloc(46 + nameBuffer.length)
    central.writeUInt32LE(0x02014b50, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(0, 8)
    central.writeUInt16LE(0, 10)
    central.writeUInt32LE(0, 12)
    central.writeUInt32LE(0, 16)
    central.writeUInt32LE(contentBuffer.length, 20)
    central.writeUInt32LE(contentBuffer.length, 24)
    central.writeUInt16LE(nameBuffer.length, 28)
    central.writeUInt32LE(offset, 42)
    nameBuffer.copy(central, 46)
    centralParts.push(central)
    offset += local.length + contentBuffer.length
  }

  const centralDirectory = Buffer.concat(centralParts)
  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50, 0)
  end.writeUInt16LE(Object.keys(entries).length, 8)
  end.writeUInt16LE(Object.keys(entries).length, 10)
  end.writeUInt32LE(centralDirectory.length, 12)
  end.writeUInt32LE(offset, 16)
  return Buffer.concat([...localParts, centralDirectory, end])
}

function contractCatalogFixture() {
  const ids = [
    'field',
    'enum-dict',
    'rule-config',
    'template',
    'standard-snapshot',
    'lint-result',
    'ai-evidence-package',
    'ai-context-manifest',
    'ai-context-field-catalog',
    'ai-task-profile'
  ]
  return {
    kind: 'dataspec-schema-registry',
    schemaVersion: 1,
    registryVersion: '2026.06.28',
    compatibilityPolicy: {
      level: 'stable-ai-contract',
      breakingChangePolicy: '删除或改名必须更新 schemaVersion'
    },
    contracts: ids.map((contractId) => ({
      contractId,
      displayName: contractId,
      schemaVersion: '1.0',
      jsonSchemaRef: `dataspec://contracts/${contractId}@1.0`,
      stableFields: contractId === 'field' ? ['name', 'dataType'] : ['schemaVersion'],
      deprecatedFields: [],
      compatibility: { level: 'stable-ai-contract' },
      docsRef: `docs/ai-contracts.md#${contractId}`
    })),
    requiredContractIds: ids
  }
}

function capabilityCatalogFixture() {
  const ids = [
    'session-bootstrap',
    'capability-catalog',
    'doctor',
    'export-ai-context',
    'lint-sql',
    'search-fields',
    'suggest-fields',
    'generate-ddl',
    'reverse-import',
    'coverage-report',
    'schema-registry',
    'export-evidence-package',
    'workflow-recipes',
    'ai-task-profiles',
    'domain-starter-kits'
  ]
  return {
    kind: 'dataspec-ai-capability-catalog',
    schemaVersion: 1,
    catalogVersion: '2026.06.28',
    generatedAt: '2026-06-28T00:00:00',
    projectId: 7,
    capabilities: ids.map((id) => ({
      id,
      category: id === 'lint-sql' ? 'sql' : 'discovery',
      title: id,
      summary: `${id} summary`,
      status: 'AVAILABLE',
      stability: 'stable-ai',
      requiresProject: !['session-bootstrap', 'capability-catalog', 'doctor', 'schema-registry', 'workflow-recipes'].includes(id),
      writeRisk: id === 'lint-sql' ? 'WRITES_DATASPEC_RECORD' : 'READ_ONLY',
      requiredInputs: id === 'lint-sql' ? ['projectId', 'sql'] : [],
      optionalInputs: [],
      outputContracts: id === 'lint-sql' ? ['lint-result'] : ['ai-capability-catalog'],
      apiEndpoints: id === 'session-bootstrap'
        ? ['GET /api/bootstrap/session']
        : id === 'lint-sql' ? ['POST /api/lint'] : ['GET /api/capabilities'],
      cliCommands: id === 'lint-sql'
        ? ['dataspec lint <file.sql> --project <id> --format json']
        : id === 'session-bootstrap' ? ['dataspec bootstrap --project <id> --format json'] : ['dataspec capability list --format json'],
      mcpResources: id === 'capability-catalog'
        ? ['dataspec://project/<id>/capability-catalog']
        : id === 'session-bootstrap' ? ['dataspec://project/<id>/session-bootstrap'] : [],
      mcpTools: id === 'lint-sql' ? ['lint_sql'] : id === 'session-bootstrap' ? ['get_session_bootstrap'] : [],
      frontendRoutes: [],
      contractIds: [],
      workflowIds: [],
      profileIds: [],
      examples: [],
      preflightChecks: ['dataspec doctor --format json'],
      nextActions: ['read catalog'],
      docsRef: 'README.md#ai-能力清单'
    })),
    requiredCapabilityIds: ids,
    recommendedFirstActions: ['run doctor'],
    diagnostics: [{ code: 'CATALOG_READY', status: 'pass', message: 'ready', nextAction: 'continue' }]
  }
}

function sessionBootstrapFixture() {
  return {
    kind: 'dataspec-ai-session-bootstrap',
    schemaVersion: 1,
    generatedAt: '2026-07-04T12:00:00',
    status: 'READY',
    projectId: 7,
    server: 'http://dataspec.local',
    authMode: 'TOKEN_PRESENT',
    specVersion: 'v2026.07.04',
    standardSnapshot: {
      snapshotId: 11,
      projectId: 7,
      specVersion: 'v2026.07.04',
      specHash: 'hash-1',
      versioned: true,
      source: 'current'
    },
    availableCapabilities: [
      {
        id: 'lint-sql',
        title: 'SQL 校验与 fixedSql',
        status: 'AVAILABLE',
        writeRisk: 'WRITES_DATASPEC_RECORD',
        requiresProject: true,
        apiEndpoints: ['POST /api/lint'],
        cliCommands: ['dataspec lint <sql-file> --project 7 --format json'],
        mcpResources: [],
        mcpTools: ['lint_sql'],
        nextActions: ['先读取字段目录和规则']
      }
    ],
    recommendedCommands: [
      'dataspec doctor --project 7 --format json',
      'dataspec export-context --project 7 --cache',
      'dataspec lint <sql-file> --project 7 --format json'
    ],
    knownRisks: ['启动包不会执行 lint、导出 Context、反向导入或生成 DDL。'],
    docsRefs: ['README.md#ai-会话启动包', 'README.md#cli'],
    checks: [
      { name: 'project', status: 'pass', message: '已选择 projectId: 7' },
      { name: 'standard', status: 'pass', message: '当前标准快照可用: v2026.07.04' }
    ],
    nextActions: [
      {
        code: 'RUN_DOCTOR',
        severity: 'info',
        message: '从 doctor 开始确认本地配置和远端契约状态。',
        command: 'dataspec doctor --project 7 --format json',
        docsRef: 'README.md#cli',
        retryable: true
      }
    ]
  }
}

function evidencePackageFixture() {
  return {
    kind: 'dataspec-ai-evidence-package',
    schemaVersion: 1,
    packageId: 'evidence-sql-check-test',
    projectId: 7,
    generatedAt: '2026-06-28T00:00:00Z',
    source: {
      sourceType: 'SQL_CHECK',
      sourceId: 11,
      sourceTitle: 'SQL 检查记录 #11',
      status: 'ERROR',
      persisted: true
    },
    standardSnapshot: {
      snapshotId: 3,
      specVersion: 'v1',
      specHash: 'hash1',
      versioned: true
    },
    inputsSummary: {
      sqlPreview: 'select id from users'
    },
    outputsSummary: {
      fixedSqlAvailable: true
    },
    validationSummary: {
      errorCount: 1,
      warningCount: 0,
      suggestionCount: 0
    },
    artifacts: [
      {
        artifactType: 'sql-check-record',
        title: 'SQL 检查记录',
        format: 'json',
        summary: { id: 11 }
      }
    ],
    nextActions: ['review fixedSql'],
    suggestedCommands: ['dataspec lint <path|-> --project 7 --format json'],
    diagnostics: []
  }
}

function taskRunListItemFixture(overrides = {}) {
  return {
    id: 91,
    projectId: 7,
    taskType: 'SQL_LINT',
    sourceType: 'AI_BATCH',
    sourceId: 31,
    status: 'FAILED',
    inputHash: 'sha256:test',
    retryable: true,
    failedStep: 'lint-items',
    resumeCommand: 'node tools/dataspec-cli.mjs task show 91 --project 7 --format json',
    nextAction: '查看失败步骤并复制 resumeCommand 重试。',
    operatorName: 'cli',
    startedAt: '2026-07-04T10:00:00',
    finishedAt: '2026-07-04T10:00:02',
    expiresAt: '2026-07-11T10:00:00',
    createdAt: '2026-07-04T10:00:00',
    ...overrides
  }
}

function taskRunDetailFixture(overrides = {}) {
  return {
    ...taskRunListItemFixture(),
    idempotencyKey: 'retry-91',
    stepStatus: [
      {
        step: 'lint-items',
        status: 'FAILED',
        message: '1 个 SQL 文件检查失败',
        artifactRef: 'sql-lint-result:bad.sql'
      }
    ],
    partialArtifacts: [
      {
        type: 'sql-lint-result',
        name: 'good.sql',
        ref: 'sql-lint-result:good.sql',
        summary: { errorCount: 0 }
      }
    ],
    metadata: {
      profileId: 'sql-fix',
      source: 'cli'
    },
    updatedAt: '2026-07-04T10:00:02',
    ...overrides
  }
}

function qualityGateResultFixture(overrides = {}) {
  return {
    projectId: 7,
    enabled: true,
    status: 'PASS',
    config: {
      projectId: 7,
      enabled: true,
      minCoverage: 80,
      minAverageFieldScore: 80,
      maxErrorIssues: 0,
      maxNewUnmanagedFields: 0,
      requiredSensitiveMarking: true
    },
    summary: {
      totalChecks: 3,
      passedChecks: 3,
      failedChecks: 0,
      warningChecks: 0,
      skippedChecks: 0
    },
    checks: [
      {
        code: 'average_field_score',
        label: '字段质量均分',
        status: 'PASS',
        severity: 'INFO',
        actualValue: 92,
        expectedValue: 80,
        operator: '>=',
        nextAction: '保持当前质量水平'
      }
    ],
    failedChecks: [],
    nextActions: ['质量门禁通过，可继续当前交付'],
    evaluatedAt: '2026-07-04T10:00:00',
    ...overrides
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

async function initGitRepo(dir, config) {
  await git(dir, ['init'])
  await git(dir, ['config', 'user.email', 'dataspec@example.local'])
  await git(dir, ['config', 'user.name', 'DataSpec Test'])
  await mkdir(path.join(dir, '.dataspec'), { recursive: true })
  await writeFile(path.join(dir, '.dataspec', 'config.json'), JSON.stringify(config), 'utf8')
}

async function git(cwd, args) {
  await execFileAsync('git', args, { cwd })
}
