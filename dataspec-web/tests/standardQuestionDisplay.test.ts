import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildStandardQuestionAnswer,
  buildStandardQuestionMarkdown,
  createStandardQuestionRequestGuard
} from '../src/utils/standardQuestion.ts'
import type { BusinessGlossary, FieldSearchResult, RuleConfig } from '../src/types/index.ts'

test('builds readonly standard answer with matched field evidence', () => {
  const fieldSearch: FieldSearchResult = {
    projectId: 7,
    query: '手机号标准字段叫什么',
    summary: {
      matchedCount: 1,
      returnedCount: 1,
      queryTokens: [{
        token: '手机号',
        normalizedToken: '手机号',
        tokenKind: 'HAN',
        resolutionStatus: 'RESOLVED',
        canonicalTerm: '手机号',
        canonicalFieldId: 11,
        canonicalFieldName: 'user_mobile',
        glossaryIds: [21],
        reason: '术语表：手机号 -> user_mobile'
      }]
    },
    items: [{
      score: 96,
      field: {
        id: 11,
        name: 'user_mobile',
        displayName: '手机号',
        dataType: 'varchar',
        length: 20,
        status: 'enabled',
        sensitive: true,
        formatType: 'mobile',
        formatPattern: '^1\\d{10}$',
        comment: '用户手机号'
      },
      matchReasons: ['命中显示名：手机号'],
      recommendedUse: '用于用户联系方式，不要保存明文外泄。'
    }]
  }
  const glossary: BusinessGlossary[] = [{
    id: 21,
    term: '手机号',
    synonyms: '手机,mobile',
    canonicalFieldId: 11,
    description: '用户联系手机号',
    status: 'ACTIVE'
  }]
  const rules: RuleConfig[] = [{
    id: 31,
    ruleCode: 'FIELD_NAMING',
    ruleName: '字段命名规则',
    paramsJson: '{"field":"user_mobile"}',
    severity: 'WARNING',
    enabled: true
  }]

  const answer = buildStandardQuestionAnswer({
    question: '手机号标准字段叫什么',
    fieldSearch,
    glossary,
    rules
  })

  assert.equal(answer.confidence, 'HIGH')
  assert.equal(answer.answerStatus, 'ADOPTABLE')
  assert.equal(answer.answerability, 'DIRECT')
  assert.equal(answer.suggestedNextQuery, '')
  assert.match(answer.answer, /user_mobile/)
  assert.match(answer.answer, /手机号/)
  assert.match(answer.answer, /敏感字段/)
  assert.equal(answer.matchedFields[0].name, 'user_mobile')
  assert.equal(answer.matchedFields[0].sensitive, true)
  assert.ok(answer.evidence.some((item) => item.type === 'field' && item.title.includes('user_mobile')))
  assert.ok(answer.evidence.some((item) => item.type === 'glossary' && item.title.includes('手机号')))
  assert.ok(answer.evidenceRefs.includes('glossary:field:7:11'))
  assert.ok(answer.relatedRules.some((item) => item.ruleCode === 'FIELD_NAMING'))
  assert.ok(answer.suggestedNextActions.some((item) => item.includes('复制答案')))

  const markdown = buildStandardQuestionMarkdown(answer)
  assert.match(markdown, /## 答案/)
  assert.match(markdown, /user_mobile/)
  assert.match(markdown, /证据/)
})

test('marks unresolved answer when no standard field evidence is found', () => {
  const answer = buildStandardQuestionAnswer({
    question: '积分等级字段用什么',
    fieldSearch: {
      projectId: 7,
      query: '积分等级字段用什么',
      summary: { matchedCount: 0, returnedCount: 0 },
      items: []
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.confidence, 'LOW')
  assert.equal(answer.answerStatus, 'UNANSWERABLE')
  assert.equal(answer.answerability, 'NONE')
  assert.equal(answer.escalateToInbox, true)
  assert.match(answer.answer, /没有找到可直接确认的标准字段/)
  assert.ok(answer.missingEvidence.some((item) => item.includes('字段标准证据')))
  assert.ok(answer.missingFacts.some((item) => item.includes('标准字段')))
  assert.ok(answer.unresolvedQuestions.some((item) => item.includes('标准候选')))
  assert.ok(answer.suggestedNextActions.some((item) => item.includes('候选 Inbox')))
  assert.ok(answer.nextActions.some((item) => item.includes('候选 Inbox')))
  assert.match(answer.suggestedNextQuery, /积分等级/)
})

test('requires confirmation for server-reported ambiguous abbreviation even with client glossary data', () => {
  const answer = buildStandardQuestionAnswer({
    question: 'amt 应该用哪个字段',
    fieldSearch: {
      projectId: 7,
      query: 'amt 应该用哪个字段',
      summary: {
        matchedCount: 1,
        returnedCount: 1,
        queryTokens: [{
          token: 'amt',
          normalizedToken: 'amt',
          tokenKind: 'WORD',
          resolutionStatus: 'AMBIGUOUS',
          glossaryIds: [41, 42],
          reason: '同一 token 指向多个 canonical 字段，需要人工确认'
        }]
      },
      items: [{
        score: 96,
        field: {
          id: 11,
          name: 'order_amount',
          displayName: '订单金额',
          dataType: 'bigint',
          status: 'enabled'
        },
        matchReasons: ['字段名精确匹配']
      }]
    },
    glossary: [{
      id: 41,
      term: '订单金额',
      abbreviations: 'amt',
      canonicalFieldId: 11,
      status: 'enabled'
    }],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.missingEvidence.some((item) => item.includes('歧义')))
  assert.ok(answer.evidence.some((item) => item.type === 'glossary' && item.description.includes('人工确认')))
})

test('links canonical-less glossary evidence only to the field trace that used it', () => {
  const fieldSearch: FieldSearchResult = {
    projectId: 7,
    query: 'fee',
    summary: {
      matchedCount: 2,
      returnedCount: 2,
      queryTokens: [{
        token: 'fee',
        normalizedToken: 'fee',
        tokenKind: 'WORD',
        resolutionStatus: 'RESOLVED',
        canonicalTerm: '费用',
        glossaryIds: [33],
        reason: '术语表：fee -> payment_amount'
      }]
    },
    items: [{
      score: 99,
      field: { id: 11, name: 'fee', displayName: '费用代码', status: 'enabled' },
      matchReasons: ['字段名精确匹配'],
      evidence: [{ sourceType: 'FIELD', sourceId: 11 }]
    }, {
      score: 95,
      field: { id: 12, name: 'payment_amount', displayName: '支付金额', status: 'enabled' },
      matchReasons: ['术语表匹配'],
      evidence: [{ sourceType: 'BUSINESS_GLOSSARY', sourceId: 33 }]
    }]
  }

  const unrelatedTop = buildStandardQuestionAnswer({
    question: 'fee',
    fieldSearch,
    glossary: [],
    rules: []
  })

  assert.ok(!unrelatedTop.answer.includes('术语证据'))
  assert.ok(!unrelatedTop.evidence.some((item) => item.type === 'glossary'))

  fieldSearch.items![0]!.evidence = [{ sourceType: 'BUSINESS_GLOSSARY', sourceId: 33 }]
  const linkedTop = buildStandardQuestionAnswer({
    question: 'fee',
    fieldSearch,
    glossary: [],
    rules: []
  })

  assert.ok(linkedTop.answer.includes('术语证据'))
  assert.ok(linkedTop.evidence.some((item) => item.type === 'glossary' && item.title.includes('费用')))
})

test('downgrades confidence for deprecated matched field', () => {
  const answer = buildStandardQuestionAnswer({
    question: '旧手机号字段还能用吗',
    fieldSearch: {
      projectId: 7,
      query: '旧手机号字段还能用吗',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 91,
        field: {
          id: 12,
          name: 'mobile_no_old',
          displayName: '旧手机号',
          status: 'deprecated',
          replacementReason: '改用 user_mobile'
        },
        matchReasons: ['命中别名：旧手机号']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.confidence, 'MEDIUM')
  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.match(answer.answer, /已废弃/)
  assert.ok(answer.confidenceReason.includes('生命周期'))
  assert.ok(answer.missingEvidence.some((item) => item.includes('替代字段')))
  assert.ok(!answer.missingFacts.some((item) => item.includes('replacementFieldId')))
  assert.ok(answer.unresolvedQuestions.some((item) => item.includes('替代字段')))
})

test('requires confirmation for disabled matched field', () => {
  const answer = buildStandardQuestionAnswer({
    question: '停用状态字段还能用吗',
    fieldSearch: {
      projectId: 7,
      query: '停用状态字段还能用吗',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 90,
        field: {
          id: 22,
          name: 'legacy_status',
          displayName: '旧状态',
          status: 'disabled'
        },
        matchReasons: ['命中显示名：旧状态']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.equal(answer.escalateToInbox, true)
  assert.ok(answer.missingEvidence.some((item) => item.includes('停用字段')))
  assert.ok(answer.unresolvedQuestions.some((item) => item.includes('生命周期状态')))
})

test('marks draft matched field as candidate only and requires confirmation', () => {
  const answer = buildStandardQuestionAnswer({
    question: '会员等级字段叫什么',
    fieldSearch: {
      projectId: 7,
      query: '会员等级字段叫什么',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 88,
        field: {
          id: 41,
          name: 'member_level',
          displayName: '会员等级',
          status: 'draft'
        },
        matchReasons: ['命中显示名：会员等级']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.equal(answer.candidateOnly, true)
  assert.equal(answer.escalateToInbox, true)
  assert.ok(answer.missingFacts.some((item) => item.includes('正式标准')))
  assert.ok(answer.nextActions.some((item) => item.includes('人工确认')))
})

test('reports conflicting standards when similarly strong fields compete', () => {
  const answer = buildStandardQuestionAnswer({
    question: '订单金额字段用哪个',
    fieldSearch: {
      projectId: 7,
      query: '订单金额字段用哪个',
      summary: { matchedCount: 2, returnedCount: 2 },
      items: [
        {
          score: 91,
          field: { id: 51, name: 'order_amount', displayName: '订单金额', status: 'enabled', dataType: 'decimal' },
          matchReasons: ['命中显示名：订单金额']
        },
        {
          score: 87,
          field: { id: 52, name: 'pay_amount', displayName: '支付金额', status: 'enabled', dataType: 'decimal' },
          matchReasons: ['同义词命中：金额']
        }
      ]
    },
    glossary: [{
      id: 61,
      term: '金额',
      synonyms: '订单金额,支付金额',
      status: 'ACTIVE'
    }],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.conflicts.length >= 1)
  assert.deepEqual(answer.conflictingStandards, answer.conflicts)
  assert.ok(answer.conflicts[0].fieldNames.includes('order_amount'))
  assert.ok(answer.confidenceReason.includes('存在多个'))
  assert.ok(answer.nextActions.some((item) => item.includes('冲突')))
})

test('requires confirmation when question asks format facts that matched field lacks', () => {
  const answer = buildStandardQuestionAnswer({
    question: '订单金额应该用什么单位',
    fieldSearch: {
      projectId: 7,
      query: '订单金额应该用什么单位',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 94,
        field: {
          id: 71,
          name: 'order_amount',
          displayName: '订单金额',
          status: 'enabled',
          dataType: 'decimal'
        },
        matchReasons: ['命中显示名：订单金额']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.missingEvidence.some((item) => item.includes('单位')))
  assert.ok(answer.missingFacts.some((item) => item.includes('formatUnit')))
  assert.ok(answer.suggestedNextQuery.includes('订单金额'))
})

test('requires confirmation when usage contract avoid condition matches question', () => {
  const answer = buildStandardQuestionAnswer({
    question: '展示订单金额字段能直接用吗',
    fieldSearch: {
      projectId: 7,
      query: '展示订单金额字段能直接用吗',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 94,
        field: {
          id: 73,
          name: 'amount_cent',
          displayName: '订单金额',
          status: 'enabled',
          dataType: 'bigint',
          preferredUseCases: '统计订单实付金额',
          avoidWhen: '展示金额时不要直接输出分单位',
          aggregationHints: 'sum(amount_cent) / 100'
        },
        usageContractSummary: [
          '推荐使用：统计订单实付金额',
          '禁用场景：展示金额时不要直接输出分单位',
          '聚合：sum(amount_cent) / 100'
        ],
        matchReasons: ['命中显示名：订单金额'],
        nextActions: ['当前问题命中字段使用契约的禁用场景，人工确认后再使用。']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.confidenceReason.includes('使用契约'))
  assert.ok(answer.missingEvidence.some((item) => item.includes('使用契约')))
  assert.ok(answer.nextActions.some((item) => item.includes('禁用场景')))
  assert.ok(answer.evidence.some((item) => item.description.includes('禁用场景')))
})

test('does not downgrade when usage contract only shares one generic Chinese bigram', () => {
  const answer = buildStandardQuestionAnswer({
    question: '统计订单金额应该用哪个字段',
    fieldSearch: {
      projectId: 7,
      query: '统计订单金额应该用哪个字段',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 94,
        field: {
          id: 73,
          name: 'amount_cent',
          displayName: '订单金额',
          status: 'enabled',
          dataType: 'bigint',
          formatUnit: 'cent',
          preferredUseCases: '统计订单实付金额',
          avoidWhen: '展示金额时不要直接输出分单位',
          aggregationHints: 'sum(amount_cent) / 100'
        },
        usageContractSummary: [
          '推荐使用：统计订单实付金额',
          '禁用场景：展示金额时不要直接输出分单位',
          '聚合：sum(amount_cent) / 100'
        ],
        matchReasons: ['命中显示名：订单金额']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'ADOPTABLE')
  assert.equal(answer.answerability, 'DIRECT')
  assert.equal(answer.confidence, 'HIGH')
  assert.ok(!answer.missingEvidence.some((item) => item.includes('使用契约')))
})

test('requires confirmation when question asks format but field only has unit evidence', () => {
  const answer = buildStandardQuestionAnswer({
    question: '订单金额格式是什么',
    fieldSearch: {
      projectId: 7,
      query: '订单金额格式是什么',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 94,
        field: {
          id: 72,
          name: 'order_amount',
          displayName: '订单金额',
          status: 'enabled',
          dataType: 'decimal',
          formatUnit: 'CNY'
        },
        matchReasons: ['命中显示名：订单金额']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.missingFacts.some((item) => item.includes('formatPattern')))
  assert.ok(answer.missingEvidence.some((item) => item.includes('格式约束')))
})

test('requires confirmation for medium score field even when lifecycle is enabled', () => {
  const answer = buildStandardQuestionAnswer({
    question: '客户来源字段叫什么',
    fieldSearch: {
      projectId: 7,
      query: '客户来源字段叫什么',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 64,
        field: {
          id: 81,
          name: 'customer_source',
          displayName: '客户来源',
          status: 'enabled',
          dataType: 'varchar'
        },
        matchReasons: ['弱匹配：来源']
      }]
    },
    glossary: [],
    rules: []
  })

  assert.equal(answer.confidence, 'MEDIUM')
  assert.equal(answer.answerStatus, 'NEEDS_CONFIRMATION')
  assert.equal(answer.answerability, 'PARTIAL')
  assert.ok(answer.confidenceReason.includes('检索分数'))
  assert.ok(answer.nextActions.some((item) => item.includes('人工确认')))
})

test('does not present unrelated enabled rules as answer evidence', () => {
  const answer = buildStandardQuestionAnswer({
    question: '手机号标准字段叫什么',
    fieldSearch: {
      projectId: 7,
      query: '手机号标准字段叫什么',
      summary: { matchedCount: 1, returnedCount: 1 },
      items: [{
        score: 93,
        field: {
          id: 11,
          name: 'user_mobile',
          displayName: '手机号',
          status: 'enabled'
        },
        matchReasons: ['命中显示名：手机号']
      }]
    },
    glossary: [],
    rules: [{
      id: 32,
      ruleCode: 'ORDER_AMOUNT_UNIT',
      ruleName: '订单金额单位规则',
      paramsJson: '{"field":"order_amount","unit":"cent"}',
      severity: 'WARNING',
      enabled: true
    }]
  })

  assert.deepEqual(answer.relatedRules, [])
  assert.ok(!answer.evidence.some((item) => item.type === 'rule'))
})

test('keeps API search item order immutable while ranking matched fields', () => {
  const fieldSearch: FieldSearchResult = {
    projectId: 7,
    query: '金额',
    summary: { matchedCount: 2, returnedCount: 2 },
    items: [
      {
        score: 20,
        field: { id: 1, name: 'amount_text', status: 'enabled' },
        matchReasons: ['弱匹配']
      },
      {
        score: 88,
        field: { id: 2, name: 'order_amount', status: 'enabled' },
        matchReasons: ['强匹配']
      }
    ]
  }
  const originalOrder = fieldSearch.items?.map((item) => item.field?.name)

  const answer = buildStandardQuestionAnswer({
    question: '金额字段用哪个',
    fieldSearch,
    glossary: [],
    rules: []
  })

  assert.deepEqual(fieldSearch.items?.map((item) => item.field?.name), originalOrder)
  assert.equal(answer.matchedFields[0].name, 'order_amount')
})

test('guards standard question requests from stale project or question results', () => {
  const guard = createStandardQuestionRequestGuard()
  const first = guard.begin(7, '手机号标准字段叫什么')
  const second = guard.begin(7, '订单金额标准字段叫什么')

  assert.equal(guard.isCurrent(first, 7, '手机号标准字段叫什么'), false)
  assert.equal(guard.isCurrent(second, 7, '订单金额标准字段叫什么'), true)
  assert.equal(guard.isCurrent(second, 8, '订单金额标准字段叫什么'), false)

  guard.invalidate()
  assert.equal(guard.isCurrent(second, 7, '订单金额标准字段叫什么'), false)
})
