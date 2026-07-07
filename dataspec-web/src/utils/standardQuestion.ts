import type { BusinessGlossary, Field, FieldSearchItem, FieldSearchResult, RuleConfig } from '@/types'

// 中文场景词很短，至少两个连续片段重叠才视为契约禁用命中，避免只因“金额”等通用词误降级。
const USAGE_CONTRACT_HAN_BIGRAM_MATCH_THRESHOLD = 2

export type StandardQuestionConfidence = 'HIGH' | 'MEDIUM' | 'LOW'
export type StandardQuestionEvidenceType = 'field' | 'glossary' | 'rule' | 'search'
export type StandardQuestionAnswerability = 'DIRECT' | 'PARTIAL' | 'NONE'
export type StandardQuestionAnswerStatus = 'ADOPTABLE' | 'NEEDS_CONFIRMATION' | 'UNANSWERABLE'

export interface StandardQuestionInput {
  /** 用户输入的自然语言问题，只用于只读匹配和答案展示。 */
  question: string
  /** 字段检索结果，作为回答标准字段问题的主证据来源。 */
  fieldSearch: FieldSearchResult
  /** 业务术语表，补充同义词、缩写和 canonical field 证据。 */
  glossary: BusinessGlossary[]
  /** 当前项目规则配置，补充命名、敏感字段和格式约束证据。 */
  rules: RuleConfig[]
}

export interface StandardQuestionMatchedField {
  /** 标准字段 ID，用于跳转字段库详情或 evidence 定位。 */
  id?: number
  /** 标准字段名。 */
  name: string
  /** 标准字段中文显示名。 */
  displayName?: string
  /** 数据库类型摘要。 */
  dataType?: string
  /** 字段生命周期状态。 */
  status?: string
  /** 是否敏感字段。 */
  sensitive?: boolean
  /** 检索分数，越高表示命中越可靠。 */
  score?: number
  /** 检索命中原因。 */
  matchReasons: string[]
  /** 后端推荐使用说明。 */
  recommendedUse?: string
  /** 字段使用契约摘要，来自检索结果或字段自身元数据。 */
  usageContractSummary: string[]
  /** 推荐使用场景。 */
  preferredUseCases?: string
  /** 禁用或需确认场景。 */
  avoidWhen?: string
  /** Join 使用提示。 */
  joinHints?: string
  /** 默认过滤提示。 */
  defaultFilters?: string
  /** 聚合口径提示。 */
  aggregationHints?: string
  /** 替代字段或迁移指导。 */
  replacementGuidance?: string
  /** 常见误用或反例。 */
  misuseExamples?: string
  /** 格式、单位、精度等约束摘要。 */
  formatSummary?: string
  /** 生命周期或替代字段提示。 */
  lifecycleSummary?: string
  /** 替代字段 ID，用于判断废弃字段是否已有结构化替代证据。 */
  replacementFieldId?: number
  /** 替代说明，用于低置信答案解释和缺失事实判断。 */
  replacementReason?: string
  /** 字段格式类型，如 mobile、money、date。 */
  formatType?: string
  /** 字段格式校验模式或正则。 */
  formatPattern?: string
  /** 字段单位，如 CNY、cent、ms。 */
  formatUnit?: string
  /** 字段精度或小数位约束。 */
  formatPrecision?: string
  /** 时间字段时区约束。 */
  formatTimezone?: string
  /** 字段库跳转链接。 */
  detailRoute: string
}

export interface StandardQuestionEvidence {
  /** 证据来源类型。 */
  type: StandardQuestionEvidenceType
  /** 证据标题。 */
  title: string
  /** 证据摘要。 */
  description: string
  /** 可选引用路径或规则编码。 */
  ref?: string
}

export interface StandardQuestionRelatedRule {
  /** 规则编码，对应 RuleConfig.ruleCode。 */
  ruleCode?: string
  /** 规则名称。 */
  ruleName?: string
  /** 严重级别。 */
  severity?: string
  /** 是否启用。 */
  enabled?: boolean
}

export interface StandardQuestionConflict {
  /** 冲突类型，用于前端和 AI 判断是否需要人工裁决。 */
  type: 'SIMILAR_SCORE' | 'LIFECYCLE'
  /** 冲突说明。 */
  message: string
  /** 涉及冲突的标准字段名。 */
  fieldNames: string[]
  /** 冲突严重度，WARNING 表示不能直接采纳答案。 */
  severity: 'WARNING' | 'INFO'
}

