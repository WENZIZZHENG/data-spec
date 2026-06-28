const AI_PROFILE_KEY_PREFIX = 'dataspec.aiProfile'

export function aiProfileStorageKey(projectId?: number | null) {
  return `${AI_PROFILE_KEY_PREFIX}.${projectId ?? 'global'}`
}

export function readSelectedAiProfile(projectId?: number | null) {
  if (!canUseStorage()) {
    return ''
  }
  return window.localStorage.getItem(aiProfileStorageKey(projectId)) || ''
}

export function saveSelectedAiProfile(projectId: number | null | undefined, profileId?: string | null) {
  if (!canUseStorage()) {
    return
  }
  const key = aiProfileStorageKey(projectId)
  const value = profileId?.trim()
  if (value) {
    window.localStorage.setItem(key, value)
  } else {
    window.localStorage.removeItem(key)
  }
}

export function resolveSelectedAiProfile(storedProfile: string, fallbackProfile?: string | null) {
  return storedProfile.trim() || fallbackProfile || ''
}

function canUseStorage() {
  return typeof window !== 'undefined' && Boolean(window.localStorage)
}
