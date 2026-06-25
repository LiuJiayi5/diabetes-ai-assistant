import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { useAuthStore } from '@/stores/auth'
import { decodeTokenPayload, getToken, removeToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
    return guardAdmin(to, authStore)
  }

  if (to.path.startsWith('/app')) {
    authStore.restoreSession('patient')
    const payload = decodeTokenPayload(getToken('patient'))
    if (payload?.role === 'admin') {
      removeToken('patient')
      authStore.clearSession('patient')
      return { path: '/login' }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - 糖尿病预治智能助手` : '糖尿病预治智能助手'
})

function guardAdmin(to, authStore) {
  const token = authStore.restoreSession('admin')
  const tokenPayload = decodeTokenPayload(token)
  if (!token || tokenPayload?.role !== 'admin') {
    removeToken('admin')
    localStorage.removeItem('diabetes_admin_user')
    authStore.clearSession('admin')
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

  authStore.role = 'admin'
  return true
}

export default router
