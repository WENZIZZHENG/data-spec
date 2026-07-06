import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  budgetRiskLabel,
  budgetRiskTagType,
  buildBudgetPlanSummary,
  formatEstimatedTokens
} from '../src/utils/aiContextBudgetPlan.ts'

test('formats budget risk tags and labels', () => {
  assert.equal(budgetRiskLabel('LOW'), '低风险')
  assert.equal(budgetRiskLabel('MEDIUM'), '中风险')
  assert.equal(budgetRiskLabel('HIGH'), '高风险')
  assert.equal(budgetRiskTagType('LOW'), 'success')
  assert.equal(budgetRiskTagType('MEDIUM'), 'warning')
  assert.equal(budgetRiskTagType('HIGH'), 'danger')
})

test('formats estimated token summary with fallback', () => {
  assert.equal(formatEstimatedTokens(undefined), '0 / 0 tokens')
  assert.equal(formatEstimatedTokens({ selectedEstimatedTokens: 4800, tokenBudget: 8000 }), '4,800 / 8,000 tokens')
})

test('builds compact budget plan summary', () => {
  const summary = buildBudgetPlanSummary({
    estimation: { selectedEstimatedTokens: 4800, tokenBudget: 8000 },
    selectedArtifacts: [{ artifact: 'field-catalog.json' }, { artifact: 'DATABASE_RULES.md' }],
    droppedArtifacts: [{ artifact: 'usage-examples.json' }],
    recommendedNextActions: ['继续导出标准包']
  })

  assert.match(summary, /4,800 \/ 8,000 tokens/)
  assert.match(summary, /已选 2 项/)
  assert.match(summary, /舍弃 1 项/)
  assert.match(summary, /继续导出标准包/)
})
