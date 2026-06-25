import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'
import { decodeTokenPayload, getToken, removeToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (!to.path.startsWith('/admin') || to.path === '/admin/login') {
    return true
  }

  const authStore = useAuthStore()
  const tokenPayload = decodeTokenPayload(getToken())
  if (tokenPayload?.role && tokenPayload.role !== 'admin') {
    removeToken()
    localStorage.removeItem('diabetes_admin_user')
    authStore.clearSession()
    return {
      path: '/admin/login',
      query: { redirect: to.fullPath }
    }
  }

  const storedAdmin = localStorage.getItem('diabetes_admin_user')
  if (!authStore.user && storedAdmin) {
    try {
      const parsed = JSON.parse(storedAdmin)
      if (parsed?.role === 'admin') {
        authStore.setUser(parsed)
      } else {
        localStorage.removeItem('diabetes_admin_user')
      }
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
