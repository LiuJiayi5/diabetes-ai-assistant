import request from './request'

export function getHealthMetricEntry() {
  return request.get('/health-metric/entry')
}

export function saveMetric(payload) {
  return request.post('/health-metric', payload)
}
