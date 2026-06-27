import request from './request'

function unwrap(response) {
  return response?.data ?? response
}

export function login(payload) {
  return request.post('/auth/login', payload).then(unwrap)
}

export function resetPassword(payload) {
  return request.post('/auth/reset-password', payload).then(unwrap)
}

export function sendEmailCode(payload) {
  return request.post('/auth/email-code', payload).then(unwrap)
}

export function register(payload) {
  return request.post('/auth/register', payload).then(unwrap)
}

export function getCurrentUser(scope = 'patient') {
  return request.get('/user/me', { sessionScope: scope }).then(unwrap)
}

export function updateCurrentUser(payload, scope = 'patient') {
  return request.put('/user/me', payload, { sessionScope: scope }).then(unwrap)
}

export function adminListUsers(params) {
  return request.get('/admin/users', { params }).then(unwrap)
}

export function adminUpdateUserStatus(userId, status) {
  return request.put(`/admin/users/${encodeURIComponent(userId)}/status`, { status }).then(unwrap)
}
