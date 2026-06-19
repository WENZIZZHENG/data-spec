export interface KeyValueRow {
  from: string
  to: string
}

export interface TypeRuleRow {
  pattern: string
  typesText: string
}

export interface RuleParamsForm {
  requiredColumns: string[]
  forbiddenNames: string[]
  recommendations: KeyValueRow[]
  suffixTypes: TypeRuleRow[]
  prefixTypes: TypeRuleRow[]
  rawJson: string
}

const STRUCTURED_RULE_CODES = new Set([
  'required_columns',
  'forbidden_field_name',
  'recommended_field_name',
  'field_suffix_type'
])

export function createRuleParamsForm(): RuleParamsForm {
  return {
    requiredColumns: [],
    forbiddenNames: [],
    recommendations: [],
    suffixTypes: [],
    prefixTypes: [],
    rawJson: '{}'
  }
}

export function isStructuredRule(ruleCode?: string): boolean {
  return STRUCTURED_RULE_CODES.has(ruleCode ?? '')
}

export function parseRuleParamsForm(ruleCode: string | undefined, paramsJson?: string): RuleParamsForm {
  const form = createRuleParamsForm()
  const params = parseJsonObject(paramsJson)
  form.rawJson = stringifyJson(params)

  if (ruleCode === 'required_columns') {
    form.requiredColumns = stringArray(params.requiredColumns)
  } else if (ruleCode === 'forbidden_field_name') {
    form.forbiddenNames = stringArray(params.forbiddenNames)
  } else if (ruleCode === 'recommended_field_name') {
    form.recommendations = objectEntries(params.recommendations).map(([from, to]) => ({ from, to }))
  } else if (ruleCode === 'field_suffix_type') {
    form.suffixTypes = typeRuleRows(params.suffixTypes)
    form.prefixTypes = typeRuleRows(params.prefixTypes)
  }

  return form
}

export function buildRuleParamsJson(ruleCode: string | undefined, form: RuleParamsForm): string {
  if (ruleCode === 'required_columns') {
    const requiredColumns = cleanStringList(form.requiredColumns)
    return stringifyJson(requiredColumns.length ? { requiredColumns } : {})
  }
  if (ruleCode === 'forbidden_field_name') {
    const forbiddenNames = cleanStringList(form.forbiddenNames)
    return stringifyJson(forbiddenNames.length ? { forbiddenNames } : {})
  }
  if (ruleCode === 'recommended_field_name') {
    const recommendations = keyValueObject(form.recommendations)
    return stringifyJson(Object.keys(recommendations).length ? { recommendations } : {})
  }
  if (ruleCode === 'field_suffix_type') {
    const suffixTypes = typeRuleObject(form.suffixTypes)
    const prefixTypes = typeRuleObject(form.prefixTypes)
    return stringifyJson({
      ...(Object.keys(suffixTypes).length ? { suffixTypes } : {}),
      ...(Object.keys(prefixTypes).length ? { prefixTypes } : {})
    })
  }
  return stringifyJson(parseJsonObject(form.rawJson))
}

export function summarizeRuleParams(ruleCode: string | undefined, paramsJson?: string): string {
  const params = parseJsonObject(paramsJson)
  if (ruleCode === 'required_columns') {
    return listSummary('必含列', stringArray(params.requiredColumns))
  }
  if (ruleCode === 'forbidden_field_name') {
    return listSummary('禁用字段', stringArray(params.forbiddenNames))
  }
  if (ruleCode === 'recommended_field_name') {
    const entries = objectEntries(params.recommendations)
    return entries.length === 0
      ? '推荐替换 0 组'
      : `推荐替换 ${entries.length} 组：${entries.slice(0, 3).map(([from, to]) => `${from}→${to}`).join('、')}`
  }
  if (ruleCode === 'field_suffix_type') {
    return `后缀 ${Object.keys(objectParam(params.suffixTypes)).length} 组，前缀 ${Object.keys(objectParam(params.prefixTypes)).length} 组`
  }
  const count = Object.keys(params).length
  return count === 0 ? '无参数' : `JSON 参数 ${count} 项`
}

function parseJsonObject(paramsJson?: string): Record<string, unknown> {
  const text = paramsJson?.trim()
  if (!text) {
    return {}
  }
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function stringifyJson(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return []
  }
  return cleanStringList(value.map((item) => String(item)))
}

function cleanStringList(values: string[]): string[] {
  return values.map((value) => value.trim()).filter(Boolean)
}

function objectParam(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function objectEntries(value: unknown): Array<[string, string]> {
  return Object.entries(objectParam(value))
    .map(([key, entryValue]) => [key.trim(), String(entryValue).trim()] as [string, string])
    .filter(([key, entryValue]) => Boolean(key && entryValue))
}

function keyValueObject(rows: KeyValueRow[]): Record<string, string> {
  return Object.fromEntries(
    rows
      .map((row) => [row.from.trim(), row.to.trim()] as [string, string])
      .filter(([from, to]) => Boolean(from && to))
  )
}

function typeRuleRows(value: unknown): TypeRuleRow[] {
  return Object.entries(objectParam(value))
    .map(([pattern, types]) => ({
      pattern: pattern.trim(),
      typesText: typeList(types).join(', ')
    }))
    .filter((row) => row.pattern && row.typesText)
}

function typeRuleObject(rows: TypeRuleRow[]): Record<string, string[]> {
  return Object.fromEntries(
    rows
      .map((row) => [row.pattern.trim(), splitTypes(row.typesText)] as [string, string[]])
      .filter(([pattern, types]) => Boolean(pattern && types.length))
  )
}

function typeList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return cleanStringList(value.map((item) => String(item)))
  }
  if (typeof value === 'string') {
    return splitTypes(value)
  }
  return []
}

function splitTypes(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function listSummary(label: string, values: string[]): string {
  return values.length === 0
    ? `${label} 0 个`
    : `${label} ${values.length} 个：${values.slice(0, 4).join('、')}`
}
