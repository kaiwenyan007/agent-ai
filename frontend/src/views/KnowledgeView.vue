<script setup lang="ts">
import { ref } from 'vue'

const knowledgeDir = ref('')
const docCount = ref(0)
const chunkCount = ref(0)
const lastIndexed = ref('—')
const info = ref('[INFO] 知识库 RAG 将在后续版本接入 Java 后端')

function handleSavePath() {
  info.value = '[WARN] 后端 Knowledge API 尚未实现（v0.6+），当前仅 UI 占位'
}

function handleScan() {
  info.value = '[EMPTY] 请先接入后端 SCAN 接口'
}

function handleRebuild() {
  info.value = '[EMPTY] 请先接入后端 REBUILD INDEX 接口'
}
</script>

<template>
  <div class="page-content">
    <div class="page-inner">
      <h2 class="hack-title">📚 LOCAL KNOWLEDGE</h2>
      <p class="hack-caption">// 本机 Markdown 目录 · 每人独立向量库 · 对标 agent-demo</p>

      <section class="hack-panel info-panel">
        在本机部署时，可索引你填写的 Windows 路径下的 .md 文件。修改 md 后需重建索引。
        当前 Java 版后端尚未开放 Knowledge API，页面为布局占位。
      </section>

      <section class="hack-panel form-panel">
        <div class="hack-field">
          <label>KNOWLEDGE_DIR</label>
          <input
            v-model="knowledgeDir"
            class="hack-input"
            placeholder="C:\Users\你的用户名\Documents\notes"
          />
        </div>

        <div class="action-row">
          <button type="button" class="hack-btn" @click="handleSavePath">SAVE PATH</button>
          <button type="button" class="hack-btn" @click="handleScan">SCAN</button>
          <button type="button" class="hack-btn primary" @click="handleRebuild">REBUILD INDEX</button>
        </div>

        <p class="warn-text">{{ info }}</p>
      </section>

      <hr class="hack-divider" />

      <h3 class="section-title">STATUS</h3>
      <ul class="dir-list">
        <li v-if="knowledgeDir">{{ knowledgeDir }}</li>
        <li v-else class="muted">_未配置目录_</li>
      </ul>

      <div class="hack-metric-grid">
        <div class="hack-metric">
          <div class="hack-metric-label">DOCS</div>
          <div class="hack-metric-value">{{ docCount }}</div>
        </div>
        <div class="hack-metric">
          <div class="hack-metric-label">CHUNKS</div>
          <div class="hack-metric-value">{{ chunkCount }}</div>
        </div>
        <div class="hack-metric">
          <div class="hack-metric-label">LAST INDEX</div>
          <div class="hack-metric-value metric-sm">{{ lastIndexed }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-panel {
  margin-top: 1rem;
  color: var(--hack-muted);
  font-size: 0.82rem;
  line-height: 1.55;
}

.form-panel {
  margin-top: 1rem;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.section-title {
  margin: 0;
  color: var(--hack-green);
  letter-spacing: 0.06em;
  font-size: 0.95rem;
}

.dir-list {
  margin: 0.75rem 0 1rem;
  padding-left: 1.25rem;
  color: var(--hack-text);
}

.metric-sm {
  font-size: 0.95rem !important;
}

@media (max-width: 768px) {
  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
