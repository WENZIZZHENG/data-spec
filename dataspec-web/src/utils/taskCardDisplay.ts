/**
 * 任务卡步骤在前端展示层使用的最小字段。
 *
 * 前端只消费任务卡协议中的只读摘要字段，不负责执行步骤命令。
 */
export interface TaskCardStep {
  /** 稳定步骤 ID，对应任务卡 currentStep 和更新命令中的 step 参数。 */
  id?: string
  /** 面向用户和 AI 的步骤标题。 */
  title?: string
  /** 步骤状态，来自任务卡协议中的受限状态集合。 */
  status?: string
  /** 下一步建议命令，仅用于展示或复制，不在前端自动执行。 */
  command?: string
}

/**
 * 任务卡产物在前端展示层使用的最小字段。
 */
export interface TaskCardArtifact {
  /** 本地或仓库内产物路径；展示前会再次脱敏。 */
  path?: string
  /** 无路径产物的简短说明。 */
  description?: string
}

/**
 * DataSpec AI 任务卡的前端只读协议子集。
 *
 * 这里保持宽松 optional 是为了兼容 CLI/MCP 后续添加字段，同时 invalid state 会拦截缺失或错误对象。
 */
export interface DataSpecTaskCard {
  /** 协议标识，当前必须是 dataspec-ai-task-card。 */
  kind?: string
  /** 单张任务卡的稳定 ID。 */
  taskId?: string
  /** 来源 workflow recipe ID。 */
  workflowId?: string
  /** 关联 DataSpec 项目 ID；本地任务卡允许为空。 */
  projectId?: number | null
  /** 本次 AI/DataSpec 工作目标。 */
  goal?: string
  /** 任务卡整体状态。 */
  status?: string
  /** 当前步骤 ID；阻塞或完成时可为空。 */
  currentStep?: string | null
  /** 从 workflow recipe 派生的步骤列表。 */
  steps?: TaskCardStep[]
  /** 建议验证命令，只展示和复制，不自动执行。 */
  validationCommands?: string[]
  /** 已产生产物列表。 */
  artifacts?: TaskCardArtifact[]
  /** 执行风险提示。 */
  risks?: string[]
  /** 需要暂停或人工确认的条件。 */
  stopConditions?: string[]
  /** 恢复查看任务卡的建议命令。 */
  resumeCommand?: string
}

/**
 * 前端页面直接渲染的任务卡摘要。
 *
 * 所有字段都已转换为非敏感文本，避免页面或复制内容暴露 token/password/JDBC URL。
 */
export interface TaskCardSummary {
  /** 是否为可展示的 DataSpec 任务卡。 */
  valid: boolean
  /** 任务卡 ID 或占位符。 */
  taskId: string
  /** workflow recipe ID 或占位符。 */
  workflowId: string
  /** 项目 ID 文本。 */
  projectId: string
  /** 任务目标。 */
  goal: string
  /** 任务状态。 */
  status: string
  /** 当前步骤标题。 */
  currentStepTitle: string
  /** 下一步建议命令。 */
  nextCommand: string
  /** 验证命令列表。 */
  validationCommands: string[]
  /** 产物列表。 */
  artifacts: string[]
  /** 风险提示列表。 */
  risks: string[]
  /** 停止条件列表。 */
  stopConditions: string[]
  /** 恢复命令。 */
  resumeCommand: string
}

/**
 * 把未知输入转换为前端可展示的任务卡摘要。
 *
 * 输入无效时返回非敏感 invalid state，页面不直接展示原始 JSON 或异常信息。
 */
export function buildTaskCardSummary(value: unknown): TaskCardSummary {
  if (!isTaskCard(value)) {
    return invalidTaskCardSummary()
  }
  const steps = value.steps ?? []
  const currentStep = steps.find((step) => step.id === value.currentStep)
  return {
    valid: true,
    taskId: displayText(value.taskId),
    workflowId: displayText(value.workflowId),
    projectId: value.projectId === null || value.projectId === undefined ? '-' : String(value.projectId),
    goal: sanitizeTaskCardText(value.goal ?? '未命名任务'),
    status: displayText(value.status),
    currentStepTitle: sanitizeTaskCardText(currentStep?.title ?? '-'),
    nextCommand: sanitizeTaskCardText(currentStep?.command ?? '-'),
    validationCommands: sanitizeList(value.validationCommands),
    artifacts: sanitizeList(value.artifacts?.map((artifact) => artifact.path ?? artifact.description ?? '-')),
    risks: sanitizeList(value.risks),
    stopConditions: sanitizeList(value.stopConditions),
    resumeCommand: sanitizeTaskCardText(value.resumeCommand ?? '-')
  }
}

