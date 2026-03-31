import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

interface AccountInfo {
  username: string
  authorities: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('ap_token') || '')
  const account = ref<AccountInfo | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => account.value?.authorities?.includes('ROLE_ADMIN') ?? false)
  const isOperator = computed(() => account.value?.authorities?.includes('ROLE_OPERATOR') ?? false)

  async function login(username: string, password: string) {
    const response = await request.post('/authenticate', { username, password })
    const idToken = response.data.id_token
    token.value = idToken
    localStorage.setItem('ap_token', idToken)
    await fetchAccount()
  }

  async function fetchAccount() {
    try {
      const response = await request.get('/account')
      account.value = response.data
    } catch {
      account.value = null
    }
  }

  function logout() {
    token.value = ''
    account.value = null
    localStorage.removeItem('ap_token')
  }

  return {
    token,
    account,
    isAuthenticated,
    isAdmin,
    isOperator,
    login,
    fetchAccount,
    logout,
  }
})