export interface StandardQuestionAnswer {
  /** 可直接复制给用户或 AI 的短回答。 */
  answer: string
  /** 答案可采纳状态，AI 应优先使用该字段决定采用、追问或停止。 */
  answerStatus: StandardQuestionAnswerStatus
  /** 当前证据能否直接回答问题。 */
  answerability: StandardQuestionAnswerability
  /** 回答置信度，只表示 DataSpec 当前标准证据是否足够。 */
  confidence: StandardQuestionConfidence
  /** 解释置信度和采纳状态的主要原因。 */
  confidenceReason: string
  /** 匹配到的标准字段。 */
  matchedFields: StandardQuestionMatchedField[]
  /** 回答引用的字段、术语、规则和检索摘要证据。 */
  evidence: StandardQuestionEvidence[]
  /** 机器可读证据引用，便于 CLI/MCP 或 AI 在复制答案时保留出处。 */
  evidenceRefs: string[]
  /** 与本次问题相关的项目规则。 */
  relatedRules: StandardQuestionRelatedRule[]
  /** 缺失的证据说明，用于低置信提示和后续补标准。 */
  missingEvidence: string[]
  /** 缺失的结构化事实字段名或业务事实。 */
  missingFacts: string[]
  /** true 表示只命中草稿、候选或未正式采纳的字段。 */
  candidateOnly: boolean
  /** 标准冲突摘要，兼容 P6-188 的命名。 */
  conflictingStandards: StandardQuestionConflict[]
  /** 标准冲突摘要，提供更短的机器读取字段名。 */
  conflicts: StandardQuestionConflict[]
  /** 建议下一步动作，不会直接写入标准库。 */
  suggestedNextActions: string[]
  /** 建议用户或 AI 继续追问时使用的问题。 */
  suggestedNextQuery: string
  /** true 表示建议转入标准候选 Inbox，而不是直接采用答案。 */
  escalateToInbox: boolean
  /** 机器读取的下一步动作，与旧字段 suggestedNextActions 保持同源。 */
  nextActions: string[]
  /** 低置信度或废弃字段时需要人工补充确认的问题。 */
  unresolvedQuestions: string[]
}

export interface StandardQuestionRequestSnapshot {
  /** 本次查询的递增序号，用于识别过期异步结果。 */
  requestId: number
  /** 发起查询时选择的项目 ID。 */
  projectId: number
  /** 发起查询时的标准化问题文本。 */
  question: string
}

export interface StandardQuestionRequestGuard {
  /** 开始一次标准问答查询，并返回本次异步结果的校验快照。 */
  begin(projectId: number, question: string): StandardQuestionRequestSnapshot
  /** 判断异步结果是否仍属于当前项目与当前问题，避免旧请求覆盖新答案。 */
  isCurrent(snapshot: StandardQuestionRequestSnapshot, currentProjectId: number | null | undefined, currentQuestion: string): boolean
  /** 主动使所有未完成请求失效，通常用于项目切换或页面状态重置。 */
  invalidate(): void
}

/**
 * 基于现有字段检索、术语表和规则配置生成只读标准问答结果。
 */
