import request from './request'

export function getAiChatEntry() {
  return request.get('/ai-chat/entry')
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
