import assert from 'node:assert/strict'
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  DATASPEC_CONFIG_SCHEMA_FILE,
  DATASPEC_CONFIG_SCHEMA_REF,
  DATASPEC_CONFIG_SCHEMA_SOURCE_FILE,
  DATASPEC_CONFIG_SCHEMA_VERSION,
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
        $schema: DATASPEC_CONFIG_SCHEMA_REF,
        configVersion: DATASPEC_CONFIG_SCHEMA_VERSION,
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
        defaultPaths: ['sql', 'db/migrations'],
        'x-team-note': 'runtime-compatible extension'
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
    assert.equal(config.schemaRef, DATASPEC_CONFIG_SCHEMA_REF)
    assert.equal(config.configVersion, DATASPEC_CONFIG_SCHEMA_VERSION)
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
    assert.equal(config.schemaRef, undefined)
    assert.equal(config.configVersion, undefined)
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

test('accepts legacy numeric project id strings and rejects boolean or object ids', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    const configPath = path.join(dir, '.dataspec', 'config.json')
    await writeFile(configPath, JSON.stringify({ projectId: '7' }), 'utf8')
    assert.equal(loadDataSpecConfig(dir).projectId, 7)

    await writeFile(configPath, JSON.stringify({ projectId: true }), 'utf8')
    assert.throws(() => loadDataSpecConfig(dir), /无效 DataSpec projectId/)

    await writeFile(configPath, JSON.stringify({ projectId: { value: 7 } }), 'utf8')
    assert.throws(() => loadDataSpecConfig(dir), /无效 DataSpec projectId/)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('rejects server URL userinfo without echoing credentials', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    await writeFile(
      path.join(dir, '.dataspec', 'config.json'),
      JSON.stringify({ projectId: 7, server: 'https://local-user:raw-server-secret@example.com' }),
      'utf8'
    )

    assert.throws(
      () => loadDataSpecConfig(dir),
      (error) => {
        assert.match(error.message, /server URL 不能包含用户名或密码/)
        assert.doesNotMatch(error.message, /local-user|raw-server-secret/)
        return true
      }
    )
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

test('rejects invalid schema metadata before commands can use the config', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-config-'))
  try {
    await mkdir(path.join(dir, '.dataspec'), { recursive: true })
    const configPath = path.join(dir, '.dataspec', 'config.json')
    await writeFile(configPath, JSON.stringify({ $schema: 1, configVersion: 1 }), 'utf8')
    assert.throws(() => loadDataSpecConfig(dir), /\$schema 必须是非空字符串/)

    await writeFile(configPath, JSON.stringify({ $schema: DATASPEC_CONFIG_SCHEMA_REF, configVersion: '1' }), 'utf8')
    assert.throws(() => loadDataSpecConfig(dir), /configVersion 必须是正整数/)

    for (const schemaRef of [null, '', '   ']) {
      await writeFile(configPath, JSON.stringify({ $schema: schemaRef, configVersion: 1 }), 'utf8')
      assert.throws(() => loadDataSpecConfig(dir), /\$schema 必须是非空字符串/)
    }

    await writeFile(
      configPath,
      JSON.stringify({ $schema: ` ${DATASPEC_CONFIG_SCHEMA_REF} `, configVersion: 1 }),
      'utf8'
    )
    assert.equal(loadDataSpecConfig(dir).schemaRef, ` ${DATASPEC_CONFIG_SCHEMA_REF} `)

    for (const configVersion of [null, '']) {
      await writeFile(configPath, JSON.stringify({ $schema: DATASPEC_CONFIG_SCHEMA_REF, configVersion }), 'utf8')
      assert.throws(() => loadDataSpecConfig(dir), /configVersion 必须是正整数/)
    }
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('bundled config schema documents all supported fields and security boundaries', async () => {
  const toolsDir = path.dirname(fileURLToPath(import.meta.url))
  const schema = JSON.parse(await readFile(path.join(toolsDir, 'schemas', DATASPEC_CONFIG_SCHEMA_SOURCE_FILE), 'utf8'))
  const topLevelFields = [
    '$schema',
    'configVersion',
    'projectId',
    'server',
    'apiToken',
    'aiProfile',
    'taskType',
    'defaultPaths',
    'securityProfile'
  ]
  const securityFields = [
    'redactionStrictness',
    'sensitiveFieldPolicy',
    'allowedAiTools',
    'neverExportPatterns',
    'localOnlyPaths',
    'samplePolicy',
    'credentialPolicy'
  ]

  assert.equal(schema.$schema, 'https://json-schema.org/draft/2020-12/schema')
  assert.equal(schema.additionalProperties, false)
  assert.deepEqual(schema.required, ['configVersion'])
  assert.deepEqual(Object.keys(schema.properties), topLevelFields)
  assert.equal(schema.properties.$schema.const, DATASPEC_CONFIG_SCHEMA_REF)
  assert.equal(schema.properties.configVersion.const, DATASPEC_CONFIG_SCHEMA_VERSION)
  assert.equal(schema.properties.apiToken.writeOnly, true)
  const serverPattern = new RegExp(schema.properties.server.pattern)
  assert.equal(serverPattern.test('http://localhost:8090'), true)
  assert.equal(serverPattern.test('https://example.com/path@segment'), true)
  assert.equal(serverPattern.test('https://user:password@example.com'), false)
  assert.deepEqual(schema.properties.projectId.oneOf, [
    { type: 'integer', minimum: 1 },
    { type: 'string', pattern: '^[1-9][0-9]*$' }
  ])
  assert.deepEqual(Object.keys(schema.properties.securityProfile.properties), securityFields)
  assert.equal(schema.properties.securityProfile.additionalProperties, false)
  assert.equal('^x-' in schema.patternProperties, true)
  assert.equal('^x-' in schema.properties.securityProfile.patternProperties, true)
  for (const field of topLevelFields) {
    assert.equal(typeof schema.properties[field].description, 'string')
  }
  for (const field of securityFields) {
    assert.equal(typeof schema.properties.securityProfile.properties[field].description, 'string')
  }
})
