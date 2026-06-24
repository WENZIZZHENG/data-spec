import assert from 'node:assert/strict'
import { test } from 'node:test'
import { canSubmitApiTokenForm, formatTokenProjectScope } from '../src/utils/apiTokenDisplay.ts'

test('formats API token project scope', () => {
  assert.equal(formatTokenProjectScope({ allProjects: true, projectIds: [] }), '全部项目')
  assert.equal(formatTokenProjectScope({ allProjects: false, projectIds: [1, 2] }), '1, 2')
  assert.equal(formatTokenProjectScope({ allProjects: false, projectIds: [] }), '未配置')
})

test('validates API token create form before submit', () => {
  assert.equal(canSubmitApiTokenForm({ name: '', operatorName: 'alice', allProjects: true, projectIds: [] }), false)
  assert.equal(canSubmitApiTokenForm({ name: 'cli', operatorName: '', allProjects: true, projectIds: [] }), false)
  assert.equal(canSubmitApiTokenForm({ name: 'cli', operatorName: 'alice', allProjects: true, projectIds: [] }), true)
  assert.equal(canSubmitApiTokenForm({ name: 'cli', operatorName: 'alice', allProjects: false, projectIds: [] }), false)
  assert.equal(canSubmitApiTokenForm({ name: 'cli', operatorName: 'alice', allProjects: false, projectIds: [1] }), true)
})
