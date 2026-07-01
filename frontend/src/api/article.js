import request from './request'

export function getHomeContents() {
  return request.get('/home')
}

export function listArticles(params) {
  return request.get('/articles', { params })
}

export function getArticleDetail(articleId) {
  return request.get(`/articles/${encodeURIComponent(articleId)}`)
}

export function getContentRecommendations(params) {
  return request.get('/content-recommendations', { params })
}

export function getPatientEducationProfile() {
  return request.get('/content-recommendations/profile')
}

export function recordArticleReadEvent(payload) {
  return request.post('/content-recommendations/read-events', payload)
}

export function getArticleCategories() {
  return Promise.resolve([])
}
