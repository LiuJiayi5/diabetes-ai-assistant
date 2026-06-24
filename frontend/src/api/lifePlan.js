import request from './request'

export function getLifePlans() {
  return request.get('/life-plans')
}

export function getLifePlanDetail(planId) {
  return request.get(`/life-plans/${encodeURIComponent(planId)}`)
}

export function generateLifePlan(payload) {
  const safePayload = {
    plan_goal: payload.plan_goal,
    avoid_items: Array.isArray(payload.avoid_items) ? payload.avoid_items : [],
    plan_days: payload.plan_days ?? 7
  }

  return request.post('/ai/life-plan/generate', safePayload)
}
