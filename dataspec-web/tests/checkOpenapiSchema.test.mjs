import assert from 'node:assert/strict'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import {
  buildDriftMessage,
  checkOpenapiSchema,
  normalizeSchemaText,
  parseCheckArgs
} from '../scripts/check-openapi-schema.mjs'

test('parseCheckArgs resolves default source, env source, and explicit source', () => {
  assert.equal(parseCheckArgs([], {}).source, 'http://localhost:8090/api-docs')
  assert.equal(parseCheckArgs([], { DATASPEC_API_DOCS_URL: '' }).source, 'http://localhost:8090/api-docs')
  assert.equal(
    parseCheckArgs([], { DATASPEC_API_DOCS_URL: 'openapi.json' }).source,
    'openapi.json'
  )
  assert.equal(
    parseCheckArgs(['--source', 'custom.json'], { DATASPEC_API_DOCS_URL: 'openapi.json' }).source,
    'custom.json'
  )
  assert.equal(parseCheckArgs(['--schema', 'custom-schema.ts'], {}).schemaPath, 'custom-schema.ts')
  assert.throws(() => parseCheckArgs(['--nope'], {}), /未知参数/)
})

test('normalizeSchemaText ignores line ending differences', () => {
  assert.equal(normalizeSchemaText('a\r\nb\r\n'), 'a\nb\n')
})

test('buildDriftMessage explains how to regenerate schema', () => {
  const message = buildDriftMessage({
    schemaPath: 'src/api/schema.ts',
    source: 'http://localhost:8090/api-docs'
  })

  assert.match(message, /OpenAPI schema.ts 已过期/)
  assert.match(message, /pnpm gen:api/)
  assert.match(message, /src\/api\/schema\.ts/)
})

test('checkOpenapiSchema returns ok when generated schema matches current schema', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-openapi-check-'))
  try {
    const schemaPath = path.join(dir, 'schema.ts')
    await writeFile(schemaPath, 'export type A = string\r\n', 'utf8')

    const result = await checkOpenapiSchema({
      argv: ['--source', 'openapi.json', '--schema', schemaPath],
      env: {},
      cwd: dir,
      runGenerator: async (_source, outputPath) => {
        await writeFile(outputPath, 'export type A = string\n', 'utf8')
      }
    })

    assert.equal(result.ok, true)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})

test('checkOpenapiSchema reports drift without modifying current schema', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'dataspec-openapi-check-'))
  try {
    const schemaPath = path.join(dir, 'schema.ts')
    await writeFile(schemaPath, 'export type A = string\n', 'utf8')

    const result = await checkOpenapiSchema({
      argv: ['--source', 'openapi.json', '--schema', schemaPath],
      env: {},
      cwd: dir,
      runGenerator: async (_source, outputPath) => {
        await writeFile(outputPath, 'export type A = number\n', 'utf8')
      }
    })

    assert.equal(result.ok, false)
    assert.match(result.message, /pnpm gen:api/)
    assert.equal(await readFile(schemaPath, 'utf8'), 'export type A = string\n')
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
