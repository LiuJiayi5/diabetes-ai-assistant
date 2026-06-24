import { defineStore } from 'pinia'
import { getToken, removeToken, setToken } from '@/utils/token'

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
      if (token) {
        setToken(token)
        this.token = token
      }
      this.user = user || null
      this.role = role || user?.role || null
    },
    setUser(user) {
      this.user = user || null
      this.role = user?.role || this.role
    },
    clearSession() {
      removeToken()
      this.token = null
      this.user = null
      this.role = null
    }
  }
})
