<template>
  <div class="admin-checkin-page">
    <section class="page-head">
      <div>
        <h2>生活打卡与行为分析</h2>
        <p>查看患者打卡、统计完成情况、AI 分析结果和调用日志。</p>
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

      <div v-show="activeTab === 'records'" class="tab-body">
        <section class="filter-card panel-filter">
          <div class="field">
            <label>患者ID</label>
            <input v-model.trim="recordFilters.patientKeyword" placeholder="例如 3" @keyup.enter="applyRecordFilters" />
          </div>
          <div class="field">
            <label>打卡开始日期</label>
            <input v-model="recordFilters.startDate" type="date" />
          </div>
          <div class="field">
            <label>打卡结束日期</label>
            <input v-model="recordFilters.endDate" type="date" />
          </div>
          <div class="field">
            <label>打卡类型</label>
            <select v-model="recordFilters.taskType">
              <option value="">全部</option>
              <option value="diet">饮食</option>
              <option value="exercise">运动</option>
            </select>
          </div>
          <div class="field">
            <label>完成状态</label>
            <select v-model="recordFilters.status">
              <option value="">全部</option>
              <option value="completed">已完成</option>
              <option value="pending">待完成</option>
              <option value="missed">未完成</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" type="button" @click="applyRecordFilters">查询</button>
            <button class="ghost-btn" type="button" @click="resetRecordFilters">重置</button>
          </div>
        </section>
        <div class="table-head">
          <strong>打卡记录列表</strong>
          <span>共 {{ records.total }} 条</span>
        </div>
        <div v-if="loading.records" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>ID</th>
              <th>患者</th>
              <th>日期</th>
              <th>类型</th>
              <th>任务</th>
              <th>状态</th>
              <th>完成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records.list" :key="item.checkin_id">
              <td>#{{ item.checkin_id }}</td>
              <td>
                <strong>{{ item.patient?.username || '未知用户' }}</strong>
                <small>ID {{ item.user_id }}</small>
              </td>
              <td>{{ item.checkin_date || '-' }}</td>
              <td>{{ taskTypeText(item.task_type) }}</td>
              <td>{{ item.task_name || '-' }}</td>
              <td><span :class="['badge', item.status]">{{ statusText(item.status) }}</span></td>
              <td>{{ formatTime(item.completed_time) }}</td>
              <td><button class="link-btn" type="button" @click="openRecord(item.checkin_id)">详情</button></td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.records && !records.list.length" text="暂无打卡记录" />
        <Pagination
          v-if="records.total > records.page_size"
          :page="records.page"
          :page-size="records.page_size"
          :total="records.total"
          @change="loadRecords"
        />
      </div>

      <div v-show="activeTab === 'analyses'" class="tab-body">
        <section class="filter-card panel-filter analysis-filter">
          <div class="field">
            <label>患者ID</label>
            <input v-model.trim="analysisFilters.patientKeyword" placeholder="例如 4" @keyup.enter="applyAnalysisFilters" />
          </div>
          <div class="field">
            <label>分析开始日期</label>
            <input v-model="analysisFilters.startDate" type="date" />
          </div>
          <div class="field">
            <label>分析结束日期</label>
            <input v-model="analysisFilters.endDate" type="date" />
          </div>
          <div class="field">
            <label>分析状态</label>
            <select v-model="analysisFilters.callStatus">
              <option value="">全部</option>
              <option value="success">成功</option>
              <option value="failed">失败</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" type="button" @click="applyAnalysisFilters">查询</button>
            <button class="ghost-btn" type="button" @click="resetAnalysisFilters">重置</button>
          </div>
        </section>
        <div class="table-head">
          <strong>AI 打卡分析结果</strong>
          <span>共 {{ analyses.total }} 条</span>
        </div>
        <div v-if="loading.analyses" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>ID</th>
              <th>患者</th>
              <th>周期</th>
              <th>完成率</th>
              <th>评分</th>
              <th>状态</th>
              <th>生成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in analyses.list" :key="item.analysis_id">
              <td>#{{ item.analysis_id }}</td>
              <td>
                <strong>{{ item.patient?.username || '未知用户' }}</strong>
                <small>ID {{ item.user_id }}</small>
              </td>
              <td>{{ item.start_date }} 至 {{ item.end_date }}</td>
              <td>{{ percent(item.completion_rate) }}</td>
              <td>{{ item.habit_score ?? '-' }}</td>
              <td><span :class="['badge', item.call_status]">{{ callStatusText(item.call_status) }}</span></td>
              <td>{{ formatTime(item.create_time) }}</td>
              <td><button class="link-btn" type="button" @click="openAnalysis(item.analysis_id)">详情</button></td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.analyses && !analyses.list.length" text="暂无分析结果" />
        <Pagination
          v-if="analyses.total > analyses.page_size"
          :page="analyses.page"
          :page-size="analyses.page_size"
          :total="analyses.total"
          @change="loadAnalyses"
        />
      </div>

      <div v-show="activeTab === 'inactive'" class="tab-body">
        <section class="filter-card panel-filter inactive-filter">
          <div class="field">
            <label>判定天数</label>
            <select v-model.number="inactiveFilters.days">
              <option :value="7">近 7 天</option>
              <option :value="14">近 14 天</option>
              <option :value="30">近 30 天</option>
            </select>
          </div>
          <div class="field">
            <label>显示数量</label>
            <select v-model.number="inactiveFilters.limit">
              <option :value="10">10 条</option>
              <option :value="20">20 条</option>
              <option :value="50">50 条</option>
            </select>
          </div>
          <div class="filter-actions">
            <button class="primary-btn" type="button" @click="loadInactiveUsers">查询</button>
          </div>
        </section>
        <div class="table-head">
          <strong>长期未打卡用户</strong>
          <span>规则：近 7 天无完成打卡或完成率低于 30%</span>
        </div>
        <div v-if="loading.inactive" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>患者</th>
              <th>最后完成打卡</th>
              <th>近期待完成/总数</th>
              <th>近期完成率</th>
              <th>未活跃天数</th>
              <th>原因</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in inactiveUsers" :key="item.patient?.user_id">
              <td>
                <strong>{{ item.patient?.username || '未知用户' }}</strong>
                <small>ID {{ item.patient?.user_id }}</small>
              </td>
              <td>{{ item.last_checkin_date || '暂无完成记录' }}</td>
              <td>{{ item.recent_completed_count }} / {{ item.recent_total_count }}</td>
              <td>{{ percent(item.recent_completion_rate) }}</td>
              <td>{{ item.inactive_days }}</td>
              <td>{{ item.reason }}</td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.inactive && !inactiveUsers.length" text="暂无长期未打卡用户" />
      </div>

      <div v-show="activeTab === 'logs'" class="tab-body">
        <section class="filter-card panel-filter log-filter">
          <div class="field">
            <label>患者ID</label>
            <input v-model.trim="logFilters.patientKeyword" placeholder="例如 5" @keyup.enter="applyLogFilters" />
          </div>
          <div class="field">
            <label>调用开始日期</label>
            <input v-model="logFilters.startDate" type="date" />
          </div>
          <div class="field">
            <label>调用结束日期</label>
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
          <strong>行为分析调用日志</strong>
          <span>共 {{ logs.total }} 条</span>
        </div>
        <div v-if="loading.logs" class="state">加载中...</div>
        <table v-else>
          <thead>
            <tr>
              <th>ID</th>
              <th>患者</th>
              <th>状态</th>
              <th>输入摘要</th>
              <th>输出摘要</th>
              <th>失败原因</th>
              <th>调用时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in logs.list" :key="item.log_id">
              <td>#{{ item.log_id }}</td>
              <td>
                <strong>{{ item.patient?.username || '未知用户' }}</strong>
                <small>ID {{ item.user_id }}</small>
              </td>
              <td><span :class="['badge', item.call_status]">{{ callStatusText(item.call_status) }}</span></td>
              <td class="wide">{{ item.request_summary || '-' }}</td>
              <td class="wide">{{ item.response_summary || '-' }}</td>
              <td class="wide">{{ item.error_message || '-' }}</td>
              <td>{{ formatTime(item.create_time) }}</td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!loading.logs && !logs.list.length" text="暂无调用日志" />
        <Pagination
          v-if="logs.total > logs.page_size"
          :page="logs.page"
          :page-size="logs.page_size"
          :total="logs.total"
          @change="loadLogs"
        />
      </div>
    </section>

    <div v-if="recordDetail" class="dialog-mask" @click="recordDetail = null">
      <section class="dialog figma-dialog" @click.stop>
        <header class="dialog-header">
          <h3>打卡记录详情</h3>
          <button class="close-icon-btn" type="button" aria-label="关闭" @click="recordDetail = null">
            <X :size="22" />
          </button>
        </header>
        <div class="dialog-content">
          <div class="dialog-subtitle-row">
            <span class="id-pill">CI{{ String(recordDetail.checkin_id).padStart(4, '0') }}</span>
            <strong>{{ recordDetail.task_name || '打卡任务' }}</strong>
          </div>
          <div class="detail-grid compact-detail-grid">
            <div v-for="[label, value] in recordDetailItems" :key="label" class="detail-item">
              <span>{{ label }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
          <section class="info-block info-blue">
            <h4><span>👤</span> 患者摘要</h4>
            <p>{{ recordDetail.patient?.profile_summary || '暂无' }}</p>
          </section>
          <section class="info-block info-green">
            <h4><span>📋</span> 关联方案摘要</h4>
            <p>{{ recordDetail.plan?.summary || '暂无' }}</p>
          </section>
          <section class="info-block info-amber">
            <h4><span>💡</span> 备注</h4>
            <p>{{ recordDetail.note || '暂无' }}</p>
          </section>
          <footer class="dialog-actions">
            <button class="outline-action" type="button" @click="recordDetail = null">关闭</button>
          </footer>
        </div>
      </section>
    </div>

    <div v-if="analysisDetail" class="dialog-mask" @click="analysisDetail = null">
      <section class="dialog figma-dialog analysis-dialog" @click.stop>
        <header class="dialog-header">
          <h3>AI 分析详情</h3>
          <button class="close-icon-btn" type="button" aria-label="关闭" @click="analysisDetail = null">
            <X :size="22" />
          </button>
        </header>
        <div class="dialog-content">
          <div class="dialog-subtitle-row">
            <span class="id-pill">AI{{ String(analysisDetail.analysis_id).padStart(4, '0') }}</span>
            <strong>{{ analysisDetail.patient?.username || '未知用户' }} 的行为分析</strong>
          </div>
          <div class="detail-grid">
            <div v-for="[label, value] in analysisDetailItems" :key="label" class="detail-item">
              <span>{{ label }}</span>
              <strong>{{ value }}</strong>
            </div>
          </div>
          <section class="info-block info-blue">
            <h4><span>📌</span> 生活评价</h4>
            <p>{{ analysisDetail.life_evaluation || '暂无' }}</p>
          </section>
          <section class="info-block info-blue">
            <h4><span>🥗</span> 饮食总结</h4>
            <p>{{ analysisDetail.diet_summary || '暂无' }}</p>
          </section>
          <section class="info-block info-green">
            <h4><span>🏃</span> 运动总结</h4>
            <p>{{ analysisDetail.exercise_summary || '暂无' }}</p>
          </section>
          <section class="info-block info-red">
            <h4><span>⚠️</span> 主要问题</h4>
            <ul v-if="analysisDetail.main_problems?.length">
              <li v-for="item in analysisDetail.main_problems" :key="item">{{ item }}</li>
            </ul>
            <p v-else>暂无</p>
          </section>
          <section class="info-block info-amber">
            <h4><span>💡</span> 改进建议</h4>
            <ul v-if="analysisDetail.improvement_suggestions?.length">
              <li v-for="item in analysisDetail.improvement_suggestions" :key="item">{{ item }}</li>
            </ul>
            <p v-else>暂无</p>
          </section>
          <section class="info-block info-green">
            <h4><span>🎯</span> 下一阶段重点</h4>
            <p>{{ analysisDetail.next_focus || '暂无' }}</p>
          </section>
          <section v-if="analysisDetail.call_status === 'failed'" class="info-block danger">
            <h4><span>❌</span> 失败原因</h4>
            <p>{{ analysisDetail.error_message || '暂无' }}</p>
          </section>
          <section class="info-block info-blue">
            <h4><span>🧾</span> 输入摘要</h4>
            <ul v-if="analysisDetail.input_items?.length">
              <li v-for="item in analysisDetail.input_items" :key="item">{{ item }}</li>
            </ul>
            <p v-else>暂无</p>
          </section>
          <footer class="dialog-actions">
            <button class="outline-action" type="button" @click="analysisDetail = null">关闭</button>
          </footer>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  ClipboardList,
  FileClock,
  RefreshCcw,
  Sparkles,
  Users,
  X
} from 'lucide-vue-next'
import {
  getAdminCheckinAnalysisDetail,
  getAdminCheckinAnalysisLogs,
  getAdminCheckinAnalyses,
  getAdminCheckinOverview,
  getAdminCheckinRecordDetail,
  getAdminCheckinRecords,
  getAdminInactiveUsers
} from '../adminApi'