/**
 * 生成可复制的任务卡 Markdown。
 *
 * Markdown 内容与前端摘要字段一致，适合粘贴到 AI 交接记录或 TODO 验证证据中。
 */
export function buildTaskCardMarkdown(value: unknown): string {
  const summary = buildTaskCardSummary(value)
  if (!summary.valid) {
    return '# DataSpec AI Task Card\n\n暂无任务卡\n'
  }
  return [
    '# DataSpec AI Task Card',
    '',
    `- Task: ${summary.taskId}`,
    `- Workflow: ${summary.workflowId}`,
    `- Project: ${summary.projectId}`,
    `- Status: ${summary.status}`,
    `- Goal: ${summary.goal}`,
    `- Current Step: ${summary.currentStepTitle}`,
    '',
    '## Next Command',
    `\`${summary.nextCommand}\``,
    '',
    '## Validation Commands',
    ...markdownList(summary.validationCommands, true),
    '',
    '## Artifacts',
    ...markdownList(summary.artifacts),
    '',
    '## Risks',
    ...markdownList(summary.risks),
    '',
    '## Stop Conditions',
    ...markdownList(summary.stopConditions),
    '',
    '## Resume',
    `\`${summary.resumeCommand}\``,
    ''
  ].join('\n')
}

function isTaskCard(value: unknown): value is DataSpecTaskCard {
  if (!value || typeof value !== 'object' || (value as DataSpecTaskCard).kind !== 'dataspec-ai-task-card') {
    return false
  }
  const card = value as DataSpecTaskCard
  return isOptionalArray(card.steps) &&
    isOptionalArray(card.validationCommands) &&
    isOptionalArray(card.artifacts) &&
    isOptionalArray(card.risks) &&
    isOptionalArray(card.stopConditions)
}

function invalidTaskCardSummary(): TaskCardSummary {
  return {
    valid: false,
    taskId: '-',
    workflowId: '-',
    projectId: '-',
    goal: '暂无任务卡',
    status: 'INVALID',
    currentStepTitle: '-',
    nextCommand: '-',
    validationCommands: [],
    artifacts: [],
    risks: [],
    stopConditions: [],
    resumeCommand: '-'
  }
}

function markdownList(items: string[], code = false): string[] {
  if (items.length === 0) {
    return ['- -']
  }
  return items.map((item) => code ? `- \`${item}\`` : `- ${item}`)
}

function sanitizeList(values?: Array<string | undefined>): string[] {
  return (values ?? [])
    .map((item) => sanitizeTaskCardText(item ?? ''))
    .filter(Boolean)
}

function displayText(value?: string): string {
  return sanitizeTaskCardText(value ?? '-')
}

function sanitizeTaskCardText(value: string): string {
  return String(value)
    .replace(/\b(https?:\/\/)[^\s/]*@/gi, '$1')
    .replace(/jdbc:[^\s"'<>]+/gi, 'jdbc:***')
    .replace(/(authorization\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\r\n,;]+)/gi, '$1***')
    .replace(/(authorization\s*[:=]\s*bearer\s+)[^\s,;]+/gi, '$1***')
    .replace(/\b(bearer\s+)[A-Za-z0-9._~+/-]+=*/gi, '$1***')
    .replace(secretAssignmentPattern(), (_match, prefix, doubleQuoted, singleQuoted) => {
      if (doubleQuoted !== undefined) {
        return `${prefix}"***"`
      }
      if (singleQuoted !== undefined) {
        return `${prefix}'***'`
      }
      return `${prefix}***`
    })
}

function secretAssignmentPattern(): RegExp {
  return /((?:"|')?\b(?:passwords?|passwds?|pwds?|tokens?|api[_-]?tokens?|dataspec[_-]?tokens?|api[_-]?keys?|secrets?|client[_-]?secrets?|access[_-]?tokens?|refresh[_-]?tokens?|plain[_-]?tokens?|token[_-]?hash(?:es)?|jdbc[_-]?urls?|connection[_-]?strings?)\b(?:"|')?\s*[:=]\s*)(?:"([^"]*)"|'([^']*)'|[^\s"',;}&]+)/gi
}

function isOptionalArray(value: unknown): boolean {
  return value === undefined || Array.isArray(value)
}
