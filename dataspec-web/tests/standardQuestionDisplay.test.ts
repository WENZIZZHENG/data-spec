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
    summary: { matchedCount: 1, returnedCount: 1 },
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
  assert.match(answer.answer, /user_mobile/)
  assert.match(answer.answer, /手机号/)
  assert.match(answer.answer, /敏感字段/)
  assert.equal(answer.matchedFields[0].name, 'user_mobile')
  assert.equal(answer.matchedFields[0].sensitive, true)
  assert.ok(answer.evidence.some((item) => item.type === 'field' && item.title.includes('user_mobile')))
  assert.ok(answer.evidence.some((item) => item.type === 'glossary' && item.title.includes('手机号')))
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
  assert.match(answer.answer, /没有找到可直接确认的标准字段/)
  assert.ok(answer.unresolvedQuestions.some((item) => item.includes('标准候选')))
  assert.ok(answer.suggestedNextActions.some((item) => item.includes('候选 Inbox')))
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
  assert.match(answer.answer, /已废弃/)
  assert.ok(answer.unresolvedQuestions.some((item) => item.includes('替代字段')))
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
