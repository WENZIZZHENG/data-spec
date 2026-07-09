import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  createTaskCard,
  renderTaskCardMarkdown,
  updateTaskCardStep
} from './dataspec-task-card.mjs'

const FIXED_NOW = '2026-07-04T08:00:00.000Z'

test('creates task cards from all workflow recipes with stable shape', () => {
  const inputsByWorkflow = {
    'create-table': { projectId: 7, businessDescription: '订单表', tableName: 'orders' },
    'review-pr-sql': { projectId: 7, repo: 'acme/app', pr: 12, GITHUB_TOKEN: 'ghp_secret_token' },
    'reverse-import-standards': {
      projectId: 7,
      databaseType: 'postgresql',
      'host/port/databaseName/schemaName': 'db.local:5432/app/public',
      'username/password': 'readonly/super_secret'
    },
    'export-min-context': { projectId: 7, scope: 'field', query: '订单' },
    'standard-evidence-review': { projectId: 7, subjectType: 'FIELD', subjectId: 42 },
    'standard-maintenance': { projectId: 7, sourceType: 'STANDARD_CANDIDATE', sourceIds: '10,11' }
  }

  for (const [workflowId, inputs] of Object.entries(inputsByWorkflow)) {
    const card = createTaskCard({
      workflowId,
      projectId: 7,
      goal: `执行 ${workflowId}`,
      inputs,
      now: FIXED_NOW
    })

    assert.equal(card.kind, 'dataspec-ai-task-card')
    assert.equal(card.schemaVersion, 1)
    assert.equal(card.workflowId, workflowId)
    assert.equal(card.projectId, 7)
    assert.equal(card.status, 'PLANNED')
    assert.equal(card.currentStep, card.steps[0].id)
    assert.ok(card.taskId.startsWith(`task-${workflowId}-`))
    assert.ok(card.steps.length > 0)
    assert.ok(card.allowedActions.includes('RUN_NEXT_COMMAND'))
    assert.ok(card.resumeCommand.includes('task-card show'))
    assert.ok(card.validationCommands.length > 0)
    assert.ok(card.stopConditions.length > 0)
    assert.equal(card.createdAt, FIXED_NOW)
    assert.equal(card.updatedAt, FIXED_NOW)
  }
})

test('blocks task card when required workflow inputs are missing', () => {
  const card = createTaskCard({
    workflowId: 'create-table',
    projectId: 7,
    goal: '创建订单表',
    inputs: { projectId: 7 },
    now: FIXED_NOW
  })

  assert.equal(card.status, 'BLOCKED')
  assert.equal(card.currentStep, null)
  assert.ok(card.stopConditions.some((item) => item.includes('businessDescription')))
  assert.ok(card.nextActions.some((item) => item.code === 'PROVIDE_REQUIRED_INPUT'))
})

test('standard evidence review task card blocks when subject id is missing', () => {
  const card = createTaskCard({
    workflowId: 'standard-evidence-review',
    projectId: 7,
    goal: '复核订单号字段证据',
    inputs: { projectId: 7, subjectType: 'FIELD' },
    now: FIXED_NOW
  })

  assert.equal(card.status, 'BLOCKED')
  assert.equal(card.currentStep, null)
  assert.ok(card.stopConditions.some((item) => item.includes('subjectId')))
  assert.ok(card.nextActions.some((item) => item.code === 'PROVIDE_REQUIRED_INPUT'))
})

test('redacts secrets from task card json and markdown', () => {
  const card = createTaskCard({
    workflowId: 'reverse-import-standards',
    projectId: 7,
    goal: '导入用户表 password="raw secret value" Authorization: Basic raw_auth_secret',
    inputs: {
      projectId: 7,
      databaseType: 'postgresql',
      'host/port/databaseName/schemaName': 'jdbc:postgresql://db.local:5432/app?password=raw_secret',
      'username/password': 'readonly/raw_secret',
      token: 'ds_plain_token',
      tokens: ['array_token_one', 'array_token_two'],
      apiKeys: ['api_key_one'],
      passwords: [{ value: 'nested password value' }],
      clientSecrets: [{ value: 'nested_client_secret' }]
    },
    now: FIXED_NOW
  })
  const text = `${JSON.stringify(card)}\n${renderTaskCardMarkdown(card)}`

  assert.doesNotMatch(text, /raw_secret|raw secret value|raw_auth_secret|ds_plain_token|array_token_one|array_token_two|api_key_one|nested password value|nested_client_secret|jdbc:postgresql:\/\/db\.local/)
  assert.match(text, /\*\*\*/)
})

test('renders task card markdown and updates step state without executing workflow', () => {
  const card = createTaskCard({
    workflowId: 'export-min-context',
    projectId: 7,
    goal: '导出订单最小上下文',
    inputs: { projectId: 7, scope: 'field', query: '订单' },
    now: FIXED_NOW
  })

  const updated = updateTaskCardStep(card, {
    stepId: card.steps[0].id,
    status: 'DONE',
    artifact: 'dataspec-ai-context.zip',
    now: '2026-07-04T08:10:00.000Z'
  })
  const markdown = renderTaskCardMarkdown(updated)

  assert.equal(updated.steps[0].status, 'DONE')
  assert.equal(updated.artifacts[0].path, 'dataspec-ai-context.zip')
  assert.equal(updated.updatedAt, '2026-07-04T08:10:00.000Z')
  assert.match(markdown, /# DataSpec AI Task Card/)
  assert.match(markdown, /导出订单最小上下文/)
  assert.match(markdown, /Validation Commands/)
  assert.match(markdown, /Stop Conditions/)
})

test('blocked step keeps task card blocked and stops next execution suggestion', () => {
  const card = createTaskCard({
    workflowId: 'export-min-context',
    projectId: 7,
    goal: '导出订单最小上下文',
    inputs: { projectId: 7, scope: 'field', query: '订单' },
    now: FIXED_NOW
  })

  const updated = updateTaskCardStep(card, {
    stepId: card.steps[0].id,
    status: 'BLOCKED',
    notes: '等待人工确认 password="raw secret value"',
    now: '2026-07-04T08:10:00.000Z'
  })
  const text = `${JSON.stringify(updated)}\n${renderTaskCardMarkdown(updated)}`

  assert.equal(updated.status, 'BLOCKED')
  assert.equal(updated.currentStep, card.steps[0].id)
  assert.equal(updated.nextActions[0].code, 'RESOLVE_BLOCKED_STEP')
  assert.doesNotMatch(updated.nextActions[0].code, /RUN_CURRENT_STEP/)
  assert.doesNotMatch(text, /raw secret value/)
})

test('missing input blocked card cannot be advanced by local step update', () => {
  const card = createTaskCard({
    workflowId: 'create-table',
    projectId: 7,
    goal: '创建订单表',
    inputs: { projectId: 7 },
    now: FIXED_NOW
  })

  const updated = updateTaskCardStep(card, {
    stepId: card.steps[0].id,
    status: 'DONE',
    now: '2026-07-04T08:10:00.000Z'
  })

  assert.equal(updated.status, 'BLOCKED')
  assert.equal(updated.currentStep, null)
  assert.equal(updated.nextActions[0].code, 'PROVIDE_REQUIRED_INPUT')
  assert.ok(updated.stopConditions.some((item) => item.includes('businessDescription')))
})
