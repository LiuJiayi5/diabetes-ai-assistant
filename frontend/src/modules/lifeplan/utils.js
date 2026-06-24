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

  return {
    ...plan,
    id: plan?.plan_id || plan?.planId || plan?.id,
    title: plan?.plan_title || planJson?.plan_title || '个性化控糖生活方案',
    goal: plan?.plan_goal || planJson?.plan_goal || '控糖和减重',
    summary: plan?.summary || planJson?.summary || '根据健康档案、近期指标和风险评估生成',
    riskSummary: planJson?.risk_summary || '',
    dietPlan: planJson?.diet_plan || {},
    exercisePlan: planJson?.exercise_plan || {},
    workRestPlan: planJson?.work_rest_plan || {},
    dailySchedule: Array.isArray(planJson?.daily_schedule) ? planJson.daily_schedule : [],
    checkinTasks: Array.isArray(checkinTasks) ? checkinTasks : [],
    healthTips: Array.isArray(planJson?.health_tips) ? planJson.health_tips : [],
    medicalWarning: planJson?.medical_warning || 'AI 建议仅供参考，不能替代线下诊疗',
    planJson
  }
}

export function formatPlanTime(value) {
  if (!value) return '今天 09:30'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  return isToday ? `今天 ${time}` : `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

export function getRiskMeta(plan) {
  const source = `${plan?.riskSummary || plan?.planJson?.risk_level || plan?.planJson?.risk_level_label || ''}`
  if (/high|高/.test(source)) return { label: '高风险', className: 'risk-high' }
  if (/low|低/.test(source)) return { label: '低风险', className: 'risk-low' }
  return { label: '中风险', className: 'risk-medium' }
}

export function groupScheduleByDay(schedule = []) {
  return schedule.reduce((groups, item, index) => {
    const day = item.day || item.date || `第 ${Math.floor(index / 4) + 1} 天`
    if (!groups[day]) groups[day] = []
    groups[day].push(item)
    return groups
  }, {})
}

export function toText(value, fallback = '暂无具体建议') {
  if (Array.isArray(value)) return value.join('，')
  if (value && typeof value === 'object') return Object.values(value).filter(Boolean).join('，')
  return value || fallback
}
