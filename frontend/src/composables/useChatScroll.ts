import { nextTick, ref, type Ref } from 'vue'

const BOTTOM_THRESHOLD = 100

/**
 * 聊天区滚动：仅在用户贴底时跟随新消息；上滑不强行拉到底部。
 */
export function useChatScroll(containerRef: Ref<HTMLElement | null>) {
  const isAtBottom = ref(true)
  const showJumpToBottom = ref(false)

  function distanceFromBottom() {
    const el = containerRef.value
    if (!el) {
      return 0
    }
    return el.scrollHeight - el.scrollTop - el.clientHeight
  }

  function updateScrollState() {
    const distance = distanceFromBottom()
    isAtBottom.value = distance <= BOTTOM_THRESHOLD
    showJumpToBottom.value = distance > BOTTOM_THRESHOLD
  }

  /** force=true 时无条件滚到底（发送新消息、切换会话） */
  async function scrollToBottom(force = false) {
    await nextTick()
    const el = containerRef.value
    if (!el) {
      return
    }
    if (force || isAtBottom.value) {
      el.scrollTop = el.scrollHeight
      isAtBottom.value = true
      showJumpToBottom.value = false
    } else {
      showJumpToBottom.value = true
    }
  }

  /** 顶部 prepend 历史消息后保持视口位置不跳动 */
  async function preserveScrollOnPrepend(prevScrollHeight: number) {
    await nextTick()
    const el = containerRef.value
    if (!el) {
      return
    }
    el.scrollTop += el.scrollHeight - prevScrollHeight
    updateScrollState()
  }

  return {
    isAtBottom,
    showJumpToBottom,
    scrollToBottom,
    preserveScrollOnPrepend,
    updateScrollState,
  }
}
