import request from './request'

function unwrap(response) {
  return response?.data ?? response
}

export function adminListUsers(params) {
  return request.get('/admin/users', { params }).then(unwrap)
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
