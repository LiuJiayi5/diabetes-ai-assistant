export const GENDER_LABELS = {
  male: '男',
  female: '女',
  other: '其他'
}

export const RISK_LEVEL_LABELS = {
  low: '低风险',
  medium: '中风险',
  high: '高风险'
}

export function formatGender(value) {
  return GENDER_LABELS[value] || value || '—'
}

export function formatRiskLevel(value) {
  return RISK_LEVEL_LABELS[value] || value || '—'
}

export function todayString() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function buildMetricSummary(metric) {
  if (!metric) return '暂无健康数据'
  const parts = []
  if (metric.fasting_glucose != null) {
    parts.push(`空腹血糖 ${metric.fasting_glucose} mmol/L`)
  }
  if (metric.weight_kg != null) {
    parts.push(`体重 ${metric.weight_kg} kg`)
  }
  if (metric.systolic_bp != null && metric.diastolic_bp != null) {
    parts.push(`血压 ${metric.systolic_bp}/${metric.diastolic_bp}`)
  }
  return parts.length ? parts.join(' · ') : '已录入，可查看详情'
}
