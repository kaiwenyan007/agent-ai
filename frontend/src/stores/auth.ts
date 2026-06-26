/**
 * 登录态 Pinia Store：token 存 localStorage，profile 存内存。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchProfile, login as apiLogin, logout as apiLogout, register as apiRegister } from '../api/auth'
import { getToken } from '../api/client'

export const useAuthStore = defineStore('auth', () => {
  const username = ref<string | null>(null)
  const userId = ref<number | null>(null)
  const loading = ref(false)

  /** 登录并更新本地 profile */
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

  /** 注册成功后自动登录 */
  async function register(usernameInput: string, password: string) {
    loading.value = true
    try {
      await apiRegister(usernameInput, password)
      await login(usernameInput, password)
    } finally {
      loading.value = false
    }
  }

  /** 调用后端 logout 并清空本地状态 */
  async function logout() {
    await apiLogout()
    username.value = null
    userId.value = null
  }

  /**
   * 应用启动时根据 localStorage token 恢复会话。
   * @returns false 表示无 token 或 token 已失效
   */
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
