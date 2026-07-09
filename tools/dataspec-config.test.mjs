import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  findDataSpecConfig,
  loadDataSpecConfig,
  resolveDefaultPaths
} from './dataspec-config.mjs'

test('loads nearest .dataspec config from parent directory', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    const nestedDir = path.join(dir, 'db', 'migrations')
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await mkdir(nestedDir, { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        server: 'http://dataspec.local/',
        apiToken: 'ds_test_token',
        aiProfile: 'sql-fix',
        taskType: 'SQL_FIX',
        securityProfile: {
          redactionStrictness: 'strict',
          sensitiveFieldPolicy: 'metadata-only',
          allowedAiTools: ['codex-local', 'mcp-local'],
          neverExportPatterns: ['customer_secret', 'token='],
          localOnlyPaths: ['.dataspec/context'],
          samplePolicy: 'synthetic-only',
          credentialPolicy: 'never-export'
        },
        defaultPaths: ['sql', 'db/migrations']
      }),
      'utf8'
    )

    const configPath = findDataSpecConfig(nestedDir)
    const config = loadDataSpecConfig(nestedDir)

    assert.equal(configPath, path.join(dir, '.dataspec', 'config.json'))
    assert.equal(config.configPath, configPath)
    assert.equal(config.rootDir, dir)
    assert.equal(config.projectId, 7)
    assert.equal(config.server, 'http://dataspec.local')
    assert.equal(config.apiToken, 'ds_test_token')
    assert.equal(config.aiProfile, 'sql-fix')
    assert.equal(config.taskType, 'SQL_FIX')
    assert.deepEqual(config.securityProfile, {
      redactionStrictness: 'strict',
      sensitiveFieldPolicy: 'metadata-only',
      allowedAiTools: ['codex-local', 'mcp-local'],
      neverExportPatterns: ['customer_secret', 'token='],
      localOnlyPaths: ['.dataspec/context'],
      samplePolicy: 'synthetic-only',
      credentialPolicy: 'never-export'
    })
    assert.deepEqual(config.defaultPaths, ['sql', 'db/migrations'])
    assert.deepEqual(resolveDefaultPaths(config), [
      path.join(dir, 'sql'),
      path.join(dir, 'db', 'migrations')
    ])
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('returns empty config when .dataspec config is absent', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    const config = loadDataSpecConfig(dir)

    assert.equal(config.configPath, null)
    assert.equal(config.rootDir, path.resolve(dir))
    assert.equal(config.projectId, undefined)
    assert.equal(config.server, undefined)
    assert.equal(config.apiToken, undefined)
    assert.equal(config.aiProfile, undefined)
    assert.equal(config.taskType, undefined)
    assert.equal(config.securityProfile, undefined)
    assert.deepEqual(config.defaultPaths, [])
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects invalid security profile shape without exposing raw secret values', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({
        projectId: 7,
        apiToken: 'ds_config_token',
        securityProfile: {
          redactionStrictness: 1,
          allowedAiTools: 'codex-local',
          neverExportPatterns: ['password=raw-config-secret']
        }
      }),
      'utf8'
    )

    assert.throws(
      () => loadDataSpecConfig(dir),
      (error) => {
        assert.match(error.message, /securityProfile\.redactionStrictness 必须是字符串/)
        assert.doesNotMatch(error.message, /raw-config-secret/)
        assert.doesNotMatch(error.message, /ds_config_token/)
        return true
      }
    )
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects non-string AI profile defaults', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, aiProfile: 123, taskType: true }),
      'utf8'
    )

    assert.throws(() => loadDataSpecConfig(dir), /aiProfile 必须是字符串/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects invalid config json with readable error', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(path.join(dir, '.dataspec', 'config.json'), '{ nope', 'utf8')

    assert.throws(() => loadDataSpecConfig(dir), /无法解析 DataSpec 配置/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects invalid project id', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 0, defaultPaths: 'sql' }),
      'utf8'
    )

    assert.throws(() => loadDataSpecConfig(dir), /无效 DataSpec projectId/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects non-array default paths', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, defaultPaths: 'sql' }),
      'utf8'
    )

    assert.throws(() => loadDataSpecConfig(dir), /defaultPaths 必须是字符串数组/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
