const LEGACY_TOKEN_KEY = 'diabetes_ai_assistant_jwt'
const PATIENT_TOKEN_KEY = 'diabetes_patient_jwt'
const ADMIN_TOKEN_KEY = 'diabetes_admin_jwt'

export function resolveSessionScope(roleOrPath) {
  const value = String(roleOrPath || '').toLowerCase()
  if (value === 'admin' || value.startsWith('/admin') || value.includes('/admin/')) return 'admin'
  return 'patient'
}

export function getToken(scope = currentScope()) {
  const key = tokenKey(scope)
  const localToken = localStorage.getItem(key)
  if (localToken) {
    if (!isTokenExpired(localToken)) return localToken
    localStorage.removeItem(key)
  }

  const sessionToken = sessionStorage.getItem(key)
  if (sessionToken) {
    if (!isTokenExpired(sessionToken)) return sessionToken
    sessionStorage.removeItem(key)
  }

  return null
}

export function setToken(token, scope = currentScope(), remember = true) {
  if (!token) return
  const key = tokenKey(scope)
  localStorage.removeItem(key)
  sessionStorage.removeItem(key)
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(key, token)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function removeToken(scope) {
  if (scope) {
    localStorage.removeItem(tokenKey(scope))
    sessionStorage.removeItem(tokenKey(scope))
    return
  }
  localStorage.removeItem(PATIENT_TOKEN_KEY)
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  sessionStorage.removeItem(PATIENT_TOKEN_KEY)
  sessionStorage.removeItem(ADMIN_TOKEN_KEY)
}

export function getTokenRole(token) {
  return decodeTokenPayload(token)?.role || null
}

export function isTokenExpired(token) {
  const payload = decodeTokenPayload(token)
  if (!payload?.exp) return false
  return payload.exp * 1000 <= Date.now()
}

export function getTokenForRequest(url = '') {
  return getToken(resolveSessionScope(url || window.location?.pathname || ''))
}

export function getTokenForScope(scope = currentScope()) {
  return getToken(resolveSessionScope(scope))
}

export function migrateLegacyToken() {
  const legacy = localStorage.getItem(LEGACY_TOKEN_KEY)
  if (!legacy) return
  const role = getTokenRole(legacy)
  setToken(legacy, role === 'admin' ? 'admin' : 'patient')
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function decodeTokenPayload(token) {
  if (!token || !token.includes('.')) return null
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

function currentScope() {
  if (typeof window === 'undefined') return 'patient'
  return resolveSessionScope(window.location?.pathname || '')
}

function tokenKey(scope) {
  return resolveSessionScope(scope) === 'admin' ? ADMIN_TOKEN_KEY : PATIENT_TOKEN_KEY
}
