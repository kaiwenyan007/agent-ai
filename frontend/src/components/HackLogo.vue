<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'auth' | 'main'
  }>(),
  { variant: 'main' },
)

const AUTH_LOGO = `┌─ AGENT://AI ──────────┐
│ [SYS] LINK STANDBY... │
│ [RAG] DB ... OFFLINE  │
│ [AUTH] AWAIT LOGIN    │
└───────────────────────┘`

const MAIN_LOGO = `┌─ AGENT://AI ──────────┐
│ [SYS] LINK ONLINE     │
│ [RAG] NEURAL READY    │
│ [AUTH] SESSION ACTIVE │
└───────────────────────┘`
</script>

<template>
  <pre class="hack-logo">{{ variant === 'auth' ? AUTH_LOGO : MAIN_LOGO }}</pre>
</template>
