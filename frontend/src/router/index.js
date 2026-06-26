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
    const token = authStore.restoreSession('patient')
    const payload = decodeTokenPayload(getToken('patient'))
    if (!token || payload?.role !== 'patient') {
      removeToken('patient')
      authStore.clearSession('patient')
      return { path: '/login' }
    }
    if (authStore.user?.role === 'admin') {
      authStore.setUser(null)
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

  if (authStore.user?.role && authStore.user.role !== 'admin') {
    authStore.setUser(null)
  }

  const storedAdmin = localStorage.getItem('diabetes_admin_user')
  if (storedAdmin) {
    try {
      const parsed = JSON.parse(storedAdmin)
      if (parsed?.role === 'admin') {
        authStore.setUser(parsed, 'admin')
      } else {
        localStorage.removeItem('diabetes_admin_user')
      }
    } catch {
      localStorage.removeItem('diabetes_admin_user')
    }
  }

  return true
}

export default router
