import assert from 'node:assert/strict'
import { test } from 'node:test'
import { canSubmitSnapshotForm, formatSnapshotLabel } from '../src/utils/standardSnapshotDisplay.ts'

test('formats standard snapshot label', () => {
  assert.equal(formatSnapshotLabel(null), '未创建快照')
  assert.equal(formatSnapshotLabel({ specVersion: 'unversioned', versioned: false }), '未创建快照')
  assert.equal(
    formatSnapshotLabel({ specVersion: 'v2026.06.24', specHash: '1234567890abcdef', versioned: true }),
    'v2026.06.24 (12345678)'
  )
})

test('validates standard snapshot create form', () => {
  assert.equal(canSubmitSnapshotForm({ version: '' }), false)
  assert.equal(canSubmitSnapshotForm({ version: '   ' }), false)
  assert.equal(canSubmitSnapshotForm({ version: 'v1' }), true)
})
