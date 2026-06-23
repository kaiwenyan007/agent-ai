import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLlmSettings } from '../api/settings'
import type { LlmSettings } from '../types/api'

export const useSettingsStore = defineStore('settings', () => {
  const llm = ref<LlmSettings | null>(null)
  const loading = ref(false)

  const configured = ref(false)

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
