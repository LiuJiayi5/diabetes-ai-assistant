<template>
  <div class="admin-ai-chat-page">
    <section class="page-head">
      <div>
        <h2>AI 医生咨询日志</h2>
        <p>查看用户咨询内容、AI 回复、上下文摘要和调用状态。</p>
      </div>
      <button class="primary-btn" type="button" @click="refreshAll">
        <RefreshCcw :size="16" />
        刷新
      </button>
    </section>

    <section class="stats-grid">
      <article v-for="card in statCards" :key="card.label" class="stat-card">
        <div :class="['stat-icon', card.tone]">
          <component :is="card.icon" :size="19" />
        </div>
        <strong>{{ card.value }}</strong>
        <span>{{ card.label }}</span>
        <small>{{ card.caption }}</small>
      </article>
    </section>

    <section class="panel">
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <component :is="tab.icon" :size="16" />
          {{ tab.label }}
        </button>
      </div>

      <div v-if="error" class="state error-state">{{ error }}</div>

      <div v-show="activeTab === 'logs'" class="tab-body">
        <section class="filter-card log-filter">
          <div class="field">
            <label>用户</label>
            <input v-model.trim="logFilters.userKeyword" placeholder="用户名或用户ID" @keyup.enter="applyLogFilters" />
          </div>
          <div class="field">
            <label>专家</label>
            <select v-model="logFilters.expertId">
              <option value="">全部专家</option>
              <option v-for="expert in experts" :key="expert.expert_id" :value="expert.expert_id">
                {{ expert.expert_name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>关键词</label>
            <input v-model.trim="logFilters.keyword" placeholder="问题或回复关键词" @keyup.enter="applyLogFilters" />
          </div>
          <div class="field">
            <label>开始日期</label>
            <input v-model="logFilters.startDate" type="date" />
          </div>
          <div class="field">
            <label>结束日期</label>
            <input v-model="logFilters.endDate" type="date" />
          </div>
          <div class="field">
            <label>调用状态</label>
            <select v-model="logFilters.callStatus">
              <option value="">全部</option>
              <option value="success">成功</option>
              <option value="failed">失败</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" type="button" @click="applyLogFilters">查询</button>
            <button class="ghost-btn" type="button" @click="resetLogFilters">重置</button>
          </div>
        </section>

        <div class="table-head">
          <strong>咨询消息日志</strong>
          <span>共 {{ logs.total }} 条</span>
        </div>
        <div v-if="loading.logs" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>专家</th>
              <th>会话</th>
              <th>问题摘要</th>
              <th>回复摘要</th>
              <th>状态</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in logs.list" :key="item.message_id">
              <td>#{{ item.message_id }}</td>
              <td>
                <strong>{{ item.username || '未知用户' }}</strong>
                <small>ID {{ item.user_id }}</small>
              </td>
              <td>
                <strong>{{ item.expert_name || '通用助手' }}</strong>
                <small>ID {{ item.expert_id || '-' }}</small>
              </td>
              <td>
                <strong>{{ item.session_title || 'AI 医生咨询' }}</strong>
                <small>会话 #{{ item.session_id }}</small>
              </td>
              <td class="wide">{{ item.user_message || '-' }}</td>
              <td class="wide">{{ item.ai_response || '-' }}</td>
              <td><span :class="['badge', item.call_status]">{{ callStatusText(item.call_status) }}</span></td>
              <td>{{ formatTime(item.create_time) }}</td>
              <td><button class="link-btn" type="button" @click="openLog(item.message_id)">详情</button></td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.logs && !logs.list.length" text="暂无咨询日志" />
        <div
          v-if="logs.total > logs.page_size"
          class="admin-pagination admin-pagination--panel"
        >
          <el-pagination
            v-model:current-page="logs.page"
            v-model:page-size="logs.page_size"
            :total="logs.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadLogs"
            @size-change="handleLogSizeChange"
          />
        </div>
      </div>

      <div v-show="activeTab === 'sessions'" class="tab-body">
        <section class="filter-card session-filter">
          <div class="field">
            <label>用户</label>
            <input v-model.trim="sessionFilters.userKeyword" placeholder="用户名或用户ID" @keyup.enter="applySessionFilters" />
          </div>
          <div class="field">
            <label>专家</label>
            <select v-model="sessionFilters.expertId">
              <option value="">全部专家</option>
              <option v-for="expert in experts" :key="expert.expert_id" :value="expert.expert_id">
                {{ expert.expert_name }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>会话关键词</label>
            <input v-model.trim="sessionFilters.keyword" placeholder="会话标题关键词" @keyup.enter="applySessionFilters" />
          </div>
          <div class="field">
            <label>开始日期</label>
            <input v-model="sessionFilters.startDate" type="date" />
          </div>
          <div class="field">
            <label>结束日期</label>
            <input v-model="sessionFilters.endDate" type="date" />
          </div>
          <div class="field">
            <label>会话状态</label>
            <select v-model="sessionFilters.status">
              <option value="">全部</option>
              <option value="active">可用</option>
              <option value="deleted">已删除</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" type="button" @click="applySessionFilters">查询</button>
            <button class="ghost-btn" type="button" @click="resetSessionFilters">重置</button>
          </div>
        </section>

        <div class="table-head">
          <strong>咨询会话列表</strong>
          <span>共 {{ sessions.total }} 条</span>
        </div>
        <div v-if="loading.sessions" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>会话ID</th>
              <th>用户</th>
              <th>专家</th>
              <th>标题</th>
              <th>消息数</th>
              <th>状态</th>
              <th>最近消息</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in sessions.list" :key="item.session_id">
              <td>#{{ item.session_id }}</td>
              <td>
                <strong>{{ item.username || '未知用户' }}</strong>
                <small>ID {{ item.user_id }}</small>
              </td>
              <td>
                <strong>{{ item.expert_name || '通用助手' }}</strong>
                <small>{{ item.expert_title || 'AI专家' }}</small>
              </td>
              <td class="wide">{{ item.session_title || 'AI 医生咨询' }}</td>
              <td>{{ item.message_count || 0 }}</td>
              <td><span :class="['badge', item.status]">{{ sessionStatusText(item.status) }}</span></td>
              <td>{{ formatTime(item.last_message_time) }}</td>
              <td>{{ formatTime(item.create_time) }}</td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.sessions && !sessions.list.length" text="暂无咨询会话" />
        <div
          v-if="sessions.total > sessions.page_size"
          class="admin-pagination admin-pagination--panel"
        >
          <el-pagination
            v-model:current-page="sessions.page"
            v-model:page-size="sessions.page_size"
            :total="sessions.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadSessions"
            @size-change="handleSessionSizeChange"
          />
        </div>
      </div>
    </section>

    <div v-if="detail" class="dialog-mask" @click="detail = null">
      <section class="dialog ai-log-dialog" @click.stop>
        <header class="dialog-header">
          <h3>咨询日志详情</h3>
          <button class="close-icon-btn" type="button" aria-label="关闭" @click="detail = null">
            <X :size="22" />
          </button>
        </header>
        <div class="dialog-content">
          <div class="dialog-subtitle-row">
            <span class="id-pill">MSG{{ String(detail.message_id).padStart(4, '0') }}</span>
            <strong>{{ detail.username || '未知用户' }} 的 AI 医生咨询</strong>
          </div>
          <div class="detail-grid">
            <div v-for="[label, value] in detailItems" :key="label" class="detail-item">
              <span>{{ label }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
          <section class="info-block info-blue">
            <h4><MessageCircle :size="18" /> 用户问题</h4>
            <p>{{ detail.user_message || '暂无' }}</p>
          </section>
          <section class="info-block info-green">
            <h4><Bot :size="18" /> AI 回复</h4>
            <p v-for="(line, index) in textLines(detail.ai_response)" :key="index">{{ line }}</p>
          </section>
          <section class="info-block info-amber">
            <h4><FileText :size="18" /> 上下文摘要</h4>
            <div class="context-block">
              <article v-for="item in contextItems" :key="item.label">
                <span>{{ item.label }}</span>
                <p>{{ item.value }}</p>
              </article>
            </div>
          </section>
          <section v-if="detail.call_status === 'failed'" class="info-block danger">
            <h4><AlertCircle :size="18" /> 失败原因</h4>
            <p>{{ detail.error_message || '暂无失败原因' }}</p>
          </section>
          <footer class="dialog-actions">
            <button class="outline-action" type="button" @click="detail = null">关闭</button>
          </footer>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { AlertCircle, Bot, FileText, FileClock, MessageCircle, MessagesSquare, RefreshCcw, X } from 'lucide-vue-next'
import { getAdminAiChatLogDetail, getAdminAiChatLogs, getAdminAiChatSessions, getAdminAiExperts } from '@/api/aiChat'

const activeTab = ref('logs')
const error = ref('')
const detail = ref(null)
const experts = ref([])
const loading = reactive({
  logs: false,
  sessions: false
})
const logs = reactive({ list: [], total: 0, page: 1, page_size: 10 })
const sessions = reactive({ list: [], total: 0, page: 1, page_size: 10 })
const logFilters = reactive({
  userKeyword: '',
  expertId: '',
  keyword: '',
  startDate: '',
  endDate: '',
  callStatus: ''
})
const sessionFilters = reactive({
  userKeyword: '',
  expertId: '',
  keyword: '',
  startDate: '',
  endDate: '',
  status: ''
})

const tabs = [
  { key: 'logs', label: '消息日志', icon: FileClock },
  { key: 'sessions', label: '会话列表', icon: MessagesSquare }
]

const statCards = computed(() => [
  { label: '消息日志', value: logs.total || 0, caption: '当前筛选范围', icon: FileClock, tone: 'blue' },
  { label: '咨询会话', value: sessions.total || 0, caption: '当前筛选范围', icon: MessagesSquare, tone: 'cyan' },
  { label: '成功调用', value: logs.list.filter((item) => item.call_status === 'success').length, caption: '当前页统计', icon: MessageCircle, tone: 'green' },
  { label: '失败调用', value: logs.list.filter((item) => item.call_status === 'failed').length, caption: '当前页统计', icon: AlertCircle, tone: 'red' }
])

const detailItems = computed(() => detail.value ? [
  ['消息ID', `#${detail.value.message_id}`],
  ['会话ID', `#${detail.value.session_id}`],
  ['用户', `${detail.value.username || '-'} (ID ${detail.value.user_id})`],
  ['专家', `${detail.value.expert_name || '通用助手'} (ID ${detail.value.expert_id || '-'})`],
  ['会话标题', detail.value.session_title || 'AI 医生咨询'],
  ['调用状态', callStatusText(detail.value.call_status)],
  ['创建时间', formatTime(detail.value.create_time)]
] : [])

const contextItems = computed(() => parseContext(detail.value?.context_summary))

watch(activeTab, (tab) => {
  if (tab === 'logs' && !logs.list.length) loadLogs()
  if (tab === 'sessions' && !sessions.list.length) loadSessions()
})

onMounted(refreshAll)

async function refreshAll() {
  await Promise.all([loadExperts(), loadLogs(), loadSessions()])
}

function buildParams(source, statusKey = 'call_status') {
  const params = {}
  if (source.userKeyword) params.user_keyword = source.userKeyword
  if (/^\d+$/.test(source.userKeyword || '')) params.user_id = Number(source.userKeyword)
  if (source.expertId) params.expert_id = Number(source.expertId)
  if (source.keyword) params.keyword = source.keyword
  if (source.startDate) params.start_date = source.startDate
  if (source.endDate) params.end_date = source.endDate
  if (source.callStatus) params[statusKey] = source.callStatus
  if (source.status) params.status = source.status
  return params
}

async function loadExperts() {
  try {
    const response = await getAdminAiExperts({ page: 1, page_size: 100, status: 'enabled' })
    experts.value = response.data?.list || []
  } catch {
    experts.value = []
  }
}

async function loadLogs(page = logs.page) {
  const scrollTop = getContentScrollTop()
  await run('logs', async () => {
    logs.page = page
    const response = await getAdminAiChatLogs({ ...buildParams(logFilters), page, page_size: logs.page_size })
    assignPage(logs, response.data)
  })
  await restoreContentScrollTop(scrollTop)
}

async function loadSessions(page = sessions.page) {
  const scrollTop = getContentScrollTop()
  await run('sessions', async () => {
    sessions.page = page
    const response = await getAdminAiChatSessions({ ...buildParams(sessionFilters, 'status'), page, page_size: sessions.page_size })
    assignPage(sessions, response.data)
  })
  await restoreContentScrollTop(scrollTop)
}

function applyLogFilters() {
  logs.page = 1
  loadLogs(1)
}

function resetLogFilters() {
  logFilters.userKeyword = ''
  logFilters.expertId = ''
  logFilters.keyword = ''
  logFilters.startDate = ''
  logFilters.endDate = ''
  logFilters.callStatus = ''
  applyLogFilters()
}

function handleLogSizeChange() {
  logs.page = 1
  loadLogs(1)
}

function applySessionFilters() {
  sessions.page = 1
  loadSessions(1)
}

function resetSessionFilters() {
  sessionFilters.userKeyword = ''
  sessionFilters.expertId = ''
  sessionFilters.keyword = ''
  sessionFilters.startDate = ''
  sessionFilters.endDate = ''
  sessionFilters.status = ''
  applySessionFilters()
}

function handleSessionSizeChange() {
  sessions.page = 1
  loadSessions(1)
}

async function openLog(messageId) {
  error.value = ''
  try {
    const response = await getAdminAiChatLogDetail(messageId)
    detail.value = response.data
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '请求失败'
  }
}

async function run(key, fn) {
  loading[key] = true
  error.value = ''
  try {
    await fn()
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '请求失败'
  } finally {
    loading[key] = false
  }
}

function assignPage(target, pageData = {}) {
  target.list = pageData.list || []
  target.total = pageData.total || 0
  target.page = pageData.page || 1
  target.page_size = pageData.page_size || 10
}

function getContentScrollTop() {
  return document.querySelector('.admin-content')?.scrollTop ?? window.scrollY
}

async function restoreContentScrollTop(scrollTop) {
  await nextTick()
  const content = document.querySelector('.admin-content')
  if (content) content.scrollTop = scrollTop
  else window.scrollTo({ top: scrollTop })
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

function callStatusText(value) {
  const map = { success: '成功', failed: '失败' }
  return map[value] || value || '-'
}

function sessionStatusText(value) {
  const map = { active: '可用', deleted: '已删除' }
  return map[value] || value || '-'
}

function textLines(value) {
  const lines = String(value || '暂无').split(/\n+/).map((line) => line.trim()).filter(Boolean)
  return lines.length ? lines : ['暂无']
}

function parseContext(raw) {
  if (!raw) return [{ label: '上下文', value: '暂无上下文摘要' }]
  try {
    const data = JSON.parse(raw)
    const checkin = data.checkin || {}
    const items = [
      ['专家身份', readable(data.expert_identity)],
      ['用户信息', readable(data.user_basic)],
      ['健康档案', data.profile_summary],
      ['最新健康数据', data.latest_health_data],
      ['风险评估', data.risk_result],
      ['生活方案', data.life_plan],
      ['最近打卡', checkin.recent_summary],
      ['最近分析', checkin.latest_analysis]
    ]
    return items
      .filter(([, value]) => value !== undefined && value !== null && value !== '')
      .map(([label, value]) => ({ label, value: readable(value) }))
  } catch {
    return [{ label: '上下文', value: raw }]
  }
}

function readable(value) {
  if (value === null || value === undefined || value === '') return '暂无'
  if (typeof value === 'string') return value
  if (Array.isArray(value)) return value.length ? value.map(readable).join('；') : '暂无'
  if (typeof value === 'object') {
    return Object.entries(value).map(([key, val]) => `${key}: ${readable(val)}`).join('；')
  }
  return String(value)
}

const EmptyState = (props) => h('div', { class: 'state empty-state' }, props.text)
EmptyState.props = ['text']

</script>

<style scoped>
.admin-ai-chat-page {
  min-height: 100%;
  padding: 20px;
  color: #172554;
  background: #f5f8ff;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 0 0;
}

.page-head h2 {
  margin: 0 0 4px;
  color: #172554;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 800;
}

.page-head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.filter-card,
.panel,
.stat-card {
  background: #fff;
  box-shadow: 0 1px 8px rgba(37, 99, 235, 0.07);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.stat-card {
  padding: 16px;
  border-radius: 12px;
}

.stat-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  margin-bottom: 12px;
  border-radius: 10px;
}

.stat-icon.blue { color: #2563eb; background: rgba(37, 99, 235, 0.1); }
.stat-icon.green { color: #16a34a; background: rgba(34, 197, 94, 0.1); }
.stat-icon.cyan { color: #0891b2; background: rgba(6, 182, 212, 0.1); }
.stat-icon.red { color: #dc2626; background: rgba(239, 68, 68, 0.1); }

.stat-card strong,
.stat-card span,
.stat-card small {
  display: block;
}

.stat-card strong {
  color: #172554;
  font-size: 26px;
  line-height: 1;
}

.stat-card span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.stat-card small {
  display: inline-block;
  width: fit-content;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.07);
  font-size: 12px;
}

.panel {
  margin-top: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.tabs {
  display: flex;
  gap: 6px;
  padding: 12px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
  background: #fff;
}

.tabs button {
  height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 12px;
  border-radius: 10px;
  color: #64748b;
  background: #fff;
  font-size: 14px;
  font-weight: 700;
}

.tabs button.active {
  color: #2563eb;
  background: #eef3ff;
}

.tab-body {
  padding: 0 0 12px;
}

.filter-card {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(180px, 1.4fr) repeat(3, minmax(120px, 150px)) auto;
  gap: 12px;
  align-items: end;
  padding: 16px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
  box-shadow: none;
}

.field label {
  display: block;
  margin-bottom: 6px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.field input,
.field select {
  width: 100%;
  height: 36px;
  border: 1.5px solid #e5eaf3;
  border-radius: 12px;
  padding: 0 12px;
  background: #f8faff;
  color: #172554;
  font-size: 14px;
  outline: none;
}

.field input:focus,
.field select:focus {
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.primary-btn,
.ghost-btn {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 12px;
  padding: 0 18px;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
}

.primary-btn {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.25);
}

.ghost-btn {
  border: 1.5px solid #e5eaf3;
  color: #64748b;
  background: #fff;
}

.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
}

.table-head strong {
  color: #172554;
  font-size: 15px;
}

.table-head span {
  padding: 4px 10px;
  border-radius: 999px;
  color: #2563eb;
  background: #eef3ff;
  font-size: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: #f8faff;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  text-transform: uppercase;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.06);
  vertical-align: top;
}

td {
  color: #172554;
  font-size: 13px;
  line-height: 1.45;
}

td small {
  display: block;
  margin-top: 4px;
  color: #94a3b8;
}

td.wide {
  max-width: 260px;
  color: #475569;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.badge.success,
.badge.active {
  color: #15803d;
  background: rgba(34, 197, 94, 0.1);
}

.badge.failed,
.badge.deleted {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.1);
}

.link-btn {
  color: #2563eb;
  background: transparent;
  font-weight: 700;
  border-radius: 8px;
  padding: 4px 8px;
}

.link-btn:hover {
  background: #eef3ff;
}

.state {
  padding: 28px;
  text-align: center;
  color: #94a3b8;
}

.error-state {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.06);
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(2px);
}

.dialog {
  width: min(820px, 100%);
  max-height: 86vh;
  overflow: auto;
  border-radius: 22px;
  background: #eef4ff;
  box-shadow: 0 20px 60px rgba(37, 99, 235, 0.15);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 22px 6px;
}

.dialog h3 {
  margin: 0;
  color: #172554;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 800;
}

.close-icon-btn {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #50607a;
  background: transparent;
}

.close-icon-btn:hover {
  background: rgba(37, 99, 235, 0.08);
}

.dialog-content {
  padding: 18px 22px 20px;
}

.dialog-subtitle-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 4px 0 18px;
}

.dialog-subtitle-row strong {
  color: #172554;
  font-size: 16px;
}

.id-pill {
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.detail-item,
.info-block {
  border: 1px solid #e5eaf3;
  border-radius: 18px;
  background: #f8faff;
}

.detail-item {
  min-height: 68px;
  padding: 12px 14px;
}

.detail-item span,
.info-block h4 {
  display: block;
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

.detail-item strong,
.info-block p {
  color: #172554;
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.info-block {
  margin-top: 12px;
  padding: 14px 18px;
}

.info-block.danger {
  border-color: rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.06);
}

.info-block h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 16px;
}

.info-blue h4 { color: #2563eb; }
.info-green h4 { color: #22c55e; }
.info-amber h4 { color: #f59e0b; }

.info-block p {
  margin: 0;
  white-space: pre-wrap;
}

.info-block p + p {
  margin-top: 8px;
}

.context-block {
  display: grid;
  gap: 8px;
}

.context-block article {
  padding: 10px 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #eef2fb;
}

.context-block span {
  display: block;
  margin-bottom: 5px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.context-block p {
  color: #475569;
  font-size: 13px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.outline-action {
  height: 48px;
  min-width: 96px;
  padding: 0 22px;
  border-radius: 22px;
  border: 3px solid rgba(37, 99, 235, 0.25);
  color: #64748b;
  background: transparent;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  font-size: 15px;
  font-weight: 800;
}

@media (max-width: 1120px) {
  .filter-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
