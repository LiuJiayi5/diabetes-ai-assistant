import request from './request'

export function getLatestInterventionReview() {
  return request.get('/intervention-reviews/latest')
}

export function getInterventionReviewHistory(params = {}) {
  return request.get('/intervention-reviews/history', { params })
}

export function getAdminInterventionReviews(params = {}) {
  return request.get('/admin/intervention-reviews', { params, sessionScope: 'admin' })
}
