import request from '@/api/request'
import type { AiTaskProfileCatalog, AiTaskProfileDetail } from '@/types'

export function listAiProfiles(projectId?: number | null, profile?: string | null) {
  return request.get<unknown, AiTaskProfileCatalog>('/ai-profiles', {
    params: {
      projectId: projectId ?? undefined,
      profile: profile || undefined
    }
  })
}

export function getAiProfile(profileOrTaskType: string, projectId?: number | null) {
  return request.get<unknown, AiTaskProfileDetail>(`/ai-profiles/${encodeURIComponent(profileOrTaskType)}`, {
    params: {
      projectId: projectId ?? undefined
    }
  })
}
