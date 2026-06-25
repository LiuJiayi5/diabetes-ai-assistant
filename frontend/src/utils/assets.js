import defaultAvatarUrl from '@/assets/default-avatar.svg'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const ORIGIN_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const DEFAULT_AVATAR_MARKERS = [
  '/uploads/avatar/default',
  '/uploads/avatars/default',
  '/avatar/default',
  'default-avatar',
  'default_avatar',
  'default.png',
  'default.jpg',
  'default.jpeg',
  'default.webp'
]

export function resolveAssetUrl(value) {
  if (!value) return ''
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  const path = value.startsWith('/') ? value : `/${value}`
  return `${ORIGIN_URL}${path}`
}

export function isDefaultAvatarValue(value) {
  const text = String(value || '').trim().toLowerCase()
  if (!text) return true
  return DEFAULT_AVATAR_MARKERS.some((marker) => text.includes(marker))
}

export function resolveAvatarUrl(value) {
  if (isDefaultAvatarValue(value)) return defaultAvatarUrl
  return resolveAssetUrl(value) || defaultAvatarUrl
}

export function useDefaultAvatar(event) {
  const image = event?.target
  if (image && image.src !== defaultAvatarUrl) {
    image.src = defaultAvatarUrl
  }
  return false
}
