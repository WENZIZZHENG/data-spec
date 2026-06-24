import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  fieldLibraryQueryForImportResult,
  loadReverseImportMemory,
  reverseImportMemoryKey,
  saveReverseImportMemory,
  sanitizeReverseImportMemory
} from '../src/utils/reverseImportMemory.ts'

class MemoryStorage {
  private readonly data = new Map<string, string>()

  getItem(key: string) {
    return this.data.get(key) ?? null
  }

  setItem(key: string, value: string) {
    this.data.set(key, value)
  }

  removeItem(key: string) {
    this.data.delete(key)
  }
}

test('builds project-scoped reverse import memory keys', () => {
  assert.equal(reverseImportMemoryKey(7), 'dataspec:reverse-import:memory:7')
  assert.notEqual(reverseImportMemoryKey(7), reverseImportMemoryKey(8))
})

test('sanitizes reverse import memory and excludes credential-like fields', () => {
  const memory = sanitizeReverseImportMemory({
    activeMode: 'database',
    tableSearch: 'order',
    compareStatusFilter: 'NEW',
    database: {
      databaseType: 'mysql',
      host: '127.0.0.1',
      port: 3306,
      databaseName: 'shop',
      schemaName: 'shop',
      username: 'reader',
      password: 'secret',
      token: 'api-token',
      jdbcUrl: 'jdbc:mysql://127.0.0.1/shop',
      connectionString: 'mysql://reader:secret@127.0.0.1/shop',
      tableNames: ['orders', 'users']
    }
  })

  assert.deepEqual(memory, {
    activeMode: 'database',
    tableSearch: 'order',
    compareStatusFilter: 'NEW',
    database: {
      databaseType: 'mysql',
      host: '127.0.0.1',
      port: 3306,
      databaseName: 'shop',
      schemaName: 'shop',
      username: 'reader',
      tableNames: ['orders', 'users']
    }
  })

  const serialized = JSON.stringify(memory)
  assert.equal(serialized.includes('secret'), false)
  assert.equal(serialized.includes('token'), false)
  assert.equal(serialized.includes('jdbc:mysql'), false)
})

test('saves and loads memory per project and tolerates damaged data', () => {
  const storage = new MemoryStorage()
  saveReverseImportMemory(storage, 1, {
    activeMode: 'database',
    tableSearch: 'user',
    database: {
      host: 'localhost',
      port: 5432,
      databaseName: 'app',
      schemaName: 'public',
      username: 'postgres',
      password: 'do-not-store',
      tableNames: ['users']
    }
  })
  saveReverseImportMemory(storage, 2, {
    activeMode: 'database',
    tableSearch: 'audit',
    database: {
      tableNames: ['event_log']
    }
  })

  assert.deepEqual(loadReverseImportMemory(storage, 1)?.database?.tableNames, ['users'])
  assert.deepEqual(loadReverseImportMemory(storage, 2)?.database?.tableNames, ['event_log'])
  assert.equal(JSON.stringify(loadReverseImportMemory(storage, 1)).includes('do-not-store'), false)

  storage.setItem(reverseImportMemoryKey(3), '{bad json')
  assert.equal(loadReverseImportMemory(storage, 3), null)
})

test('builds field library keyword query from first imported field', () => {
  assert.deepEqual(fieldLibraryQueryForImportResult(['mobile_no', 'user_id']), { keyword: 'mobile_no' })
  assert.deepEqual(fieldLibraryQueryForImportResult([]), {})
  assert.deepEqual(fieldLibraryQueryForImportResult(['  ']), {})
})