export function buildStandardQuestionAnswer(input: StandardQuestionInput): StandardQuestionAnswer {
  const question = input.question.trim()
  const matchedFields = normalizeMatchedFields(input.fieldSearch.items ?? [])
  const topField = matchedFields[0]
  const glossaryEvidence = findGlossaryEvidence(question, input.glossary, topField?.id)
  const relatedRules = resolveRelatedRules(question, matchedFields, input.rules)

  const baseConfidence = resolveConfidence(topField)
  const conflicts = detectConflictingStandards(matchedFields)
  const usageContractConflict = usageContractContradictsQuestion(question, topField)
  const missingFacts = buildMissingFacts(question, topField)
  const missingEvidence = buildMissingEvidence(topField, missingFacts, conflicts, usageContractConflict)
  const candidateOnly = topField?.status === 'draft'
  const answerStatus = resolveAnswerStatus(topField, baseConfidence, missingEvidence, conflicts)
  const answerability = resolveAnswerability(answerStatus)
  const confidence = resolveAnswerConfidence(baseConfidence, answerStatus, missingEvidence, conflicts)
  const confidenceReason = buildConfidenceReason(answerStatus, confidence, topField, missingEvidence, conflicts, usageContractConflict)
  const evidence = buildEvidence(input.fieldSearch, matchedFields, glossaryEvidence, relatedRules)
  const evidenceRefs = buildEvidenceRefs(evidence)
  const unresolvedQuestions = buildUnresolvedQuestions(confidence, topField)
  const suggestedNextQuery = buildSuggestedNextQuery(question, topField, answerStatus, missingFacts, conflicts)
  const suggestedNextActions = uniqueText([
    ...input.fieldSearch.nextActions ?? [],
    ...((input.fieldSearch.items ?? []).flatMap((item) => item.nextActions ?? [])),
    ...buildAnswerabilityActions(answerStatus, missingEvidence, conflicts, topField),
    '复制答案并附带证据给 AI 或协作者',
    topField ? `打开字段库查看 ${topField.name} 的完整标准` : '进入标准候选 Inbox 补充候选字段',
    confidence === 'LOW' ? '把问题转成标准候选草案后人工确认' : ''
  ])
  const escalateToInbox = answerStatus !== 'ADOPTABLE'

  return {
    answer: buildAnswerText(confidence, question, topField, glossaryEvidence, usageContractConflict),
    answerStatus,
    answerability,
    confidence,
    confidenceReason,
    matchedFields,
    evidence,
    evidenceRefs,
    relatedRules,
    missingEvidence,
    missingFacts,
    candidateOnly,
    conflictingStandards: conflicts,
    conflicts,
    suggestedNextActions,
    suggestedNextQuery,
    escalateToInbox,
    nextActions: suggestedNextActions,
    unresolvedQuestions
  }
}

/**
 * 把标准问答结果转换为可复制 Markdown，供 AI 交接或人工沟通使用。
 */
export function buildStandardQuestionMarkdown(answer: StandardQuestionAnswer) {
  const fields = answer.matchedFields
    .map((field) => `- ${field.name}${field.displayName ? `（${field.displayName}）` : ''}：${field.lifecycleSummary || field.formatSummary || '当前标准字段'}`)
    .join('\n') || '- 无'
  const evidence = answer.evidence
    .map((item) => `- [${evidenceTypeText(item.type)}] ${item.title}：${item.description}${item.ref ? `（${item.ref}）` : ''}`)
    .join('\n') || '- 无'
  const nextActions = answer.suggestedNextActions.map((item) => `- ${item}`).join('\n') || '- 无'

  return [
    '## 答案',
    answer.answer,
    '',
    `采纳状态：${answerStatusText(answer.answerStatus)}`,
    `置信度：${confidenceText(answer.confidence)}`,
    `原因：${answer.confidenceReason}`,
    '',
    '## 匹配字段',
    fields,
    '',
    '## 证据',
    evidence,
    '',
    '## 缺失证据',
    answer.missingEvidence.map((item) => `- ${item}`).join('\n') || '- 无',
    '',
    '## 冲突',
    answer.conflicts.map((item) => `- ${item.message}`).join('\n') || '- 无',
    '',
    '## 下一步',
    nextActions
  ].join('\n')
}

/**
 * 创建标准问答请求守卫，保证页面只应用最后一次仍匹配当前项目和问题的异步结果。
 */
export function createStandardQuestionRequestGuard(): StandardQuestionRequestGuard {
  let latestRequestId = 0
  return {
    begin(projectId, question) {
      latestRequestId += 1
      return {
        requestId: latestRequestId,
        projectId,
        question: question.trim()
      }
    },
    isCurrent(snapshot, currentProjectId, currentQuestion) {
      return snapshot.requestId === latestRequestId
        && snapshot.projectId === currentProjectId
        && snapshot.question === currentQuestion.trim()
    },
    invalidate() {
      latestRequestId += 1
    }
  }
}

