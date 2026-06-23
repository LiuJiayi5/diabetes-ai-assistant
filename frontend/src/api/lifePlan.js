import request from './request'

export function getLifePlanEntry() {
  return request.get('/life-plan/entry')
}

export function generateLifePlan(payload) {
  return request.post('/life-plan/generate', payload)
}
