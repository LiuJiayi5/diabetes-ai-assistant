export default [
  { path: 'ai-chat', name: 'AiChat', component: () => import('./views/AiChatEntryView.vue'), meta: { title: 'AI 医生助手' } },
  { path: 'ai-chat/chat', name: 'AiChatConversation', component: () => import('./views/AiChatConversationView.vue'), meta: { title: 'AI 医生咨询' } },
  { path: 'ai-chat/history', name: 'AiChatHistory', component: () => import('./views/AiChatHistoryView.vue'), meta: { title: '咨询历史' } }
]
