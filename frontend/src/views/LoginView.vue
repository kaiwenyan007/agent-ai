<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HackLogo from '../components/HackLogo.vue'
import { toApiError } from '../api/errors'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const REMEMBER_KEY = 'agent-ai-remember-user'

const authMode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const rememberLogin = ref(false)
const error = ref('')

watch(authMode, () => {
  confirmPassword.value = ''
  error.value = ''
})

function validateForm(): string | null {
  if (authMode.value === 'register' && password.value !== confirmPassword.value) {
    return '[ERR] 两次密码不一致'
  }
  return null
}

async function submit() {
  error.value = ''
  const validationError = validateForm()
  if (validationError) {
    error.value = validationError
    return
  }

  try {
    if (authMode.value === 'register') {
      await auth.register(username.value, password.value)
    } else {
      await auth.login(username.value, password.value)
      if (rememberLogin.value) {
        localStorage.setItem(REMEMBER_KEY, username.value)
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/chat'
    await router.push(redirect)
  } catch (e) {
    error.value = `[DENIED] ${toApiError(e, '操作失败')}`
  }
}

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    username.value = saved
    rememberLogin.value = true
  }
})
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <HackLogo variant="auth" />
      <h1 class="hack-title">AGENT AI</h1>
      <p class="hack-caption">// SECURE ACCESS · RAG NEURAL INTERFACE v0.4</p>

      <div class="hack-tabs">
        <button
          type="button"
          class="hack-tab"
          :class="{ active: authMode === 'login' }"
          @click="authMode = 'login'"
        >
          [ LOGIN ]
        </button>
        <button
          type="button"
          class="hack-tab"
          :class="{ active: authMode === 'register' }"
          @click="authMode = 'register'"
        >
          [ REGISTER ]
        </button>
      </div>

      <hr class="hack-divider" />

      <form @submit.prevent="submit">
        <div class="hack-field">
          <label>{{ authMode === 'login' ? 'USERNAME' : 'NEW USER' }}</label>
          <input
            v-model="username"
            class="hack-input"
            autocomplete="username"
            :placeholder="authMode === 'login' ? 'root@local' : 'hacker007'"
          />
        </div>

        <div class="hack-field">
          <label>PASSWORD</label>
          <input
            v-model="password"
            class="hack-input"
            type="password"
            autocomplete="current-password"
          />
        </div>

        <div v-if="authMode === 'register'" class="hack-field">
          <label>CONFIRM</label>
          <input v-model="confirmPassword" class="hack-input" type="password" autocomplete="new-password" />
        </div>

        <label v-if="authMode === 'login'" class="remember-row">
          <input v-model="rememberLogin" type="checkbox" />
          <span>记住账号（仅本机浏览器，公共电脑请勿勾选）</span>
        </label>

        <p v-if="error" class="error-text">{{ error }}</p>

        <button type="submit" class="hack-btn primary" :disabled="auth.loading">
          {{ authMode === 'login' ? '>> AUTHENTICATE' : '>> CREATE ACCOUNT' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem 1rem 1.5rem;
}

.auth-card {
  width: min(480px, 94vw);
  background: linear-gradient(145deg, var(--hack-panel) 0%, var(--hack-input-bg) 100%);
  border: 1px solid var(--hack-green);
  border-radius: 4px;
  padding: 1.5rem 1.25rem;
  box-shadow: 0 0 24px rgba(0, 255, 65, 0.1);
}

.remember-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.85rem;
  color: var(--hack-muted);
  font-size: 0.75rem;
  cursor: pointer;
}

.remember-row input {
  margin-top: 0.15rem;
}
</style>
