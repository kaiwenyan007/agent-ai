<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getTokenSummary } from '../api/stats'
import { toApiError } from '../api/errors'

const loading = ref(true)
const error = ref('')
const summary = ref({
  totalTokens: 0,
  promptTokens: 0,
  completionTokens: 0,
  estimatedCost: 0,
})
const recent = ref<
  Array<{
    id: number
    model: string
    totalTokens: number
    estimatedCost: number
    createdAt: string
  }>
>([])

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await getTokenSummary()
    summary.value = {
      totalTokens: data.totalTokens,
      promptTokens: data.promptTokens,
      completionTokens: data.completionTokens,
      estimatedCost: data.estimatedCost,
    }
    recent.value = data.recent
  } catch (e) {
    error.value = toApiError(e, '加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-content">
    <div class="page-inner">
      <h2 class="hack-title">📊 TOKEN & RAG METRICS</h2>
      <p class="hack-caption">// 用量统计 · 模型分布 · RAG 检索日志</p>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="loading" class="muted">// LOADING...</p>

      <template v-else>
        <div class="hack-metric-grid top-metrics">
          <div class="hack-metric">
            <div class="hack-metric-label">TOTAL TOKENS</div>
            <div class="hack-metric-value">{{ summary.totalTokens.toLocaleString() }}</div>
          </div>
          <div class="hack-metric">
            <div class="hack-metric-label">PROMPT</div>
            <div class="hack-metric-value">{{ summary.promptTokens.toLocaleString() }}</div>
          </div>
          <div class="hack-metric">
            <div class="hack-metric-label">COMPLETION</div>
            <div class="hack-metric-value">{{ summary.completionTokens.toLocaleString() }}</div>
          </div>
          <div class="hack-metric">
            <div class="hack-metric-label">COST ¥</div>
            <div class="hack-metric-value">{{ summary.estimatedCost.toFixed(4) }}</div>
          </div>
        </div>

        <hr class="hack-divider" />

        <h3 class="section-title">RECENT</h3>
        <section v-if="recent.length === 0" class="hack-panel empty-panel">
          [EMPTY] 暂无使用记录 · 发起对话后将自动统计
        </section>
        <section v-else class="hack-panel table-panel">
          <div v-for="item in recent" :key="item.id" class="table-row">
            <span class="model">{{ item.model }}</span>
            <span class="tokens">{{ item.totalTokens }} tokens</span>
            <span class="cost">¥{{ item.estimatedCost.toFixed(4) }}</span>
            <span class="time muted">{{ item.createdAt }}</span>
          </div>
        </section>

        <hr class="hack-divider" />

        <h3 class="section-title">RAG LOG</h3>
        <section class="hack-panel empty-panel">[EMPTY] 暂无 RAG 检索记录 · v0.7 接入知识库后开始记录</section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.top-metrics {
  margin-top: 1rem;
}

.section-title {
  margin: 0 0 0.75rem;
  color: var(--hack-green);
  letter-spacing: 0.06em;
  font-size: 0.95rem;
}

.empty-panel {
  color: var(--hack-muted);
  font-size: 0.82rem;
  margin-bottom: 1.25rem;
}

.table-panel {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 0.6fr 1fr;
  gap: 0.5rem;
  font-size: 0.82rem;
  border-bottom: 1px solid var(--hack-border);
  padding-bottom: 0.35rem;
}

.model {
  color: var(--hack-cyan);
}

@media (max-width: 768px) {
  .table-row {
    grid-template-columns: 1fr;
  }
}
</style>
