import request from './request'

export function getAiChatEntry() {
  return request.get('/ai-chat/entry')
}

export function getAiExperts() {
  return request.get('/ai-chat/experts')
}

export function sendAiDoctorMessage(payload) {
  return request.post('/ai-chat/message', payload)
}

export function getAiChatSessions(params = {}) {
  return request.get('/ai-chat/sessions', { params })
}

export function getAiChatMessages(sessionId) {
  return request.get(`/ai-chat/sessions/${sessionId}/messages`)
}

export function clearAiChatSession(sessionId) {
  return request.post(`/ai-chat/sessions/${sessionId}/clear`)
}

export function deleteAiChatSession(sessionId) {
  return request.delete(`/ai-chat/sessions/${sessionId}`)
}

export function getAdminAiChatSessions(params = {}) {
  return request.get('/admin/ai-chat/sessions', { params })
}

export function getAdminAiChatLogs(params = {}) {
  return request.get('/admin/ai-chat/logs', { params })
}

export function getAdminAiChatLogDetail(messageId) {
  return request.get(`/admin/ai-chat/logs/${messageId}`)
}

export function getAdminAiExperts(params = {}) {
  return request.get('/admin/ai-experts', { params })
}

export function createAdminAiExpert(payload) {
  return request.post('/admin/ai-experts', payload)
}

export function updateAdminAiExpert(expertId, payload) {
  return request.put(`/admin/ai-experts/${expertId}`, payload)
}

export function deleteAdminAiExpert(expertId) {
  return request.delete(`/admin/ai-experts/${expertId}`)
}
