import type { ApiResponse, Conversation, ChatMessage } from '../types/api'
import { apiClient } from './client'
import { toApiError } from './errors'

export async function listConversations() {
  try {
    const { data } = await apiClient.get<ApiResponse<Conversation[]>>('/api/conversations')
    if (data.code !== 0) {
      throw new Error(data.message || '加载会话失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '加载会话失败'))
  }
}

export async function createConversation(title?: string) {
  try {
    const { data } = await apiClient.post<ApiResponse<Conversation>>(
      '/api/conversations',
      title ? { title } : {},
    )
    if (data.code !== 0) {
      throw new Error(data.message || '创建会话失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '创建会话失败'))
  }
}

export async function deleteConversation(id: number) {
  try {
    const { data } = await apiClient.delete<ApiResponse<null>>(`/api/conversations/${id}`)
    if (data.code !== 0) {
      throw new Error(data.message || '删除会话失败')
    }
  } catch (error) {
    throw new Error(toApiError(error, '删除会话失败'))
  }
}

export async function listMessages(conversationId: number) {
  try {
    const { data } = await apiClient.get<ApiResponse<ChatMessage[]>>(
      `/api/conversations/${conversationId}/messages`,
    )
    if (data.code !== 0) {
      throw new Error(data.message || '加载消息失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '加载消息失败'))
  }
}

export async function appendMessage(conversationId: number, content: string) {
  try {
    const { data } = await apiClient.post<ApiResponse<ChatMessage>>(
      `/api/conversations/${conversationId}/messages`,
      { content, role: 'user' },
    )
    if (data.code !== 0) {
      throw new Error(data.message || '发送消息失败')
    }
    return data.data
  } catch (error) {
    throw new Error(toApiError(error, '发送消息失败'))
  }
}
