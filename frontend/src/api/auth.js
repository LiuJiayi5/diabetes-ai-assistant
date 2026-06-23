import request from './request'

export function login(payload) {
  return request.post('/auth/login', payload)
}

export function register(payload) {
  return request.post('/auth/register', payload)
}

export function getCurrentUser() {
  return request.get('/user/current')
}

export function adminListUsers(params) {
  return request.get('/user/admin/list', { params })
}
