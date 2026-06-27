import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  normalizeDatabaseConnectionPresetPayload,
  presetConnectionSummary,
  presetOptionLabel
} from '../src/utils/databaseConnectionPreset.ts'

test('normalizes preset payload and excludes credential fields', () => {
  const payload = normalizeDatabaseConnectionPresetPayload({
    projectId: 1,
    name: ' 本地 PG ',
    databaseType: 'postgresql',
    host: ' localhost ',
    port: 5432,
    databaseName: ' dataspec_demo ',
    schemaName: ' public ',
    username: 'reader',
    password: 'secret',
    token: 'api-token',
    jdbcUrl: 'jdbc:postgresql://localhost/dataspec_demo',
    connectionString: 'postgres://reader:secret@localhost/dataspec_demo',
    tableNames: [' users ', 'orders', 'users', '', 123]
  })

  assert.deepEqual(payload, {
    projectId: 1,
    name: '本地 PG',
    databaseType: 'postgresql',
    host: 'localhost',
    port: 5432,
    databaseName: 'dataspec_demo',
    schemaName: 'public',
    tableNames: ['users', 'orders']
  })

  const serialized = JSON.stringify(payload)
  assert.equal(serialized.includes('reader'), false)
  assert.equal(serialized.includes('secret'), false)
  assert.equal(serialized.includes('token'), false)
  assert.equal(serialized.includes('jdbc:'), false)
})

test('formats preset option label and summary', () => {
  assert.equal(
    presetOptionLabel({
      name: '生产只读',
      databaseType: 'postgresql',
      host: '10.0.0.8',
      port: 5432,
      databaseName: 'shop',
      schemaName: 'public',
      tableNames: ['orders']
    }),
    '生产只读'
  )
  assert.equal(
    presetConnectionSummary({
      databaseType: 'mysql',
      host: '127.0.0.1',
      port: 3306,
      databaseName: 'shop',
      tableNames: ['orders', 'users']
    }),
    'mysql · 127.0.0.1:3306 · shop，2 张表'
  )
})
