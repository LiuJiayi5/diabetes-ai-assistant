import { describe, expect, it } from 'vitest'
import { formatMetricValidationError, validateMetricForm } from './metricValidation'

describe('metricValidation', () => {
  const validForm = {
    recorded_at: '2026-06-30',
    weight_kg: '81.6',
    waist_cm: '',
    systolic_bp: '',
    diastolic_bp: '',
    fasting_glucose: '6.2',
    postprandial_glucose: '',
    hba1c: ''
  }

  it('accepts valid metric form', () => {
    expect(validateMetricForm(validForm)).toEqual([])
  })

  it('rejects hba1c out of range', () => {
    const errors = validateMetricForm({ ...validForm, hba1c: '62' })
    expect(errors.some((item) => item.includes('糖化血红蛋白'))).toBe(true)
  })

  it('requires at least one metric besides date', () => {
    const errors = validateMetricForm({
      recorded_at: '2026-06-30',
      weight_kg: '',
      waist_cm: '',
      systolic_bp: '',
      diastolic_bp: '',
      fasting_glucose: '',
      postprandial_glucose: '',
      hba1c: ''
    })
    expect(errors).toContain('请至少填写一项健康指标')
  })

  it('joins validation messages', () => {
    const message = formatMetricValidationError(['错误1', '错误2'])
    expect(message).toBe('错误1；错误2')
  })
})
