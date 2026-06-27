import request from '@/api/request'
import type { AiContextScopeParams } from '@/utils/aiContextScope'

export function previewDatabaseRules(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/database-rules', {
    params: scopeParams(projectId, options)
  })
}

export function previewFieldCatalog(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/field-catalog', {
    params: scopeParams(projectId, options)
  })
}

export function previewRulesYaml(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, string>('/ai-context/rules-yaml', {
    params: snapshotParams(projectId, options)
  })
}

export function downloadAiContextPackage(projectId: number, options: AiContextScopeParams = {}) {
  return request.get<unknown, Blob>('/ai-context/package/download', {
    params: scopeParams(projectId, options),
    responseType: 'blob'
  })
}

function scopeParams(projectId: number, options: AiContextScopeParams) {
  return {
    projectId,
    ...(options.scope ? { scope: options.scope } : {}),
    ...(options.query ? { query: options.query } : {}),
    ...(options.status ? { status: options.status } : {}),
    ...(options.limit ? { limit: options.limit } : {}),
    ...(options.snapshotId ? { snapshotId: options.snapshotId } : {}),
    ...(options.snapshotVersion ? { snapshotVersion: options.snapshotVersion } : {})
  }
}

function snapshotParams(projectId: number, options: AiContextScopeParams) {
  return {
    projectId,
    ...(options.snapshotId ? { snapshotId: options.snapshotId } : {}),
    ...(options.snapshotVersion ? { snapshotVersion: options.snapshotVersion } : {})
  }
}
