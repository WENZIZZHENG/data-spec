import type { LocationQuery, LocationQueryRaw, RouteLocationNormalizedLoaded, Router } from 'vue-router'

type QueryPatchValue = string | number | boolean | null | undefined

const MAX_SAFE_QUERY_VALUE_LENGTH = 256
const UNSAFE_QUERY_KEYS = new Set([
  'authorization',
  'bearer',
  'token',
  'apiToken',
  'dataspecToken',
  'password',
  'pwd',
  'secret',
  'jdbcUrl',
  'connectionString',
  'sql',
  'originalSql',
  'fixedSql',
  'payload',
  'inputPayload',
  'outputPayload',
  'replayPayload'
].map((key) => key.toLowerCase()))

const UNSAFE_QUERY_KEY_PATTERN = /(password|pwd|token|authorization|bearer|secret|jdbc|connectionstring|sql|payload)/i

export function firstQueryValue(value: LocationQuery[string] | undefined): string {
  if (Array.isArray(value)) {
    return value.find((item): item is string => typeof item === 'string')?.trim() ?? ''
  }
  return typeof value === 'string' ? value.trim() : ''
}

export function readStringQuery(query: LocationQuery, key: string): string {
  return firstQueryValue(query[key])
}

export function readPositiveIntQuery(query: LocationQuery, key: string): number | null {
  const raw = readStringQuery(query, key)
  if (!/^\d+$/.test(raw)) {
    return null
  }
  const parsed = Number(raw)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
}

export function readEnumQuery<T extends string>(query: LocationQuery, key: string, allowedValues: readonly T[]): T | null {
  const raw = readStringQuery(query, key)
  return allowedValues.includes(raw as T) ? raw as T : null
}

export function isUnsafeQueryKey(key: string) {
  return UNSAFE_QUERY_KEYS.has(key.toLowerCase()) || UNSAFE_QUERY_KEY_PATTERN.test(key)
}

export function sanitizeQuery(query: LocationQueryRaw): LocationQueryRaw {
  const sanitized: LocationQueryRaw = {}
  for (const [key, value] of Object.entries(query)) {
    if (isUnsafeQueryKey(key)) {
      continue
    }
    const normalized = normalizeQueryValue(value)
    if (normalized !== undefined) {
      sanitized[key] = normalized
    }
  }
  return sanitized
}

export function mergeRouteQuery(query: LocationQuery, patch: Record<string, QueryPatchValue>): LocationQueryRaw {
  return sanitizeQuery({
    ...query,
    ...normalizePatch(patch)
  })
}

export async function replaceRouteQuery(
  router: Router,
  route: RouteLocationNormalizedLoaded,
  patch: Record<string, QueryPatchValue>
) {
  const nextQuery = mergeRouteQuery(route.query, patch)
  if (queryEqual(route.query, nextQuery)) {
    return false
  }
  await router.replace({ path: route.path, query: nextQuery, hash: route.hash })
  return true
}

export function buildRouteUrl(origin: string, path: string, query: LocationQueryRaw) {
  const url = new URL(path || '/', origin)
  const safeQuery = sanitizeQuery(query)
  for (const [key, value] of Object.entries(safeQuery)) {
    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (typeof item === 'string') {
          url.searchParams.append(key, item)
        }
      })
    } else if (typeof value === 'string') {
      url.searchParams.set(key, value)
    }
  }
  return url.toString()
}

export async function copyRouteUrl(
  route: Pick<RouteLocationNormalizedLoaded, 'path' | 'query'>,
  clipboard: Pick<Clipboard, 'writeText'>,
  origin = globalThis.location?.origin ?? 'http://localhost'
) {
  const url = buildRouteUrl(origin, route.path, route.query)
  await clipboard.writeText(url)
  return url
}

function normalizePatch(patch: Record<string, QueryPatchValue>): LocationQueryRaw {
  const normalized: LocationQueryRaw = {}
  for (const [key, value] of Object.entries(patch)) {
    if (value === undefined || value === null || value === '') {
      normalized[key] = undefined
    } else {
      normalized[key] = String(value)
    }
  }
  return normalized
}

function normalizeQueryValue(value: LocationQueryRaw[string]): string | string[] | undefined {
  if (Array.isArray(value)) {
    const items = value
      .map((item) => normalizeSingleValue(item))
      .filter((item): item is string => item !== undefined)
    return items.length > 0 ? items : undefined
  }
  return normalizeSingleValue(value)
}

function normalizeSingleValue(value: string | number | boolean | null | undefined) {
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  if (!trimmed || trimmed.length > MAX_SAFE_QUERY_VALUE_LENGTH) {
    return undefined
  }
  return trimmed
}

function queryEqual(left: LocationQuery, right: LocationQueryRaw) {
  return JSON.stringify(sanitizeQuery(left)) === JSON.stringify(sanitizeQuery(right))
}