const activeTab = ref('records')
const error = ref('')
const recordFilters = reactive({
  patientKeyword: '',
  startDate: '',
  endDate: '',
  taskType: '',
  status: ''
})
const analysisFilters = reactive({
  patientKeyword: '',
  startDate: '',
  endDate: '',
  callStatus: ''
})
const logFilters = reactive({
  patientKeyword: '',
  startDate: '',
  endDate: '',
  callStatus: ''
})
const inactiveFilters = reactive({
  days: 7,
  limit: 20
})
const loading = reactive({
  overview: false,
  records: false,
  analyses: false,
  inactive: false,
  logs: false
})
const overview = ref(null)
const records = reactive({ list: [], total: 0, page: 1, page_size: 10 })
const analyses = reactive({ list: [], total: 0, page: 1, page_size: 10 })
const logs = reactive({ list: [], total: 0, page: 1, page_size: 10 })
const inactiveUsers = ref([])
const recordDetail = ref(null)
const analysisDetail = ref(null)

const tabs = [
  { key: 'records', label: '打卡记录', icon: ClipboardList },
  { key: 'analyses', label: 'AI 分析结果', icon: Sparkles },
  { key: 'inactive', label: '长期未打卡', icon: Users },
  { key: 'logs', label: '调用日志', icon: FileClock }
]

