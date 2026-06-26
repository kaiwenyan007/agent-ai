/**
 * Token 统计 API，对应后端 GET /api/stats/tokens。
 */
import type { ApiResponse } from '../types/api'
import { apiClient } from './client'
import { toApiError } from './errors'

/** GET /api/stats/tokens 响应 data */
export interface TokenSummary {
  totalTokens: number
  promptTokens: number
  completionTokens: number
  estimatedCost: number
  recent: Array<{
    id: number
    model: string
    totalTokens: number
    estimatedCost: number
    createdAt: string
  }>
}

/** 获取当前用户 Token 汇总与最近 20 条记录 */
export async function getTokenSummary() {
  try {
    const { data } = await apiClient.get<ApiResponse<TokenSummary>>('/api/stats/tokens')
    if (data.code !== 0) {
      throw new Error(data.message || '加载统计失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '加载统计失败'))
  }
}
