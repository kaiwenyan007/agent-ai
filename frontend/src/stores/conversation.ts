/**
 * 会话列表与消息 Pinia Store，供侧栏 SESSIONS 与 ChatView 共享。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
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
  /** 是否已拉取过会话列表（避免重复 bootstrap） */
  const initialized = ref(false)
  const streaming = ref(false)

  async function loadConversations() {
    conversations.value = await listConversations()
    if (!activeId.value && conversations.value.length > 0) {
      await selectConversation(conversations.value[0].id)
    }
  }

  /** 首次进入 CHAT 页时加载会话列表 */
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

  /** 切换会话并加载消息 */
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

  /** 侧栏会话标题截断（对标 agent-demo 20 字） */
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
    truncateTitle,
  }
})
