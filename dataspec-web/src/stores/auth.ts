import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentAuth } from '@/api/auth'
import type { AuthMe } from '@/types'
import {
  clearAuthStorage,
  readAuthPrincipal,
  readAuthToken,
  writeAuthPrincipal,
  writeAuthToken
} from '@/api/authStorage'
import type { StoredAuthPrincipal } from '@/api/authStorage'

/** API token 登录态。安全模式关闭时可保持空 token，本地请求仍会正常工作。 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(readAuthToken())
  const principal = ref<StoredAuthPrincipal | null>(readAuthPrincipal())
  const loading = ref(false)
  const loginDialogVisible = ref(false)

  const hasToken = computed(() => Boolean(token.value))
  const operatorName = computed(() => principal.value?.operatorName ?? '')

  async function login(rawToken: string) {
    const nextToken = rawToken.trim()
    if (!nextToken) {
      throw new Error('API token 不能为空')
    }
    loading.value = true
    writeAuthToken(nextToken)
    token.value = nextToken
    try {
      const me = await getCurrentAuth()
      const normalized = normalizePrincipal(me)
      principal.value = normalized
      writeAuthPrincipal(normalized)
      loginDialogVisible.value = false
    } catch (error) {
      logout()
      throw error
    } finally {
      loading.value = false
    }
  }

  async function restore() {
    if (!token.value) {
      return
    }
    loading.value = true
    try {
      const me = await getCurrentAuth()
      const normalized = normalizePrincipal(me)
      principal.value = normalized
      writeAuthPrincipal(normalized)
    } catch {
      logout()
    } finally {
      loading.value = false
    }
  }

  function logout() {
    clearAuthStorage()
    token.value = ''
    principal.value = null
  }

  function openLoginDialog() {
    loginDialogVisible.value = true
  }

  function handleAuthCleared() {
    token.value = ''
    principal.value = null
    loginDialogVisible.value = true
  }

  function normalizePrincipal(me: AuthMe): StoredAuthPrincipal {
    return {
      operatorName: me.operatorName ?? '',
      allProjects: me.allProjects ?? false,
      projectIds: me.projectIds ?? []
    }
  }

  return {
    token,
    principal,
    loading,
    loginDialogVisible,
    hasToken,
    operatorName,
    login,
    restore,
    logout,
    openLoginDialog,
    handleAuthCleared
  }
})
