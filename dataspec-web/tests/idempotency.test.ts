import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createClientIdempotencyKey, withIdempotencyKey } from '../src/api/idempotency.ts'

test('builds client idempotency keys and header config', () => {
  const first = createClientIdempotencyKey('reverse-import:database-import')
  const second = createClientIdempotencyKey('reverse-import:database-import')

  assert.match(first, /^reverse-import:database-import:/)
  assert.match(second, /^reverse-import:database-import:/)
  assert.notEqual(first, second)
  assert.deepEqual(withIdempotencyKey(first), {
    headers: { 'Idempotency-Key': first }
  })
  assert.equal(withIdempotencyKey(''), undefined)
})
