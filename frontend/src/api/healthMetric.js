import request from './request'

export function getHealthMetricEntry() {
  return request.get('/health-metric/entry')
}

export function getLatestMetric() {
  return request.get('/health-metric/latest')
}

export function getMetricHistory(params) {
  return request.get('/health-metric/history', { params })
}

export function saveMetric(payload) {
  return request.post('/health-metric', payload)
}

export function adminListMetrics(params) {
  return request.get('/health-metric/admin', { params })
}

export function adminListAbnormalMetrics(params) {
  return request.get('/health-metric/admin/abnormal', { params })
}
