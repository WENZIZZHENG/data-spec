export const AUTH_TOKEN_KEY = 'dataspec.apiToken'
export const AUTH_PRINCIPAL_KEY = 'dataspec.authPrincipal'
export const AUTH_CLEARED_EVENT = 'dataspec-auth-cleared'

export interface StoredAuthPrincipal {
  operatorName: string
  allProjects: boolean
  projectIds: number[]
}

function storageAvailable() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

export function readAuthToken() {
  if (!storageAvailable()) {
    return ''
  }
  return window.localStorage.getItem(AUTH_TOKEN_KEY) ?? ''
}

export function writeAuthToken(token: string) {
  if (storageAvailable()) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token)
  }
}

export function readAuthPrincipal(): StoredAuthPrincipal | null {
  if (!storageAvailable()) {
    return null
  }
  const raw = window.localStorage.getItem(AUTH_PRINCIPAL_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as StoredAuthPrincipal
  } catch {
    window.localStorage.removeItem(AUTH_PRINCIPAL_KEY)
    return null
  }
}

export function writeAuthPrincipal(principal: StoredAuthPrincipal) {
  if (storageAvailable()) {
    window.localStorage.setItem(AUTH_PRINCIPAL_KEY, JSON.stringify(principal))
  }
}

export function clearAuthStorage() {
  if (storageAvailable()) {
    window.localStorage.removeItem(AUTH_TOKEN_KEY)
    window.localStorage.removeItem(AUTH_PRINCIPAL_KEY)
  }
}
