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

export function getArticleCategories() {
  return Promise.resolve([])
}
