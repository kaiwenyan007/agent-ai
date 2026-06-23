import { defineStore } from 'pinia'
import { ref } from 'vue'
import { streamChat } from '../api/chat'
import {
  createConversation,
  deleteConversation,
  listConversations,
  listMessages,
} from '../api/conversation'
import type { ChatMessage, Conversation } from '../types/api'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<Conversation[]>([])
  const activeId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const initialized = ref(false)
  const streaming = ref(false)

  async function loadConversations() {
    conversations.value = await listConversations()
    if (!activeId.value && conversations.value.length > 0) {
      await selectConversation(conversations.value[0].id)
    }
  }

  async function bootstrap() {
    if (initialized.value) {
      return
    }
    loading.value = true
    try {
      await loadConversations()
      initialized.value = true
    } finally {
      loading.value = false
    }
  }

  async function refreshConversations() {
    conversations.value = await listConversations()
  }

  async function selectConversation(id: number) {
    activeId.value = id
    messages.value = await listMessages(id)
  }

  async function createNew() {
    const created = await createConversation()
    conversations.value.unshift(created)
    activeId.value = created.id
    messages.value = []
  }

  async function deleteActive() {
    if (!activeId.value) {
      return
    }
    const id = activeId.value
    await deleteConversation(id)
    conversations.value = conversations.value.filter((item) => item.id !== id)
    activeId.value = conversations.value[0]?.id ?? null
    messages.value = activeId.value ? await listMessages(activeId.value) : []
  }

  async function sendChatMessage(
    content: string,
    handlers?: { onDelta?: (chunk: string) => void; onStatus?: (status: string) => void },
  ) {
    if (!activeId.value) {
      await createNew()
    }
    if (!activeId.value) {
      return
    }

    const conversationId = activeId.value
    streaming.value = true
    handlers?.onStatus?.('正在连接模型...')

    const tempAssistantId = -Date.now()
    const assistantMsg: ChatMessage = {
      id: tempAssistantId,
      conversationId,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString(),
    }

    try {
      await streamChat(conversationId, content, {
        onDelta: (chunk) => {
          if (assistantMsg.content === '') {
            handlers?.onStatus?.('')
          }
          assistantMsg.content += chunk
          handlers?.onDelta?.(chunk)
        },
        onDone: async () => {
          await selectConversation(conversationId)
          await refreshConversations()
        },
        onError: (message) => {
          throw new Error(message)
        },
      })
    } catch (error) {
      await selectConversation(conversationId)
      await refreshConversations()
      throw error
    } finally {
      streaming.value = false
    }
  }

  function truncateTitle(title: string, max = 20) {
    return title.length > max ? `${title.slice(0, max)}…` : title
  }

  return {
    conversations,
    activeId,
    messages,
    loading,
    initialized,
    streaming,
    bootstrap,
    loadConversations,
    refreshConversations,
    selectConversation,
    createNew,
    deleteActive,
    sendChatMessage,
    truncateTitle,
  }
})
