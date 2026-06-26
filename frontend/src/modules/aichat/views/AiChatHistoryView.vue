<template>
  <AiChatPageShell title="咨询历史" subtitle="继续上一次 AI 医生对话" @refresh="loadSessions">
    <section v-if="loading" class="state-card">
      <LoaderCircle class="spin" :size="24" />
      <p>正在读取咨询历史</p>
    </section>

    <section v-else-if="sessions.length === 0" class="state-card">
      <MessagesSquare :size="34" />
      <h3>暂无咨询记录</h3>
      <p>开始一次 AI 医生咨询后，这里会保留你的历史会话。</p>
      <RouterLink class="primary-link" to="/app/ai-chat/chat">开始咨询</RouterLink>
    </section>

    <section v-else class="session-list">
      <article v-for="item in sessions" :key="item.session_id" class="session-card">
        <button class="session-main" type="button" @click="openSession(item)">
          <div class="session-icon">
            <MessageCircle :size="20" />
          </div>
          <div>
            <h3>{{ item.session_title || 'AI 医生咨询' }}</h3>
            <p>{{ timeText(item.last_message_time || item.create_time) }} · {{ statusText(item.status) }}</p>
          </div>
          <ChevronRight :size="17" />
        </button>
        <div class="session-actions">
          <button type="button" @click="clearSession(item)">清空</button>
          <button type="button" class="danger" @click="deleteSession(item)">删除</button>
        </div>
      </article>
    </section>

    <div v-if="total > pageSize" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPage }}</span>
      <button type="button" :disabled="page >= totalPage || loading" @click="changePage(page + 1)">下一页</button>
    </div>
  </AiChatPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { ChevronRight, LoaderCircle, MessageCircle, MessagesSquare } from 'lucide-vue-next'
import { clearAiChatSession, deleteAiChatSession, getAiChatSessions } from '@/api/aiChat'
import AiChatPageShell from '../components/AiChatPageShell.vue'

const router = useRouter()
const loading = ref(false)
const sessions = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const totalPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function unwrap(response) {
  return response?.data ?? response
}

async function loadSessions() {
  loading.value = true
  try {
    const data = unwrap(await getAiChatSessions({ page: page.value, page_size: pageSize }))
    sessions.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (error) {
    showFailToast(error?.response?.data?.message || '咨询历史读取失败')
  } finally {
    loading.value = false
  }
}

function changePage(nextPage) {
  page.value = nextPage
  loadSessions()
}

function openSession(item) {
  router.push({ path: '/app/ai-chat/chat', query: { session_id: item.session_id } })
}

async function clearSession(item) {
  try {
    await showConfirmDialog({ title: '清空会话', message: '清空后该会话的消息将不可见，是否继续？' })
    await clearAiChatSession(item.session_id)
    showSuccessToast('会话已清空')
    loadSessions()
  } catch (error) {
    if (error) showFailToast(error?.response?.data?.message || '清空失败')
  }
}

async function deleteSession(item) {
  try {
    await showConfirmDialog({ title: '删除会话', message: '删除后该会话将从历史中移除，是否继续？' })
    await deleteAiChatSession(item.session_id)
    showSuccessToast('会话已删除')
    loadSessions()
  } catch (error) {
    if (error) showFailToast(error?.response?.data?.message || '删除失败')
  }
}

function statusText(status) {
  return status === 'deleted' ? '已删除' : '可继续咨询'
}

function timeText(value) {
  if (!value) return '暂无时间'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadSessions)
</script>

<style scoped>
.session-list {
  display: grid;
  gap: 10px;
}

.session-card,
.state-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.session-card {
  padding: 14px;
}

.session-main {
  width: 100%;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 11px;
  background: transparent;
  text-align: left;
}

.session-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #E5F6EE;
  color: #4FB783;
}

.session-main h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main p {
  margin: 4px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
}

.session-main svg:last-child {
  color: var(--figma-icon-muted);
}

.session-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.session-actions button {
  min-height: 30px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: var(--figma-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.session-actions .danger {
  color: #E87878;
  background: rgba(239, 143, 143, 0.12);
}

.state-card {
  min-height: 230px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 26px 22px;
  text-align: center;
  color: var(--figma-text-muted);
}

.state-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 16px;
}

.state-card p {
  margin: 0;
  color: var(--figma-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

.primary-link {
  width: 180px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button);
}

.pager {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  color: var(--figma-text-muted);
  font-size: 12px;
}

.pager button {
  min-height: 38px;
  border-radius: var(--figma-radius-pill);
  background: #FFFFFF;
  color: var(--figma-tabbar-active);
  border: 1px solid rgba(174, 232, 199, 0.7);
  font-size: 12px;
  font-weight: 600;
}

.pager button:disabled {
  opacity: 0.45;
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
