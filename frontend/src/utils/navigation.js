export function encodeBackPath(path) {
  return encodeURIComponent(path || '')
}

export function backPathFromRoute(route, fallback = '/') {
  const value = route?.query?.back
  if (Array.isArray(value)) {
    return value[0] || fallback
  }
  return value || fallback
}

export function pushWithBack(router, path, backPath) {
  const separator = path.includes('?') ? '&' : '?'
  router.push(`${path}${separator}back=${encodeBackPath(backPath)}`)
}
