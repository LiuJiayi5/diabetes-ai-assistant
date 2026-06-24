import request from './request'

function unwrap(response) {
  return response?.data ?? response
}

export function login(payload) {
  return request.post('/auth/login', payload).then(unwrap)
}

export function register(payload) {
  return request.post('/auth/register', payload).then(unwrap)
}

export function getCurrentUser() {
  return request.get('/user/me').then(unwrap)
}

export function updateCurrentUser(payload) {
  return request.put('/user/me', payload).then(unwrap)
}

export function adminListUsers(params) {
  return request.get('/admin/users', { params }).then(unwrap)
}

export function adminUpdateUserStatus(userId, status) {
  return request.put(`/admin/users/${encodeURIComponent(userId)}/status`, { status }).then(unwrap)
}
