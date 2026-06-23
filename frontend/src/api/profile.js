import request from './request'

export function getMyProfile() {
  return request.get('/profile/entry')
}

export function saveProfile(payload) {
  return request.post('/profile', payload)
}