const statCards = computed(() => {
  const data = overview.value || {}
  return [
    { label: '总完成率', value: percent(data.completion_rate), caption: `${data.completed_task_count || 0}/${data.total_task_count || 0} 项`, icon: BarChart3, tone: 'blue' },
    { label: '饮食完成', value: `${data.diet_completed_count || 0}`, caption: `共 ${data.diet_total_count || 0} 项`, icon: ClipboardList, tone: 'green' },
    { label: '运动完成', value: `${data.exercise_completed_count || 0}`, caption: `共 ${data.exercise_total_count || 0} 项`, icon: Activity, tone: 'cyan' },
    { label: '未完成数量', value: `${data.unfinished_task_count || 0}`, caption: 'pending 与 missed', icon: AlertTriangle, tone: 'red' }
  ]
})

function buildCommonParams(source) {
  const params = {}
  if (source.patientKeyword) params.patient_keyword = source.patientKeyword
  if (/^\d+$/.test(source.patientKeyword || '')) params.user_id = Number(source.patientKeyword)
  if (source.startDate) params.start_date = source.startDate
  if (source.endDate) params.end_date = source.endDate
  return params
}

const overviewParams = computed(() => buildCommonParams(recordFilters))

const recordParams = computed(() => ({
  ...buildCommonParams(recordFilters),
  ...(recordFilters.taskType ? { task_type: recordFilters.taskType } : {}),
  ...(recordFilters.status ? { status: recordFilters.status } : {})
}))

