/**
 * LLM 配置 Pinia Store：configured 状态供 ChatView 禁用输入框。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLlmSettings } from '../api/settings'
import type { LlmSettings } from '../types/api'

export const useSettingsStore = defineStore('settings', () => {
  const llm = ref<LlmSettings | null>(null)
  const loading = ref(false)

  /** Key + URL + Model 均已配置，可发起对话 */
  const configured = ref(false)

  /** 从后端加载配置并更新 configured */
  async function loadLlm() {
    loading.value = true
    try {
      llm.value = await getLlmSettings()
      configured.value = llm.value.configured
      return llm.value
    } finally {
      loading.value = false
    }
  }

  /** 保存配置后同步本地状态 */
  function applySaved(saved: LlmSettings) {
    llm.value = saved
    configured.value = saved.configured
  }

  return {
    llm,
    loading,
    configured,
    loadLlm,
    applySaved,
  }
})