function normalizeMatchedFields(items: FieldSearchItem[]): StandardQuestionMatchedField[] {
  return [...items]
    .filter((item) => Boolean(item.field?.name))
    .sort((left, right) => (right.score ?? 0) - (left.score ?? 0))
    .map((item) => {
      const field = item.field as Field & { name: string }
      return {
        id: field.id,
        name: field.name,
        displayName: field.displayName,
        dataType: formatDataType(field),
        status: field.status,
        sensitive: field.sensitive,
        score: item.score,
        matchReasons: item.matchReasons ?? [],
        recommendedUse: item.recommendedUse,
        usageContractSummary: usageContractSummary(item, field),
        preferredUseCases: field.preferredUseCases,
        avoidWhen: field.avoidWhen,
        joinHints: field.joinHints,
        defaultFilters: field.defaultFilters,
        aggregationHints: field.aggregationHints,
        replacementGuidance: field.replacementGuidance,
        misuseExamples: field.misuseExamples,
        formatSummary: formatConstraintSummary(field),
        lifecycleSummary: formatLifecycleSummary(field),
        replacementFieldId: field.replacementFieldId,
        replacementReason: field.replacementReason,
        formatType: field.formatType,
        formatPattern: field.formatPattern,
        formatUnit: field.formatUnit,
        formatPrecision: field.formatPrecision,
        formatTimezone: field.formatTimezone,
        detailRoute: `/fields?keyword=${encodeURIComponent(field.name)}`
      }
    })
}

function buildAnswerText(
  confidence: StandardQuestionConfidence,
  question: string,
  topField: StandardQuestionMatchedField | undefined,
  glossaryEvidence: BusinessGlossary[],
  usageContractConflict: boolean
) {
  if (!topField) {
    return `没有找到可直接确认的标准字段。当前问题“${question}”需要进入标准候选 Inbox 或先补业务术语后再确认。`
  }
  const fieldLabel = topField.displayName ? `${topField.name}（${topField.displayName}）` : topField.name
  const glossaryText = glossaryEvidence.length > 0
    ? `术语证据：${glossaryEvidence.slice(0, 2).map((item) => item.term).filter(Boolean).join('、')}。`
    : ''
  const sensitiveText = topField.sensitive ? '这是敏感字段。' : ''
  const formatText = topField.formatSummary ? `${topField.formatSummary}。` : ''

  if (topField.status === 'deprecated') {
    return `匹配到 ${fieldLabel}，但该字段已废弃。${topField.lifecycleSummary || '请确认替代字段后再使用。'}${glossaryText}`
  }
  if (topField.status === 'disabled') {
    return `匹配到 ${fieldLabel}，但该字段已停用。请先人工确认当前标准是否应恢复或改用候选字段。${glossaryText}`
  }
  if (topField.status === 'draft') {
    return `匹配到草稿字段 ${fieldLabel}。可以作为参考，但需要人工确认后再作为正式标准使用。${glossaryText}`
  }
  if (usageContractConflict) {
    return `匹配到 ${fieldLabel}，但字段使用契约提示当前场景存在禁用场景或常见误用。请先人工确认或改用契约中的替代指导。${glossaryText}`
  }

  const confidenceHint = confidence === 'HIGH' ? '建议使用' : '优先参考'
  return `${confidenceHint} ${fieldLabel}。${topField.dataType ? `类型：${topField.dataType}。` : ''}${sensitiveText}${formatText}${topField.recommendedUse || ''}${glossaryText}`
}

function resolveConfidence(field: StandardQuestionMatchedField | undefined): StandardQuestionConfidence {
  if (!field) {
    return 'LOW'
  }
  if (field.status === 'deprecated' || field.status === 'disabled' || field.status === 'draft') {
    return 'MEDIUM'
  }
  return (field.score ?? 0) >= 75 ? 'HIGH' : 'MEDIUM'
}

function resolveAnswerConfidence(
  confidence: StandardQuestionConfidence,
  status: StandardQuestionAnswerStatus,
  missingEvidence: string[],
  conflicts: StandardQuestionConflict[]
): StandardQuestionConfidence {
  if (status === 'UNANSWERABLE') {
    return 'LOW'
  }
  if (status === 'NEEDS_CONFIRMATION' && (missingEvidence.length > 0 || conflicts.length > 0)) {
    return confidence === 'LOW' ? 'LOW' : 'MEDIUM'
  }
  return confidence
}