const analysisParams = computed(() => ({
  ...buildCommonParams(analysisFilters),
  ...(analysisFilters.callStatus ? { call_status: analysisFilters.callStatus } : {})
}))

const logParams = computed(() => ({
  ...buildCommonParams(logFilters),
  ...(logFilters.callStatus ? { call_status: logFilters.callStatus } : {})
}))

const recordDetailItems = computed(() => recordDetail.value ? [
  ['记录ID', `#${recordDetail.value.checkin_id}`],
  ['患者', `${recordDetail.value.patient?.username || '-'} (ID ${recordDetail.value.user_id})`],
  ['类型', taskTypeText(recordDetail.value.task_type)],
  ['任务', recordDetail.value.task_name || '-'],
  ['状态', statusText(recordDetail.value.status)],
  ['日期', recordDetail.value.checkin_date || '-'],
  ['完成时间', formatTime(recordDetail.value.completed_time)],
  ['方案', recordDetail.value.plan?.plan_title || '-']
] : [])

const analysisDetailItems = computed(() => analysisDetail.value ? [
  ['分析ID', `#${analysisDetail.value.analysis_id}`],
  ['患者', `${analysisDetail.value.patient?.username || '-'} (ID ${analysisDetail.value.user_id})`],
  ['分析周期', `${analysisDetail.value.start_date} 至 ${analysisDetail.value.end_date}`],
  ['总天数', `${analysisDetail.value.total_days || 0}`],
  ['饮食完成', `${analysisDetail.value.diet_completion_count || 0}`],
  ['运动完成', `${analysisDetail.value.exercise_completion_count || 0}`],
  ['完成率', percent(analysisDetail.value.completion_rate)],
  ['生活评分', analysisDetail.value.habit_score ?? '-'],
  ['状态', callStatusText(analysisDetail.value.call_status)]
] : [])

