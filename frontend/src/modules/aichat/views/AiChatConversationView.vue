<template>
  <AiChatPageShell title="AI 医生咨询" subtitle="连续追问你的控糖问题" flush @refresh="reloadCurrentSession">
    <section class="chat-view">
      <div class="doctor-strip">
        <div class="doctor-mini-avatar">
          <Bot :size="22" />
        </div>
        <div>
          <h2>AI 医生助手</h2>
          <p>糖尿病预治智能助手 · 在线</p>
        </div>
        <button class="history-button" type="button" @click="router.push('/app/ai-chat/history')">
          <History :size="15" />
          <span>历史</span>
        </button>
      </div>

      <div ref="messageListRef" class="message-list mobile-scroll">
        <article class="message-row message-row--ai">
          <div class="avatar avatar--ai"><Bot :size="17" /></div>
          <div class="bubble bubble--ai">
            <p>你好，我可以帮你解释血糖、饮食、运动、风险评估和打卡分析相关问题。你也可以直接问我最近打卡情况应该怎么调整。</p>
          </div>
        </article>

        <template v-for="message in displayMessages" :key="message.local_id || message.message_id">
          <article class="message-row message-row--user">
            <div class="bubble bubble--user">
              <p>{{ message.user_message }}</p>
            </div>
            <div class="avatar avatar--user">我</div>
          </article>
          <article class="message-row message-row--ai">
            <div class="avatar avatar--ai"><Bot :size="17" /></div>
            <div class="bubble bubble--ai" :class="{ 'bubble--failed': message.call_status === 'failed' }">
              <p v-for="(line, index) in answerLines(message)" :key="`${message.message_id || message.local_id}-${index}`">{{ line }}</p>
              <span v-if="message.call_status === 'failed'" class="error-text">{{ message.error_message || '本次回复不稳定，可以稍后重试。' }}</span>
            </div>
          </article>
        </template>

        <article v-if="sending" class="message-row message-row--ai">
          <div class="avatar avatar--ai"><Bot :size="17" /></div>
          <div class="bubble bubble--ai bubble--typing">
            <LoaderCircle class="spin" :size="16" />
            <span>正在整理回复</span>
          </div>
        </article>

        <section v-if="!loading && displayMessages.length === 0" class="hint-card">
          <p>可以从一个具体问题开始，比如“帮我解释最近一周打卡分析”。</p>
          <div>
            <button v-for="item in quickQuestions" :key="item" type="button" @click="draft = item">{{ item }}</button>
          </div>
        </section>
      </div>

      <footer class="composer">
        <div class="composer-actions">
          <button type="button" :disabled="!sessionId || clearing || sending" @click="clearSession">
            <Eraser :size="15" />
            <span>清空</span>
          </button>
          <button type="button" :disabled="!sessionId || deleting || sending" @click="deleteSession">
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
        </div>
        <div class="input-row">
          <textarea
            v-model="draft"
            maxlength="2000"
            rows="1"
            placeholder="输入你的问题"
            :disabled="sending"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <button class="send-button" type="button" :disabled="sending || !draft.trim()" @click="sendMessage">
            <LoaderCircle v-if="sending" class="spin" :size="17" />
            <SendHorizontal v-else :size="18" />
          </button>
        </div>
        <p class="composer-note">AI 建议仅供参考，不能替代线下诊疗。</p>
      </footer>
    </section>
  </AiChatPageShell>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { Bot, Eraser, History, LoaderCircle, SendHorizontal, Trash2 } from 'lucide-vue-next'
import {
  clearAiChatSession,
  deleteAiChatSession as deleteAiChatSessionApi,
  getAiChatMessages,
  sendAiDoctorMessage
} from '@/api/aiChat'
import AiChatPageShell from '../components/AiChatPageShell.vue'

const route = useRoute()
const router = useRouter()
const messageListRef = ref(null)
const messages = ref([])
const draft = ref('')
const sessionId = ref(null)
const conversationId = ref('')
const loading = ref(false)
const sending = ref(false)
const clearing = ref(false)
const deleting = ref(false)

