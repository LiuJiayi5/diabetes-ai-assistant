import { defineStore } from 'pinia'
import { getToken, migrateLegacyToken, removeToken, resolveSessionScope, setToken } from '@/utils/token'

migrateLegacyToken()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: null,
    role: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setSession({ token, user, role }) {
      const sessionRole = role || user?.role || 'patient'
      const scope = resolveSessionScope(sessionRole)
      if (token) {
        setToken(token, scope)
        this.token = token
      }
      this.user = user || null
      this.role = sessionRole
    },
    restoreSession(scope = 'patient') {
      const token = getToken(scope)
      this.token = token
      this.role = scope === 'admin' ? 'admin' : this.role
      return token
    },
    setUser(user) {
      this.user = user || null
      this.role = user?.role || this.role
    },
    clearSession(scope) {
      removeToken(scope)
      this.token = null
      this.user = null
      this.role = null
    }
  }
})
