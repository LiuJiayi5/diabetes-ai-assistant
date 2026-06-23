import request from './request'

export function getAiChatEntry() {
  return request.get('/ai-chat/entry')
}

export function sendAiDoctorMessage(payload) {
  return request.post('/ai-chat/message', payload)
}