watch(activeTab, (tab) => {
  if (tab === 'records' && !records.list.length) loadRecords()
  if (tab === 'analyses' && !analyses.list.length) loadAnalyses()
  if (tab === 'inactive' && !inactiveUsers.value.length) loadInactiveUsers()
  if (tab === 'logs' && !logs.list.length) loadLogs()
})

onMounted(refreshAll)

async function refreshAll() {
  await Promise.all([loadOverview(), loadRecords(), loadAnalyses(), loadInactiveUsers(), loadLogs()])
}

function applyRecordFilters() {
  records.page = 1
  loadOverview()
  loadRecords(1)
}

function resetRecordFilters() {
  recordFilters.patientKeyword = ''
  recordFilters.startDate = ''
  recordFilters.endDate = ''
  recordFilters.taskType = ''
  recordFilters.status = ''
  applyRecordFilters()
}

function applyAnalysisFilters() {
  analyses.page = 1
  loadAnalyses(1)
}

function resetAnalysisFilters() {
  analysisFilters.patientKeyword = ''
  analysisFilters.startDate = ''
  analysisFilters.endDate = ''
  analysisFilters.callStatus = ''
  applyAnalysisFilters()
}

function applyLogFilters() {
  logs.page = 1
  loadLogs(1)
}

function resetLogFilters() {
  logFilters.patientKeyword = ''
  logFilters.startDate = ''
  logFilters.endDate = ''
  logFilters.callStatus = ''
  applyLogFilters()
}

async function loadOverview() {
  await run('overview', async () => {
    const response = await getAdminCheckinOverview(overviewParams.value)
    overview.value = response.data
  })
}

async function loadRecords(page = records.page) {
  const scrollTop = getContentScrollTop()
  await run('records', async () => {
    records.page = page
    const response = await getAdminCheckinRecords({ ...recordParams.value, page, page_size: records.page_size })
    assignPage(records, response.data)
  })
  await restoreContentScrollTop(scrollTop)
}

async function loadAnalyses(page = analyses.page) {
  const scrollTop = getContentScrollTop()
  await run('analyses', async () => {
    analyses.page = page
    const response = await getAdminCheckinAnalyses({ ...analysisParams.value, page, page_size: analyses.page_size })
    assignPage(analyses, response.data)
  })
  await restoreContentScrollTop(scrollTop)
}

async function loadInactiveUsers() {
  await run('inactive', async () => {
    const response = await getAdminInactiveUsers({ days: inactiveFilters.days, limit: inactiveFilters.limit })
    inactiveUsers.value = response.data || []
  })
}

async function loadLogs(page = logs.page) {
  const scrollTop = getContentScrollTop()
  await run('logs', async () => {
    logs.page = page
    const response = await getAdminCheckinAnalysisLogs({ ...logParams.value, page, page_size: logs.page_size })
    assignPage(logs, response.data)
  })
  await restoreContentScrollTop(scrollTop)
}

async function openRecord(id) {
  error.value = ''
  try {
    const response = await getAdminCheckinRecordDetail(id)
    recordDetail.value = response.data
  } catch (err) {
    error.value = err?.response?.data?.message || err?.message || '请求失败'
  }
}

