import request from './request'

export function getRiskEntry() {
  return request.get('/risk/entry')
}

export function predictRisk(payload) {
  return request.post('/risk/predict', payload)
}
