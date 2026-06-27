export type AiContextScope = 'all' | 'field' | 'domain' | 'tag' | 'table' | 'changed'

export interface AiContextScopeForm {
  scope: AiContextScope
  query?: string
  status?: string
  limit?: number | null
}

export interface AiContextScopeParams {
  scope?: AiContextScope
  query?: string
  status?: string
  limit?: number
}

export function normalizeAiContextScopeParams(form: AiContextScopeForm): AiContextScopeParams {
  const query = normalizeText(form.query)
  const status = normalizeText(form.status)
  const limit = typeof form.limit === 'number' && Number.isFinite(form.limit) && form.limit > 0
    ? Math.floor(form.limit)
    : undefined
  const scoped = form.scope !== 'all' || Boolean(query) || Boolean(status) || Boolean(limit)
  if (!scoped) {
    return {}
  }
  return {
    scope: form.scope,
    ...(query ? { query } : {}),
    ...(status ? { status } : {}),
    ...(limit ? { limit } : {})
  }
}

export function aiContextScopeFilename(params: AiContextScopeParams) {
  return params.scope && params.scope !== 'all'
    ? `dataspec-ai-context-${params.scope}.zip`
    : 'dataspec-ai-context.zip'
}

function normalizeText(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}
