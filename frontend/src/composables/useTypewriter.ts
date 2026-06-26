import { onUnmounted, ref } from 'vue'

/**
 * 打字机效果：网络 chunk 先入 buffer，再按帧逐字 reveal 到 displayed。
 * 大 chunk 一次性到达时也能平滑输出；小 chunk 则接近实时跟随。
 */
export function useTypewriter(onTick?: () => void) {
  const buffer = ref('')
  const displayed = ref('')
  let rafId = 0
  let running = false

  function tick() {
    if (buffer.value.length === 0) {
      running = false
      rafId = 0
      return
    }

    const backlog = buffer.value.length
    const step = backlog > 120 ? 12 : backlog > 40 ? 6 : backlog > 8 ? 3 : 1
    displayed.value += buffer.value.slice(0, step)
    buffer.value = buffer.value.slice(step)
    onTick?.()
    rafId = requestAnimationFrame(tick)
  }

  function enqueue(text: string) {
    if (!text) {
      return
    }
    buffer.value += text
    if (!running) {
      running = true
      rafId = requestAnimationFrame(tick)
    }
  }

  /** 流结束时立即展示剩余 buffer */
  function flush() {
    if (buffer.value) {
      displayed.value += buffer.value
      buffer.value = ''
    }
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    running = false
    onTick?.()
  }

  function reset() {
    buffer.value = ''
    displayed.value = ''
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    running = false
  }

  function isAnimating() {
    return running || buffer.value.length > 0
  }

  onUnmounted(() => {
    if (rafId) {
      cancelAnimationFrame(rafId)
    }
  })

  return { displayed, enqueue, flush, reset, isAnimating }
}
