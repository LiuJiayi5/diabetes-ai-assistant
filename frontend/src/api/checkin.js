import request from './request'

export function getCheckinEntry() {
  return request.get('/checkin/entry')
}

export function submitCheckin(payload) {
  return request.post('/checkin', payload)
}

export function generateCheckinAnalysis(payload) {
  return request.post('/checkin/analysis', payload)
}
