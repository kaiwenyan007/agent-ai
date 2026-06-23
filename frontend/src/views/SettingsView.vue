<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchModels, getLlmSettings, saveLlmSettings } from '../api/settings'
import { toApiError } from '../api/errors'
import { useSettingsStore } from '../stores/settings'

const settingsStore = useSettingsStore()

const baseUrl = ref('https://api.deepseek.com')
const model = ref('deepseek-chat')
const apiKey = ref('')
const modelOptions = ref<string[]>([])
const modelFetchError = ref('')
const loading = ref(false)
const fetchingModels = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

async function handleFetchModels() {
  fetchingModels.value = true
  modelFetchError.value = ''
  error.value = ''
  try {
    const result = await fetchModels({
      baseUrl: baseUrl.value.trim(),
      apiKey: apiKey.value.trim() || undefined,
    })
    modelOptions.value = result.models
    if (modelOptions.value.length > 0 && !modelOptions.value.includes(model.value)) {
      model.value = modelOptions.value[0]
    }
    if (!result.fromRemote) {
      modelFetchError.value = '[WARN] 未能从远程拉取，使用本地默认列表'
    }
  } catch (e) {
    modelFetchError.value = toApiError(e, '拉取模型失败')
  } finally {
    fetchingModels.value = false
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const cfg = await getLlmSettings()
    baseUrl.value = cfg.baseUrl || 'https://api.deepseek.com'
    model.value = cfg.model || 'deepseek-chat'
    modelOptions.value = cfg.model ? [cfg.model] : ['deepseek-chat']
    settingsStore.applySaved(cfg)
  } catch (e) {
    error.value = toApiError(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!model.value) {
    error.value = '[ERR] 请选择模型'
    return
  }
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const saved = await saveLlmSettings({
      baseUrl: baseUrl.value.trim(),
      model: model.value,
      apiKey: apiKey.value.trim() || undefined,
    })
    settingsStore.applySaved(saved)
    apiKey.value = ''
    success.value = '[OK] 配置已保存'
    if (modelOptions.value.length === 0) {
      modelOptions.value = [saved.model]
    }
  } catch (e) {
    error.value = toApiError(e, '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-content">
    <div class="page-inner">
      <h2 class="hack-title">⚙️ API CONFIG</h2>
      <p class="hack-caption">// 独立密钥 · 独立模型 · 零交叉污染</p>

      <section v-if="loading" class="hack-panel muted">// LOADING...</section>

      <section v-else class="hack-panel form-panel">
        <div class="hack-field">
          <label>OPENAI_API_KEY</label>
          <input
            v-model="apiKey"
            class="hack-input"
            type="password"
            autocomplete="off"
            placeholder="sk-..."
          />
        </div>

        <div class="hack-field">
          <label>OPENAI_BASE_URL</label>
          <input v-model="baseUrl" class="hack-input" type="url" placeholder="https://api.deepseek.com" />
        </div>

        <button type="button" class="hack-btn" :disabled="fetchingModels" @click="handleFetchModels">
          {{ fetchingModels ? 'FETCHING...' : 'FETCH MODELS' }}
        </button>

        <p v-if="modelFetchError" class="warn-text">{{ modelFetchError }}</p>

        <div class="hack-field">
          <label>OPENAI_MODEL</label>
          <select v-model="model" class="hack-select">
            <option v-for="item in modelOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>

        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="success" class="success-text">{{ success }}</p>

        <button type="button" class="hack-btn primary" :disabled="saving" @click="save">
          {{ saving ? 'SAVING...' : 'SAVE CONFIG' }}
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.form-panel {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
</style>