function resolveAnswerStatus(
  field: StandardQuestionMatchedField | undefined,
  confidence: StandardQuestionConfidence,
  missingEvidence: string[],
  conflicts: StandardQuestionConflict[]
): StandardQuestionAnswerStatus {
  if (!field) {
    return 'UNANSWERABLE'
  }
  if (confidence !== 'HIGH' || missingEvidence.length > 0 || conflicts.length > 0) {
    return 'NEEDS_CONFIRMATION'
  }
  return 'ADOPTABLE'
}

function resolveAnswerability(status: StandardQuestionAnswerStatus): StandardQuestionAnswerability {
  if (status === 'ADOPTABLE') {
    return 'DIRECT'
  }
  if (status === 'UNANSWERABLE') {
    return 'NONE'
  }
  return 'PARTIAL'
}

function buildConfidenceReason(
  status: StandardQuestionAnswerStatus,
  confidence: StandardQuestionConfidence,
  field: StandardQuestionMatchedField | undefined,
  missingEvidence: string[],
  conflicts: StandardQuestionConflict[],
  usageContractConflict: boolean
) {
  if (!field) {
    return '未命中可引用的字段标准证据。'
  }
  if (conflicts.length > 0) {
    return '存在多个分数接近的候选标准，需先裁决冲突后再采纳。'
  }
  if (field.status === 'deprecated' || field.status === 'disabled' || field.status === 'draft') {
    return `字段生命周期状态为${statusText(field.status)}，需要人工确认是否可用于当前问题。`
  }
  if (usageContractConflict) {
    return '命中字段使用契约的禁用场景或误用样例，需要人工确认后再采纳。'
  }
  if (missingEvidence.length > 0) {
    return `已命中字段，但缺少${missingEvidence.join('、')}。`
  }
  if (status === 'ADOPTABLE' && confidence === 'HIGH') {
    return '字段检索、生命周期和必要证据均满足直接采纳条件。'
  }
  return '已命中字段，但检索分数或证据完整性不足以高置信采纳。'
}

function buildMissingFacts(question: string, field: StandardQuestionMatchedField | undefined) {
  if (!field) {
    return ['标准字段名或显示名', '字段所属业务术语或候选来源']
  }
  const normalizedQuestion = normalizeMatchText(question)
  const lifecycleSummary = field.lifecycleSummary ?? ''
  const formatSummary = field.formatSummary ?? ''
  const hasReplacementEvidence = Boolean(field.replacementFieldId || field.replacementReason || /替代|改用/.test(lifecycleSummary))
  const hasUnitEvidence = Boolean(field.formatUnit || formatSummary.includes('单位'))
  const hasFormatPatternEvidence = Boolean(field.formatType || field.formatPattern || /格式：|模式：/.test(formatSummary))
  const hasPrecisionEvidence = Boolean(field.formatPrecision || formatSummary.includes('精度'))
  const hasTimezoneEvidence = Boolean(field.formatTimezone || formatSummary.includes('时区'))
  const missing: string[] = []
  if ((field.status === 'deprecated' || field.status === 'disabled') && !hasReplacementEvidence) {
    missing.push('replacementFieldId 或 replacementReason')
  }
  if (field.status === 'draft') {
    missing.push('正式标准采纳状态')
  }
  if (/单位|币种|金额口径/.test(normalizedQuestion) && !hasUnitEvidence) {
    missing.push('formatUnit')
  }
  if (/格式|正则|模式|校验/.test(normalizedQuestion) && !hasFormatPatternEvidence) {
    missing.push('formatPattern 或 formatType')
  }
  if (/精度|小数/.test(normalizedQuestion) && !hasPrecisionEvidence) {
    missing.push('formatPrecision')
  }
  if (/时区/.test(normalizedQuestion) && !hasTimezoneEvidence) {
    missing.push('formatTimezone')
  }
  return uniqueText(missing)
}

function buildMissingEvidence(
  field: StandardQuestionMatchedField | undefined,
  missingFacts: string[],
  conflicts: StandardQuestionConflict[],
  usageContractConflict: boolean
) {
  if (!field) {
    return ['字段标准证据']
  }
  const missing: string[] = []
  if (field.status === 'deprecated') {
    missing.push('可采用替代字段证据')
  }
  if (field.status === 'disabled') {
    missing.push('停用字段恢复或替代方案证据')
  }
  if (field.status === 'draft') {
    missing.push('已采纳标准证据')
  }
  if (missingFacts.includes('formatUnit')) {
    missing.push('单位(formatUnit)证据')
  }
  if (missingFacts.includes('formatPattern 或 formatType')) {
    missing.push('格式约束证据')
  }
  if (missingFacts.includes('formatPrecision')) {
    missing.push('精度证据')
  }
  if (missingFacts.includes('formatTimezone')) {
    missing.push('时区证据')
  }
  if (conflicts.length > 0) {
    missing.push('冲突裁决证据')
  }
  if (usageContractConflict) {
    missing.push('使用契约禁用场景确认')
  }
  return uniqueText(missing)
}

