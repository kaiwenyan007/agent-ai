import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/client'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/chat' },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      children: [
        {
          path: 'chat',
          name: 'chat',
          component: () => import('../views/ChatView.vue'),
        },
        {
          path: 'settings',
          name: 'settings',
          component: () => import('../views/SettingsView.vue'),
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('../views/KnowledgeView.vue'),
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('../views/StatsView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
