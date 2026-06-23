<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { streamChat } from '../api/chat'
import { useConversationStore } from '../stores/conversation'
import { useSettingsStore } from '../stores/settings'
import { toApiError } from '../api/errors'

const conv = useConversationStore()
const settings = useSettingsStore()

const inputValue = ref('')
const sending = ref(false)
const error = ref('')
const streamStatus = ref('')
const contentRef = ref<HTMLElement | null>(null)

const apiReady = computed(() => settings.configured)
const canSend = computed(() => apiReady.value && !sending.value && !conv.loading && !conv.streaming)

async function scrollToBottom() {
  await nextTick()
  if (contentRef.value) {
    contentRef.value.scrollTop = contentRef.value.scrollHeight
  }
}

async function onSubmit() {
  const text = inputValue.value.trim()
  if (!text || !canSend.value) {
    return
  }

  sending.value = true
  error.value = ''
  streamStatus.value = '正在连接模型...'
  inputValue.value = ''

  const conversationId = conv.activeId
  if (!conversationId) {
    await conv.createNew()
  }
  if (!conv.activeId) {
    sending.value = false
    return
  }

  const tempUserId = -Date.now()
  conv.messages.push({
    id: tempUserId,
    conversationId: conv.activeId,
    role: 'user',
    content: text,
    createdAt: new Date().toISOString(),
  })

  const tempAssistantId = tempUserId - 1
  const assistantMsg = {
    id: tempAssistantId,
    conversationId: conv.activeId,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
  }
  conv.messages.push(assistantMsg)
  await scrollToBottom()

  try {
    await streamChat(conv.activeId, text, {
      onDelta: (chunk) => {
        if (streamStatus.value) {
          streamStatus.value = ''
        }
        assistantMsg.content += chunk
        scrollToBottom()
      },
      onDone: async () => {
        await conv.selectConversation(conv.activeId!)
        await conv.refreshConversations()
        await scrollToBottom()
      },
      onError: (message) => {
        throw new Error(message)
      },
    })
    streamStatus.value = ''
  } catch (e) {
    error.value = toApiError(e, '对话失败')
    streamStatus.value = ''
    if (conv.activeId) {
      await conv.selectConversation(conv.activeId)
    }
  } finally {
    sending.value = false
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    onSubmit()
  }
}

watch(
  () => conv.messages.length,
  () => {
    scrollToBottom()
  },
)

watch(
  () => conv.activeId,
  () => {
    scrollToBottom()
  },
)

onMounted(async () => {
  try {
    await settings.loadLlm()
    await conv.bootstrap()
    await scrollToBottom()
  } catch (e) {
    error.value = toApiError(e, '加载失败')
  }
})
</script>

<template>
  <div class="page-content chat-page">
    <div class="page-inner">
      <h2 class="hack-title">💬 NEURAL CHAT</h2>
      <p v-if="!apiReady" class="warn-text">[WARN] 请先在 API CONFIG 中配置 Key / URL / Model</p>
      <p v-else class="hack-caption">// Spring AI 流式对话 · 多轮上下文 · SSE</p>
      <p v-if="error" class="error-text">{{ error }}</p>

      <div ref="contentRef" class="message-list">
        <p v-if="conv.loading" class="agent-status">⏳ <span>加载会话...</span></p>

        <article
          v-for="msg in conv.messages"
          :key="msg.id"
          class="chat-message"
          :class="msg.role"
        >
          <div class="chat-role">{{ msg.role === 'user' ? 'USER' : 'ASSISTANT' }}</div>
          <div class="chat-content">{{ msg.content || (sending && msg.role === 'assistant' ? '...' : '') }}</div>
        </article>

        <p v-if="streamStatus" class="agent-status">⏳ <span>{{ streamStatus }}</span></p>
      </div>
    </div>

    <div class="chat-input-bar">
      <div class="chat-input-wrap">
        <input
          v-model="inputValue"
          class="hack-input chat-input"
          :disabled="!apiReady || sending"
          placeholder=">> 输入指令..."
          @keydown="onKeydown"
        />
        <button
          type="button"
          class="hack-btn primary send-btn"
          :disabled="!canSend || !inputValue.trim()"
          @click="onSubmit"
        >
          {{ sending ? 'SEND...' : 'SEND' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100vh;
  padding-bottom: 0;
}

.page-inner {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.message-list {
  flex: 1;
  min-height: 200px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-bottom: 1rem;
}

.chat-input-bar {
  flex-shrink: 0;
  border-top: 1px solid var(--hack-border);
  background: rgba(6, 8, 13, 0.92);
  padding: 0.85rem 1.75rem 1rem;
}

.chat-input-wrap {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  gap: 0.5rem;
}

.chat-input {
  flex: 1;
}

.send-btn {
  width: auto;
  min-width: 5.5rem;
}
</style>
