import request from './request'

function unwrap(response) {
  return response?.data ?? response
}

export function adminListUsers(params) {
  return request.get('/admin/users', { params }).then(unwrap)
}

export function adminListProfiles(params) {
  return request.get('/profile/admin', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminGetProfileDetail(userId) {
  return request.get(`/profile/admin/${encodeURIComponent(userId)}`, { sessionScope: 'admin' }).then(unwrap)
}

export function adminListHealthMetrics(params) {
  return request.get('/health-metric/admin', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminListRiskAssessments(params) {
  return request.get('/risk/admin', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminGetRiskAssessmentDetail(assessmentId) {
  return request.get(`/risk/admin/${encodeURIComponent(assessmentId)}`, { sessionScope: 'admin' }).then(unwrap)
}

export function adminGetRiskSimilarCases(assessmentId, params) {
  return request.get(`/risk/admin/${encodeURIComponent(assessmentId)}/similar-cases`, {
    params,
    sessionScope: 'admin'
  }).then(unwrap)
}

export function adminGetRiskTrends(params) {
  return request.get('/risk/admin/trends', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminGetMetricTrends(params) {
  return request.get('/health-metric/admin/trends', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminUpdateUserStatus(userId, status) {
  return request.put(`/admin/users/${encodeURIComponent(userId)}/status`, { status }).then(unwrap)
}

export function adminListLifePlans(params) {
  return request.get('/admin/life-plans', { params }).then(unwrap)
}

export function adminGetLifePlanDetail(planId) {
  return request.get('/admin/life-plans', {
    params: { plan_id: planId, page: 1, page_size: 1 }
  }).then(unwrap)
}

export function adminGetContentManagement(params) {
  return request.get('/admin/content-management', { params }).then(unwrap)
}

export function adminGetRecommendationDashboard(params) {
  return request.get('/admin/content-recommendations/dashboard', { params, sessionScope: 'admin' }).then(unwrap)
}

export function adminSaveArticle(payload) {
  return request.post('/admin/articles/save', payload).then(unwrap)
}

export function adminSaveHomeContent(payload) {
  return request.post('/admin/home-contents/save', payload).then(unwrap)
}

export function adminDeleteArticle(articleId) {
  return request.delete(`/admin/articles/${encodeURIComponent(articleId)}`).then(unwrap)
}

export function adminDeleteHomeContent(contentId) {
  return request.delete(`/admin/home-contents/${encodeURIComponent(contentId)}`).then(unwrap)
}
