import axios from 'axios'
import type { ApiResponse } from '../types/api'

export function toApiError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    if (status === 404) {
      return '接口 404：请确认 agent-server 已启动，且已编译包含 v0.4 的会话接口后重新运行'
    }
    if (status === 401) {
      return '登录已过期，请重新登录'
    }
    if (status === 502 || status === 503) {
      return '无法连接后端，请先启动 agent-server（端口 8080）'
    }
    const body = error.response?.data as Partial<ApiResponse<unknown>> | undefined
    if (body?.message) {
      return body.message
    }
    if (error.code === 'ERR_NETWORK') {
      return '网络错误：请确认 agent-server 已在 http://localhost:8080 启动'
    }
    return error.message || fallback
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallback
}
