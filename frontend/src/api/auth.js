import request from './request'

const MOCK_USER_KEY = 'diabetes_ai_assistant_mock_user'

const defaultUser = {
  id: 10001,
  username: '小代',
  phone: '138****2026',
  email: 'xiaodai@example.com',
  avatar: '',
  role: 'patient',
  status: 'active',
  lastLogin: '今天 09:30'
}

function delay(data) {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(data), 220)
  })
}

function readMockUser() {
  const raw = localStorage.getItem(MOCK_USER_KEY)
  if (!raw) return { ...defaultUser }

  try {
    return { ...defaultUser, ...JSON.parse(raw) }
  } catch {
    return { ...defaultUser }
  }
}

function writeMockUser(user) {
  localStorage.setItem(MOCK_USER_KEY, JSON.stringify(user))
}

export function login(payload) {
  return request.post('/auth/login', payload)
}

export function register(payload) {
  return request.post('/auth/register', payload)
}

export function getCurrentUser() {
  return request.get('/user/me')
}

export function updateCurrentUser(payload) {
  return request.put('/user/me', payload)
}

export function adminListUsers(params) {
  return request.get('/admin/users', { params })
}

export function mockLogin(payload) {
  const user = {
    ...readMockUser(),
    username: payload?.username || readMockUser().username,
    lastLogin: '今天 09:30'
  }
  writeMockUser(user)
  return delay({
    token: `mock-token-${Date.now()}`,
    user,
    role: user.role
  })
}

export function mockRegister(payload) {
  const user = {
    ...defaultUser,
    id: Date.now(),
    username: payload?.username || '新用户',
    phone: payload?.phone || '',
    email: payload?.email || '',
    lastLogin: '刚刚'
  }
  writeMockUser(user)
  return delay({
    token: `mock-token-${Date.now()}`,
    user,
    role: 'patient'
  })
}

export function getMockCurrentUser() {
  return delay(readMockUser())
}

export function updateMockCurrentUser(payload) {
  const user = {
    ...readMockUser(),
    ...payload
  }
  writeMockUser(user)
  return delay(user)
}
