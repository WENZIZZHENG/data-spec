import request from '@/api/request'

export function previewDatabaseRules(projectId: number) {
  return request.get<unknown, string>('/ai-context/database-rules', {
    params: { projectId }
  })
}

export function previewFieldCatalog(projectId: number) {
  return request.get<unknown, string>('/ai-context/field-catalog', {
    params: { projectId }
  })
}

export function previewRulesYaml(projectId: number) {
  return request.get<unknown, string>('/ai-context/rules-yaml', {
    params: { projectId }
  })
}

export function downloadAiContextPackage(projectId: number) {
  return request.get<unknown, Blob>('/ai-context/package/download', {
    params: { projectId },
    responseType: 'blob'
  })
}
