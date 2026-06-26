<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { streamChat } from '../api/chat'
import { useChatScroll } from '../composables/useChatScroll'
import { useTypewriter } from '../composables/useTypewriter'
import { useConversationStore } from '../stores/conversation'
import { useSettingsStore } from '../stores/settings'
import { toApiError } from '../api/errors'

const conv = useConversationStore()
const settings = useSettingsStore()

const inputValue = ref('')
const sending = ref(false)
const error = ref('')
const contentRef = ref<HTMLElement | null>(null)

const streamingMessageId = ref<number | null>(null)
const streamStatus = ref('')
const hasStreamContent = ref(false)

const {
  showJumpToBottom,
  scrollToBottom,
  preserveScrollOnPrepend,
  updateScrollState,
} = useChatScroll(contentRef)

const typewriter = useTypewriter(() => {
  scrollToBottom(false)
})

const apiReady = computed(() => settings.configured)
const canSend = computed(() => apiReady.value && !sending.value && !conv.loading)

function isStreamingMessage(messageId: number) {
  return sending.value && streamingMessageId.value === messageId
}

/** 滚到顶部附近时自动加载更早消息 */
async function loadOlder() {
  if (!conv.hasMoreMessages || conv.loadingMore) {
    return
  }
  const el = contentRef.value
  const prevHeight = el?.scrollHeight ?? 0
  await conv.loadOlderMessages()
  if (prevHeight > 0) {
    await preserveScrollOnPrepend(prevHeight)
  }
}

function onMessageScroll() {
  updateScrollState()
  const el = contentRef.value
  if (el && el.scrollTop <= 80) {
    loadOlder()
  }
}

async function onSubmit() {
  const text = inputValue.value.trim()
  if (!text || !canSend.value) {
    return
  }

  sending.value = true
  error.value = ''
  streamStatus.value = '正在发送请求…'
  hasStreamContent.value = false
  typewriter.reset()
  inputValue.value = ''

  if (!conv.activeId) {
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
  streamingMessageId.value = tempAssistantId
  conv.messages.push({
    id: tempAssistantId,
    conversationId: conv.activeId,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
  })
  await scrollToBottom(true)

  try {
    await streamChat(conv.activeId, text, {
      onStatus: (message) => {
        if (!hasStreamContent.value) {
          streamStatus.value = message
          scrollToBottom(false)
        }
      },
      onDelta: (chunk) => {
        if (!hasStreamContent.value) {
          hasStreamContent.value = true
          streamStatus.value = ''
        }
        typewriter.enqueue(chunk)
        conv.appendMessageChunk(tempAssistantId, chunk)
      },
      onDone: async () => {
        typewriter.flush()
        streamStatus.value = ''
        await conv.syncAfterStream()
        await conv.refreshConversations()
        await scrollToBottom(false)
      },
      onError: (message) => {
        throw new Error(message)
      },
    })
  } catch (e) {
    error.value = toApiError(e, '对话失败')
    streamStatus.value = ''
    if (conv.activeId) {
      await conv.syncAfterStream()
    }
  } finally {
    sending.value = false
    streamingMessageId.value = null
    hasStreamContent.value = false
    typewriter.reset()
    updateScrollState()
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    onSubmit()
  }
}

watch(
  () => conv.activeId,
  async () => {
    await nextTick()
    await scrollToBottom(true)
  },
)

onMounted(async () => {
  try {
    await settings.loadLlm()
    await conv.bootstrap()
    await scrollToBottom(true)
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
      <p v-else class="hack-caption">// 上滑加载历史 · 贴底时跟随输出</p>
      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="message-scroll-wrap">
        <div ref="contentRef" class="message-list" @scroll="onMessageScroll">
          <div v-if="conv.hasMoreMessages" class="load-more-hint">
            <button
              type="button"
              class="load-more-btn"
              :disabled="conv.loadingMore"
              @click="loadOlder"
            >
              {{ conv.loadingMore ? '加载中…' : '↑ 上滑或点击加载更早消息' }}
            </button>
          </div>

          <p v-if="conv.loading" class="agent-status">⏳ <span>加载会话...</span></p>

          <article
            v-for="msg in conv.messages"
            :key="msg.id"
            class="chat-message"
            :class="[msg.role, { streaming: isStreamingMessage(msg.id) }]"
          >
            <div class="chat-role">{{ msg.role === 'user' ? 'USER' : 'ASSISTANT' }}</div>

            <template v-if="isStreamingMessage(msg.id)">
              <p v-if="streamStatus && !hasStreamContent" class="stream-phase">
                <span class="phase-dot" />
                {{ streamStatus }}
              </p>
              <div v-if="hasStreamContent || typewriter.displayed" class="chat-content">
                {{ typewriter.displayed }}<span class="cursor-blink">▍</span>
              </div>
              <p v-else-if="!streamStatus" class="stream-phase">
                <span class="phase-dot" />
                思考中…
              </p>
            </template>

            <div v-else class="chat-content">{{ msg.content }}</div>
          </article>
        </div>

        <button
          v-if="showJumpToBottom"
          type="button"
          class="jump-bottom-btn"
          @click="scrollToBottom(true)"
        >
          ↓ 回到底部
        </button>
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
  height: 100%;
  overflow: hidden;
  padding-bottom: 0;
}

.page-inner {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-scroll-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 1rem;
}

.message-list {
  flex: 1;
  min-height: 200px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-bottom: 1rem;
}

.load-more-hint {
  display: flex;
  justify-content: center;
  padding: 0.25rem 0 0.5rem;
  flex-shrink: 0;
}

.load-more-btn {
  border: 1px dashed var(--hack-border);
  background: transparent;
  color: var(--hack-muted);
  font-size: 0.78rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  cursor: pointer;
  letter-spacing: 0.03em;
}

.load-more-btn:hover:not(:disabled) {
  border-color: var(--hack-cyan);
  color: var(--hack-cyan);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.jump-bottom-btn {
  position: absolute;
  left: 50%;
  bottom: 12px;
  transform: translateX(-50%);
  z-index: 2;
  border: 1px solid var(--hack-border);
  background: rgba(13, 17, 23, 0.92);
  color: var(--hack-green);
  font-size: 0.82rem;
  padding: 0.4rem 0.85rem;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}

.jump-bottom-btn:hover {
  border-color: var(--hack-green);
  box-shadow: 0 0 12px rgba(0, 255, 65, 0.2);
}

.chat-message.streaming {
  border-color: rgba(0, 229, 255, 0.35);
  box-shadow: 0 0 12px rgba(0, 229, 255, 0.08);
}

.stream-phase {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  color: var(--hack-muted);
  font-size: 0.85rem;
  letter-spacing: 0.03em;
}

.phase-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hack-cyan);
  animation: pulse 1.2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.cursor-blink {
  color: var(--hack-green);
  animation: blink 1s step-end infinite;
  margin-left: 1px;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
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
