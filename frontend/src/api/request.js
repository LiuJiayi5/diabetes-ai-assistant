import axios from 'axios'
import { getToken, removeToken } from '@/utils/token'

const ADMIN_LOGIN_PATH = '/admin/login'

function isAdminRequest(error) {
  const requestUrl = error?.config?.url || ''
  const pagePath = window.location?.pathname || ''
  return requestUrl.includes('/admin/') || pagePath.startsWith('/admin')
}

function clearInvalidSession(error) {
  removeToken()
  localStorage.removeItem('diabetes_admin_user')
  if (isAdminRequest(error) && window.location.pathname !== ADMIN_LOGIN_PATH) {
    const redirect = `${window.location.pathname}${window.location.search || ''}`
    window.location.href = `${ADMIN_LOGIN_PATH}?redirect=${encodeURIComponent(redirect)}`
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if ([401, 403].includes(error?.response?.status)) {
      clearInvalidSession(error)
    }
    return Promise.reject(error)
  }
)

export default request
