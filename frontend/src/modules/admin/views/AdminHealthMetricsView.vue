<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">健康数据管理</h1>
        <p class="admin-page-desc">查看患者体重、血压、血糖、饮食与运动记录，快速识别需要重点关注的数据。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="loadList">刷新数据</el-button>
    </div>

    <div class="admin-stat-grid">
      <section v-for="stat in stats" :key="stat.label" class="admin-card admin-stat-card">
        <span class="admin-stat-icon" :style="{ background: stat.bg, color: stat.color }">
          <component :is="stat.icon" :size="18" />
        </span>
        <div class="admin-stat-value">{{ stat.value }}</div>
        <div class="admin-stat-label">{{ stat.label }}</div>
      </section>
    </div>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid metrics-filter-grid">
        <label>
          <span class="admin-label">用户 ID</span>
          <el-input-number v-model="filters.user_id" :min="1" controls-position="right" placeholder="全部用户" />
        </label>
        <label>
          <span class="admin-label">开始日期</span>
          <el-date-picker v-model="filters.start_date" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        </label>
        <label>
          <span class="admin-label">结束日期</span>
          <el-date-picker v-model="filters.end_date" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        </label>
        <label class="admin-switch-field">
          <span class="admin-label">数据范围</span>
          <el-switch v-model="filters.abnormal_only" active-text="仅异常" inactive-text="全部" />
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button :disabled="loading" @click="reset">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">健康数据列表</span>
        <span class="admin-count-pill">共 {{ total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="list" row-key="metric_id" empty-text="暂无健康数据">
        <el-table-column label="记录" min-width="150">
          <template #default="{ row }">
            <strong class="admin-table-title">#{{ row.metric_id }}</strong>
            <span class="admin-table-subtitle">{{ row.recorded_at || formatDateTime(row.create_time) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="140">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.username || `用户 #${row.user_id}` }}</strong>
            <span class="admin-table-subtitle">ID {{ row.user_id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="体重/腰围" min-width="130">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ display(row.weight_kg, 'kg') }}</strong>
            <span class="admin-table-subtitle">腰围 {{ display(row.waist_cm, 'cm') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="血压" width="120">
          <template #default="{ row }">
            <el-tag :type="bpTagType(row)" round>{{ formatBp(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="血糖" min-width="150">
          <template #default="{ row }">
            <strong class="admin-table-title">空腹 {{ display(row.fasting_glucose, 'mmol/L') }}</strong>
            <span class="admin-table-subtitle">餐后 {{ display(row.postprandial_glucose, 'mmol/L') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="糖化" width="110">
          <template #default="{ row }">
            <el-tag :type="hba1cTagType(row.hba1c)" round>{{ display(row.hba1c, '%') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生活记录" min-width="220">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.diet_status || '暂无饮食记录' }}</strong>
            <span class="admin-table-subtitle">{{ row.exercise_status || '暂无运动记录' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="right">
          <template #default="{ row }">
            <el-tag :type="isAbnormal(row) ? 'danger' : 'success'" round>
              {{ isAbnormal(row) ? '需关注' : '平稳' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="changePage"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Activity, AlertTriangle, Scale, Users } from 'lucide-vue-next'
import { adminListHealthMetrics } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const list = ref([])
const statsSource = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  user_id: undefined,
  start_date: '',
  end_date: '',
  abnormal_only: false
})

const stats = computed(() => {
  const source = statsSource.value.length ? statsSource.value : list.value
  const users = new Set(source.map((item) => item.user_id).filter(Boolean))
  return [
    { label: '数据总数', value: total.value || source.length, icon: Activity, bg: 'rgba(92,142,248,0.12)', color: '#5C8EF8' },
    { label: '覆盖用户', value: users.size, icon: Users, bg: 'rgba(14,165,233,0.12)', color: '#0EA5E9' },
    { label: '需关注记录', value: source.filter(isAbnormal).length, icon: AlertTriangle, bg: 'rgba(239,68,68,0.12)', color: '#EF4444' },
    { label: '体重记录', value: source.filter((item) => item.weight_kg).length, icon: Scale, bg: 'rgba(34,197,94,0.12)', color: '#22C55E' }
  ]
})

onMounted(() => {
  loadList()
  loadStats()
})

async function loadList() {
  loading.value = true
  try {
    const data = await adminListHealthMetrics(buildParams())
    list.value = data.list || []
    total.value = data.total || 0
    page.value = data.page || page.value
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '健康数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await adminListHealthMetrics({ page: 1, page_size: 100 })
    statsSource.value = data.list || []
  } catch {
    statsSource.value = []
  }
}

function buildParams() {
  return {
    page: page.value,
    page_size: pageSize.value,
    user_id: filters.user_id || undefined,
    start_date: filters.start_date || undefined,
    end_date: filters.end_date || undefined,
    abnormal_only: filters.abnormal_only ? 'yes' : undefined
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
  filters.abnormal_only = false
  search()
}

function changePage(next) {
  page.value = next
  loadList()
}

function handleSizeChange() {
  page.value = 1
  loadList()
}

function display(value, unit) {
  return value == null || value === '' ? '-' : `${value} ${unit}`
}

function formatBp(item) {
  if (item.systolic_bp == null && item.diastolic_bp == null) return '-'
  return `${item.systolic_bp ?? '-'}/${item.diastolic_bp ?? '-'}`
}

function bpTagType(item) {
  const systolic = Number(item.systolic_bp)
  const diastolic = Number(item.diastolic_bp)
  if (!systolic && !diastolic) return 'info'
  if (systolic >= 140 || diastolic >= 90) return 'danger'
  if (systolic >= 130 || diastolic >= 80) return 'warning'
  return 'success'
}

function hba1cTagType(value) {
  const number = Number(value)
  if (!number) return 'info'
  if (number >= 8) return 'danger'
  if (number >= 7) return 'warning'
  return 'success'
}

function isAbnormal(item) {
  const fasting = Number(item.fasting_glucose)
  const postprandial = Number(item.postprandial_glucose)
  const hba1c = Number(item.hba1c)
  return bpTagType(item) === 'danger'
    || fasting >= 7
    || postprandial >= 11.1
    || hba1c >= 8
}
</script>

<style scoped>
.metrics-filter-grid {
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
}

.admin-switch-field {
  min-height: 60px;
}

:deep(.el-input-number),
:deep(.el-date-editor.el-input) {
  width: 100%;
}
</style>
