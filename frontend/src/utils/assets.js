const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const ORIGIN_URL = API_BASE_URL.replace(/\/api\/?$/, '')

export function resolveAssetUrl(value) {
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  const path = value.startsWith('/') ? value : `/${value}`
  return `${ORIGIN_URL}${path}`
}
