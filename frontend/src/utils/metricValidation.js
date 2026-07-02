const RULES = [
  { key: 'recorded_at', label: '记录日期', required: true, pattern: /^\d{4}-\d{2}-\d{2}$/, patternMessage: '记录日期格式应为 YYYY-MM-DD' },
  { key: 'weight_kg', label: '体重(kg)', min: 20, max: 300 },
  { key: 'waist_cm', label: '腰围(cm)', min: 30, max: 200 },
  { key: 'systolic_bp', label: '收缩压', min: 60, max: 260, integer: true },
  { key: 'diastolic_bp', label: '舒张压', min: 30, max: 180, integer: true },
  { key: 'fasting_glucose', label: '空腹血糖', min: 1, max: 30, unit: 'mmol/L' },
  { key: 'postprandial_glucose', label: '餐后血糖', min: 1, max: 35, unit: 'mmol/L' },
  {
    key: 'hba1c',
    label: '糖化血红蛋白',
    min: 3,
    max: 20
  }
]

const METRIC_KEYS = RULES.filter((rule) => rule.key !== 'recorded_at').map((rule) => rule.key)

function parseNumber(value) {
  if (value === '' || value == null) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : NaN
}

function rangeMessage(rule) {
  const unit = rule.unit ? ` ${rule.unit}` : ''
  const hint = rule.hint ? `（${rule.hint}）` : ''
  return `${rule.label}应在 ${rule.min}～${rule.max}${unit} 之间${hint}`
}

export function validateMetricForm(form) {
  const errors = []

  for (const rule of RULES) {
    const raw = form[rule.key]
    if (rule.required && (raw == null || String(raw).trim() === '')) {
      errors.push(`请填写${rule.label}`)
      continue
    }
    if (rule.pattern && raw) {
      if (!rule.pattern.test(String(raw).trim())) {
        errors.push(rule.patternMessage || `${rule.label}格式不正确`)
      }
      continue
    }
    if (rule.min == null) continue

    const value = rule.integer ? parseInt(raw, 10) : parseNumber(raw)
    if (raw === '' || raw == null) continue
    if (Number.isNaN(value)) {
      errors.push(`${rule.label}请输入有效数字`)
      continue
    }
    if (value < rule.min || value > rule.max) {
      errors.push(rangeMessage(rule))
    }
  }

  const hasMetric = METRIC_KEYS.some((key) => form[key] !== '' && form[key] != null)
  if (!hasMetric) {
    errors.push('请至少填写一项健康指标')
  }

  return errors
}

export function formatMetricValidationError(errors) {
  return errors.length ? errors.join('；') : ''
}
