export type ReverseImportMode = 'sql' | 'database'

export interface ReverseImportDatabaseMemory {
  databaseType?: string
  host?: string
  port?: number
  databaseName?: string
  schemaName?: string
  username?: string
  tableNames?: string[]
}

export interface ReverseImportMemoryState {
  activeMode?: ReverseImportMode
  tableSearch?: string
  compareStatusFilter?: string
  database?: ReverseImportDatabaseMemory
}

export interface StorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem?(key: string): void
}

const KEY_PREFIX = 'dataspec:reverse-import:memory'

export function reverseImportMemoryKey(projectId: number): string {
  return `${KEY_PREFIX}:${projectId}`
}

export function sanitizeReverseImportMemory(input: unknown): ReverseImportMemoryState | null {
  if (!isRecord(input)) {
    return null
  }

  const memory: ReverseImportMemoryState = {}
  if (input.activeMode === 'sql' || input.activeMode === 'database') {
    memory.activeMode = input.activeMode
  }
  const tableSearch = stringValue(input.tableSearch)
  if (tableSearch !== undefined) {
    memory.tableSearch = tableSearch
  }
  const compareStatusFilter = stringValue(input.compareStatusFilter)
  if (compareStatusFilter !== undefined) {
    memory.compareStatusFilter = compareStatusFilter
  }

  if (isRecord(input.database)) {
    const database = sanitizeDatabaseMemory(input.database)
    if (Object.keys(database).length > 0) {
      memory.database = database
    }
  }

  return Object.keys(memory).length > 0 ? memory : null
}

export function saveReverseImportMemory(storage: StorageLike, projectId: number, state: unknown): void {
  const sanitized = sanitizeReverseImportMemory(state)
  const key = reverseImportMemoryKey(projectId)
  if (!sanitized) {
    storage.removeItem?.(key)
    return
  }
  storage.setItem(key, JSON.stringify(sanitized))
}

export function loadReverseImportMemory(storage: StorageLike, projectId: number): ReverseImportMemoryState | null {
  const raw = storage.getItem(reverseImportMemoryKey(projectId))
  if (!raw) {
    return null
  }
  try {
    return sanitizeReverseImportMemory(JSON.parse(raw))
  } catch {
    return null
  }
}

export function fieldLibraryQueryForImportResult(
  importedFields: Array<string | undefined | null> = []
): Record<string, string> {
  const keyword = importedFields
    .map((field) => field?.trim())
    .find((field): field is string => Boolean(field))
  return keyword ? { keyword } : {}
}

function sanitizeDatabaseMemory(input: Record<string, unknown>): ReverseImportDatabaseMemory {
  const database: ReverseImportDatabaseMemory = {}
  assignString(database, 'databaseType', input.databaseType)
  assignString(database, 'host', input.host)
  const port = numberValue(input.port)
  if (port !== undefined) {
    database.port = port
  }
  assignString(database, 'databaseName', input.databaseName)
  assignString(database, 'schemaName', input.schemaName)
  assignString(database, 'username', input.username)
  const tableNames = stringArrayValue(input.tableNames)
  if (tableNames !== undefined) {
    database.tableNames = tableNames
  }
  return database
}

function assignString<T extends object, K extends keyof T>(target: T, key: K, value: unknown): void {
  const text = stringValue(value)
  if (text !== undefined) {
    target[key] = text as T[K]
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined
}

function numberValue(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function stringArrayValue(value: unknown): string[] | undefined {
  if (!Array.isArray(value)) {
    return undefined
  }
  const values = value
    .map((item) => typeof item === 'string' ? item.trim() : '')
    .filter(Boolean)
  return values.length > 0 ? values : undefined
}