function detectConflictingStandards(fields: StandardQuestionMatchedField[]): StandardQuestionConflict[] {
  const [topField, secondField] = fields
  if (!topField || !secondField) {
    return []
  }
  const topScore = topField.score ?? 0
  const secondScore = secondField.score ?? 0
  const isSimilarlyStrong = topScore >= 75 && secondScore >= 70 && topScore - secondScore <= 8
  if (!isSimilarlyStrong) {
    return []
  }
  return [{
    type: 'SIMILAR_SCORE',
    message: `存在多个高分标准字段：${topField.name} 与 ${secondField.name} 分数接近，需确认业务口径后再采纳。`,
    fieldNames: [topField.name, secondField.name],
    severity: 'WARNING'
  }]
}

function buildEvidenceRefs(evidence: StandardQuestionEvidence[]) {
  return evidence.map((item) => {
    const ref = item.ref || item.title
    return `${item.type}:${ref}`
  })
}

function buildSuggestedNextQuery(
  question: string,
  field: StandardQuestionMatchedField | undefined,
  status: StandardQuestionAnswerStatus,
  missingFacts: string[],
  conflicts: StandardQuestionConflict[]
) {
  if (status === 'ADOPTABLE') {
    return ''
  }
  if (!field) {
    return `补充“${question}”对应的标准字段名、显示名、业务术语和使用场景`
  }
  if (conflicts.length > 0) {
    return `确认“${question}”在 ${conflicts[0].fieldNames.join(' / ')} 中应采用哪个标准字段`
  }
  if (missingFacts.length > 0) {
    return `补充 ${field.name} 的 ${missingFacts.join('、')} 后再回答“${question}”`
  }
  return `确认 ${field.name} 是否可用于回答“${question}”`
}

function buildAnswerabilityActions(
  status: StandardQuestionAnswerStatus,
  missingEvidence: string[],
  conflicts: StandardQuestionConflict[],
  field: StandardQuestionMatchedField | undefined
) {
  if (status === 'ADOPTABLE') {
    return []
  }
  return uniqueText([
    conflicts.length > 0 ? '先处理标准字段冲突，再决定可采纳答案' : '',
    missingEvidence.length > 0 ? `补充缺失证据：${missingEvidence.join('、')}` : '',
    field?.status === 'draft' ? '将草稿字段转入人工确认，采纳后再作为正式答案' : '',
    status === 'UNANSWERABLE' ? '进入标准候选 Inbox 补充候选字段和证据' : '人工确认后再采纳答案'
  ])
}

function buildEvidence(
  fieldSearch: FieldSearchResult,
  fields: StandardQuestionMatchedField[],
  glossary: BusinessGlossary[],
  rules: StandardQuestionRelatedRule[]
): StandardQuestionEvidence[] {
  return [
    ...fields.slice(0, 5).map((field): StandardQuestionEvidence => ({
      type: 'field',
      title: `${field.name}${field.displayName ? ` / ${field.displayName}` : ''}`,
      description: uniqueText([
        field.dataType ? `类型 ${field.dataType}` : '',
        statusText(field.status),
        field.sensitive ? '敏感字段' : '',
        field.matchReasons.join('；'),
        field.recommendedUse,
        field.usageContractSummary.join('；')
      ]).join('；'),
      ref: field.detailRoute
    })),
    ...glossary.slice(0, 5).map((item): StandardQuestionEvidence => ({
      type: 'glossary',
      title: item.term || `术语 #${item.id ?? '-'}`,
      description: uniqueText([item.description, item.synonyms ? `同义词：${item.synonyms}` : '', item.abbreviations ? `缩写：${item.abbreviations}` : '']).join('；'),
      ref: item.canonicalFieldId ? `field:${item.canonicalFieldId}` : undefined
    })),
    ...rules.slice(0, 3).map((rule): StandardQuestionEvidence => ({
      type: 'rule',
      title: rule.ruleName || rule.ruleCode || '规则',
      description: uniqueText([rule.ruleCode, rule.severity ? `级别：${rule.severity}` : '', rule.enabled === false ? '未启用' : '已启用']).join('；'),
      ref: rule.ruleCode
    })),
    {
      type: 'search',
      title: '字段标准检索',
      description: `命中 ${fieldSearch.summary?.matchedCount ?? fields.length}，返回 ${fieldSearch.summary?.returnedCount ?? fields.length}`
    }
  ]
}

