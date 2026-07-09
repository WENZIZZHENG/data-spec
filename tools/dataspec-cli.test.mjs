import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { createHash } from 'node:crypto'
import { mkdir, mkdtemp, readFile, rename, rm, stat, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { promisify } from 'node:util'
import { buildFixedSqlPatchPlan, buildInlineReviewPlan, buildPullRequestLineMap, runCli } from './dataspec-cli.mjs'

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

test('lint-debug reads sql file, posts to debug endpoint, prints json, and returns 0 for lint errors', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-lint-debug-'))
  try {
    const sqlPath = path.join(dir, 'bad.sql')
    await writeFile(sqlPath, 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            debugVersion: 'sql-rule-debug@1',
            lintResult: { errorCount: 1, warningCount: 0, suggestionCount: 0, issues: [] },
            rules: []
          }
        })
      }
    }
    const io = createIo()

    const code = await runCli([
      'lint-debug',
      sqlPath,
      '--project',
      '7',
      '--format',
      'json',
      '--server',
      'http://dataspec.local',
      '--fix-mode',
      'dry-run',
      '--max-risk',
      'low',
      '--include-explanations',
      'false',
      '--enable-rule',
      'table_naming_snake_case',
      '--disable-rule',
      'required_columns'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/lint/debug')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      sql: 'CREATE TABLE UserOrder (id bigint);',
      projectId: 7,
      fixPolicy: {
        mode: 'DRY_RUN',
        maxRiskLevel: 'LOW',
        enabledRuleCodes: ['table_naming_snake_case'],
        disabledRuleCodes: ['required_columns'],
        includeExplanations: false
      }
    })
    assert.equal(calls[0].options.headers['Idempotency-Key'], undefined)
    assert.equal(JSON.parse(io.stdout).debugVersion, 'sql-rule-debug@1')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-debug reads stdin and forwards profile selection', async () => {
  const calls = []
  const fetchFn = async (url, options) => {
    calls.push({ url, options })
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: {
          debugVersion: 'sql-rule-debug@1',
          lintResult: { errorCount: 2, warningCount: 0, suggestionCount: 0, issues: [] },
          rules: []
        }
      })
    }
  }
  const io = createIo('CREATE TABLE UserOrder (id bigint);')

  const code = await runCli([
    'lint-debug',
    '-',
    '--project',
    '7',
    '--profile',
    'sql-fix',
    '--format',
    'json'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.deepEqual(JSON.parse(calls[0].options.body), {
    sql: 'CREATE TABLE UserOrder (id bigint);',
    projectId: 7,
    profileId: 'sql-fix'
  })
})

test('lint-debug returns 2 when server request fails and redacts diagnostics', async () => {
  const fetchFn = async () => ({
    ok: false,
    status: 500,
    json: async () => ({
      message: 'debug failed token=plain-secret jdbc:postgresql://db.internal/app',
      error: {
        code: 'INTERNAL_ERROR',
        category: 'SERVER',
        retryable: true,
        suggestedAction: '检查 token=plain-secret 和 jdbc:postgresql://db.internal/app'
      }
    })
  })
  const io = createIo('CREATE TABLE users (id bigint);')

  const code = await runCli(['lint-debug', '-', '--project', '7', '--format', 'json'], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /DataSpecError/)
  assert.doesNotMatch(io.stderr, /plain-secret/)
  assert.doesNotMatch(io.stderr, /db\.internal/)
})

test('buildFixedSqlPatchPlan creates dry-run plan without changing target content', () => {
  const originalSql = 'CREATE TABLE UserOrder (userId bigint);\n'
  const fixedSql = 'CREATE TABLE user_order (user_id bigint);\n'
  const plan = buildFixedSqlPatchPlan({
    cwd: 'D:\\workspace\\demo',
    targetPath: 'sql/bad.sql',
    targetContent: originalSql,
    lintResult: {
      sql: originalSql,
      fixedSql
    }
  })

  assert.equal(plan.kind, 'dataspec.fixed-sql.patch-plan')
  assert.equal(plan.schemaVersion, 1)
  assert.equal(plan.targetPath, 'sql/bad.sql')
  assert.equal(plan.dryRunResult.status, 'READY')
  assert.equal(plan.dryRunResult.willWrite, false)
  assert.equal(plan.conflictWarnings.length, 0)
  assert.match(plan.unifiedDiff, /-CREATE TABLE UserOrder/)
  assert.match(plan.unifiedDiff, /\+CREATE TABLE user_order/)
  assert.match(plan.planHash, /^[a-f0-9]{64}$/)
  assert.match(plan.applyCommand, /fixed-sql patch/)
  assert.match(plan.applyCommand, /--confirm/)
  assert.equal(plan.rollbackHint.originalSha256, plan.currentFileSha256)
  assert.equal(plan.evidenceRef, `fixed-sql-patch:${plan.planHash.slice(0, 12)}`)
})

test('buildFixedSqlPatchPlan rejects missing fixedSql and reports no-change plans', () => {
  assert.throws(() => buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: 'select 1;\n',
    lintResult: { sql: 'select 1;\n' }
  }), /fixedSql/)

  const plan = buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/good.sql',
    targetContent: 'select 1;\n',
    lintResult: { sql: 'select 1;\n', fixedSql: 'select 1;\n' }
  })

  assert.equal(plan.dryRunResult.status, 'NO_CHANGE')
  assert.equal(plan.applyCommand, null)
})

test('buildFixedSqlPatchPlan rejects fixedSql without original sql evidence', () => {
  assert.throws(() => buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: 'select 1;\n',
    lintResult: { fixedSql: 'select 2;\n' }
  }), /originalSql|sql|原始 SQL/)
})

test('buildFixedSqlPatchPlan supports original sql hash and rejects hash mismatch', () => {
  const originalSql = 'select 1;\n'
  const fixedSql = 'select 2;\n'
  const originalSqlSha256 = createHash('sha256').update(originalSql).digest('hex')
  const plan = buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: originalSql,
    lintResult: { originalSqlSha256, fixedSql }
  })

  assert.equal(plan.dryRunResult.status, 'READY')
  assert.equal(plan.lintOriginalSha256, originalSqlSha256)
  assert.throws(() => buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: 'select 3;\n',
    lintResult: { originalSqlSha256, fixedSql }
  }), /hash|不匹配/)
})

test('buildFixedSqlPatchPlan reports drift and confirmation mismatch', () => {
  const plan = buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: 'CREATE TABLE user_order (id bigint, updated_at timestamp);\n',
    lintResult: {
      sql: 'CREATE TABLE UserOrder (id bigint);\n',
      fixedSql: 'CREATE TABLE user_order (id bigint);\n'
    },
    confirm: 'not-the-current-plan'
  })

  assert.equal(plan.dryRunResult.status, 'CONFLICT')
  assert.equal(plan.dryRunResult.confirmed, false)
  assert.ok(plan.conflictWarnings.some((item) => item.code === 'TARGET_CONTENT_DRIFT'))
  assert.ok(plan.conflictWarnings.some((item) => item.code === 'CONFIRM_HASH_MISMATCH'))
})

test('buildFixedSqlPatchPlan redacts secret-like values from diff output', () => {
  const plan = buildFixedSqlPatchPlan({
    cwd: '/workspace/demo',
    targetPath: 'sql/bad.sql',
    targetContent: "SELECT 'jdbc:postgresql://db.internal/app?password=raw' AS dsn;\n",
    lintResult: {
      sql: "SELECT 'jdbc:postgresql://db.internal/app?password=raw' AS dsn;\n",
      fixedSql: "SELECT 'jdbc:postgresql://db.internal/app?password=fixed' AS dsn;\n"
    }
  })

  assert.match(plan.unifiedDiff, /jdbc:\*\*\*/)
  assert.doesNotMatch(plan.unifiedDiff, /db\.internal/)
  assert.doesNotMatch(plan.unifiedDiff, /password=raw|password=fixed/)
})

test('fixed-sql patch prints dry-run json and leaves target file untouched', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-patch-'))
  try {
    const sqlDir = path.join(dir, 'sql')
    await mkdir(sqlDir, { recursive: true })
    const targetPath = path.join(sqlDir, 'bad.sql')
    const lintPath = path.join(dir, 'lint-result.json')
    const originalSql = 'CREATE TABLE UserOrder (userId bigint);\n'
    const fixedSql = 'CREATE TABLE user_order (user_id bigint);\n'
    await writeFile(targetPath, originalSql, 'utf8')
    await writeFile(lintPath, JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--format',
      'json'
    ], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.dryRunResult.status, 'READY')
    assert.equal(output.safety.requiresDryRun, true)
    assert.equal(await readFile(targetPath, 'utf8'), originalSql)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('fixed-sql patch applies only with matching confirm hash', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-apply-'))
  try {
    const sqlDir = path.join(dir, 'sql')
    await mkdir(sqlDir, { recursive: true })
    const targetPath = path.join(sqlDir, 'bad.sql')
    const lintPath = path.join(dir, 'lint-result.json')
    const originalSql = 'CREATE TABLE UserOrder (userId bigint);\n'
    const fixedSql = 'CREATE TABLE user_order (user_id bigint);\n'
    await writeFile(targetPath, originalSql, 'utf8')
    await writeFile(lintPath, JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')

    const dryRunIo = createIo('', dir)
    await runCli(['fixed-sql', 'patch', '--lint-result', 'lint-result.json', '--target', 'sql/bad.sql', '--format', 'json'], dryRunIo)
    const dryRun = JSON.parse(dryRunIo.stdout)
    const applyIo = createIo('', dir)

    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--apply',
      '--confirm',
      dryRun.planHash,
      '--format',
      'json'
    ], applyIo)
    const output = JSON.parse(applyIo.stdout)

    assert.equal(code, 0)
    assert.equal(output.dryRunResult.status, 'APPLIED')
    assert.equal(await readFile(targetPath, 'utf8'), fixedSql)
    assert.equal(applyIo.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('fixed-sql patch blocks missing confirmation and path escape', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-block-'))
  const outsideDir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-outside-'))
  try {
    const lintPath = path.join(dir, 'lint-result.json')
    const originalSql = 'CREATE TABLE UserOrder (id bigint);\n'
    await writeFile(path.join(dir, 'bad.sql'), originalSql, 'utf8')
    await writeFile(path.join(outsideDir, 'outside.sql'), originalSql, 'utf8')
    await writeFile(lintPath, JSON.stringify({
      sql: originalSql,
      fixedSql: 'CREATE TABLE user_order (id bigint);\n'
    }), 'utf8')

    const missingConfirmIo = createIo('', dir)
    const missingConfirmCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'bad.sql',
      '--apply',
      '--format',
      'json'
    ], missingConfirmIo)

    assert.equal(missingConfirmCode, 2)
    assert.match(missingConfirmIo.stderr, /confirm/)

    const escapeIo = createIo('', dir)
    const escapeCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      path.join(outsideDir, 'outside.sql'),
      '--format',
      'json'
    ], escapeIo)

    assert.equal(escapeCode, 2)
    assert.match(escapeIo.stderr, /越界|当前工作目录/)

    const lintEscapeIo = createIo('', dir)
    const lintEscapeCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      path.join(outsideDir, 'lint-result.json'),
      '--target',
      'bad.sql',
      '--format',
      'json'
    ], lintEscapeIo)

    assert.equal(lintEscapeCode, 2)
    assert.match(lintEscapeIo.stderr, /越界|当前工作目录/)
  } finally {
    await rm(dir, { recursive: true, force: true })
    await rm(outsideDir, { recursive: true, force: true })
  }
})

