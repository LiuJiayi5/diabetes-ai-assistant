import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (!to.path.startsWith('/admin') || to.path === '/admin/login') {
    return true
  }

  const authStore = useAuthStore()
  const storedAdmin = localStorage.getItem('diabetes_admin_user')
  if (!authStore.user && storedAdmin) {
    try {
      authStore.setUser(JSON.parse(storedAdmin))
    } catch {
      localStorage.removeItem('diabetes_admin_user')
    }
  }

  if (!authStore.isLoggedIn || authStore.role !== 'admin') {
    return {
      path: '/admin/login',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - 糖尿病预治智能助手` : '糖尿病预治智能助手'
})

export default router
