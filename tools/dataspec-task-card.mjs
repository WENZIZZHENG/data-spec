import { createHash } from 'node:crypto'
import {
  getWorkflowRecipe,
  supportedWorkflowRecipeIds
} from './dataspec-workflows.mjs'

export const TASK_CARD_KIND = 'dataspec-ai-task-card'
export const TASK_CARD_SCHEMA_VERSION = 1
export const TASK_CARD_STATUSES = new Set(['PLANNED', 'IN_PROGRESS', 'BLOCKED', 'READY_FOR_REVIEW', 'DONE'])
export const TASK_STEP_STATUSES = new Set(['PENDING', 'IN_PROGRESS', 'DONE', 'SKIPPED', 'BLOCKED'])

/**
 * 从 workflow recipe 生成本地 AI 任务卡。
 *
 * 任务卡是 AI 会话恢复和交接用的计划快照，不会执行 workflow、连接远端服务或写入 DataSpec。
 * 所有用户输入都会在进入 JSON/Markdown 前做脱敏，保证本地任务卡可安全提交到业务仓库。
 */
export function createTaskCard({
  workflowId,
  projectId,
  goal,
  inputs = {},
  now = new Date().toISOString(),
  outputPath
}) {
  const recipe = getWorkflowRecipe(workflowId)
  if (!recipe) {
    throw new Error(`未知 workflow recipe: ${workflowId}。支持的 recipe: ${supportedWorkflowRecipeIds().join(', ')}`)
  }
  const sanitizedInputs = sanitizeTaskCardValue({ ...inputs, projectId: projectId ?? inputs.projectId })
  const missingInputs = requiredMissingInputs(recipe, sanitizedInputs, projectId)
  const blocked = missingInputs.length > 0
  const steps = buildTaskCardSteps(recipe)
  const currentStep = blocked ? null : steps[0]?.id ?? null
  const taskId = buildTaskId(workflowId, projectId, goal, now)
  const resumeCommand = outputPath
    ? `node tools/dataspec-cli.mjs task-card show --file ${sanitizeSecretText(outputPath)} --format markdown`
    : `node tools/dataspec-cli.mjs task-card show --file .dataspec/task-card-${workflowId}.json --format markdown`

  return {
    kind: TASK_CARD_KIND,
    schemaVersion: TASK_CARD_SCHEMA_VERSION,
    taskId,
    workflowId,
    projectId: projectId ?? sanitizedInputs.projectId ?? null,
    goal: sanitizeSecretText(goal || recipe.goal),
    inputs: sanitizedInputs,
    status: blocked ? 'BLOCKED' : 'PLANNED',
    currentStep,
    steps,
    allowedActions: blocked
      ? ['PROVIDE_INPUTS', 'RECREATE_TASK_CARD', 'RENDER_MARKDOWN']
      : ['RUN_NEXT_COMMAND', 'MARK_STEP_DONE', 'MARK_BLOCKED', 'RENDER_MARKDOWN'],
    artifacts: [],
    resumeCommand,
    validationCommands: buildValidationCommands(recipe),
    stopConditions: buildStopConditions(recipe, missingInputs),
    risks: [
      '任务卡只描述执行计划和恢复点，不会自动执行命令或写入 DataSpec。',
      '继续执行前需要逐条运行 recommended/next command，并保存验证结果。'
    ],
    nextActions: blocked
      ? missingInputs.map((name) => ({
          code: 'PROVIDE_REQUIRED_INPUT',
          severity: 'error',
          message: `补充必填输入 ${name} 后重新生成任务卡。`,
          retryable: true
        }))
      : [{
          code: 'RUN_CURRENT_STEP',
          severity: 'info',
          message: `运行当前步骤: ${steps[0]?.title ?? '无步骤'}`,
          command: steps[0]?.command ?? null,
          retryable: true
        }],
    createdAt: now,
    updatedAt: now
  }
}

/**
 * 更新任务卡中的单个步骤状态，并返回新的任务卡对象。
 *
 * 该函数只修改内存中的任务卡数据，不执行步骤命令；CLI 写文件时会把返回值持久化到用户指定的任务卡文件。
 */
