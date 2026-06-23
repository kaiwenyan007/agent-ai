import type { ApiResponse, LlmSettings, ModelsResponse } from '../types/api'
import { apiClient } from './client'
import { toApiError } from './errors'

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

/** 根据表单中的 Base URL / API Key 拉取模型列表（Key 留空则用已保存的） */
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
