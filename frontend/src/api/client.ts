/**
 * Axios 客户端与 Token 存取。
 * 请求拦截器自动附加 Authorization: Bearer {token}。
 */
import axios from 'axios'

const TOKEN_KEY = 'agent_ai_token'

/** 开发环境直连后端，避免 IDEA/Vite 代理未生效导致 404 */
const defaultBaseURL = import.meta.env.DEV ? 'http://localhost:8080' : ''

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || defaultBaseURL,
  timeout: 30000,
  withCredentials: false,
})

//  outgoing：附加 Sa-Token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// incoming：401 时清除本地 token
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearToken()
    }
    return Promise.reject(error)
  },
)

/** 登录成功后持久化 token */
export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 注销或 401 时清除 token */
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
