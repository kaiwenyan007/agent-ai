/**
 * SSE 流式对话 API，对应后端 POST /api/chat/stream。
 *
 * SSE 事件：
 * - delta：模型输出片段
 * - done：流结束，JSON 含 userMessageId / assistantMessageId
 * - error：调用失败
 */
import { getToken } from './client'

const defaultBaseURL = import.meta.env.DEV ? 'http://localhost:8080' : ''

function apiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || defaultBaseURL
}

/** done 事件 payload */
export interface ChatStreamDone {
  userMessageId: number
  assistantMessageId: number
}

export interface ChatStreamHandlers {
  onDelta: (text: string) => void
  onDone: (payload: ChatStreamDone) => void
  onError: (message: string) => void
}

/** 解析单个 SSE 块（event + data 行） */
function parseSseBlock(block: string, handlers: ChatStreamHandlers) {
  const lines = block.split('\n')
  let event = 'message'
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  const data = dataLines.join('\n')
  if (!data && event === 'message') {
    return
  }
  if (event === 'delta') {
    handlers.onDelta(data)
  } else if (event === 'done') {
    handlers.onDone(JSON.parse(data) as ChatStreamDone)
  } else if (event === 'error') {
    handlers.onError(data)
  }
}

/**
 * 发起流式对话。
 * 使用 fetch + ReadableStream（非 EventSource），以便携带 Authorization 头。
 */
export async function streamChat(
  conversationId: number,
  prompt: string,
  handlers: ChatStreamHandlers,
  signal?: AbortSignal,
) {
  const token = getToken()
  const response = await fetch(`${apiBaseUrl()}/api/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ conversationId, prompt }),
    signal,
  })

  if (!response.ok) {
    const text = await response.text()
    let message = text || `HTTP ${response.status}`
    try {
      const json = JSON.parse(text) as { message?: string }
      if (json.message) {
        message = json.message
      }
    } catch {
      // 非 JSON 响应，保留原始 text
    }
    throw new Error(message)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取流式响应')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  // 按 \n\n 分割 SSE 事件块
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() ?? ''
    for (const part of parts) {
      if (part.trim()) {
        parseSseBlock(part, handlers)
      }
    }
  }

  if (buffer.trim()) {
    parseSseBlock(buffer, handlers)
  }
}