function resolveRelatedRules(
  question: string,
  fields: StandardQuestionMatchedField[],
  rules: RuleConfig[]
): StandardQuestionRelatedRule[] {
  const tokens = buildRuleMatchTokens(question, fields)
  return rules
    .filter((rule) => rule.enabled !== false)
    .filter((rule) => isRuleRelevant(rule, tokens))
    .slice(0, 5)
    .map((rule) => ({
      ruleCode: rule.ruleCode,
      ruleName: rule.ruleName,
      severity: rule.severity,
      enabled: rule.enabled
    }))
}

function buildRuleMatchTokens(question: string, fields: StandardQuestionMatchedField[]) {
  const fieldTokens = fields.flatMap((field) => [
    field.name,
    field.displayName,
    field.dataType,
    field.status,
    ...field.matchReasons
  ])
  return uniqueText([
    question,
    ...splitList(question),
    ...fieldTokens
  ])
    .map(normalizeMatchText)
    .filter((token) => token.length >= 2)
}

function isRuleRelevant(rule: RuleConfig, tokens: string[]) {
  const ruleText = normalizeMatchText(uniqueText([
    rule.ruleCode,
    rule.ruleName,
    rule.paramsJson
  ]).join(' '))
  return tokens.some((token) => ruleText.includes(token))
}

function buildUnresolvedQuestions(confidence: StandardQuestionConfidence, field: StandardQuestionMatchedField | undefined) {
  if (confidence === 'LOW') {
    return ['标准库中缺少直接证据，建议进入标准候选 Inbox 补充候选字段和确认理由。']
  }
  if (field?.status === 'deprecated') {
    return ['确认替代字段是否已存在，并在使用前补充替代字段引用。']
  }
  if (field?.status === 'disabled' || field?.status === 'draft') {
    return ['确认字段生命周期状态是否允许用于当前场景。']
  }
  return []
}

function findGlossaryEvidence(question: string, glossary: BusinessGlossary[], fieldId?: number) {
  const normalizedQuestion = question.toLowerCase()
  return glossary.filter((item) => {
    if (fieldId && item.canonicalFieldId === fieldId) {
      return true
    }
    return splitGlossaryTerms(item)
      .some((token) => normalizedQuestion.includes(token.toLowerCase()))
  })
}

function splitGlossaryTerms(item: BusinessGlossary) {
  return uniqueText([
    item.term,
    ...splitList(item.synonyms),
    ...splitList(item.rootTerms),
    ...splitList(item.abbreviations),
    ...splitList(item.disabledTerms),
    ...splitList(item.exampleFields)
  ])
}

