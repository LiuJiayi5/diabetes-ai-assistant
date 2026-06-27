export function unwrapPage(response, key) {
  const data = response?.data ?? response
  const source = key ? data?.[key] : data

  if (Array.isArray(source)) {
    return { list: source, total: source.length, page: 1, page_size: source.length }
  }

  if (Array.isArray(source?.list)) return source
  if (Array.isArray(source?.records)) {
    return {
      list: source.records,
      total: source.total ?? source.records.length,
      page: source.page ?? 1,
      page_size: source.page_size ?? source.size ?? source.records.length
    }
  }

  return { list: [], total: 0, page: 1, page_size: 10 }
}

export function createPagination(pageSize = 10) {
  return {
    page: 1,
    page_size: pageSize,
    total: 0
  }
}

export function assignPage(pagination, pageResult) {
  pagination.page = Number(pageResult?.page || pagination.page || 1)
  pagination.page_size = Number(pageResult?.page_size || pageResult?.pageSize || pagination.page_size || 10)
  pagination.total = Number(pageResult?.total || 0)
}

export function pageParams(pagination) {
  return {
    page: pagination.page || 1,
    page_size: pagination.page_size || 10
  }
}

export function totalPages(pagination) {
  const size = pagination.page_size || 10
  return Math.max(1, Math.ceil((pagination.total || 0) / size))
}

export function statusPriority(value) {
  const map = {
    published: 0,
    enabled: 0,
    active: 0,
    success: 0,
    running: 1,
    draft: 2,
    offline: 3,
    disabled: 3,
    failed: 3
  }
  return map[value] ?? 2
}

export function sortByStatusThenOrder(list = [], statusKey = 'status', orderKey = 'sort_order') {
  return [...list].sort((a, b) => {
    const statusDiff = statusPriority(a?.[statusKey]) - statusPriority(b?.[statusKey])
    if (statusDiff) return statusDiff
    const orderDiff = Number(a?.[orderKey] || 0) - Number(b?.[orderKey] || 0)
    if (orderDiff) return orderDiff
    return Number(b?.update_time ? new Date(b.update_time).getTime() : 0) - Number(a?.update_time ? new Date(a.update_time).getTime() : 0)
  })
}

export function ensurePageAfterDelete(pagination) {
  const total = Math.max(0, (pagination.total || 0) - 1)
  const pages = Math.max(1, Math.ceil(total / (pagination.page_size || 10)))
  pagination.total = total
  if (pagination.page > pages) pagination.page = pages
}

export function safeJsonParse(value, fallback = null) {
  if (value == null || value === '') return fallback
  if (typeof value === 'object') return value
  if (typeof value !== 'string') return fallback

  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

export function resolveAdminError(error, fallback = '请求失败，请稍后重试') {
  const status = error?.response?.status
  const message = error?.response?.data?.message || error?.response?.data?.error || error?.message
  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) return '当前账号无管理员权限'
  if (status === 404) return '数据不存在'
  if (message && !/exception|stack|sql|\/api|http/i.test(message)) return message
  return fallback
}

export function categoryLabel(value) {
  const map = {
    diet: '饮食指导',
    exercise: '运动指南',
    habit: '日常习惯',
    science: '糖尿病科普',
    complication: '并发症预防',
    mistake: '控糖误区'
  }
  return map[value] || value || '未分类'
}

export function statusLabel(value) {
  const map = {
    active: '正常',
    disabled: '禁用',
    patient: '患者',
    admin: '管理员',
    success: '生成成功',
    failed: '生成失败',
    running: '生成中',
    published: '已上架',
    draft: '草稿',
    offline: '已下架',
    enabled: '启用'
  }
  return map[value] || value || '-'
}

export function toText(value, fallback = '暂无') {
  if (Array.isArray(value)) return value.filter(Boolean).join('，') || fallback
  if (value && typeof value === 'object') return Object.values(value).filter(Boolean).join('，') || fallback
  return value || fallback
}
