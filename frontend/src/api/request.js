import axios from 'axios'
import { getTokenForRequest, getTokenForScope, getTokenRole, removeToken, resolveSessionScope } from '@/utils/token'
import { resolveApiBaseUrl } from '@/utils/apiBase'

const ADMIN_LOGIN_PATH = '/admin/login'
const PATIENT_LOGIN_PATH = '/login'

function requestScope(configOrError) {
  const explicitScope = configOrError?.config?.sessionScope || configOrError?.sessionScope
  if (explicitScope) return resolveSessionScope(explicitScope)
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
  baseURL: resolveApiBaseUrl(),
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const scope = config.sessionScope ? resolveSessionScope(config.sessionScope) : resolveSessionScope(config.url || window.location?.pathname || '')
  const token = config.sessionScope ? getTokenForScope(scope) : getTokenForRequest(config.url)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    const role = getTokenRole(token)
    if ((scope === 'admin' && role !== 'admin') || (scope === 'patient' && role === 'admin')) {
      removeToken(scope)
      return Promise.reject(new Error('当前登录身份与访问入口不匹配，请重新登录'))
    }
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