const quickQuestions = [
  '最近打卡情况怎么样？',
  '为什么我的完成率不高？',
  '根据我的饮食和运动打卡，我应该怎么改？'
]

const displayMessages = computed(() => messages.value.filter((item) => item.user_message || item.ai_response || item.answer))

function unwrap(response) {
  return response?.data ?? response
}

function normalizeMessage(item) {
  return {
    message_id: item.message_id,
    session_id: item.session_id,
    user_message: item.user_message || '',
    ai_response: item.ai_response || item.answer || '',
    call_status: item.call_status || 'success',
    error_message: item.error_message || '',
    create_time: item.create_time
  }
}

async function loadMessages(id) {
  if (!id) return
  loading.value = true
  try {
    const data = unwrap(await getAiChatMessages(id))
    messages.value = Array.isArray(data) ? data.map(normalizeMessage) : []
    sessionId.value = Number(id)
    await scrollToBottom()
  } catch (error) {
    showFailToast(error?.response?.data?.message || '咨询记录读取失败')
  } finally {
    loading.value = false
  }
}

function resetConversationState() {
  messages.value = []
  sessionId.value = null
  conversationId.value = ''
}

async function reloadCurrentSession() {
  if (sessionId.value) {
    await loadMessages(sessionId.value)
  }
}

async function sendMessage() {
  const text = draft.value.trim()
  if (!text) {
    showFailToast('请输入咨询问题')
    return
  }
  if (text.length > 2000) {
    showFailToast('问题内容过长，请精简后再发送')
    return
  }

  sending.value = true
  draft.value = ''
  const localMessage = {
    local_id: `local-${Date.now()}`,
    session_id: sessionId.value,
    user_message: text,
    ai_response: '',
    call_status: 'pending',
    create_time: new Date().toISOString()
  }
  messages.value.push(localMessage)
  await scrollToBottom()

  try {
    const payload = {
      message: text,
      session_id: sessionId.value || undefined,
      conversation_id: conversationId.value || undefined
    }
    const data = unwrap(await sendAiDoctorMessage(payload))
    sessionId.value = data?.session_id || sessionId.value
    conversationId.value = data?.conversation_id || conversationId.value
    const index = messages.value.findIndex((item) => item.local_id === localMessage.local_id)
    const normalized = normalizeMessage({
      ...data,
      ai_response: data?.answer
    })
    if (index >= 0) {
      messages.value[index] = normalized
    } else {
      messages.value.push(normalized)
    }
    router.replace({ path: '/app/ai-chat/chat', query: sessionId.value ? { session_id: sessionId.value } : {} })
    await scrollToBottom()
  } catch (error) {
    const index = messages.value.findIndex((item) => item.local_id === localMessage.local_id)
    if (index >= 0) {
      messages.value[index] = {
        ...localMessage,
        ai_response: '本次咨询暂时没有成功发送，请稍后再试。',
        call_status: 'failed',
        error_message: error?.response?.data?.message || '网络连接不稳定'
      }
    }
    showFailToast(error?.response?.data?.message || '发送失败，请稍后重试')
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

async function clearSession() {
  if (!sessionId.value) return
  try {
    await showConfirmDialog({ title: '清空当前会话', message: '清空后当前会话消息将不可见，是否继续？' })
    clearing.value = true
    await clearAiChatSession(sessionId.value)
    messages.value = []
    conversationId.value = ''
    showSuccessToast('已清空当前会话')
  } catch (error) {
    if (error) showFailToast(error?.response?.data?.message || '清空失败')
  } finally {
    clearing.value = false
  }
}

async function deleteSession() {
  if (!sessionId.value) return
  try {
    await showConfirmDialog({ title: '删除当前会话', message: '删除后可在历史列表中移除该会话，是否继续？' })
    deleting.value = true
    await deleteAiChatSessionApi(sessionId.value)
    messages.value = []
    sessionId.value = null
    conversationId.value = ''
    showSuccessToast('会话已删除')
    router.replace('/app/ai-chat/chat')
  } catch (error) {
    if (error) showFailToast(error?.response?.data?.message || '删除失败')
  } finally {
    deleting.value = false
  }
}

function answerLines(message) {
  const text = message.ai_response || message.answer || ''
  return text.split(/\n+/).map((line) => line.trim()).filter(Boolean)
}

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(() => route.query.session_id, (value) => {
  if (value) {
    loadMessages(value)
  } else {
    resetConversationState()
  }
})

onMounted(async () => {
  const querySessionId = route.query.session_id
  const prefill = route.query.q
  if (querySessionId) {
    await loadMessages(querySessionId)
  } else {
    resetConversationState()
  }
  if (typeof prefill === 'string' && prefill) {
    draft.value = prefill
  }
  await scrollToBottom()
})
</script>

<style scoped>
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--figma-bg-page);
}

