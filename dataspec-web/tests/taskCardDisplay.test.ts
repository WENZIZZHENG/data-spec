import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildTaskCardMarkdown,
  buildTaskCardSummary
} from '../src/utils/taskCardDisplay.ts'

const taskCard = {
  kind: 'dataspec-ai-task-card',
  schemaVersion: 1,
  taskId: 'task-create-table-1',
  workflowId: 'create-table',
  projectId: 7,
  goal: '创建订单表',
  status: 'IN_PROGRESS',
  currentStep: 'step-1',
  steps: [
    { id: 'step-1', title: '导出 Context', status: 'IN_PROGRESS', command: 'dataspec export-context --project 7' },
    { id: 'step-2', title: '校验 SQL', status: 'PENDING', command: 'dataspec lint order.sql --project 7' }
  ],
  validationCommands: ['dataspec lint order.sql --project 7'],
  artifacts: [{ path: 'dataspec-ai-context.zip' }],
  risks: ['不要自动执行命令'],
  stopConditions: ['缺少 token=password_raw 时停止', '密码 password="raw secret value" 时停止', 'Authorization: Basic raw_auth_secret'],
  resumeCommand: 'dataspec task-card show --file task-card.json'
}

test('builds task card summary for display', () => {
  const summary = buildTaskCardSummary(taskCard)

  assert.equal(summary.valid, true)
  assert.equal(summary.goal, '创建订单表')
  assert.equal(summary.status, 'IN_PROGRESS')
  assert.equal(summary.currentStepTitle, '导出 Context')
  assert.equal(summary.nextCommand, 'dataspec export-context --project 7')
  assert.deepEqual(summary.validationCommands, ['dataspec lint order.sql --project 7'])
  assert.deepEqual(summary.artifacts, ['dataspec-ai-context.zip'])
})

test('builds non-sensitive task card markdown', () => {
  const markdown = buildTaskCardMarkdown(taskCard)

  assert.match(markdown, /DataSpec AI Task Card/)
  assert.match(markdown, /创建订单表/)
  assert.match(markdown, /Validation Commands/)
  assert.match(markdown, /Stop Conditions/)
  assert.doesNotMatch(markdown, /password_raw|raw secret value|raw_auth_secret/)
})

test('returns invalid state for missing task card', () => {
  const summary = buildTaskCardSummary(null)

  assert.equal(summary.valid, false)
  assert.equal(summary.goal, '暂无任务卡')
  assert.equal(summary.nextCommand, '-')
})

test('returns invalid state for malformed task card object', () => {
  const summary = buildTaskCardSummary({
    kind: 'dataspec-ai-task-card',
    steps: 'bad'
  })
  const markdown = buildTaskCardMarkdown({
    kind: 'dataspec-ai-task-card',
    validationCommands: 'bad'
  })

  assert.equal(summary.valid, false)
  assert.equal(summary.goal, '暂无任务卡')
  assert.match(markdown, /暂无任务卡/)
})
