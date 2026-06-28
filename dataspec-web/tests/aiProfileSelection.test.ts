import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiProfileStorageKey,
  readSelectedAiProfile,
  resolveSelectedAiProfile,
  saveSelectedAiProfile
} from '../src/utils/aiProfileSelection.ts'

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

test('builds project-scoped AI profile storage keys', () => {
  assert.equal(aiProfileStorageKey(7), 'dataspec.aiProfile.7')
  assert.equal(aiProfileStorageKey(null), 'dataspec.aiProfile.global')
  assert.notEqual(aiProfileStorageKey(7), aiProfileStorageKey(8))
})

test('resolves stored profile before fallback default', () => {
  assert.equal(resolveSelectedAiProfile(' sql-fix ', 'create-table'), 'sql-fix')
  assert.equal(resolveSelectedAiProfile(' ', 'create-table'), 'create-table')
  assert.equal(resolveSelectedAiProfile('', null), '')
})

test('saves, reads, and clears the selected AI profile from localStorage', () => {
  const previousWindow = (globalThis as { window?: unknown }).window
  const localStorage = new MemoryStorage()
  ;(globalThis as { window?: unknown }).window = { localStorage }

  try {
    saveSelectedAiProfile(7, 'sql-fix')
    saveSelectedAiProfile(8, 'minimal-context')

    assert.equal(readSelectedAiProfile(7), 'sql-fix')
    assert.equal(readSelectedAiProfile(8), 'minimal-context')

    saveSelectedAiProfile(7, '')
    assert.equal(readSelectedAiProfile(7), '')
  } finally {
    ;(globalThis as { window?: unknown }).window = previousWindow
  }
})