.doctor-strip {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 10px;
  margin: 0 16px 10px;
  padding: 12px 14px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.doctor-mini-avatar,
.avatar--ai {
  display: grid;
  place-items: center;
  background: #E5F6EE;
  color: #4FB783;
}

.doctor-mini-avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
}

.doctor-strip h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 700;
}

.doctor-strip p {
  margin: 2px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
}

.history-button {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 10px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-secondary-green);
  color: #4FB783;
  font-size: 12px;
  font-weight: 700;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 16px 12px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.message-row--user {
  justify-content: flex-end;
}

.avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.avatar--user {
  display: grid;
  place-items: center;
  color: #FFFFFF;
  background: linear-gradient(135deg, #9FDEB8 0%, #7FD5B2 100%);
}

.bubble {
  max-width: min(76%, 268px);
  padding: 11px 13px;
  overflow-wrap: anywhere;
  word-break: break-word;
  box-shadow: var(--figma-shadow-card);
}

.bubble p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.bubble p + p {
  margin-top: 7px;
}

.bubble--ai {
  border-radius: 6px 18px 18px;
  background: #FFFFFF;
  color: var(--figma-text-secondary);
}

.bubble--user {
  border-radius: 18px 6px 18px 18px;
  background: var(--figma-green-button);
  color: #FFFFFF;
}

.bubble--failed {
  background: rgba(239, 143, 143, 0.12);
}

.bubble--typing {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--figma-text-muted);
  font-size: 12px;
}

.error-text {
  display: block;
  margin-top: 8px;
  color: #E87878;
  font-size: 11px;
  line-height: 1.5;
}

.hint-card {
  margin-top: 8px;
  padding: 14px;
  border-radius: var(--figma-radius-card);
  background: rgba(255, 255, 255, 0.72);
  color: var(--figma-text-muted);
}

.hint-card p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
}

.hint-card div {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.hint-card button {
  min-height: 34px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: #FFFFFF;
  color: #4FB783;
  font-size: 12px;
  font-weight: 600;
  text-align: left;
}

.composer {
  flex-shrink: 0;
  padding: 8px 16px 12px;
  background: var(--figma-bg-page);
  border-top: 1px solid rgba(174, 232, 199, 0.25);
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}

.composer-actions button {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 10px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.78);
  color: var(--figma-text-muted);
  font-size: 11px;
  font-weight: 600;
}

.composer-actions button:disabled {
  opacity: 0.45;
}

.input-row {
  display: grid;
  grid-template-columns: 1fr 44px;
  align-items: end;
  gap: 8px;
}

.input-row textarea {
  width: 100%;
  min-height: 44px;
  max-height: 112px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: 22px;
  background: #FFFFFF;
  color: var(--figma-text-primary);
  font-size: 13px;
  line-height: 1.5;
  resize: none;
  box-shadow: var(--figma-shadow-card);
}

.input-row textarea:focus {
  border-color: var(--figma-primary-green);
  outline: none;
}

.send-button {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--figma-green-button);
  color: #FFFFFF;
  box-shadow: var(--figma-shadow-button);
}

.send-button:disabled {
  opacity: 0.55;
}

.composer-note {
  margin: 7px 0 0;
  color: rgba(107, 114, 128, 0.7);
  font-size: 10px;
  text-align: center;
}

.spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
