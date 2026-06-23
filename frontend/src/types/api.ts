export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  username: string
  userId: number
}

export interface Conversation {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  role: string
  content: string
  createdAt: string
}

export interface LlmSettings {
  baseUrl: string
  model: string
  apiKeyMasked: string
  configured: boolean
}

export interface ModelsResponse {
  models: string[]
  fromRemote: boolean
}
