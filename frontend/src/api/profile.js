import request from './request'

export function getMyProfile() {
  return request.get('/profile/entry')
}

export function saveProfile(payload) {
  return request.post('/profile', payload)
}

export function adminListProfiles(params) {
  return request.get('/profile/admin', { params })
}

export function adminGetProfileDetail(userId) {
  return request.get(`/profile/admin/${userId}`)
}
