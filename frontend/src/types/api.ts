/**
 * 后端统一响应结构。
 * code=0 成功，非 0 失败；message 为提示信息。
 */
export interface ApiResponse<T> {
  /** 0 成功，1 业务失败 */
  code: number
  message: string
  data: T
}

/** POST /api/auth/login 响应 data */
export interface LoginResponse {
  /** Bearer Token，存入 localStorage */
  token: string
  username: string
  userId: number
}

/** GET /api/conversations 列表项 */
export interface Conversation {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

/** 单条聊天消息，role: user | assistant | system */
export interface ChatMessage {
  id: number
  conversationId: number
  role: string
  content: string
  createdAt: string
}

/** GET/PUT /api/settings/llm 响应 data */
export interface LlmSettings {
  baseUrl: string
  model: string
  /** 脱敏 Key，如 sk-a****xyz */
  apiKeyMasked: string
  /** Key + URL + Model 均已配置 */
  configured: boolean
}

/** GET/POST /api/settings/models 响应 data */
export interface ModelsResponse {
  models: string[]
  /** true=远程拉取，false=本地兜底列表 */
  fromRemote: boolean
}
