import request from './request'

export function getRiskEntry() {
  return request.get('/risk/entry')
}

export function predictRisk() {
  return request.post('/risk/predict')
}

export function getLatestRisk() {
  return request.get('/risk/latest')
}

export function getRiskHistory(params) {
  return request.get('/risk/history', { params })
}

export function getRiskDetail(assessmentId) {
  return request.get(`/risk/${assessmentId}`)
}
