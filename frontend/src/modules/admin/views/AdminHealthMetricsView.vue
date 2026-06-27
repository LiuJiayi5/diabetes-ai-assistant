<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>健康数据管理</h1>
    </div>

    <div class="admin-filters">
      <input v-model.number="filters.user_id" type="number" placeholder="用户ID" />
      <input v-model="filters.start_date" type="date" />
      <input v-model="filters.end_date" type="date" />
      <label style="display:flex;align-items:center;gap:6px;font-size:13px">
        <input v-model="filters.abnormal_only" type="checkbox" true-value="true" false-value="" />
        仅异常
      </label>
      <button class="admin-btn" type="button" :disabled="loading" @click="search">查询</button>
      <button class="admin-btn admin-btn--ghost" type="button" :disabled="loading" @click="reset">重置</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="list.length === 0" class="admin-empty">暂无健康数据</div>
    <div v-else class="admin-table-wrap">
      <table class="admin-table">
        <thead>
          <tr>
            <th>记录ID</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>体重</th>
            <th>腰围</th>
            <th>血压</th>
            <th>空腹血糖</th>
            <th>餐后血糖</th>
            <th>糖化</th>
            <th>饮食</th>
            <th>运动</th>
            <th>记录日期</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.metric_id">
            <td>{{ item.metric_id }}</td>
            <td>{{ item.user_id }}</td>
            <td>{{ item.username || '—' }}</td>
            <td>{{ item.weight_kg ?? '—' }}</td>
            <td>{{ item.waist_cm ?? '—' }}</td>
            <td>{{ formatBp(item) }}</td>
            <td>{{ item.fasting_glucose ?? '—' }}</td>
            <td>{{ item.postprandial_glucose ?? '—' }}</td>
            <td>{{ item.hba1c ?? '—' }}</td>
            <td class="wrap">{{ item.diet_status || '—' }}</td>
            <td class="wrap">{{ item.exercise_status || '—' }}</td>
            <td>{{ item.recorded_at || formatDateTime(item.create_time) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > 0" class="admin-pagination">
      <span>共 {{ total }} 条，第 {{ page }} / {{ totalPages }} 页</span>
      <div class="admin-page__actions">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">上一页</button>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import { adminListMetrics } from '@/api/healthMetric'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const filters = reactive({
  user_id: undefined,
  start_date: '',
  end_date: '',
  abnormal_only: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

onMounted(() => loadList())

function formatBp(item) {
  if (item.systolic_bp == null && item.diastolic_bp == null) return '—'
  return `${item.systolic_bp ?? '—'}/${item.diastolic_bp ?? '—'}`
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      page_size: pageSize,
      user_id: filters.user_id || undefined,
      start_date: filters.start_date || undefined,
      end_date: filters.end_date || undefined,
      abnormal_only: filters.abnormal_only || undefined
    }
    const data = assertSuccess(await adminListMetrics(params))
    list.value = data.list || []
    total.value = data.total || 0
    page.value = data.page || page.value
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadList()
}

function reset() {
  filters.user_id = undefined
  filters.start_date = ''
  filters.end_date = ''
  filters.abnormal_only = ''
  search()
}

function changePage(next) {
  page.value = next
  loadList()
}
</script>

<style scoped>
@import '@/styles/admin-page.css';
</style>
