/**
 * 会话列表与消息 Pinia Store，供侧栏 SESSIONS 与 ChatView 共享。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createConversation,
  deleteConversation,
  listConversations,
  listMessagesPage,
  MESSAGE_PAGE_SIZE,
} from '../api/conversation'
import type { ChatMessage, Conversation } from '../types/api'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<Conversation[]>([])
  const activeId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const loading = ref(false)
  const loadingMore = ref(false)
  const hasMoreMessages = ref(false)
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

  /** 加载会话最新一页消息 */
  async function selectConversation(id: number) {
    activeId.value = id
    loading.value = true
    try {
      const page = await listMessagesPage(id, { limit: MESSAGE_PAGE_SIZE })
      messages.value = page.messages
      hasMoreMessages.value = page.hasMore
    } finally {
      loading.value = false
    }
  }

  /** 上滑加载更早消息，返回 prepend 前的 scrollHeight 供恢复滚动位置 */
  async function loadOlderMessages(): Promise<number | null> {
    if (!activeId.value || loadingMore.value || !hasMoreMessages.value) {
      return null
    }
    const firstReal = messages.value.find((item) => item.id > 0)
    if (!firstReal) {
      return null
    }

    loadingMore.value = true
    try {
      const page = await listMessagesPage(activeId.value, {
        limit: MESSAGE_PAGE_SIZE,
        beforeId: firstReal.id,
      })
      if (page.messages.length === 0) {
        hasMoreMessages.value = false
        return null
      }
      messages.value = [...page.messages, ...messages.value]
      hasMoreMessages.value = page.hasMore
      return page.messages.length
    } finally {
      loadingMore.value = false
    }
  }

  /** 流式结束后同步服务端最新消息（不丢弃已加载的历史） */
  async function syncAfterStream() {
    if (!activeId.value) {
      return
    }
    const page = await listMessagesPage(activeId.value, { limit: MESSAGE_PAGE_SIZE })
    if (!hasMoreMessages.value) {
      messages.value = page.messages
      hasMoreMessages.value = page.hasMore
      return
    }
    const realMessages = messages.value.filter((item) => item.id > 0)
    const lastId = realMessages.length > 0 ? realMessages[realMessages.length - 1].id : 0
    const newer = page.messages.filter((item) => item.id > lastId)
    if (newer.length > 0) {
      messages.value = [...realMessages, ...newer]
    } else {
      messages.value = realMessages
    }
  }

  async function createNew() {
    const created = await createConversation()
    conversations.value.unshift(created)
    activeId.value = created.id
    messages.value = []
    hasMoreMessages.value = false
  }

  async function deleteActive() {
    if (!activeId.value) {
      return
    }
    const id = activeId.value
    await deleteConversation(id)
    conversations.value = conversations.value.filter((item) => item.id !== id)
    activeId.value = conversations.value[0]?.id ?? null
    if (activeId.value) {
      await selectConversation(activeId.value)
    } else {
      messages.value = []
      hasMoreMessages.value = false
    }
  }

  function truncateTitle(title: string, max = 20) {
    return title.length > max ? `${title.slice(0, max)}…` : title
  }

  function appendMessageChunk(messageId: number, chunk: string) {
    const idx = messages.value.findIndex((item) => item.id === messageId)
    if (idx < 0) {
      return
    }
    const current = messages.value[idx]
    messages.value[idx] = { ...current, content: current.content + chunk }
  }

  function setMessageContent(messageId: number, content: string) {
    const idx = messages.value.findIndex((item) => item.id === messageId)
    if (idx < 0) {
      return
    }
    messages.value[idx] = { ...messages.value[idx], content }
  }

  return {
    conversations,
    activeId,
    messages,
    loading,
    loadingMore,
    hasMoreMessages,
    initialized,
    streaming,
    bootstrap,
    loadConversations,
    refreshConversations,
    selectConversation,
    loadOlderMessages,
    syncAfterStream,
    createNew,
    deleteActive,
    truncateTitle,
    appendMessageChunk,
    setMessageContent,
  }
})
