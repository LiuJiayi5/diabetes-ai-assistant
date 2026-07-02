import { describe, expect, it } from 'vitest'
import {
  buildMetricSummary,
  formatGender,
  formatRiskLevel,
  todayString
} from './health'

describe('health utils', () => {
  it('formatRiskLevel maps known levels', () => {
    expect(formatRiskLevel('low')).toBe('低风险')
    expect(formatRiskLevel('medium')).toBe('中风险')
    expect(formatRiskLevel('high')).toBe('高风险')
    expect(formatRiskLevel('unknown')).toBe('unknown')
  })

  it('formatGender maps known values', () => {
    expect(formatGender('male')).toBe('男')
    expect(formatGender('female')).toBe('女')
  })

  it('buildMetricSummary returns placeholder when metric missing', () => {
    expect(buildMetricSummary(null)).toBe('暂无健康数据')
  })

  it('buildMetricSummary joins available fields', () => {
    const summary = buildMetricSummary({
      fasting_glucose: 6.2,
      weight_kg: 82.5,
      systolic_bp: 130,
      diastolic_bp: 82
    })
    expect(summary).toContain('空腹血糖 6.2 mmol/L')
    expect(summary).toContain('体重 82.5 kg')
    expect(summary).toContain('血压 130/82')
  })

  it('todayString returns yyyy-mm-dd format', () => {
    expect(todayString()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