async function openAnalysis(id) {
  error.value = ''
  try {
    const response = await getAdminCheckinAnalysisDetail(id)
    analysisDetail.value = response.data
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
  if (content) {
    content.scrollTop = scrollTop
  } else {
    window.scrollTo({ top: scrollTop })
  }
}

function percent(value) {
  const number = Number(value || 0)
  return `${number.toFixed(2)}%`
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

function taskTypeText(value) {
  return value === 'diet' ? '饮食' : value === 'exercise' ? '运动' : value || '-'
}

function statusText(value) {
  const map = { completed: '已完成', pending: '待完成', missed: '未完成' }
  return map[value] || value || '-'
}

function callStatusText(value) {
  const map = { success: '成功', failed: '失败' }
  return map[value] || value || '-'
}

const EmptyState = (props) => h('div', { class: 'state empty-state' }, props.text)
EmptyState.props = ['text']

const Pagination = (props, { emit }) => {
  const totalPages = Math.max(1, Math.ceil(props.total / props.pageSize))
  return h('div', { class: 'pagination' }, [
    h('div', { class: 'pager-group' }, [
      h('button', { class: 'pager-action pager-action-prev', type: 'button', disabled: props.page <= 1, onClick: () => emit('change', props.page - 1) }, '上一页'),
      h('span', { class: 'pager-count' }, `${props.page} / ${totalPages}`),
      h('button', { class: 'pager-action pager-action-next', type: 'button', disabled: props.page >= totalPages, onClick: () => emit('change', props.page + 1) }, '下一页')
    ])
  ])
}
Pagination.props = ['page', 'pageSize', 'total']
Pagination.emits = ['change']

</script>

<style scoped>
.admin-checkin-page {
  min-height: 100%;
  padding: 20px;
  color: #172554;
  background: #f5f8ff;
}

.page-head,
.filter-card,
.panel,
.stat-card {
  background: #fff;
  box-shadow: 0 1px 8px rgba(37, 99, 235, 0.07);
  border: 0;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 0 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
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

.filter-card {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) repeat(4, minmax(120px, 150px)) auto;
  gap: 12px;
  align-items: end;
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
}

.panel-filter {
  margin: 0;
  border-radius: 0;
  box-shadow: none;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
}

.analysis-filter,
.log-filter {
  grid-template-columns: minmax(150px, 1fr) repeat(3, minmax(130px, 160px)) auto;
}

.inactive-filter {
  grid-template-columns: repeat(2, minmax(130px, 160px)) auto;
  justify-content: start;
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
  border-radius: 10px;
  margin-bottom: 12px;
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
  margin-top: 8px;
  color: #2563eb;
  font-size: 12px;
  display: inline-block;
  width: fit-content;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.07);
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
  max-width: 230px;
  color: #475569;
  line-height: 1.5;
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

.badge.completed,
.badge.success {
  color: #15803d;
  background: rgba(34, 197, 94, 0.1);
}

.badge.pending {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
}

.badge.missed,
.badge.failed {
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 16px;
}

.pagination :deep(.pager-group) {
  display: inline-flex;
  align-items: center;
  height: 34px;
  overflow: hidden;
  border: 1.5px solid #1e3a8a;
  border-radius: 999px;
  background: transparent !important;
  box-shadow: none;
}

.pagination :deep(.pager-action),
.pagination :deep(.pager-count) {
  height: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1e3a8a;
  background: transparent !important;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.pagination :deep(.pager-action) {
  appearance: none;
  -webkit-appearance: none;
  min-width: 74px;
  padding: 0 14px;
  margin: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.pagination :deep(.pager-count) {
  min-width: 58px;
  border-left: 1.5px solid #1e3a8a;
  border-right: 1.5px solid #1e3a8a;
}

.pagination :deep(.pager-action:hover:not(:disabled)) {
  color: #2563eb;
}

.pagination :deep(.pager-action:disabled) {
  color: #94a3b8;
  background: transparent !important;
  cursor: not-allowed;
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
  width: min(676px, 100%);
  max-height: 86vh;
  overflow: auto;
  border-radius: 22px;
  background: #eef4ff;
  box-shadow: 0 20px 60px rgba(37, 99, 235, 0.15);
}

.analysis-dialog {
  width: min(760px, 100%);
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.compact-detail-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
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

.compact-detail-grid .detail-item {
  min-height: 54px;
  padding: 8px 10px;
  border-radius: 14px;
}

.detail-item span,
.info-block h4 {
  display: block;
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

.compact-detail-grid .detail-item span {
  margin-bottom: 3px;
  font-size: 11px;
}

.detail-item strong,
.info-block p,
.info-block li {
  color: #172554;
  font-size: 14px;
  line-height: 1.6;
}

.detail-item strong {
  font-size: 14px;
  overflow-wrap: anywhere;
}

.compact-detail-grid .detail-item strong {
  font-size: 12px;
  line-height: 1.35;
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
.info-red h4 { color: #dc2626; }

.info-block p,
.info-block ul {
  margin: 0;
}

.info-block ul {
  padding-left: 18px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.outline-action,
.solid-action {
  height: 48px;
  min-width: 96px;
  padding: 0 22px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 800;
}

.outline-action {
  border: 3px solid rgba(37, 99, 235, 0.25);
  color: #64748b;
  background: transparent;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);
}

.solid-action {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
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
