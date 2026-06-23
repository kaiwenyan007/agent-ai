import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchProfile, login as apiLogin, logout as apiLogout, register as apiRegister } from '../api/auth'
import { getToken } from '../api/client'

export const useAuthStore = defineStore('auth', () => {
  const username = ref<string | null>(null)
  const userId = ref<number | null>(null)
  const loading = ref(false)

  async function login(usernameInput: string, password: string) {
    loading.value = true
    try {
      const result = await apiLogin(usernameInput, password)
      username.value = result.username
      userId.value = result.userId
    } finally {
      loading.value = false
    }
  }

  async function register(usernameInput: string, password: string) {
    loading.value = true
    try {
      await apiRegister(usernameInput, password)
      await login(usernameInput, password)
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    await apiLogout()
    username.value = null
    userId.value = null
  }

  async function restoreSession() {
    if (!getToken()) {
      return false
    }
    try {
      const profile = await fetchProfile()
      username.value = profile.username
      userId.value = profile.id
      return true
    } catch {
      return false
    }
  }

  return {
    username,
    userId,
    loading,
    login,
    register,
    logout,
    restoreSession,
  }
})
