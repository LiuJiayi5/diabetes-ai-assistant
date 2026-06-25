import axios from 'axios'
import { getToken, removeToken } from '@/utils/token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (import.meta.env.DEV) {
    const isAdminApi = String(config.url || '').startsWith('/admin/')
    config.headers['X-Dev-User-Id'] = isAdminApi
      ? (import.meta.env.VITE_DEV_ADMIN_USER_ID || '2')
      : (import.meta.env.VITE_DEV_USER_ID || '1')
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      removeToken()
    }
    return Promise.reject(error)
  }
)

export default request
