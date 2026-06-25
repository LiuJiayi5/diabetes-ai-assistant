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

export function normalizePlan(plan) {
  const planJson = safeJsonParse(plan?.plan_json, plan?.plan_json || {})
  const checkinTasks = safeJsonParse(plan?.checkin_tasks_json, planJson?.checkin_tasks || [])
  const dailySchedule = normalizeWeeklySchedule(planJson?.daily_schedule || planJson?.weekly_schedule || planJson?.days || planJson)

  return {
    ...plan,
    id: plan?.plan_id || plan?.planId || plan?.id,
    title: cleanText(plan?.plan_title || planJson?.plan_title || '个性化控糖生活方案'),
    goal: cleanText(plan?.plan_goal || planJson?.plan_goal || '控糖和体重管理'),
    summary: cleanText(plan?.summary || planJson?.summary || '根据健康档案、近期指标和风险评估生成'),
    riskSummary: cleanText(planJson?.risk_summary || plan?.risk_level || ''),
    riskLevel: plan?.risk_level || planJson?.risk_level || planJson?.risk_level_label || '',
    dietPlan: planJson?.diet_plan || {},
    exercisePlan: planJson?.exercise_plan || {},
    workRestPlan: planJson?.work_rest_plan || {},
    dailySchedule,
    checkinTasks: Array.isArray(checkinTasks) ? checkinTasks : [],
    healthTips: toArray(planJson?.health_tips),
    medicalWarning: cleanText(planJson?.medical_warning || 'AI 建议仅供参考，不能替代线下诊疗'),
    planJson
  }
}

export function formatPlanTime(value) {
  if (!value) return '暂无更新时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return cleanText(value, '暂无更新时间')
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  return isToday ? `今天 ${time}` : `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

export function getRiskMeta(plan) {
  const source = `${plan?.riskLevel || plan?.riskSummary || plan?.planJson?.risk_level || plan?.planJson?.risk_level_label || ''}`
  if (/high|高/.test(source)) return { label: '高风险', className: 'risk-high' }
  if (/low|低/.test(source)) return { label: '低风险', className: 'risk-low' }
  return { label: '中风险', className: 'risk-medium' }
}

export function normalizeWeeklySchedule(source) {
  if (Array.isArray(source)) return normalizeScheduleArray(source)
  if (typeof source === 'string') return normalizeTextSchedule(source)
  if (!source || typeof source !== 'object') return []

  const candidate = source.daily_schedule || source.weekly_schedule || source.days || source.plan_days
  if (Array.isArray(candidate)) return normalizeScheduleArray(candidate)
  if (typeof candidate === 'string') return normalizeTextSchedule(candidate)

  const days = Object.entries(source)
    .filter(([key]) => /day|第|周|星期/i.test(key))
    .map(([key, value], index) => normalizeDay(value, index, key))
  return days.length ? days : []
}

export function groupScheduleByDay(schedule = []) {
  return normalizeWeeklySchedule(schedule).reduce((groups, day) => {
    groups[day.title] = [
      { time: '饮食', content: day.diet },
      { time: '运动', content: day.exercise },
      { time: '提醒', content: day.reminder }
    ]
    return groups
  }, {})
}

export function toText(value, fallback = '暂无具体建议') {
  return cleanText(value, fallback)
}

function normalizeScheduleArray(items) {
  if (!items.length) return []
  if (items.every((item) => item && typeof item === 'object' && hasDayFields(item))) {
    return items.map((item, index) => normalizeDay(item, index))
  }

  const buckets = new Map()
  items.forEach((item, index) => {
    const dayKey = item?.day || item?.date || item?.day_index || Math.floor(index / 3) + 1
    const normalizedKey = normalizeDayTitle(dayKey, buckets.size)
    const list = buckets.get(normalizedKey) || []
    list.push(item)
    buckets.set(normalizedKey, list)
  })

  return Array.from(buckets.entries()).map(([title, list], index) => normalizeDay({ title, items: list }, index))
}

function normalizeTextSchedule(value) {
  const text = cleanText(value, '')
  if (!text) return []

  const parts = text
    .split(/(?=第\s*\d+\s*天|Day\s*\d+|DAY\s*\d+)/)
    .map((item) => item.trim())
    .filter(Boolean)

  const dayTexts = parts.length > 1 ? parts : [text]
  return dayTexts.map((item, index) => normalizeDay({ title: `第 ${index + 1} 天`, reminder: item }, index))
}

function normalizeDay(value, index = 0, fallbackTitle = '') {
  const source = value && typeof value === 'object' ? value : { reminder: value }
  const items = Array.isArray(source.items) ? source.items : []

  return {
    title: normalizeDayTitle(source.day || source.date || source.title || fallbackTitle, index),
    diet: cleanText(source.diet || source.diet_advice || source.meal || source.meals || pickItem(items, /饮食|早餐|午餐|晚餐|meal|diet/i), '暂无饮食建议'),
    exercise: cleanText(source.exercise || source.exercise_advice || source.sport || source.training || pickItem(items, /运动|锻炼|exercise|sport/i), '暂无运动建议'),
    reminder: cleanText(source.reminder || source.sleep || source.work_rest || source.tip || source.content || pickItem(items, /提醒|作息|控糖|睡眠|tip|reminder/i), '暂无作息提醒')
  }
}

function hasDayFields(item) {
  return ['diet', 'diet_advice', 'exercise', 'exercise_advice', 'reminder', 'sleep', 'work_rest', 'content', 'items'].some((key) => key in item)
}

function normalizeDayTitle(value, index = 0) {
  const text = cleanText(value, '')
  if (!text) return `第 ${index + 1} 天`
  if (/^\d+$/.test(String(text))) return `第 ${text} 天`
  return text
}

function pickItem(items, pattern) {
  const match = items.find((item) => pattern.test(`${item?.type || ''}${item?.task_type || ''}${item?.name || ''}${item?.task_name || ''}${item?.time || ''}`))
  return match?.content || match?.description || match?.task_name || match?.name || match
}

function toArray(value) {
  if (Array.isArray(value)) return value.map((item) => cleanText(item)).filter(Boolean)
  const text = cleanText(value, '')
  return text ? [text] : []
}

function cleanText(value, fallback = '') {
  if (Array.isArray(value)) {
    return value.map((item) => cleanText(item, '')).filter(Boolean).join('，') || fallback
  }
  if (value && typeof value === 'object') {
    return Object.values(value).map((item) => cleanText(item, '')).filter(Boolean).join('，') || fallback
  }
  const text = String(value ?? '').trim()
  if (!text || ['undefined', 'null', 'NaN', '[object Object]'].includes(text)) return fallback
  return text
}