export function updateTaskCardStep(card, {
  stepId,
  status,
  artifact,
  notes,
  resumeCommand,
  now = new Date().toISOString()
}) {
  validateTaskCard(card)
  if (!TASK_STEP_STATUSES.has(status)) {
    throw new Error(`无效 task card step status: ${status}`)
  }
  const steps = card.steps.map((step) => step.id === stepId
    ? { ...step, status, notes: notes ? sanitizeSecretText(notes) : step.notes, updatedAt: now }
    : step)
  if (!steps.some((step) => step.id === stepId)) {
    throw new Error(`未知 task card step: ${stepId}`)
  }
  const artifacts = [...(card.artifacts ?? [])]
  if (artifact) {
    artifacts.push({
      stepId,
      path: sanitizeSecretText(artifact),
      createdAt: now
    })
  }
  const inputBlockedActions = (card.nextActions ?? []).filter((action) => action.code === 'PROVIDE_REQUIRED_INPUT')
  const inputBlocked = card.status === 'BLOCKED' && inputBlockedActions.length > 0
  const blockedStep = steps.find((step) => step.status === 'BLOCKED')
  const nextPending = inputBlocked ? null : blockedStep ?? steps.find((step) => step.status === 'PENDING' || step.status === 'IN_PROGRESS')
  const nextActions = inputBlocked
    ? inputBlockedActions
    : blockedStep
    ? [{
        code: 'RESOLVE_BLOCKED_STEP',
        severity: 'error',
        message: `先解除阻塞步骤: ${blockedStep.title}`,
        command: null,
        retryable: true
      }]
    : nextPending
    ? [{
        code: 'RUN_CURRENT_STEP',
        severity: 'info',
        message: `运行当前步骤: ${nextPending.title}`,
        command: nextPending.command,
        retryable: true
      }]
    : [{
        code: 'READY_FOR_REVIEW',
        severity: 'info',
        message: '所有步骤已完成或跳过，请运行验证命令并交付任务卡。',
        retryable: false
      }]

  return {
    ...card,
    status: inputBlocked ? 'BLOCKED' : computeCardStatus(steps),
    currentStep: inputBlocked ? null : nextPending?.id ?? null,
    steps,
    artifacts,
    resumeCommand: resumeCommand ? sanitizeSecretText(resumeCommand) : card.resumeCommand,
    nextActions,
    updatedAt: now
  }
}

/**
 * 将任务卡渲染成可复制的 Markdown 摘要。
 *
 * Markdown 是人读交接产物，JSON 仍是任务卡源数据；渲染阶段会再次脱敏，避免 notes/artifact 等更新字段泄露敏感信息。
 */
export function renderTaskCardMarkdown(card) {
  validateTaskCard(card)
  const current = card.steps.find((step) => step.id === card.currentStep)
  return [
    '# DataSpec AI Task Card',
    '',
    `- Task: ${card.taskId}`,
    `- Workflow: ${card.workflowId}`,
    `- Project: ${card.projectId ?? '-'}`,
    `- Status: ${card.status}`,
    `- Goal: ${sanitizeSecretText(card.goal)}`,
    `- Current Step: ${current ? `${current.id} ${current.title}` : '-'}`,
    '',
    '## Next Command',
    current?.command ? `\`${sanitizeSecretText(current.command)}\`` : '-',
    '',
    '## Steps',
    ...(card.steps ?? []).map((step) => `- [${step.status}] ${step.id} ${step.title}: ${sanitizeSecretText(step.command ?? step.purpose ?? '')}`),
    '',
    '## Validation Commands',
    ...listOrDash(card.validationCommands).map((command) => command === '-' ? '-' : `- \`${sanitizeSecretText(command)}\``),
    '',
    '## Artifacts',
    ...listOrDash((card.artifacts ?? []).map((artifact) => artifact.path ?? artifact.description)).map((artifact) => `- ${sanitizeSecretText(artifact)}`),
    '',
    '## Risks',
    ...listOrDash(card.risks).map((risk) => `- ${sanitizeSecretText(risk)}`),
    '',
    '## Stop Conditions',
    ...listOrDash(card.stopConditions).map((condition) => `- ${sanitizeSecretText(condition)}`),
    '',
    '## Resume',
    `\`${sanitizeSecretText(card.resumeCommand ?? '-')}\``,
    ''
  ].join('\n')
}

/**
 * 校验输入对象是否满足 DataSpec task card 的最小协议形状。
 *
 * 这里刻意只做跨端稳定字段校验，不绑定前端展示细节；更严格的业务校验由创建/更新流程负责。
 */
export function validateTaskCard(card) {
  if (!card || typeof card !== 'object' || card.kind !== TASK_CARD_KIND) {
    throw new Error('无效 DataSpec task card')
  }
  if (!TASK_CARD_STATUSES.has(card.status)) {
    throw new Error(`无效 task card status: ${card.status}`)
  }
  if (!Array.isArray(card.steps)) {
    throw new Error('task card steps 必须是数组')
  }
  return true
}

function buildTaskCardSteps(recipe) {
  const prechecks = (recipe.prechecks ?? []).map((check, index) => ({
    id: `precheck-${index + 1}`,
    type: 'precheck',
    title: check.title,
    command: sanitizeSecretText(check.command),
    purpose: sanitizeSecretText(check.expected),
    status: 'PENDING'
  }))
  const steps = (recipe.steps ?? []).map((step) => ({
    id: `step-${step.order}`,
    type: 'step',
    title: step.title,
    command: sanitizeSecretText(step.command),
    purpose: sanitizeSecretText(step.purpose),
    expectedOutput: sanitizeSecretText(step.output),
    status: 'PENDING'
  }))
  return [...prechecks, ...steps]
}

