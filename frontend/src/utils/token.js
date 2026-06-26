const LEGACY_TOKEN_KEY = 'diabetes_ai_assistant_jwt'
const PATIENT_TOKEN_KEY = 'diabetes_patient_jwt'
const ADMIN_TOKEN_KEY = 'diabetes_admin_jwt'

export function resolveSessionScope(roleOrPath) {
  const value = String(roleOrPath || '').toLowerCase()
  if (value === 'admin' || value.startsWith('/admin') || value.includes('/admin/')) return 'admin'
  return 'patient'
}

export function getToken(scope = currentScope()) {
  return localStorage.getItem(tokenKey(scope))
}

export function setToken(token, scope = currentScope()) {
  if (!token) return
  localStorage.setItem(tokenKey(scope), token)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function removeToken(scope) {
  if (scope) {
    localStorage.removeItem(tokenKey(scope))
    return
  }
  localStorage.removeItem(PATIENT_TOKEN_KEY)
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function getTokenRole(token) {
  return decodeTokenPayload(token)?.role || null
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
