/**
 * LLM 配置 API，对应后端 /api/settings。
 */
import type { ApiResponse, LlmSettings, ModelsResponse } from '../types/api'
import { apiClient } from './client'
import { toApiError } from './errors'

/** GET /api/settings/llm — 读取配置（Key 脱敏） */
export async function getLlmSettings() {
  try {
    const { data } = await apiClient.get<ApiResponse<LlmSettings>>('/api/settings/llm')
    if (data.code !== 0) {
      throw new Error(data.message || '加载配置失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '加载配置失败'))
  }
}

/**
 * PUT /api/settings/llm — 保存配置。
 * apiKey 留空表示不修改已保存的 Key。
 */
export async function saveLlmSettings(payload: {
  apiKey?: string
  baseUrl: string
  model: string
}) {
  try {
    const { data } = await apiClient.put<ApiResponse<LlmSettings>>('/api/settings/llm', payload)
    if (data.code !== 0) {
      throw new Error(data.message || '保存配置失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '保存配置失败'))
  }
}

/**
 * POST /api/settings/models — 按表单 baseUrl / apiKey 拉取模型列表。
 * Key 留空则使用服务端已保存的 Key。
 */
export async function fetchModels(payload: { baseUrl: string; apiKey?: string }) {
  try {
    const { data } = await apiClient.post<ApiResponse<ModelsResponse>>('/api/settings/models', payload)
    if (data.code !== 0) {
      throw new Error(data.message || '拉取模型失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '拉取模型失败'))
  }
}