test('fixed-sql patch blocks drift, wrong confirm, no-change, and mismatched files item', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-guards-'))
  try {
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    const targetPath = path.join(dir, 'sql', 'bad.sql')
    const lintPath = path.join(dir, 'lint-result.json')
    const originalSql = 'CREATE TABLE UserOrder (id bigint);\n'
    const fixedSql = 'CREATE TABLE user_order (id bigint);\n'
    await writeFile(targetPath, 'CREATE TABLE UserOrder (id bigint, name text);\n', 'utf8')
    await writeFile(lintPath, JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')

    const driftIo = createIo('', dir)
    const driftCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--format',
      'json'
    ], driftIo)

    assert.equal(driftCode, 2)
    assert.equal(JSON.parse(driftIo.stdout).dryRunResult.status, 'CONFLICT')
    assert.match(driftIo.stderr, /FIXED_SQL_PATCH_CONFLICT/)
    assert.equal(await readFile(targetPath, 'utf8'), 'CREATE TABLE UserOrder (id bigint, name text);\n')

    await writeFile(targetPath, originalSql, 'utf8')
    const wrongConfirmIo = createIo('', dir)
    const wrongConfirmCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--apply',
      '--confirm',
      'not-the-current-plan',
      '--format',
      'json'
    ], wrongConfirmIo)

    assert.equal(wrongConfirmCode, 2)
    assert.match(wrongConfirmIo.stderr, /FIXED_SQL_PATCH_APPLY_BLOCKED|确认失败/)
    assert.equal(await readFile(targetPath, 'utf8'), originalSql)

    await writeFile(lintPath, JSON.stringify({ sql: fixedSql, fixedSql }), 'utf8')
    await writeFile(targetPath, fixedSql, 'utf8')
    const noChangeIo = createIo('', dir)
    const noChangeCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--format',
      'json'
    ], noChangeIo)

    assert.equal(noChangeCode, 0)
    assert.equal(JSON.parse(noChangeIo.stdout).dryRunResult.status, 'NO_CHANGE')

    await writeFile(lintPath, JSON.stringify({
      files: [{
        path: 'sql/other.sql',
        result: { sql: originalSql, fixedSql }
      }]
    }), 'utf8')
    await writeFile(targetPath, originalSql, 'utf8')
    const mismatchIo = createIo('', dir)
    const mismatchCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--format',
      'json'
    ], mismatchIo)

    assert.equal(mismatchCode, 2)
    assert.match(mismatchIo.stderr, /path|target|目标文件/)
    assert.equal(await readFile(targetPath, 'utf8'), originalSql)

    await writeFile(lintPath, JSON.stringify({
      sql: originalSql,
      fixedSql,
      files: [{
        path: 'sql/other.sql',
        result: { sql: originalSql, fixedSql }
      }]
    }), 'utf8')
    const mixedMismatchIo = createIo('', dir)
    const mixedMismatchCode = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'sql/bad.sql',
      '--format',
      'json'
    ], mixedMismatchIo)

    assert.equal(mixedMismatchCode, 2)
    assert.match(mixedMismatchIo.stderr, /path|target|目标文件/)
    assert.equal(await readFile(targetPath, 'utf8'), originalSql)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('fixed-sql patch rejects missing original sql in CLI lint result', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-original-'))
  try {
    const originalSql = 'CREATE TABLE UserOrder (id bigint);\n'
    await writeFile(path.join(dir, 'bad.sql'), originalSql, 'utf8')
    await writeFile(path.join(dir, 'lint-result.json'), JSON.stringify({
      fixedSql: 'CREATE TABLE user_order (id bigint);\n'
    }), 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'bad.sql',
      '--format',
      'json'
    ], io)

    assert.equal(code, 2)
    assert.match(io.stderr, /originalSql|sql|原始 SQL/)
    assert.equal(await readFile(path.join(dir, 'bad.sql'), 'utf8'), originalSql)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('fixed-sql patch redacts secret-like values from CLI dry-run output', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-redact-'))
  try {
    const originalSql = "SELECT 'jdbc:postgresql://db.internal/app?password=raw' AS dsn;\n"
    const fixedSql = "SELECT 'jdbc:postgresql://db.internal/app?password=fixed' AS dsn;\n"
    await writeFile(path.join(dir, 'bad.sql'), originalSql, 'utf8')
    await writeFile(path.join(dir, 'lint-result.json'), JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'bad.sql',
      '--format',
      'json'
    ], io)

    assert.equal(code, 0)
    assert.match(io.stdout, /jdbc:\*\*\*/)
    assert.doesNotMatch(io.stdout, /db\.internal/)
    assert.doesNotMatch(io.stdout, /password=raw|password=fixed/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('fixed-sql patch rejects symlink paths when the platform supports symlinks', async (t) => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-symlink-'))
  const outsideDir = await mkdtemp(path.join(tmpdir(), 'dataspec-fixed-sql-symlink-outside-'))
  try {
    const originalSql = 'CREATE TABLE UserOrder (id bigint);\n'
    const fixedSql = 'CREATE TABLE user_order (id bigint);\n'
    const outsideTarget = path.join(outsideDir, 'outside.sql')
    await writeFile(outsideTarget, originalSql, 'utf8')
    await writeFile(path.join(dir, 'lint-result.json'), JSON.stringify({ sql: originalSql, fixedSql }), 'utf8')
    try {
      await symlink(outsideTarget, path.join(dir, 'linked.sql'), 'file')
    } catch (error) {
      if (['EPERM', 'EACCES', 'ENOSYS'].includes(error.code)) {
        t.skip(`当前平台无法创建 symlink: ${error.code}`)
        return
      }
      throw error
    }

    const io = createIo('', dir)
    const code = await runCli([
      'fixed-sql',
      'patch',
      '--lint-result',
      'lint-result.json',
      '--target',
      'linked.sql',
      '--format',
      'json'
    ], io)

    assert.equal(code, 2)
    assert.match(io.stderr, /符号链接|symlink|当前工作目录/)
    assert.equal(await readFile(outsideTarget, 'utf8'), originalSql)
  } finally {
    await rm(dir, { recursive: true, force: true })
    await rm(outsideDir, { recursive: true, force: true })
  }
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

test('install-hook writes managed pre-commit hook and vscode task files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    const io = createIo('', dir)

    const code = await runCli(['install-hook', '--with-vscode', '--format', 'json'], io)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec.local-sql-check.install-hook')
    assert.equal(output.hook.name, 'pre-commit')
    assert.equal(output.safety.writesProject, true)
    assert.equal(output.safety.overwritesUnmanagedFiles, false)
    assert.deepEqual(output.diagnostics, [])
    assert.ok(output.writtenFiles.some((item) => item.path === '.git/hooks/pre-commit'))
    assert.ok(output.writtenFiles.some((item) => item.path === '.vscode/tasks.json'))
    assert.ok(output.writtenFiles.some((item) => item.path === '.vscode/dataspec-problem-matcher.json'))

    const hook = await readFile(path.join(dir, '.git', 'hooks', 'pre-commit'), 'utf8')
    assert.match(hook, /dataspec-install-hook:start/)
    assert.match(hook, /lint-changed --format json/)
    assert.doesNotMatch(hook, /token|password|Authorization|jdbc:/i)

    const hookStat = await stat(path.join(dir, '.git', 'hooks', 'pre-commit'))
    if (process.platform !== 'win32') {
      assert.equal((hookStat.mode & 0o111) !== 0, true)
    }

    const tasks = JSON.parse(await readFile(path.join(dir, '.vscode', 'tasks.json'), 'utf8'))
    assert.equal(tasks.tasks[0].label, 'DataSpec: lint changed SQL')
    assert.match(tasks.tasks[0].command, /lint-changed --format text/)
    assert.equal(tasks.tasks[0].problemMatcher.name, 'dataspec-sql')
    assert.match(tasks.tasks[0].problemMatcher.pattern.regexp, /suggestion/)

    const matcher = JSON.parse(await readFile(path.join(dir, '.vscode', 'dataspec-problem-matcher.json'), 'utf8'))
    assert.equal(matcher.problemMatcher[0].name, 'dataspec-sql')
    assert.match(matcher.problemMatcher[0].pattern.regexp, /suggestion/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('install-hook refuses to overwrite unmanaged pre-commit hook', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-unmanaged-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    const hookPath = path.join(dir, '.git', 'hooks', 'pre-commit')
    await writeFile(hookPath, '#!/bin/sh\necho custom-local-value\n', 'utf8')
    const io = createIo('', dir)

    const code = await runCli(['install-hook', '--format', 'json'], io)

    assert.equal(code, 2)
    const output = JSON.parse(io.stdout)
    assert.equal(output.diagnostics[0].code, 'HOOK_EXISTS_UNMANAGED')
    assert.equal(output.skippedFiles[0].path, '.git/hooks/pre-commit')
    assert.equal(await readFile(hookPath, 'utf8'), '#!/bin/sh\necho custom-local-value\n')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('install-hook refuses symlinked managed targets without writing outside repository', async (t) => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-symlink-'))
  const outsideDir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-symlink-outside-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    const outsideTarget = path.join(outsideDir, 'pre-commit')
    await writeFile(outsideTarget, '#!/bin/sh\necho outside\n', 'utf8')
    try {
      await rm(path.join(dir, '.git', 'hooks', 'pre-commit'), { force: true })
      await symlink(outsideTarget, path.join(dir, '.git', 'hooks', 'pre-commit'), 'file')
    } catch (error) {
      if (['EPERM', 'EACCES', 'ENOTSUP'].includes(error.code)) {
        t.skip(`当前平台无法创建 symlink: ${error.code}`)
        return
      }
      throw error
    }
    const io = createIo('', dir)

    const code = await runCli(['install-hook', '--format', 'json'], io)

    assert.equal(code, 2)
    const output = JSON.parse(io.stdout)
    assert.equal(output.diagnostics[0].code, 'MANAGED_FILE_IS_SYMLINK')
    assert.equal(await readFile(outsideTarget, 'utf8'), '#!/bin/sh\necho outside\n')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
    await rm(outsideDir, { recursive: true, force: true })
  }
})

test('install-hook refuses symlinked git directory without writing outside repository', async (t) => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-gitlink-'))
  const outsideDir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-gitlink-outside-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    const outsideGitDir = path.join(outsideDir, 'git-real')
    await rename(path.join(dir, '.git'), outsideGitDir)
    try {
      await symlink(outsideGitDir, path.join(dir, '.git'), process.platform === 'win32' ? 'junction' : 'dir')
    } catch (error) {
      if (['EPERM', 'EACCES', 'ENOTSUP'].includes(error.code)) {
        t.skip(`当前平台无法创建 git 目录链接: ${error.code}`)
        return
      }
      throw error
    }
    const outsideHook = path.join(outsideGitDir, 'hooks', 'pre-commit')
    const io = createIo('', dir)

    const code = await runCli(['install-hook', '--format', 'json'], io)

    assert.equal(code, 2)
    const output = JSON.parse(io.stdout)
    assert.equal(output.diagnostics[0].code, 'MANAGED_FILE_IS_SYMLINK')
    await assert.rejects(readFile(outsideHook, 'utf8'), /ENOENT/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
    await rm(outsideDir, { recursive: true, force: true })
  }
})

test('install-hook resolves hook path in linked worktrees', async () => {
  const sourceDir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-source-'))
  const worktreeDir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-linked-'))
  try {
    await initGitRepo(sourceDir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    await writeFile(path.join(sourceDir, 'README.md'), 'baseline\n', 'utf8')
    await git(sourceDir, ['add', '.'])
    await git(sourceDir, ['commit', '-m', 'baseline'])
    await rm(worktreeDir, { recursive: true, force: true })
    await git(sourceDir, ['worktree', 'add', worktreeDir, '-b', 'dataspec-linked-hook-test'])
    await mkdir(path.join(worktreeDir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(worktreeDir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, server: 'http://dataspec.local', defaultPaths: ['sql'] }),
      'utf8'
    )
    const hookPath = (await execFileAsync('git', ['rev-parse', '--git-path', 'hooks/pre-commit'], {
      cwd: worktreeDir,
      encoding: 'utf8'
    })).stdout.trim()
    const resolvedHookPath = path.resolve(worktreeDir, hookPath)
    const io = createIo('', worktreeDir)

    const code = await runCli(['install-hook', '--format', 'json'], io)

    assert.equal(code, 0)
    assert.match(await readFile(resolvedHookPath, 'utf8'), /dataspec-install-hook:start/)
    assert.equal(JSON.parse(io.stdout).writtenFiles[0].path, '.git/hooks/pre-commit')
  } finally {
    await execFileAsync('git', ['worktree', 'remove', '--force', worktreeDir], { cwd: sourceDir }).catch(() => {})
    await execFileAsync('git', ['branch', '-D', 'dataspec-linked-hook-test'], { cwd: sourceDir }).catch(() => {})
    await rm(sourceDir, { recursive: true, force: true })
    await rm(worktreeDir, { recursive: true, force: true })
  }
})

test('install-hook reports no git repository without writing files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-install-hook-no-git-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: ['sql'] }),
      'utf8'
    )
    const io = createIo('', dir)

    const code = await runCli(['install-hook', '--format', 'json'], io)

    assert.equal(code, 2)
    const output = JSON.parse(io.stdout)
    assert.equal(output.diagnostics[0].code, 'NO_GIT_REPOSITORY')
    assert.deepEqual(output.writtenFiles, [])
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-changed text output prints problem matcher issue lines', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-lint-changed-text-'))
  try {
    await initGitRepo(dir, {
      projectId: 7,
      server: 'http://dataspec.local',
      defaultPaths: ['sql']
    })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE users (id bigint);', 'utf8')
    await git(dir, ['add', '.'])
    await git(dir, ['commit', '-m', 'baseline'])

    await writeFile(path.join(dir, 'sql', 'existing.sql'), 'CREATE TABLE UserOrder (id bigint);', 'utf8')
    const fetchFn = async () => ({
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
            message: '表名不符合 snake_case',
            line: 1,
            column: 14,
            suggestion: '改为 user_order'
          }]
        }
      })
    })
    const io = createIo('', dir)

    const code = await runCli(['lint-changed', '--format', 'text'], io, fetchFn)

    assert.equal(code, 1)
    assert.match(
      io.stdout,
      /sql\/existing\.sql:1:14: ERROR table_naming_snake_case - 表名不符合 snake_case suggestion: 改为 user_order/
    )
    assert.match(io.stdout, /Next actions:/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('lint-changed text output reports no sql diagnostic without calling server', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-lint-changed-text-no-sql-'))
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

    const code = await runCli(['lint-changed', '--format', 'text'], io, fetchFn)

    assert.equal(code, 0)
    assert.match(io.stdout, /NO_CHANGED_SQL_FILES/)
    assert.match(io.stdout, /Next actions:/)
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

test('index-refs scans default paths, classifies references, redacts snippets, and skips generated dirs', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(path.join(dir, 'sql'), { recursive: true })
    await mkdir(path.join(dir, 'models'), { recursive: true })
    await mkdir(path.join(dir, 'node_modules', 'pkg'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: ['sql', 'models', 'node_modules'] }),
      'utf8'
    )
    await writeFile(
      path.join(dir, 'sql', 'orders.sql'),
      "SELECT phone FROM user_order WHERE dsn = 'jdbc:postgresql://localhost/demo' AND password='secret';",
      'utf8'
    )
    await writeFile(
      path.join(dir, 'models', 'user-order.ts'),
      'export interface UserOrder { mobile_phone?: string; phoneLabel?: string }\n',
      'utf8'
    )
    await writeFile(path.join(dir, 'node_modules', 'pkg', 'skip.sql'), 'SELECT phone FROM ignored;', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['index-refs', '--field', 'phone', '--alias', 'mobile_phone', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(io.stderr, '')
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec.code-field-reference-index')
    assert.equal(output.schemaVersion, 1)
    assert.equal(output.fields[0].fieldName, 'phone')
    assert.deepEqual(output.fields[0].aliases, ['mobile_phone'])
    assert.equal(output.renameRisk, 'HIGH')
    assert.ok(output.summary.totalReferences >= 2)
    assert.ok(output.summary.skippedDirectoryCount >= 1)
    assert.ok(output.references.some((item) =>
      item.file === 'sql/orders.sql'
        && item.referenceKind === 'SQL_IDENTIFIER'
        && item.confidence === 'HIGH'
        && item.line === 1
        && item.column > 0
    ))
    assert.ok(output.references.some((item) =>
      item.file === 'models/user-order.ts'
        && item.matchedText === 'mobile_phone'
        && item.confidence === 'MEDIUM'
    ))
    assert.equal(output.references.some((item) => item.file.includes('node_modules')), false)
    assert.doesNotMatch(JSON.stringify(output), /secret|jdbc:postgresql/i)
    assert.ok(output.nextActions.some((action) => action.includes('重命名')))
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs scans explicit paths and reports low risk when no references are found', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-empty-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(path.join(dir, 'src', 'user.ts'), 'export const userId = 1\n', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['index-refs', '--field', 'phone', '--path', 'src', '--format', 'json'], io, fetchFn)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.summary.totalReferences, 0)
    assert.equal(output.renameRisk, 'LOW')
    assert.match(output.suggestedAction, /未发现/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs maps aliases to canonical fields in multi-field scans', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-multi-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(
      path.join(dir, 'src', 'user.sql'),
      'SELECT phone, email, mobile_phone, email_address FROM user_profile;',
      'utf8'
    )
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'index-refs',
      '--field',
      'phone',
      '--field',
      'email',
      '--alias',
      'phone=mobile_phone',
      '--alias',
      'email=email_address',
      '--path',
      'src',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    const matchedFields = output.references.map((item) => `${item.fieldName}:${item.matchedText}`).sort()
    assert.deepEqual(matchedFields, [
      'email:email',
      'email:email_address',
      'phone:mobile_phone',
      'phone:phone'
    ])
    assert.equal(output.fields.find((item) => item.fieldName === 'phone').aliases[0], 'mobile_phone')
    assert.equal(output.fields.find((item) => item.fieldName === 'email').aliases[0], 'email_address')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs rejects ambiguous aliases when multiple fields are requested', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-ambiguous-alias-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(path.join(dir, 'src', 'user.sql'), 'SELECT mobile_phone FROM user_profile;', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'index-refs',
      '--field',
      'phone',
      '--field',
      'email',
      '--alias',
      'mobile_phone',
      '--path',
      'src',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.equal(io.stdout, '')
    assert.match(io.stderr, /field=alias/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs rejects scan paths outside the business repository root', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-path-boundary-'))
  const outsideDir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-outside-'))
  try {
    await writeFile(path.join(outsideDir, 'outside.sql'), 'SELECT phone FROM outside_table;', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'index-refs',
      '--field',
      'phone',
      '--path',
      outsideDir,
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.equal(io.stdout, '')
    assert.match(io.stderr, /DATASPEC_SCAN_PATH_OUT_OF_SCOPE/)
    assert.doesNotMatch(io.stderr, /outside_table/)
  } finally {
    await rm(dir, { recursive: true, force: true })
    await rm(outsideDir, { recursive: true, force: true })
  }
})

test('index-refs returns scan path diagnostics for missing explicit paths', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-missing-explicit-'))
  try {
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'index-refs',
      '--field',
      'phone',
      '--path',
      'missing',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.equal(io.stdout, '')
    assert.match(io.stderr, /DATASPEC_SCAN_PATH_NOT_FOUND/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs text output includes bounded-scan diagnostics', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-text-diagnostics-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(path.join(dir, 'src', 'large.sql'), 'x'.repeat(1024 * 1024 + 1), 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'index-refs',
      '--field',
      'phone',
      '--path',
      'src',
      '--format',
      'text'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.match(io.stdout, /Skipped: 0 directories, 1 files/)
    assert.match(io.stdout, /Diagnostics:/)
    assert.match(io.stdout, /SCAN_FILE_TOO_LARGE/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('index-refs stops without scanning the whole repository when paths are missing', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-index-refs-missing-paths-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(path.join(dir, '.dataspec', 'config.json'), JSON.stringify({ projectId: 7, defaultPaths: [] }), 'utf8')
    await writeFile(path.join(dir, 'outside.sql'), 'SELECT phone FROM should_not_scan;', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli(['index-refs', '--field', 'phone', '--format', 'json'], io, fetchFn)

    assert.equal(code, 2)
    assert.equal(io.stdout, '')
    assert.match(io.stderr, /DATASPEC_DEFAULT_PATHS_MISSING/)
    assert.doesNotMatch(io.stderr, /outside\.sql/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('code-patch plan creates rename dry-run JSON without changing business files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-code-patch-rename-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await mkdir(path.join(dir, 'db', 'migration'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: ['src', 'db'] }),
      'utf8'
    )
    const dtoPath = path.join(dir, 'src', 'UserDto.java')
    const migrationPath = path.join(dir, 'db', 'migration', 'V1__user.sql')
    const dtoContent = 'class UserDto { String phone; String mobile; String dsn = "jdbc:postgresql://localhost/demo"; String password = "secret"; }\n'
    const migrationContent = 'ALTER TABLE user_account ADD COLUMN phone varchar(20);\n'
    await writeFile(dtoPath, dtoContent, 'utf8')
    await writeFile(migrationPath, migrationContent, 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'code-patch',
      'plan',
      '--field',
      'phone',
      '--to-field',
      'mobile_phone',
      '--alias',
      'mobile',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(io.stderr, '')
    assert.equal(await readFile(dtoPath, 'utf8'), dtoContent)
    assert.equal(await readFile(migrationPath, 'utf8'), migrationContent)
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec.code-field.patch-plan')
    assert.equal(output.schemaVersion, 1)
    assert.equal(output.change.fieldName, 'phone')
    assert.equal(output.change.renameTo, 'mobile_phone')
    assert.equal(output.riskLevel, 'HIGH')
    assert.equal(output.dryRunResult.willWrite, false)
    assert.equal(output.safety.readOnly, true)
    assert.equal(output.safety.writesProject, false)
    assert.equal(output.safety.externalNetworkUsed, false)
    assert.ok(output.candidateEdits.some((item) =>
      item.fileRef.path === 'db/migration/V1__user.sql'
        && item.riskLevel === 'HIGH'
        && item.suggestedEdit.replacement === 'mobile_phone'
        && item.dryRunDiff.includes('mobile_phone')
    ))
    assert.ok(output.candidateEdits.every((item) => item.requiresHumanReview))
    assert.ok(output.verificationCommands.some((item) =>
      item.command.includes('code-patch plan') && item.command.includes('--alias mobile')
    ))
    assert.ok(output.verificationCommands.some((item) => item.command.includes('index-refs')))
    assert.doesNotMatch(JSON.stringify(output), /secret|jdbc:postgresql/i)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('code-patch plan redacts user-controlled secrets from JSON output', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-code-patch-redact-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(path.join(dir, 'src', 'UserDto.java'), 'class UserDto { String phone; }\n', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'code-patch',
      'plan',
      '--field',
      'phone',
      '--to-field',
      'token=raw-secret',
      '--from-type',
      'password=old-secret',
      '--to-type',
      'dsn=postgresql://admin:secret-pass@localhost/demo',
      '--enum-change',
      'DRAFT=api_key=enum-secret',
      '--path',
      'src',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 0)
    const outputText = io.stdout
    assert.doesNotMatch(outputText, /raw-secret|old-secret|secret-pass|enum-secret/i)
    const output = JSON.parse(outputText)
    assert.match(output.change.renameTo, /token=\*\*\*/)
    assert.ok(output.verificationCommands.every((item) => !/raw-secret|old-secret|secret-pass|enum-secret/i.test(item.command)))
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('code-patch plan prints markdown for type and enum review steps', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-code-patch-markdown-'))
  try {
    await mkdir(path.join(dir, 'src'), { recursive: true })
    await writeFile(path.join(dir, 'src', 'OrderStatus.java'), 'enum OrderStatus { DRAFT } class Order { String order_status; }\n', 'utf8')
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'code-patch',
      'plan',
      '--field',
      'order_status',
      '--from-type',
      'varchar',
      '--to-type',
      'enum',
      '--enum-change',
      'DRAFT=PENDING',
      '--path',
      'src',
      '--format',
      'markdown'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(io.stderr, '')
    assert.match(io.stdout, /DataSpec Code Patch Plan/)
    assert.match(io.stdout, /Risk: MEDIUM/)
    assert.match(io.stdout, /Manual steps/)
    assert.match(io.stdout, /DRAFT=PENDING/)
    assert.match(io.stdout, /OrderStatus\.java/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('code-patch plan rejects missing change intent and missing bounded paths', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-code-patch-errors-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(path.join(dir, '.dataspec', 'config.json'), JSON.stringify({ projectId: 7, defaultPaths: [] }), 'utf8')
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }
    const noIntentIo = createIo('', dir)

    const noIntentCode = await runCli(['code-patch', 'plan', '--field', 'phone', '--path', '.', '--format', 'json'], noIntentIo, fetchFn)

    assert.equal(noIntentCode, 2)
    assert.equal(noIntentIo.stdout, '')
    assert.match(noIntentIo.stderr, /CODE_PATCH_CHANGE_REQUIRED/)
    const missingPathsIo = createIo('', dir)

    const missingPathsCode = await runCli(['code-patch', 'plan', '--field', 'phone', '--to-field', 'mobile_phone', '--format', 'json'], missingPathsIo, fetchFn)

    assert.equal(missingPathsCode, 2)
    assert.equal(missingPathsIo.stdout, '')
    assert.match(missingPathsIo.stderr, /DATASPEC_DEFAULT_PATHS_MISSING/)
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

test('context-budget plan posts request, prints json, and does not write context files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-budget-'))
  try {
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: budgetPlanFixture({
            projectId: 9,
            request: {
              projectId: 9,
              tokenBudget: 2400,
              taskType: 'CREATE_TABLE',
              profileId: 'standard-context',
              scope: 'field',
              query: '用户手机',
              status: 'enabled',
              limit: 20,
              targetTable: 'user_profile',
              targetFile: 'db/user.sql',
              totalFieldCount: 12,
              matchedFieldCount: 2,
              returnedFieldCount: 2
            }
          })
        })
      }
    }
    const io = createIo('', dir)

    const code = await runCli([
      'context-budget',
      'plan',
      '--project',
      '9',
      '--token-budget',
      '2400',
      '--profile',
      'standard-context',
      '--task-type',
      'CREATE_TABLE',
      '--scope',
      'field',
      '--query',
      '用户手机',
      '--status',
      'enabled',
      '--limit',
      '20',
      '--target-table',
      'user_profile',
      '--target-file',
      'db/user.sql',
      '--format',
      'json'
    ], io, fetchFn)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://localhost:8090/api/ai-context/budget/plan')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      projectId: 9,
      tokenBudget: 2400,
      profileId: 'standard-context',
      taskType: 'CREATE_TABLE',
      scope: 'field',
      query: '用户手机',
      status: 'enabled',
      limit: 20,
      targetTable: 'user_profile',
      targetFile: 'db/user.sql'
    })
    assert.equal(calls[0].options.method, 'POST')
    assert.equal(calls[0].options.headers['Content-Type'], 'application/json')
    assert.equal(output.kind, 'dataspec-ai-context-budget-plan')
    assert.equal(output.qualityRisk, 'MEDIUM')
    await assert.rejects(readFile(path.join(dir, '.dataspec', 'context', 'cache-metadata.json')), /ENOENT/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-budget plan prints text summary', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({ code: 200, data: budgetPlanFixture() })
  })
  const io = createIo()

  const code = await runCli([
    'context-budget',
    'plan',
    '--project',
    '9',
    '--token-budget',
    '2400',
    '--format',
    'text'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.match(io.stdout, /AI Context 预算计划/)
  assert.match(io.stdout, /qualityRisk: MEDIUM/)
  assert.match(io.stdout, /selectedArtifacts/)
})

test('context-budget plan uses config profile defaults and explicit task type overrides config', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-budget-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local/',
        aiProfile: 'minimal-context',
        taskType: 'SQL_FIX'
      }),
      'utf8'
    )
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({ code: 200, data: budgetPlanFixture({ projectId: 7 }) })
      }
    }
    const io = createIo('', dir)

    const code = await runCli([
      'context-budget',
      'plan',
      '--token-budget',
      '1600',
      '--task-type',
      'CREATE_TABLE',
      '--format',
      'json'
    ], io, fetchFn)
    const body = JSON.parse(calls[0].options.body)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/ai-context/budget/plan')
    assert.equal(body.projectId, 7)
    assert.equal(body.tokenBudget, 1600)
    assert.equal(body.taskType, 'CREATE_TABLE')
    assert.equal(body.profileId, undefined)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-budget plan rejects missing or invalid token budget before calling server', async () => {
  const calls = []
  const fetchFn = async () => {
    calls.push('called')
    throw new Error('fetch should not be called')
  }

  const missingIo = createIo()
  const missingCode = await runCli(['context-budget', 'plan', '--project', '9', '--format', 'json'], missingIo, fetchFn)
  const invalidIo = createIo()
  const invalidCode = await runCli(['context-budget', 'plan', '--project', '9', '--token-budget', '0', '--format', 'json'], invalidIo, fetchFn)

  assert.equal(missingCode, 2)
  assert.equal(invalidCode, 2)
  assert.match(missingIo.stderr, /token-budget/)
  assert.match(invalidIo.stderr, /token budget/)
  assert.deepEqual(calls, [])
})

test('context-budget plan redacts service error diagnostics', async () => {
  const fetchFn = async () => ({
    ok: false,
    status: 500,
    json: async () => ({
      message: 'budget failed token=plain-secret jdbc:postgresql://db.internal/app',
      error: {
        code: 'INTERNAL_ERROR',
        category: 'SERVER',
        retryable: true,
        suggestedAction: '检查 token=plain-secret 和 jdbc:postgresql://db.internal/app'
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'context-budget',
    'plan',
    '--project',
    '9',
    '--token-budget',
    '2400',
    '--format',
    'json'
  ], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /DataSpecError/)
  assert.doesNotMatch(io.stderr, /plain-secret/)
  assert.doesNotMatch(io.stderr, /db\.internal/)
})

test('context-quality check evaluates context directory without writing cache files', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-context-quality-dir-'))
  try {
    const contextDir = path.join(dir, 'context')
    await mkdir(path.join(contextDir, 'catalog'), { recursive: true })
    await writeFile(
      path.join(contextDir, 'manifest.json'),
      JSON.stringify({ kind: 'dataspec-ai-context-manifest', schemaVersion: 1 }),
      'utf8'
    )
    await writeFile(
      path.join(contextDir, 'catalog', 'FIELD_CATALOG.md'),
      '字段目录\n- user_phone: 用户手机号\n',
      'utf8'
    )
    await writeFile(
      path.join(contextDir, 'DATABASE_RULES.md'),
      '命名规则\n[TRUNCATED] token budget exceeded\n',
      'utf8'
    )
    const io = createIo('', dir)
    const fetchFn = async () => {
      throw new Error('fetch should not be called')
    }

    const code = await runCli([
      'context-quality',
      'check',
      '--context-dir',
      'context',
      '--format',
      'json'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(io.stderr, '')
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec-ai-context-quality-check')
    assert.equal(output.schemaVersion, 1)
    assert.deepEqual(output.input, { sourceType: 'context-dir', path: 'context' })
    assert.equal(output.qualityLevel, 'MEDIUM')
    assert.equal(output.coverageByCategory.fieldCatalog.present, true)
    assert.equal(output.coverageByCategory.schemaRegistry.present, false)
    assert.equal(output.missingCriticalResources.some((item) => item.category === 'schemaRegistry'), true)
    assert.equal(output.truncatedResources.some((item) => item.path === 'DATABASE_RULES.md'), true)
    assert.equal(output.tokenBudgetBreakdown.source, 'context-files')
    assert.ok(output.contextQualityScore >= 50 && output.contextQualityScore < 80)
    assert.match(output.nextContextActions.join('\n'), /schema|Schema|契约|重新导出/)
    await assert.rejects(readFile(path.join(dir, '.dataspec', 'context', 'cache-metadata.json')), /ENOENT/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-quality check evaluates context zip and rejects unsafe zip entries', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-context-quality-zip-'))
  try {
    const zipPath = path.join(dir, 'context.zip')
    const zip = makeZip({
      'manifest.json': JSON.stringify({ kind: 'dataspec-ai-context-manifest', schemaVersion: 1 }),
      'catalog/field-catalog.json': JSON.stringify({ fields: [{ name: 'user_id' }] }),
      'DATABASE_RULES.md': '数据库规则完整说明',
      'schema-registry.json': JSON.stringify({ contracts: [] }),
      'prompts.md': '生成 SQL 前必须引用字段标准',
      'examples/good.sql': 'CREATE TABLE user_profile (user_id bigint);'
    })
    await writeFile(zipPath, zip)
    const io = createIo('', dir)

    const code = await runCli([
      'context-quality',
      'check',
      '--context-zip',
      'context.zip',
      '--format',
      'json'
    ], io)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.input.sourceType, 'context-zip')
    assert.equal(output.qualityLevel, 'HIGH')
    assert.equal(output.missingCriticalResources.length, 0)
    assert.equal(output.coverageByCategory.examples.present, true)

    const unsafeZipPath = path.join(dir, 'unsafe.zip')
    await writeFile(unsafeZipPath, makeZip({ '../secret.txt': 'raw-secret' }))
    const unsafeIo = createIo('', dir)
    const unsafeCode = await runCli([
      'context-quality',
      'check',
      '--context-zip',
      'unsafe.zip',
      '--format',
      'json'
    ], unsafeIo)

    assert.equal(unsafeCode, 2)
    assert.match(unsafeIo.stderr, /AI Context zip 包含越界路径/)
    assert.equal(unsafeIo.stdout, '')
    await assert.rejects(readFile(path.join(dir, 'secret.txt')), /ENOENT/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-quality check evaluates context budget plan and prints text summary', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-context-quality-plan-'))
  try {
    const planPath = path.join(dir, 'budget-plan.json')
    await writeFile(
      planPath,
      JSON.stringify(budgetPlanFixture({
        estimation: {
          tokenBudget: 1200,
          selectedEstimatedTokens: 960,
          totalEstimatedTokens: 3600,
          estimationMethod: 'deterministic-local-character-weight-v1',
          confidence: 'conservative'
        },
        selectedArtifacts: [
          { artifact: '.dataspec/DATABASE_RULES.md', estimatedTokens: 720, riskImpact: '缺失后规则风险升高。' }
        ],
        droppedArtifacts: [
          { artifact: '.dataspec/field-catalog.json', estimatedTokens: 1200, riskImpact: '缺失后字段选择风险升高。' },
          { artifact: '.dataspec/schema-registry.json', estimatedTokens: 900, riskImpact: '缺失后契约校验风险升高。' }
        ],
        qualityRisk: 'HIGH',
        fallbackSteps: ['提高 tokenBudget 或收窄 query 后重新运行 context-budget plan。'],
        recommendedNextActions: ['先补导出字段目录和 schema registry，再交给 AI 生成 SQL。']
      }), null, 2),
      'utf8'
    )
    const io = createIo('', dir)

    const code = await runCli([
      'context-quality',
      'check',
      '--budget-plan',
      'budget-plan.json',
      '--format',
      'json'
    ], io)

    assert.equal(code, 0)
    const output = JSON.parse(io.stdout)
    assert.equal(output.input.sourceType, 'budget-plan')
    assert.equal(output.qualityLevel, 'LOW')
    assert.equal(output.tokenBudgetBreakdown.tokenBudget, 1200)
    assert.equal(output.tokenBudgetBreakdown.droppedEstimatedTokens, 2100)
    assert.equal(output.missingCriticalResources.some((item) => item.category === 'fieldCatalog'), true)
    assert.equal(output.missingCriticalResources.some((item) => item.category === 'schemaRegistry'), true)
    assert.match(output.taskFitHints.join('\n'), /HIGH|高风险|不建议/)
    assert.match(output.nextContextActions.join('\n'), /tokenBudget|字段目录|schema registry/)

    const textIo = createIo('', dir)
    const textCode = await runCli([
      'context-quality',
      'check',
      '--budget-plan',
      'budget-plan.json',
      '--format',
      'text'
    ], textIo)

    assert.equal(textCode, 0)
    assert.match(textIo.stdout, /AI Context 质量检查/)
    assert.match(textIo.stdout, /qualityLevel: LOW/)
    assert.match(textIo.stdout, /nextContextActions/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-quality check rejects degenerated budget plan json before scoring', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-context-quality-invalid-plan-'))
  try {
    await writeFile(path.join(dir, 'budget-plan.json'), '{}', 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'context-quality',
      'check',
      '--budget-plan',
      'budget-plan.json',
      '--format',
      'json'
    ], io)

    assert.equal(code, 2)
    assert.equal(io.stdout, '')
    assert.match(io.stderr, /context-budget plan JSON/)
    assert.doesNotMatch(io.stderr, /可以继续使用当前 AI Context/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('context-quality check reports unclassified context files in coverage', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-cli-context-quality-unclassified-'))
  try {
    const contextDir = path.join(dir, 'context')
    await mkdir(contextDir, { recursive: true })
    await writeFile(path.join(contextDir, 'random-notes.md'), 'misc local context that does not match known categories\n', 'utf8')
    const io = createIo('', dir)

    const code = await runCli([
      'context-quality',
      'check',
      '--context-dir',
      'context',
      '--format',
      'json'
    ], io)
    const output = JSON.parse(io.stdout)

    assert.equal(code, 0)
    assert.equal(output.coverageByCategory.unclassified.present, true)
    assert.equal(output.coverageByCategory.unclassified.resourceCount, 1)
    assert.deepEqual(output.coverageByCategory.unclassified.sampleResources, ['random-notes.md'])
    assert.equal(output.missingCriticalResources.some((item) => item.category === 'fieldCatalog'), true)
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
  const standardEvidence = output.capabilities.find((capability) => capability.id === 'standard-evidence')
  assert.equal(standardEvidence.writeRisk, 'READ_ONLY')
  assert.deepEqual(standardEvidence.apiEndpoints, ['GET /api/standard-evidence'])
  assert.deepEqual(standardEvidence.cliCommands, [])
  assert.deepEqual(standardEvidence.mcpResources, [])
  assert.deepEqual(standardEvidence.mcpTools, [])
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

test('capability show prints standard evidence as api-only capability', async () => {
  const fetchFn = async (url) => {
    assert.equal(url, 'http://dataspec.local/api/capabilities/standard-evidence?projectId=7')
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: capabilityCatalogFixture().capabilities.find((item) => item.id === 'standard-evidence')
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'capability',
    'show',
    'standard-evidence',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'json'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.id, 'standard-evidence')
  assert.equal(output.category, 'evidence')
  assert.equal(output.writeRisk, 'READ_ONLY')
  assert.deepEqual(output.requiredInputs, ['projectId', 'subjectType', 'subjectId'])
  assert.deepEqual(output.outputContracts, ['cross-source-standard-evidence-view'])
  assert.deepEqual(output.apiEndpoints, ['GET /api/standard-evidence'])
  assert.deepEqual(output.cliCommands, [])
  assert.deepEqual(output.mcpResources, [])
  assert.deepEqual(output.mcpTools, [])
  assert.equal(output.safety.readOnly, true)
  assert.equal(output.safety.writesProject, false)
})

test('capability show text includes write safety summary', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: capabilityCatalogFixture().capabilities.find((item) => item.id === 'reverse-import')
    })
  })
  const io = createIo()

  const code = await runCli([
    'capability',
    'show',
    'reverse-import',
    '--project',
    '7',
    '--server',
    'http://dataspec.local',
    '--format',
    'text'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.match(io.stdout, /safety:/)
  assert.match(io.stdout, /requiresDryRun: true/)
  assert.match(io.stdout, /requiresIdempotencyKey: true/)
  assert.match(io.stdout, /sensitiveInputs: databasePassword/)
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

test('capability check fails when a write capability lacks safety metadata', async () => {
  const catalog = capabilityCatalogFixture()
  delete catalog.capabilities.find((item) => item.id === 'lint-sql').safety
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: catalog
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
  assert.match(JSON.stringify(output.diagnostics), /MISSING_SAFETY/)
  assert.match(JSON.stringify(output.diagnostics), /lint-sql/)
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

test('cli preserves structured safety diagnostic fields and redacts secrets', async () => {
  const io = createIo('CREATE TABLE users (id bigint);')
  const fetchFn = async () => ({
    ok: false,
    status: 400,
    json: async () => ({
      code: 400,
      message: '缺少 Idempotency-Key password=raw-secret Authorization: Bearer ds_cli_secret jdbc:postgresql://host/db dsn=postgres://user:dsn-secret@host/db postgres://user:naked-secret@host/db',
      error: {
        code: 'IDEMPOTENCY_KEY_REQUIRED',
        category: 'SAFETY',
        retryable: true,
        suggestedAction: '携带 --idempotency-key 重试 password=raw-secret dsn=postgres://user:dsn-secret@host/db mysql://user:naked-secret@host/db',
        docsRef: 'README.md#ai-写入安全协议',
        missing: ['Idempotency-Key'],
        operation: 'project-restore:apply',
        safety: { readOnly: false, writesProject: true, requiresIdempotencyKey: true },
        nextActions: ['重新运行命令并传入 --idempotency-key <key>']
      }
    })
  })

  const code = await runCli(['lint', '-', '--project', '7', '--format', 'json'], io, fetchFn)
  const diagnosticLine = io.stderr.split(/\r?\n/).find((line) => line.startsWith('DataSpecError: '))
  const diagnostic = JSON.parse(diagnosticLine.replace('DataSpecError: ', ''))

  assert.equal(code, 2)
  assert.equal(diagnostic.code, 'IDEMPOTENCY_KEY_REQUIRED')
  assert.equal(diagnostic.category, 'SAFETY')
  assert.deepEqual(diagnostic.missing, ['Idempotency-Key'])
  assert.equal(diagnostic.operation, 'project-restore:apply')
  assert.equal(diagnostic.safety.requiresIdempotencyKey, true)
  assert.match(diagnostic.nextActions[0], /idempotency-key/)
  assert.doesNotMatch(io.stderr, /raw-secret|ds_cli_secret|jdbc:postgresql:\/\/host\/db|dsn-secret|naked-secret|dsn=postgres:\/\/user:|postgres:\/\/user:|mysql:\/\/user:/)
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

test('synthetic-examples generate calls synthetic examples api and prints json', async () => {
  const calls = []
  const fetchFn = async (url) => {
    calls.push(url)
    return {
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: syntheticExamplesFixture('order')
      })
    }
  }
  const io = createIo()

  const code = await runCli([
    'synthetic-examples',
    'generate',
    '--project',
    '7',
    '--scenario',
    'order',
    '--max-cases',
    '5',
    '--format',
    'json',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.equal(
    calls[0],
    'http://dataspec.local/api/synthetic-examples/generate?projectId=7&scenario=order&maxCases=5'
  )
  const output = JSON.parse(io.stdout)
  assert.equal(output.kind, 'dataspec.synthetic-standard-examples')
  assert.equal(output.scenario, 'order')
  assert.equal(output.safety.readOnly, true)
  assert.equal(output.safety.writesProject, false)
  assert.equal(io.stderr, '')
})

test('synthetic-examples generate text prints concise summary', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: syntheticExamplesFixture('audit')
    })
  })
  const io = createIo()

  const code = await runCli([
    'synthetic-examples',
    'generate',
    '--project',
    '7',
    '--scenario',
    'audit',
    '--format',
    'text',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.match(io.stdout, /DataSpec Synthetic Examples/)
  assert.match(io.stdout, /scenario: audit/)
  assert.match(io.stdout, /specHash: synthetic-hash-audit/)
  assert.match(io.stdout, /goodSql: 1/)
  assert.match(io.stdout, /writesProject: false/)
  assert.equal(io.stderr, '')
})

test('synthetic-examples generate redacts server diagnostics', async () => {
  const fetchFn = async () => ({
    ok: false,
    status: 400,
    json: async () => ({
      code: 400,
      message: '不支持场景 token=raw-secret',
      error: {
        code: 'SYNTHETIC_SCENARIO_INVALID',
        category: 'VALIDATION',
        suggestedAction: '不要使用 Authorization: Bearer raw-secret'
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'synthetic-examples',
    'generate',
    '--project',
    '7',
    '--scenario',
    'order',
    '--format',
    'json',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  assert.equal(code, 2)
  assert.match(io.stderr, /DataSpecError/)
  assert.match(io.stderr, /\*\*\*/)
  assert.doesNotMatch(io.stderr, /raw-secret/)
})

test('contract-import preview calls preview api and prints json', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-contract-import-'))
  try {
    const inputPath = path.join(dir, 'order-openapi.yaml')
    const contractContent = 'openapi: 3.0.3\ncomponents:\n  schemas:\n    Order:\n      type: object\n'
    await writeFile(inputPath, contractContent, 'utf8')
    const calls = []
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: contractCandidatePreviewFixture('openapi')
        })
      }
    }
    const io = createIo('', dir)

    const code = await runCli([
      'contract-import',
      'preview',
      '--project',
      '7',
      '--source-kind',
      'openapi',
      '--input',
      'order-openapi.yaml',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/contract-import/preview')
    assert.equal(calls[0].options.method, 'POST')
    assert.equal(calls[0].options.headers['Content-Type'], 'application/json')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      projectId: 7,
      sourceKind: 'openapi',
      sourcePath: 'order-openapi.yaml',
      contractContent
    })
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec.contract-candidate-preview')
    assert.equal(output.sourceKind, 'openapi')
    assert.equal(output.safety.readOnly, true)
    assert.equal(output.safety.writesProject, false)
    assert.equal(output.candidateFields[0].inboxPayload.sourceType, 'CONTRACT_IMPORT')
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('contract-import preview text prints concise summary', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-contract-import-text-'))
  try {
    await writeFile(path.join(dir, 'customer.schema.json'), '{"type":"object"}', 'utf8')
    const fetchFn = async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        code: 200,
        data: contractCandidatePreviewFixture('json-schema')
      })
    })
    const io = createIo('', dir)

    const code = await runCli([
      'contract-import',
      'preview',
      '--project',
      '7',
      '--source-kind',
      'json-schema',
      '--input',
      'customer.schema.json',
      '--format',
      'text',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.match(io.stdout, /DataSpec Contract Import Preview/)
    assert.match(io.stdout, /sourceKind: json-schema/)
    assert.match(io.stdout, /contractHash: contract-hash-json-schema/)
    assert.match(io.stdout, /candidateFields: 1/)
    assert.match(io.stdout, /readOnly: true/)
    assert.match(io.stdout, /writesProject: false/)
    assert.equal(io.stderr, '')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('contract-import preview rejects missing input file and unsupported source kind safely', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-contract-import-invalid-'))
  try {
    const missingIo = createIo('', dir)
    const missingCode = await runCli([
      'contract-import',
      'preview',
      '--project',
      '7',
      '--source-kind',
      'openapi',
      '--input',
      'missing-token=raw-secret.yaml',
      '--format',
      'json'
    ], missingIo, async () => {
      throw new Error('fetch should not be called')
    })

    const unsupportedIo = createIo('', dir)
    const unsupportedCode = await runCli([
      'contract-import',
      'preview',
      '--project',
      '7',
      '--source-kind',
      'swagger-token=raw-secret',
      '--input',
      'missing.yaml',
      '--format',
      'json'
    ], unsupportedIo, async () => {
      throw new Error('fetch should not be called')
    })

    assert.equal(missingCode, 2)
    assert.match(missingIo.stderr, /输入文件|input/)
    assert.doesNotMatch(missingIo.stderr, /raw-secret/)
    assert.equal(unsupportedCode, 2)
    assert.match(unsupportedIo.stderr, /openapi\|json-schema\|protobuf|openapi/)
    assert.doesNotMatch(unsupportedIo.stderr, /raw-secret/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('contract-import preview redacts server diagnostics', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-contract-import-error-'))
  try {
    await writeFile(path.join(dir, 'order.yaml'), 'openapi: 3.0.3', 'utf8')
    const fetchFn = async () => ({
      ok: false,
      status: 400,
      json: async () => ({
        code: 400,
        message: '契约导入失败 token=raw-secret jdbc:postgresql://db.internal/app',
        error: {
          code: 'CONTRACT_IMPORT_INVALID',
          category: 'VALIDATION',
          suggestedAction: '检查 Authorization: Bearer raw-secret'
        }
      })
    })
    const io = createIo('', dir)

    const code = await runCli([
      'contract-import',
      'preview',
      '--project',
      '7',
      '--source-kind',
      'openapi',
      '--input',
      'order.yaml',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 2)
    assert.match(io.stderr, /DataSpecError/)
    assert.match(io.stderr, /\*\*\*/)
    assert.doesNotMatch(io.stderr, /raw-secret/)
    assert.doesNotMatch(io.stderr, /db\.internal/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('schema-plan posts database connection request and prints readonly plan json', async () => {
  const calls = []
  const previous = process.env.DATASPEC_DB_PASSWORD
  process.env.DATASPEC_DB_PASSWORD = 'raw-db-secret'
  try {
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            kind: 'dataspec-database-schema-change-plan',
            riskLevel: 'BLOCKED',
            currentSchemaHash: 'a'.repeat(64),
            targetSpecHash: 'b'.repeat(64),
            changeSet: [{ tableName: 'user_order', columnName: 'legacy_col', action: 'DROP_CANDIDATE' }],
            migrationSql: '-- BLOCKED DROP_CANDIDATE "user_order"."legacy_col": review manually before writing destructive SQL.',
            blockedReasons: ['legacy_col 需要人工确认'],
            nextActions: ['高风险或阻塞项需要人工确认后再交给迁移工具。']
          }
        })
      }
    }
    const io = createIo()

    const code = await runCli([
      'schema-plan',
      '--project',
      '7',
      '--database-type',
      'postgresql',
      '--host',
      'localhost',
      '--port',
      '5432',
      '--database',
      'demo',
      '--schema',
      'public',
      '--username',
      'readonly',
      '--password-env',
      'DATASPEC_DB_PASSWORD',
      '--table',
      'user_order',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/reverse-import/database/schema-plan')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      projectId: 7,
      databaseType: 'postgresql',
      host: 'localhost',
      port: 5432,
      databaseName: 'demo',
      schemaName: 'public',
      username: 'readonly',
      password: 'raw-db-secret',
      tableNames: ['user_order']
    })
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec-database-schema-change-plan')
    assert.equal(output.riskLevel, 'BLOCKED')
    assert.doesNotMatch(io.stdout, /raw-db-secret|jdbc:/)
    assert.equal(io.stderr, '')
  } finally {
    if (previous === undefined) {
      delete process.env.DATASPEC_DB_PASSWORD
    } else {
      process.env.DATASPEC_DB_PASSWORD = previous
    }
  }
})

test('comment-plan preview posts database connection request and prints plan json', async () => {
  const calls = []
  const previous = process.env.DATASPEC_DB_PASSWORD
  process.env.DATASPEC_DB_PASSWORD = 'raw-db-secret'
  try {
    const fetchFn = async (url, options) => {
      calls.push({ url, options })
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            kind: 'dataspec-database-comment-patch-plan',
            riskLevel: 'LOW',
            metadataFingerprint: 'c'.repeat(64),
            planHash: 'd'.repeat(64),
            summary: {
              tableCount: 1,
              columnCount: 1,
              executableChangeCount: 1,
              unsupportedCount: 0
            },
            dryRunSql: 'COMMENT ON COLUMN "public"."user_order"."phone" IS \'手机号\';',
            nextActions: ['先审阅 dry-run SQL，再交给迁移工具。']
          }
        })
      }
    }
    const io = createIo()

    const code = await runCli([
      'comment-plan',
      'preview',
      '--project',
      '7',
      '--database-type',
      'postgresql',
      '--host',
      'localhost',
      '--port',
      '5432',
      '--database',
      'demo',
      '--schema',
      'public',
      '--username',
      'readonly',
      '--password-env',
      'DATASPEC_DB_PASSWORD',
      '--table',
      'user_order',
      '--metadata-cache-mode',
      'REFRESH',
      '--format',
      'json',
      '--server',
      'http://dataspec.local'
    ], io, fetchFn)

    assert.equal(code, 0)
    assert.equal(calls[0].url, 'http://dataspec.local/api/reverse-import/database/comment-plan')
    assert.deepEqual(JSON.parse(calls[0].options.body), {
      projectId: 7,
      databaseType: 'postgresql',
      host: 'localhost',
      port: 5432,
      databaseName: 'demo',
      schemaName: 'public',
      username: 'readonly',
      password: 'raw-db-secret',
      tableNames: ['user_order'],
      metadataCacheMode: 'REFRESH'
    })
    const output = JSON.parse(io.stdout)
    assert.equal(output.kind, 'dataspec-database-comment-patch-plan')
    assert.equal(output.riskLevel, 'LOW')
    assert.doesNotMatch(io.stdout, /raw-db-secret|jdbc:/)
    assert.equal(io.stderr, '')
  } finally {
    if (previous === undefined) {
      delete process.env.DATASPEC_DB_PASSWORD
    } else {
      process.env.DATASPEC_DB_PASSWORD = previous
    }
  }
})

test('comment-plan preview text output summarizes risk sql and next actions without stable json contract', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        kind: 'dataspec-database-comment-patch-plan',
        riskLevel: 'MEDIUM',
        metadataFingerprint: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
        summary: {
          tableCount: 1,
          columnCount: 2,
          executableChangeCount: 1,
          unsupportedCount: 1
        },
        dryRunSql: 'COMMENT ON TABLE "public"."user_order" IS \'用户订单\';',
        nextActions: ['MySQL 列注释需人工补完整列定义。'],
        rollbackHint: 'password=raw-secret jdbc:postgresql://db.internal/app'
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'comment-plan',
    'preview',
    '--project',
    '7',
    '--database-type',
    'postgresql',
    '--host',
    'localhost',
    '--database',
    'demo',
    '--username',
    'readonly',
    '--table',
    'user_order',
    '--format',
    'text'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.match(io.stdout, /COMMENT patch plan/)
  assert.match(io.stdout, /risk=MEDIUM/)
  assert.match(io.stdout, /unsupported=1/)
  assert.match(io.stdout, /dryRunSql=yes/)
  assert.match(io.stdout, /metadataFingerprint=abcdef123456/)
  assert.match(io.stdout, /MySQL 列注释/)
  assert.doesNotMatch(io.stdout, /raw-secret|jdbc:postgresql|db\.internal/)
})

test('comment-plan preview text reports no executable sql for no-op explanatory dry run text', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: {
        kind: 'dataspec-database-comment-patch-plan',
        riskLevel: 'SAFE',
        metadataFingerprint: 'abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890',
        summary: {
          tableCount: 1,
          columnCount: 2,
          executableChangeCount: 0,
          unsupportedCount: 0
        },
        dryRunSql: '-- 当前无 COMMENT 变更。',
        nextActions: ['无需执行 COMMENT SQL。']
      }
    })
  })
  const io = createIo()

  const code = await runCli([
    'comment-plan',
    'preview',
    '--project',
    '7',
    '--database-type',
    'postgresql',
    '--host',
    'localhost',
    '--database',
    'demo',
    '--username',
    'readonly',
    '--table',
    'user_order',
    '--format',
    'text'
  ], io, fetchFn)

  assert.equal(code, 0)
  assert.match(io.stdout, /totalChanges=0/)
  assert.match(io.stdout, /dryRunSql=no/)
  assert.match(io.stdout, /无需执行 COMMENT SQL/)
})

test('comment-plan preview rejects missing table and redacts server failures', async () => {
  const missingIo = createIo()
  const missingCode = await runCli([
    'comment-plan',
    'preview',
    '--project',
    '7',
    '--database-type',
    'postgresql',
    '--host',
    'localhost',
    '--database',
    'demo',
    '--username',
    'readonly',
    '--format',
    'json'
  ], missingIo, async () => {
    throw new Error('fetch should not be called')
  })

  assert.equal(missingCode, 2)
  assert.match(missingIo.stderr, /至少提供一个 --table/)

  const failIo = createIo()
  const failCode = await runCli([
    'comment-plan',
    'preview',
    '--project',
    '7',
    '--database-type',
    'postgresql',
    '--host',
    'localhost',
    '--database',
    'demo',
    '--username',
    'readonly',
    '--password',
    'raw-secret',
    '--table',
    'user_order',
    '--format',
    'json'
  ], failIo, async () => ({
    ok: false,
    status: 500,
    json: async () => ({
      message: 'comment plan failed token=plain-secret jdbc:postgresql://db.internal/app password=raw-secret Authorization: Basic raw-basic-secret'
    })
  }))

  assert.equal(failCode, 2)
  assert.match(failIo.stderr, /DataSpecError/)
  assert.doesNotMatch(failIo.stderr, /plain-secret|raw-secret|jdbc:postgresql|db\.internal|raw-basic-secret/)
})

test('comment-plan preview rejects invalid metadata cache mode before server calls', async () => {
  const io = createIo()
  const code = await runCli([
    'comment-plan',
    'preview',
    '--project',
    '7',
    '--database-type',
    'postgresql',
    '--host',
    'localhost',
    '--database',
    'demo',
    '--username',
    'readonly',
    '--table',
    'user_order',
    '--metadata-cache-mode',
    'refreshh',
    '--format',
    'json'
  ], io, async () => {
    throw new Error('fetch should not be called')
  })

  assert.equal(code, 2)
  assert.match(io.stderr, /metadata-cache-mode/)
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

test('compat check prints version payload json and returns 0 when compatible', async () => {
  const calls = []
  const fetchFn = async (url, options = {}) => {
    calls.push({ url, options })
    if (url === 'http://dataspec.local/api/capabilities/version?client=cli&clientVersion=0.1.0') {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: versionCompatibilityPayload()
        })
      }
    }
    throw new Error(`unexpected fetch: ${url}`)
  }
  const io = createIo()

  const code = await runCli([
    'compat',
    'check',
    '--format',
    'json',
    '--server',
    'http://dataspec.local',
    '--dataspec-token',
    'ds_cli_token'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 0)
  assert.equal(output.localCliVersion, '0.1.0')
  assert.equal(output.compatibility.status, 'COMPATIBLE')
  assert.equal(calls[0].options.headers.Authorization, 'Bearer ds_cli_token')
  assert.equal(io.stderr, '')
})

test('compat check returns 1 when server reports incompatible client', async () => {
  const fetchFn = async () => ({
    ok: true,
    status: 200,
    json: async () => ({
      code: 200,
      data: versionCompatibilityPayload({
        compatibility: {
          status: 'INCOMPATIBLE',
          clientVersion: '0.1.0',
          compatible: false,
          reasons: ['CLI 版本过旧'],
          nextActions: ['升级 dataspec CLI']
        }
      })
    })
  })
  const io = createIo()

  const code = await runCli(['compat', 'check', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 1)
  assert.equal(output.compatibility.compatible, false)
  assert.match(output.compatibility.nextActions.join('\n'), /升级/)
  assert.equal(io.stderr, '')
})

test('compat check returns 2 and redacts diagnostics when server is unreachable', async () => {
  const fetchFn = async () => {
    throw new Error('connect failed token=raw-secret jdbc:postgresql://localhost/db')
  }
  const io = createIo()

  const code = await runCli([
    'compat',
    'check',
    '--format',
    'json',
    '--server',
    'http://dataspec.local'
  ], io, fetchFn)

  const output = JSON.parse(io.stdout)
  assert.equal(code, 2)
  assert.equal(output.ok, false)
  assert.equal(output.diagnostic.code, 'VERSION_COMPATIBILITY_UNAVAILABLE')
  assert.doesNotMatch(JSON.stringify(output), /raw-secret|jdbc:postgresql:\/\/localhost\/db/)
  assert.equal(io.stderr, '')
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
      if (url === 'http://dataspec.local/api/capabilities/version?client=cli&clientVersion=0.1.0') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            code: 200,
            data: versionCompatibilityPayload()
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
      'compatibility',
      'defaultPaths',
      'ai-profile',
      'openapi',
      'context-cache'
    ])
    assert.equal(output.checks.every((check) => check.status === 'pass'), true)
    assert.equal(output.checks.find((check) => check.name === 'compatibility').details.localCliVersion, '0.1.0')
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
    'export-min-context',
    'standard-evidence-review',
    'standard-maintenance'
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

test('workflow show prints standard evidence review as api-only plan', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'show', 'standard-evidence-review', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const inputNames = output.recipe.requiredInputs.map((input) => input.name)
  const commands = [
    ...output.recipe.prechecks.map((precheck) => precheck.command),
    ...output.recipe.steps.map((step) => step.command)
  ].join('\n')
  assert.equal(code, 0)
  assert.equal(output.recipe.id, 'standard-evidence-review')
  assert.deepEqual(inputNames, ['projectId', 'subjectType', 'subjectId'])
  assert.equal(output.recipe.sideEffectPolicy, 'plan-only')
  assert.match(commands, /GET \/api\/standard-evidence/)
  assert.match(commands, /capability show standard-evidence/)
  assert.doesNotMatch(commands, /dataspec(?:-cli\.mjs)?\s+standard-evidence\b/)
  assert.doesNotMatch(commands, /dataspec:\/\/project\/<projectId>\/standard-evidence/)
  assert.ok(output.recipe.expectedArtifacts.some((artifact) => artifact.includes('证据摘要')))
  assert.equal(io.stderr, '')
})

test('workflow show prints standard maintenance as dry-run plan', async () => {
  const io = createIo()
  const fetchFn = async () => {
    throw new Error('fetch should not be called')
  }

  const code = await runCli(['workflow', 'show', 'standard-maintenance', '--format', 'json'], io, fetchFn)

  const output = JSON.parse(io.stdout)
  const inputNames = output.recipe.requiredInputs.map((input) => input.name)
  const commands = [
    ...output.recipe.prechecks.map((precheck) => precheck.command),
    ...output.recipe.steps.map((step) => step.command)
  ].join('\n')
  assert.equal(code, 0)
  assert.equal(output.recipe.id, 'standard-maintenance')
  assert.ok(inputNames.includes('projectId'))
  assert.ok(inputNames.includes('sourceType'))
  assert.match(commands, /\/api\/standard-maintenance\/workflows\/plan/)
  assert.match(commands, /workflowPlan/)
  assert.doesNotMatch(commands, /accept\|merge\|ignore\|postpone.*--auto/)
  assert.equal(output.recipe.sideEffectPolicy, 'plan-only')
  assert.ok(output.recipe.failureHandling.some((item) => item.nextAction.includes('人工确认')))
  assert.equal(io.stderr, '')
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
    if (url === `${server}/api/capabilities/version?client=cli&clientVersion=0.1.0`) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: versionCompatibilityPayload()
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

function budgetPlanFixture(overrides = {}) {
  return {
    kind: 'dataspec-ai-context-budget-plan',
    schemaVersion: 1,
    projectId: 9,
    request: {
      projectId: 9,
      tokenBudget: 2400,
      taskType: null,
      profileId: null,
      scope: 'field',
      query: '用户手机',
      status: 'enabled',
      limit: 20,
      targetTable: null,
      targetFile: null,
      totalFieldCount: 12,
      matchedFieldCount: 2,
      returnedFieldCount: 2
    },
    estimation: {
      tokenBudget: 2400,
      selectedEstimatedTokens: 1800,
      totalEstimatedTokens: 4200,
      estimationMethod: 'deterministic-local-character-weight-v1',
      confidence: 'conservative'
    },
    selectedArtifacts: [
      {
        artifact: '.dataspec/DATABASE_RULES.md',
        estimatedTokens: 720,
        reason: '保留数据库命名和规则说明。',
        riskImpact: '缺失后 DDL/SQL 生成风险显著升高。',
        appliedScope: { scope: 'field', query: '用户手机', status: 'enabled', limit: 20, profileId: null, taskType: null }
      }
    ],
    droppedArtifacts: [
      {
        artifact: '.dataspec/prompts.md',
        estimatedTokens: 320,
        reason: '保留提示词模板。',
        riskImpact: '缺失后 prompt 复用能力下降。',
        appliedScope: { scope: 'field', query: '用户手机', status: 'enabled', limit: 20, profileId: null, taskType: null }
      }
    ],
    qualityRisk: 'MEDIUM',
    fallbackSteps: ['补充 query 或目标提示以提升低预算裁剪质量。'],
    recommendedExportParams: { scope: 'field', query: '用户手机', status: 'enabled', limit: 20, profileId: null, taskType: null },
    diagnostics: [],
    recommendedNextActions: ['可使用 recommendedExportParams 显式填充导出参数后预览字段目录。'],
    ...overrides
  }
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
    'sql-rule-debugger',
    'search-fields',
    'suggest-fields',
    'generate-ddl',
    'reverse-import',
    'coverage-report',
    'schema-registry',
    'export-evidence-package',
    'standard-evidence',
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
      category: ['lint-sql', 'sql-rule-debugger'].includes(id) ? 'sql' : id === 'standard-evidence' ? 'evidence' : 'discovery',
      title: id,
      summary: `${id} summary`,
      status: 'AVAILABLE',
      stability: 'stable-ai',
      requiresProject: !['session-bootstrap', 'capability-catalog', 'doctor', 'schema-registry', 'workflow-recipes'].includes(id),
      writeRisk: id === 'lint-sql' ? 'WRITES_DATASPEC_RECORD' : 'READ_ONLY',
      safety: capabilitySafetyFixture(id),
      requiredInputs: id === 'standard-evidence'
        ? ['projectId', 'subjectType', 'subjectId']
        : ['lint-sql', 'sql-rule-debugger'].includes(id) ? ['projectId', 'sql'] : [],
      optionalInputs: [],
      outputContracts: id === 'lint-sql'
        ? ['lint-result']
        : id === 'sql-rule-debugger' ? ['sql-rule-debug-result', 'lint-result']
        : id === 'standard-evidence' ? ['cross-source-standard-evidence-view'] : ['ai-capability-catalog'],
      apiEndpoints: id === 'session-bootstrap'
        ? ['GET /api/bootstrap/session']
        : id === 'lint-sql' ? ['POST /api/lint']
        : id === 'sql-rule-debugger' ? ['POST /api/lint/debug']
        : id === 'standard-evidence' ? ['GET /api/standard-evidence'] : ['GET /api/capabilities'],
      cliCommands: id === 'lint-sql'
        ? ['dataspec lint <file.sql> --project <id> --format json']
        : id === 'sql-rule-debugger' ? ['dataspec lint-debug <file.sql> --project <id> --format json']
        : id === 'standard-evidence' ? []
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

function syntheticExamplesFixture(scenario = 'order') {
  return {
    kind: 'dataspec.synthetic-standard-examples',
    schemaVersion: 1,
    projectId: 7,
    scenario,
    specHash: `synthetic-hash-${scenario}`,
    generationParams: { scenario, maxCases: 5 },
    sourceSummary: {
      standardFieldCount: 2,
      templateCount: 1,
      codeSetReferenceCount: 0,
      fallbackUsed: false,
      selectedFieldNames: [`${scenario}_id`, 'created_at']
    },
    goodSql: [{ id: `${scenario}-good-sql-1`, sql: 'CREATE TABLE synthetic_case (id bigint);' }],
    badSql: [{ id: `${scenario}-bad-sql-1`, expectedDiagnosticIds: [`${scenario}-NON_STANDARD_FIELD_NAME`] }],
    ddlPreviewInputs: [{ id: `${scenario}-ddl-preview-1`, tableName: `synthetic_${scenario}_preview` }],
    fieldSuggestionQuestions: [{ id: `${scenario}-field-question-1`, question: `${scenario} 字段推荐` }],
    standardQaCases: [{ id: `${scenario}-qa-1`, question: `${scenario} 标准字段是什么？` }],
    expectedDiagnostics: [{ id: `${scenario}-NON_STANDARD_FIELD_NAME`, severity: 'ERROR' }],
    diagnostics: [],
    safety: {
      readOnly: true,
      writesProject: false,
      containsRealBusinessRows: false,
      externalLlmUsed: false,
      sensitiveInputs: []
    },
    nextActions: ['人工审核后再采纳为 usage example']
  }
}

function contractCandidatePreviewFixture(sourceKind = 'openapi') {
  return {
    kind: 'dataspec.contract-candidate-preview',
    schemaVersion: 1,
    projectId: 7,
    sourceKind,
    sourcePath: `contracts/sample-${sourceKind}.yaml`,
    contractHash: `contract-hash-${sourceKind}`,
    summary: {
      sourceFieldCount: 1,
      candidateCount: 1,
      duplicateCount: 0,
      existingMatchCount: 0,
      diagnosticCount: 0,
      truncated: false
    },
    candidateFields: [
      {
        candidateKey: `${sourceKind}:order_id`,
        candidateName: 'order_id',
        displayName: '订单ID',
        dataType: 'bigint',
        required: true,
        enumValues: [],
        exampleValues: ['1001'],
        sourcePath: '#/components/schemas/Order/properties/orderId',
        schemaVersion: 1,
        confidence: 82,
        conflictReasons: [],
        recommendedAction: 'CREATE_CANDIDATE',
        inboxPayload: {
          projectId: 7,
          candidateName: 'order_id',
          displayName: '订单ID',
          dataType: 'bigint',
          comment: '订单ID',
          sourceType: 'CONTRACT_IMPORT',
          sourceRef: `${sourceKind}:#/components/schemas/Order/properties/orderId`,
          evidenceJson: '{}',
          confidence: 82
        }
      }
    ],
    diagnostics: [],
    safety: {
      readOnly: true,
      writesProject: false,
      externalNetworkUsed: false,
      externalLlmUsed: false,
      containsRealBusinessRows: false,
      sensitiveInputs: []
    },
    nextActions: ['人工复核后再提交 inboxPayload']
  }
}

function capabilitySafetyFixture(id) {
  const writes = ['lint-sql', 'reverse-import', 'generate-ddl', 'domain-starter-kits'].includes(id)
  const highRisk = ['reverse-import', 'domain-starter-kits'].includes(id)
  return {
    readOnly: !writes,
    writesProject: writes,
    requiresDryRun: highRisk,
    supportsUndo: highRisk,
    requiresIdempotencyKey: highRisk,
    sensitiveInputs: id === 'reverse-import' ? ['databasePassword'] : [],
    nextActions: highRisk
      ? ['先运行 preview，再携带 --idempotency-key 执行 apply']
      : ['按 capability nextActions 执行']
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

function versionCompatibilityPayload(overrides = {}) {
  return {
    kind: 'dataspec-version-compatibility',
    schemaVersion: 1,
    serverVersion: '0.1.0-SNAPSHOT',
    apiSchemaHash: 'sha256:test',
    minCliVersion: '0.1.0',
    supportedCapabilities: [
      { id: 'version-compatibility', status: 'AVAILABLE', minClientVersion: '0.1.0' }
    ],
    deprecatedFields: [],
    compatibility: {
      status: 'COMPATIBLE',
      clientVersion: '0.1.0',
      compatible: true,
      reasons: ['兼容'],
      nextActions: ['继续执行']
    },
    upgradeHints: ['先运行 dataspec compat check --format json'],
    generatedAt: '2026-07-05T10:00:00',
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
