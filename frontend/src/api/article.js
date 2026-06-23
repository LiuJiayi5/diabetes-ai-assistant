import request from './request'

export function getHomeContents() {
  return request.get('/content/entry')
}

export function listArticles(params) {
  return request.get('/content/articles', { params })
}
