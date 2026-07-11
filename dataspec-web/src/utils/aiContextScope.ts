export type AiContextScope =
  | 'all'
  | 'field'
  | 'domain'
  | 'tag'
  | 'table'
  | 'business-object'
  | 'table-template'
  | 'changed'

export interface AiContextScopeForm {
  scope: AiContextScope
  query?: string
  status?: string
  limit?: number | null
  snapshotId?: number | null
  snapshotVersion?: string
}

export interface AiContextScopeParams {
  scope?: AiContextScope
  query?: string
  status?: string
  limit?: number
  snapshotId?: number
  snapshotVersion?: string
}

export function normalizeAiContextScopeParams(form: AiContextScopeForm): AiContextScopeParams {
  const query = normalizeText(form.query)
  const status = normalizeText(form.status)
  const snapshotVersion = normalizeText(form.snapshotVersion)
  const limit = typeof form.limit === 'number' && Number.isFinite(form.limit) && form.limit > 0
    ? Math.floor(form.limit)
    : undefined
  const snapshotId = typeof form.snapshotId === 'number' && Number.isFinite(form.snapshotId) && form.snapshotId > 0
    ? Math.floor(form.snapshotId)
    : undefined
  const scoped = form.scope !== 'all' ||
    Boolean(query) ||
    Boolean(status) ||
    Boolean(limit) ||
    Boolean(snapshotId) ||
    Boolean(snapshotVersion)
  if (!scoped) {
    return {}
  }
  return {
    scope: form.scope,
    ...(query ? { query } : {}),
    ...(status ? { status } : {}),
    ...(limit ? { limit } : {}),
    ...(snapshotId ? { snapshotId } : {}),
    ...(snapshotVersion ? { snapshotVersion } : {})
  }
}

export function aiContextScopeFilename(params: AiContextScopeParams) {
  if (params.snapshotId) {
    return `dataspec-ai-context-snapshot-${params.snapshotId}.zip`
  }
  if (params.snapshotVersion) {
    return `dataspec-ai-context-snapshot-${safeFilenamePart(params.snapshotVersion)}.zip`
  }
  return params.scope && params.scope !== 'all'
    ? `dataspec-ai-context-${params.scope}.zip`
    : 'dataspec-ai-context.zip'
}

function normalizeText(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

function safeFilenamePart(value: string) {
  return value.trim().replace(/[^a-zA-Z0-9._-]+/g, '-').replace(/^-+|-+$/g, '') || 'version'
}