function buildValidationCommands(recipe) {
  const commands = (recipe.prechecks ?? []).map((check) => check.command)
  const finalLintStep = (recipe.steps ?? []).find((step) => /lint|doctor|check/i.test(step.command ?? ''))
  if (finalLintStep) {
    commands.push(finalLintStep.command)
  }
  return [...new Set(commands.map((command) => sanitizeSecretText(command)))]
}

function buildStopConditions(recipe, missingInputs) {
  const result = [
    '不要自动执行任务卡中的命令；每一步都需要显式运行和记录结果。',
    '如果命令需要 token/password/JDBC URL，不要把明文写入任务卡或提交文件。'
  ]
  for (const name of missingInputs) {
    result.push(`缺少必填输入 ${name}，先补充后再继续。`)
  }
  for (const item of recipe.failureHandling ?? []) {
    result.push(`${item.condition}: ${item.nextAction}`)
  }
  return result.map((item) => sanitizeSecretText(item))
}

function requiredMissingInputs(recipe, inputs, projectId) {
  return (recipe.requiredInputs ?? [])
    .filter((input) => input.required)
    .map((input) => input.name)
    .filter((name) => {
      if (name === 'projectId' && (projectId || inputs.projectId)) {
        return false
      }
      const value = inputs[name]
      return value === undefined || value === null || String(value).trim() === ''
    })
}

function computeCardStatus(steps) {
  if (steps.some((step) => step.status === 'BLOCKED')) {
    return 'BLOCKED'
  }
  if (steps.every((step) => step.status === 'DONE' || step.status === 'SKIPPED')) {
    return 'READY_FOR_REVIEW'
  }
  if (steps.some((step) => step.status === 'DONE' || step.status === 'IN_PROGRESS')) {
    return 'IN_PROGRESS'
  }
  return 'PLANNED'
}

function buildTaskId(workflowId, projectId, goal, now) {
  const digest = createHash('sha256')
    .update(`${workflowId}:${projectId ?? ''}:${goal ?? ''}:${now}`)
    .digest('hex')
    .slice(0, 10)
  return `task-${workflowId}-${digest}`
}

function listOrDash(items) {
  return items && items.length > 0 ? items : ['-']
}

function sanitizeTaskCardValue(value, key = '') {
  if (isSensitiveSecretKey(key) && value !== undefined && value !== null && value !== '') {
    return '***'
  }
  if (typeof value === 'string') {
    return sanitizeSecretText(value)
  }
  if (Array.isArray(value)) {
    return value.map((item) => sanitizeTaskCardValue(item, key))
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([itemKey, itemValue]) => [
      itemKey,
      sanitizeTaskCardValue(itemValue, itemKey)
    ]))
  }
  return value
}

function sanitizeSecretText(value) {
  if (value === undefined || value === null) {
    return value
  }
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/]*@/gi, '$1')
    .replace(/jdbc:[^\s"'<>]+/gi, 'jdbc:***')
    .replace(/(authorization\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\r\n,;]+)/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1***')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(secretAssignmentPattern(), (match, prefix, doubleQuoted, singleQuoted) => {
      if (doubleQuoted !== undefined) {
        return `${prefix}"***"`
      }
      if (singleQuoted !== undefined) {
        return `${prefix}'***'`
      }
      return `${prefix}***`
    })
}

function secretAssignmentPattern() {
  return /((?:"|')?\b(?:passwords?|passwds?|pwds?|tokens?|api[_-]?tokens?|dataspec[_-]?tokens?|api[_-]?keys?|secrets?|client[_-]?secrets?|access[_-]?tokens?|refresh[_-]?tokens?|plain[_-]?tokens?|token[_-]?hash(?:es)?|jdbc[_-]?urls?|connection[_-]?strings?)\b(?:"|')?\s*[:=]\s*)(?:"([^"]*)"|'([^']*)'|[^\s"',;}&]+)/gi
}

function isSensitiveSecretKey(key) {
  const normalized = String(key ?? '').replace(/[^A-Za-z0-9]/g, '').toLowerCase()
  return [
    'password',
    'passwords',
    'passwd',
    'passwds',
    'pwd',
    'pwds',
    'token',
    'tokens',
    'githubtoken',
    'githubtokens',
    'apitoken',
    'apitokens',
    'dataspectoken',
    'dataspectokens',
    'apikey',
    'apikeys',
    'authorization',
    'secret',
    'secrets',
    'clientsecret',
    'clientsecrets',
    'accesstoken',
    'accesstokens',
    'refreshtoken',
    'refreshtokens',
    'plaintoken',
    'plaintokens',
    'tokenhash',
    'tokenhashes',
    'jdbcurl',
    'jdbcurls',
    'connectionstring',
    'connectionstrings',
    'usernamepassword'
  ].includes(normalized)
}
