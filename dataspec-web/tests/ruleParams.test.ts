import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  buildRuleParamsJson,
  createRuleParamsForm,
  parseRuleParamsForm,
  summarizeRuleParams
} from '../src/utils/ruleParams.ts'

test('parses and builds required columns params', () => {
  const form = parseRuleParamsForm('required_columns', '{"requiredColumns":["id","created_at"]}')

  assert.deepEqual(form.requiredColumns, ['id', 'created_at'])
  assert.equal(buildRuleParamsJson('required_columns', form), '{\n  "requiredColumns": [\n    "id",\n    "created_at"\n  ]\n}')
  assert.equal(summarizeRuleParams('required_columns', buildRuleParamsJson('required_columns', form)), '必含列 2 个：id、created_at')
})

test('parses and builds forbidden field params', () => {
  const form = parseRuleParamsForm('forbidden_field_name', '{"forbiddenNames":["tmp","flag1"]}')

  assert.deepEqual(form.forbiddenNames, ['tmp', 'flag1'])
  assert.equal(summarizeRuleParams('forbidden_field_name', buildRuleParamsJson('forbidden_field_name', form)), '禁用字段 2 个：tmp、flag1')
})

test('parses and builds recommendation params', () => {
  const form = parseRuleParamsForm('recommended_field_name', '{"recommendations":{"create_time":"created_at"}}')

  assert.deepEqual(form.recommendations, [{ from: 'create_time', to: 'created_at' }])
  assert.equal(buildRuleParamsJson('recommended_field_name', form), '{\n  "recommendations": {\n    "create_time": "created_at"\n  }\n}')
  assert.equal(summarizeRuleParams('recommended_field_name', buildRuleParamsJson('recommended_field_name', form)), '推荐替换 1 组：create_time→created_at')
})

test('parses and builds suffix and prefix type params', () => {
  const form = parseRuleParamsForm(
    'field_suffix_type',
    '{"suffixTypes":{"_id":["bigint","integer"]},"prefixTypes":{"is_":"boolean"}}'
  )

  assert.deepEqual(form.suffixTypes, [{ pattern: '_id', typesText: 'bigint, integer' }])
  assert.deepEqual(form.prefixTypes, [{ pattern: 'is_', typesText: 'boolean' }])
  assert.equal(
    buildRuleParamsJson('field_suffix_type', form),
    '{\n  "suffixTypes": {\n    "_id": [\n      "bigint",\n      "integer"\n    ]\n  },\n  "prefixTypes": {\n    "is_": [\n      "boolean"\n    ]\n  }\n}'
  )
  assert.equal(summarizeRuleParams('field_suffix_type', buildRuleParamsJson('field_suffix_type', form)), '后缀 1 组，前缀 1 组')
})

test('preserves unknown rule json through raw editor', () => {
  const form = createRuleParamsForm()
  form.rawJson = '{"custom":true}'

  assert.equal(buildRuleParamsJson('custom_rule', form), '{\n  "custom": true\n}')
  assert.equal(summarizeRuleParams('custom_rule', form.rawJson), 'JSON 参数 1 项')
})

test('omits empty structured params to preserve backend defaults', () => {
  const form = createRuleParamsForm()

  assert.equal(buildRuleParamsJson('required_columns', form), '{}')
  assert.equal(buildRuleParamsJson('forbidden_field_name', form), '{}')
  assert.equal(buildRuleParamsJson('recommended_field_name', form), '{}')
  assert.equal(buildRuleParamsJson('field_suffix_type', form), '{}')
})
