import type { ApiResponse } from '../types/api'
import { apiClient } from './client'
import { toApiError } from './errors'

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
