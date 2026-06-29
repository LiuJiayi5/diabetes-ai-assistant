import { defineStore } from 'pinia'
import { decodeTokenPayload, getToken, migrateLegacyToken, removeToken, resolveSessionScope, setToken } from '@/utils/token'

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
    setSession({ token, user, role, remember = true }) {
      const sessionRole = role || user?.role || 'patient'
      const scope = resolveSessionScope(sessionRole)
      const tokenRole = decodeTokenPayload(token)?.role
      if (token && ((scope === 'admin' && tokenRole !== 'admin') || (scope === 'patient' && tokenRole === 'admin'))) {
        removeToken(scope)
        this.token = null
        this.user = null
        this.role = null
        return false
      }
      if (token) {
        setToken(token, scope, remember)
        this.token = token
      }
      this.user = user || null
      this.role = sessionRole
      return true
    },
    restoreSession(scope = 'patient') {
      const token = getToken(scope)
      const tokenRole = decodeTokenPayload(token)?.role
      const normalizedScope = resolveSessionScope(scope)
      if (token && ((normalizedScope === 'admin' && tokenRole !== 'admin') || (normalizedScope === 'patient' && tokenRole === 'admin'))) {
        this.clearSession(normalizedScope)
        return null
      }
      this.token = token
      this.role = normalizedScope === 'admin' ? 'admin' : (token ? 'patient' : null)
      return token
    },
    setUser(user, expectedScope) {
      const expected = expectedScope ? resolveSessionScope(expectedScope) : null
      if (user && expected && ((expected === 'admin' && user.role !== 'admin') || (expected === 'patient' && user.role === 'admin'))) {
        this.clearSession(expected)
        return false
      }
      this.user = user || null
      this.role = user?.role || this.role
      return true
    },
    clearSession(scope) {
      removeToken(scope)
      this.token = null
      this.user = null
      this.role = null
    }
  }
})
