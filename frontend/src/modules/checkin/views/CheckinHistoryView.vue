<template>
  <CheckinPageShell title="打卡历史" subtitle="回顾最近饮食与运动记录" @refresh="loadHistory">
    <section class="filter-card">
      <div class="filter-row">
        <label>
          <span>开始日期</span>
          <input v-model="filters.start_date" type="date" />
        </label>
        <label>
          <span>结束日期</span>
          <input v-model="filters.end_date" type="date" />
        </label>
      </div>
      <div class="chip-row">
        <button
          v-for="item in typeOptions"
          :key="item.value"
          type="button"
          :class="{ active: filters.task_type === item.value }"
          @click="filters.task_type = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="chip-row">
        <button
          v-for="item in statusOptions"
          :key="item.value"
          type="button"
          :class="{ active: filters.status === item.value }"
          @click="filters.status = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <button class="primary-button" type="button" @click="applyFilters">
        <Search :size="16" />
        <span>筛选记录</span>
      </button>
    </section>

    <section v-if="loading" class="state-card">
      <LoaderCircle class="spin" :size="24" />
      <p>正在读取历史记录</p>
    </section>

    <section v-else-if="records.length === 0" class="state-card">
      <Inbox :size="34" />
      <h3>暂无打卡记录</h3>
      <p>完成今日打卡后，这里会沉淀你的饮食和运动执行情况。</p>
    </section>

    <section v-else class="history-list">
      <article v-for="record in records" :key="record.checkin_id" class="history-card">
        <div class="record-date">
          <span>{{ dayText(record.checkin_date) }}</span>
          <strong>{{ monthText(record.checkin_date) }}</strong>
        </div>
        <div class="record-body">
          <div class="record-title">
            <h3>{{ record.task_name || taskTypeText(record.task_type) }}</h3>
            <span class="status-pill" :class="`status-pill--${record.status}`">{{ statusText(record.status) }}</span>
          </div>
          <p>{{ taskTypeText(record.task_type) }} · {{ record.completed_time ? formatTime(record.completed_time) : '未记录完成时间' }}</p>
          <p v-if="record.note" class="record-note">{{ record.note }}</p>
        </div>
      </article>
    </section>

    <div v-if="total > pageSize" class="pager">
      <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPage }}</span>
      <button type="button" :disabled="page >= totalPage || loading" @click="changePage(page + 1)">下一页</button>
    </div>
  </CheckinPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import { Inbox, LoaderCircle, Search } from 'lucide-vue-next'
import { getCheckinHistory } from '@/api/checkin'
import CheckinPageShell from '../components/CheckinPageShell.vue'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const filters = reactive({
  start_date: String(route.query.start_date || ''),
  end_date: String(route.query.end_date || ''),
  task_type: String(route.query.task_type || ''),
  status: String(route.query.status || '')
})

const typeOptions = [
  { label: '全部', value: '' },
  { label: '饮食', value: 'diet' },
  { label: '运动', value: 'exercise' }
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待完成', value: 'pending' },
  { label: '已完成', value: 'completed' },
  { label: '未完成', value: 'missed' }
]

const totalPage = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

function unwrap(response) {
  return response?.data ?? response
}

function buildParams() {
  return Object.fromEntries(Object.entries({
    page: page.value,
    page_size: pageSize,
    start_date: filters.start_date,
    end_date: filters.end_date,
    task_type: filters.task_type,
    status: filters.status
  }).filter(([, value]) => value !== '' && value !== null && value !== undefined))
}

async function loadHistory() {
  loading.value = true
  try {
    const data = unwrap(await getCheckinHistory(buildParams()))
    records.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (error) {
    showFailToast(error?.response?.data?.message || '历史记录读取失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  loadHistory()
}

function changePage(nextPage) {
  page.value = nextPage
  loadHistory()
}

function taskTypeText(type) {
  return type === 'exercise' ? '运动打卡' : '饮食打卡'
}

function statusText(status) {
  const map = {
    pending: '待完成',
    completed: '已完成',
    missed: '未完成'
  }
  return map[status] || '待完成'
}

function toDate(value) {
  if (!value) return null
  return new Date(`${value}T00:00:00`)
}

function dayText(value) {
  const date = toDate(value)
  return date ? String(date.getDate()).padStart(2, '0') : '--'
}

function monthText(value) {
  const date = toDate(value)
  return date ? `${date.getMonth() + 1}月` : '日期'
}

function formatTime(value) {
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadHistory)
</script>

<style scoped>
.filter-card,
.history-card,
.state-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.filter-card {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
  padding: 16px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

label span {
  display: block;
  margin-bottom: 7px;
  color: var(--figma-text-primary);
  font-size: 13px;
  font-weight: 500;
}

input {
  width: 100%;
  height: 42px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: #F7FCF9;
  color: var(--figma-text-primary);
  font-size: 12px;
}

input:focus {
  border-color: var(--figma-primary-green);
  background: #FFFFFF;
  outline: none;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-row button {
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: var(--figma-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.chip-row button.active {
  background: var(--figma-secondary-green);
  color: #4FB783;
}

.primary-button {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  box-shadow: var(--figma-shadow-button);
}

.history-list {
  display: grid;
  gap: 10px;
}

.history-card {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 12px;
  padding: 14px;
}

.record-date {
  min-height: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
  color: #4A8A6A;
}

.record-date span {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.1;
}

.record-date strong {
  margin-top: 2px;
  font-size: 11px;
  font-weight: 600;
}

.record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.record-title h3 {
  min-width: 0;
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 600;
}

.record-body p {
  margin: 4px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
  line-height: 1.6;
}

.record-note {
  padding-top: 4px;
  color: var(--figma-text-secondary) !important;
}

.status-pill {
  flex-shrink: 0;
  padding: 3px 9px;
  border-radius: var(--figma-radius-pill);
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill--completed {
  background: rgba(111, 207, 151, 0.15);
  color: #4FB783;
}

.status-pill--missed {
  background: rgba(239, 143, 143, 0.12);
  color: #E87878;
}

.status-pill--pending {
  background: #F7E9CC;
  color: #B8862A;
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
  font-size: 12px;
  line-height: 1.7;
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
