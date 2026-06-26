/**
 * 鉴权相关 API，对应后端 /api/auth。
 */
import type { ApiResponse, LoginResponse } from '../types/api'
import { apiClient, clearToken, saveToken } from './client'
import { toApiError } from './errors'

/** POST /api/auth/login — 登录并保存 token */
export async function login(username: string, password: string) {
  try {
    const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', {
      username,
      password,
    })
    if (data.code !== 0 || !data.data?.token) {
      throw new Error(data.message || '登录失败')
    }
    saveToken(data.data.token)
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '登录失败'))
  }
}

/** POST /api/auth/register — 仅注册，不自动登录 */
export async function register(username: string, password: string) {
  try {
    const { data } = await apiClient.post<ApiResponse<null>>('/api/auth/register', {
      username,
      password,
    })
    if (data.code !== 0) {
      throw new Error(data.message || '注册失败')
    }
  } catch (error) {
    throw new Error(toApiError(error, '注册失败'))
  }
}

/** POST /api/auth/logout — 注销并清除本地 token */
export async function logout() {
  try {
    await apiClient.post('/api/auth/logout')
  } finally {
    clearToken()
  }
}

/** GET /api/auth/me — 恢复会话时校验 token 有效性 */
export async function fetchProfile() {
  try {
    const { data } = await apiClient.get<ApiResponse<{ id: number; username: string }>>('/api/auth/me')
    if (data.code !== 0) {
      throw new Error(data.message || '获取用户信息失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '获取用户信息失败'))
  }
}
