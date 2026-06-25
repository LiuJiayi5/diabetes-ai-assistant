import axios from 'axios'
import { getTokenForRequest, removeToken, resolveSessionScope } from '@/utils/token'

const ADMIN_LOGIN_PATH = '/admin/login'
const PATIENT_LOGIN_PATH = '/login'

function requestScope(configOrError) {
  const requestUrl = configOrError?.config?.url || configOrError?.url || ''
  const pagePath = window.location?.pathname || ''
  return resolveSessionScope(requestUrl || pagePath)
}

function clearInvalidSession(error) {
  const scope = requestScope(error)
  removeToken(scope)

  if (scope === 'admin') {
    localStorage.removeItem('diabetes_admin_user')
    if (window.location.pathname.startsWith('/admin') && window.location.pathname !== ADMIN_LOGIN_PATH) {
      const redirect = `${window.location.pathname}${window.location.search || ''}`
      window.location.href = `${ADMIN_LOGIN_PATH}?redirect=${encodeURIComponent(redirect)}`
    }
    return
  }

  if (window.location.pathname.startsWith('/app')) {
    window.location.href = PATIENT_LOGIN_PATH
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = getTokenForRequest(config.url)
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