function splitList(value?: string | null) {
  return (value ?? '')
    .split(/[,，、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeMatchText(value?: string | null) {
  return (value ?? '').trim().toLowerCase()
}

function formatConstraintSummary(field: Field) {
  return uniqueText([
    field.formatType ? `格式：${formatTypeText(field.formatType)}` : '',
    field.formatPattern ? `模式：${field.formatPattern}` : '',
    field.formatUnit ? `单位：${field.formatUnit}` : '',
    field.formatPrecision ? `精度：${field.formatPrecision}` : '',
    field.formatTimezone ? `时区：${field.formatTimezone}` : '',
    field.formatNotes
  ]).join('；')
}

function formatLifecycleSummary(field: Field) {
  if (field.status === 'deprecated') {
    return field.replacementReason ? `已废弃，${field.replacementReason}` : '已废弃，需要确认替代字段。'
  }
  if (field.status === 'disabled') {
    return '已停用，需要人工确认后再使用。'
  }
  if (field.status === 'draft') {
    return '草稿字段，需要人工确认后再作为正式标准。'
  }
  return ''
}

function usageContractSummary(item: FieldSearchItem, field: Field) {
  const summary = item.usageContractSummary?.filter(Boolean) ?? []
  if (summary.length > 0) {
    return summary
  }
  return uniqueText([
    field.preferredUseCases ? `推荐使用：${field.preferredUseCases}` : '',
    field.avoidWhen ? `禁用场景：${field.avoidWhen}` : '',
    field.joinHints ? `Join 提示：${field.joinHints}` : '',
    field.defaultFilters ? `默认过滤：${field.defaultFilters}` : '',
    field.aggregationHints ? `聚合提示：${field.aggregationHints}` : '',
    field.replacementGuidance ? `替代指导：${field.replacementGuidance}` : '',
    field.misuseExamples ? `误用样例：${field.misuseExamples}` : ''
  ])
}

function usageContractContradictsQuestion(question: string, field: StandardQuestionMatchedField | undefined) {
  if (!field) {
    return false
  }
  return usageContractTextMatches(question, field.avoidWhen)
    || usageContractTextMatches(question, field.misuseExamples)
    || field.usageContractSummary.some((item) => {
      if (!/^禁用场景|^误用样例/.test(item)) {
        return false
      }
      return usageContractTextMatches(question, item)
    })
}

function usageContractTextMatches(question: string, value?: string | null) {
  const left = compactMatchText(question)
  const right = compactMatchText(value)
  if (!left || !right) {
    return false
  }
  if (left.includes(right) || right.includes(left)) {
    return true
  }
  return hanBigramOverlap(left, right) >= USAGE_CONTRACT_HAN_BIGRAM_MATCH_THRESHOLD
}

function compactMatchText(value?: string | null) {
  return (value ?? '').toLowerCase().replace(/[^\p{Script=Han}a-z0-9]+/gu, '')
}

function hanBigramOverlap(left: string, right: string) {
  let count = 0
  for (let index = 0; index < left.length - 1; index += 1) {
    const part = left.slice(index, index + 2)
    if (/\p{Script=Han}/u.test(part) && right.includes(part)) {
      count += 1
    }
  }
  return count
}

function formatDataType(field: Field) {
  if (!field.dataType) {
    return ''
  }
  if (field.precisionVal !== undefined && field.precisionVal !== null) {
    const scale = field.scaleVal !== undefined && field.scaleVal !== null ? `,${field.scaleVal}` : ''
    return `${field.dataType}(${field.precisionVal}${scale})`
  }
  return field.length ? `${field.dataType}(${field.length})` : field.dataType
}

export function confidenceText(confidence: StandardQuestionConfidence) {
  if (confidence === 'HIGH') {
    return '高'
  }
  if (confidence === 'MEDIUM') {
    return '中'
  }
  return '低'
}

export function answerStatusText(status: StandardQuestionAnswerStatus) {
  if (status === 'ADOPTABLE') {
    return '可直接采用'
  }
  if (status === 'NEEDS_CONFIRMATION') {
    return '需要确认'
  }
  return '不能回答'
}

export function answerabilityText(answerability: StandardQuestionAnswerability) {
  if (answerability === 'DIRECT') {
    return '可直接回答'
  }
  if (answerability === 'PARTIAL') {
    return '部分可回答'
  }
  return '暂无可用答案'
}

export function evidenceTypeText(type: StandardQuestionEvidenceType) {
  if (type === 'field') {
    return '字段'
  }
  if (type === 'glossary') {
    return '术语'
  }
  if (type === 'rule') {
    return '规则'
  }
  return '检索'
}

export function statusText(status?: string) {
  if (status === 'draft') {
    return '草稿'
  }
  if (status === 'disabled') {
    return '停用'
  }
  if (status === 'deprecated') {
    return '废弃'
  }
  return '启用'
}

function formatTypeText(formatType: string) {
  const labels: Record<string, string> = {
    mobile: '手机号',
    email: '邮箱',
    money: '金额',
    timestamp: '时间戳',
    date: '日期',
    json: 'JSON',
    status: '状态码',
    code: '编码',
    text: '文本'
  }
  return labels[formatType] ?? formatType
}

function uniqueText(values: Array<string | undefined | null>) {
  return Array.from(new Set(values.map((value) => value?.trim()).filter((value): value is string => Boolean(value))))
}
