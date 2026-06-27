import type { DatabaseConnectionPreset, DatabaseConnectionPresetReq } from '@/types'

type PresetInput = Partial<DatabaseConnectionPresetReq> & Record<string, unknown>

export function normalizeDatabaseConnectionPresetPayload(input: PresetInput): DatabaseConnectionPresetReq {
  return {
    projectId: numberValue(input.projectId),
    name: trimToUndefined(input.name),
    databaseType: databaseTypeValue(input.databaseType),
    host: trimToUndefined(input.host),
    port: numberValue(input.port),
    databaseName: trimToUndefined(input.databaseName),
    schemaName: trimToNull(input.schemaName),
    tableNames: normalizeTableNames(input.tableNames)
  }
}

export function presetOptionLabel(preset: DatabaseConnectionPreset) {
  return preset.name?.trim() || presetConnectionSummary(preset)
}

export function presetConnectionSummary(preset: DatabaseConnectionPreset) {
  const endpoint = [preset.host, preset.port].filter((value) => value !== undefined && value !== null && value !== '').join(':')
  const database = [preset.databaseName, preset.schemaName].filter(Boolean).join(' / ')
  const tableCount = preset.tableNames?.length ? `，${preset.tableNames.length} 张表` : ''
  return [preset.databaseType, endpoint, database].filter(Boolean).join(' · ') + tableCount
}

function normalizeTableNames(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return []
  }
  return Array.from(new Set(
    value
      .map((item) => typeof item === 'string' ? item.trim() : '')
      .filter(Boolean)
  ))
}

function trimToUndefined(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed || undefined
}

function trimToNull(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed || null
}

function numberValue(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function databaseTypeValue(value: unknown): DatabaseConnectionPresetReq['databaseType'] {
  return value === 'postgresql' || value === 'mysql' ? value : undefined
}
