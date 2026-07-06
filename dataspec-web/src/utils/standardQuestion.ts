import type { BusinessGlossary, Field, FieldSearchItem, FieldSearchResult, RuleConfig } from '@/types'

export type StandardQuestionConfidence = 'HIGH' | 'MEDIUM' | 'LOW'
export type StandardQuestionEvidenceType = 'field' | 'glossary' | 'rule' | 'search'

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
  /** 格式、单位、精度等约束摘要。 */
  formatSummary?: string
  /** 生命周期或替代字段提示。 */
  lifecycleSummary?: string
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

export interface StandardQuestionAnswer {
  /** 可直接复制给用户或 AI 的短回答。 */
  answer: string
  /** 回答置信度，只表示 DataSpec 当前标准证据是否足够。 */
  confidence: StandardQuestionConfidence
  /** 匹配到的标准字段。 */
  matchedFields: StandardQuestionMatchedField[]
  /** 回答引用的字段、术语、规则和检索摘要证据。 */
  evidence: StandardQuestionEvidence[]
  /** 与本次问题相关的项目规则。 */
  relatedRules: StandardQuestionRelatedRule[]
  /** 建议下一步动作，不会直接写入标准库。 */
  suggestedNextActions: string[]
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

  const confidence = resolveConfidence(topField)
  const evidence = buildEvidence(input.fieldSearch, matchedFields, glossaryEvidence, relatedRules)
  const unresolvedQuestions = buildUnresolvedQuestions(confidence, topField)
  const suggestedNextActions = uniqueText([
    ...input.fieldSearch.nextActions ?? [],
    ...((input.fieldSearch.items ?? []).flatMap((item) => item.nextActions ?? [])),
    '复制答案并附带证据给 AI 或协作者',
    topField ? `打开字段库查看 ${topField.name} 的完整标准` : '进入标准候选 Inbox 补充候选字段',
    confidence === 'LOW' ? '把问题转成标准候选草案后人工确认' : ''
  ])

  return {
    answer: buildAnswerText(confidence, question, topField, glossaryEvidence),
    confidence,
    matchedFields,
    evidence,
    relatedRules,
    suggestedNextActions,
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
    `置信度：${confidenceText(answer.confidence)}`,
    '',
    '## 匹配字段',
    fields,
    '',
    '## 证据',
    evidence,
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
        formatSummary: formatConstraintSummary(field),
        lifecycleSummary: formatLifecycleSummary(field),
        detailRoute: `/fields?keyword=${encodeURIComponent(field.name)}`
      }
    })
}

function buildAnswerText(
  confidence: StandardQuestionConfidence,
  question: string,
  topField: StandardQuestionMatchedField | undefined,
  glossaryEvidence: BusinessGlossary[]
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
        field.recommendedUse
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
