<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useConversationStore } from '../stores/conversation'

const auth = useAuthStore()
const conv = useConversationStore()
const router = useRouter()
const route = useRoute()
const booting = ref(true)

const isChatPage = computed(() => route.path.startsWith('/chat'))

function navClass(path: string) {
  return route.path.startsWith(path) ? 'hack-btn nav active' : 'hack-btn nav'
}

async function handleLogout() {
  await auth.logout()
  await router.push('/login')
}

async function handleNewSession() {
  await conv.createNew()
}

async function handleSelectSession(id: number) {
  await conv.selectConversation(id)
}

async function handleDeleteSession() {
  await conv.deleteActive()
}

onMounted(async () => {
  const ok = await auth.restoreSession()
  booting.value = false
  if (!ok) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (isChatPage.value) {
    await conv.bootstrap()
  }
})

watch(
  () => route.path,
  async (path) => {
    if (path.startsWith('/chat')) {
      await conv.bootstrap()
    }
  },
)
</script>

<template>
  <div v-if="booting" class="app-boot">// LOADING SESSION...</div>
  <div v-else class="app-root">
    <aside class="app-sidebar">
      <h3 class="sidebar-user">// {{ auth.username }}</h3>

      <RouterLink to="/chat" :class="navClass('/chat')">💬 CHAT</RouterLink>
      <RouterLink to="/settings" :class="navClass('/settings')">⚙️ API CONFIG</RouterLink>
      <RouterLink to="/knowledge" :class="navClass('/knowledge')">📚 KNOWLEDGE</RouterLink>
      <RouterLink to="/stats" :class="navClass('/stats')">📊 TOKEN STATS</RouterLink>

      <hr class="hack-divider" />

      <template v-if="isChatPage">
        <p class="sidebar-section">SESSIONS</p>
        <button type="button" class="hack-btn nav" @click="handleNewSession">➕ NEW</button>

        <button
          v-for="item in conv.conversations"
          :key="item.id"
          type="button"
          class="hack-btn session"
          :class="{ active: item.id === conv.activeId }"
          @click="handleSelectSession(item.id)"
        >
          {{ item.id === conv.activeId ? '▸ ' : '  ' }}{{ conv.truncateTitle(item.title) }}
        </button>

        <button
          v-if="conv.activeId"
          type="button"
          class="hack-btn nav"
          @click="handleDeleteSession"
        >
          🗑️ DELETE
        </button>

        <hr class="hack-divider" />
      </template>

      <button type="button" class="hack-btn nav logout-btn" @click="handleLogout">LOGOUT</button>
    </aside>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-boot {
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: var(--hack-muted);
  letter-spacing: 0.06em;
}

.app-root {
  display: flex;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
}

.app-sidebar {
  width: 16rem;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 1rem;
  background: linear-gradient(180deg, #0a0f18 0%, var(--hack-bg) 100%);
  border-right: 1px solid var(--hack-border);
  overflow-y: auto;
  height: 100%;
}

.sidebar-user {
  margin: 0 0 0.75rem;
  color: var(--hack-green);
  font-size: 0.95rem;
  letter-spacing: 0.04em;
  word-break: break-all;
}

.sidebar-section {
  margin: 0 0 0.5rem;
  color: var(--hack-text);
  font-size: 0.82rem;
  letter-spacing: 0.06em;
}

.logout-btn {
  margin-top: auto;
}

.app-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.app-main > * {
  flex: 1;
  min-height: 0;
}

@media (max-width: 768px) {
  .app-root {
    flex-direction: column;
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .app-sidebar {
    width: 100%;
    height: auto;
    max-height: 40vh;
    border-right: none;
    border-bottom: 1px solid var(--hack-border);
  }

  .app-main {
    height: calc(100vh - 40vh);
    min-height: 320px;
  }

  .logout-btn {
    margin-top: 0;
  }
}
</style>
