import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  aiFeedbackPriorityTagType,
  aiFeedbackSeverityTagType,
  buildAiFeedbackRoute,
  formatAiFeedbackTime
} from '../src/utils/aiFeedbackDisplay.ts'

test('formats ai feedback severity and priority tags', () => {
  assert.equal(aiFeedbackSeverityTagType('warning'), 'warning')
  assert.equal(aiFeedbackSeverityTagType('danger'), 'danger')
  assert.equal(aiFeedbackSeverityTagType('unknown'), 'info')
  assert.equal(aiFeedbackPriorityTagType('HIGH'), 'danger')
  assert.equal(aiFeedbackPriorityTagType('MEDIUM'), 'warning')
  assert.equal(aiFeedbackPriorityTagType('LOW'), 'info')
})

test('builds ai feedback route fallback', () => {
  assert.equal(buildAiFeedbackRoute('/fields?keyword=user_id'), '/fields?keyword=user_id')
  assert.equal(buildAiFeedbackRoute(undefined), '/ai-replay')
})

test('formats ai feedback time fallback', () => {
  assert.equal(formatAiFeedbackTime(undefined), '-')
  assert.equal(formatAiFeedbackTime('2026-06-28T10:30:00'), '2026-06-28 10:30:00')
})
