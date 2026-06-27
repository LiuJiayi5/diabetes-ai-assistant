<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">风险评估管理</h1>
        <p class="admin-page-desc">查看 AI 风险预测记录、风险分层、生成状态与摘要，帮助管理端快速定位高风险患者。</p>
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
      <div class="admin-filter-grid risk-filter-grid">
        <label>
          <span class="admin-label">用户 ID</span>
          <el-input-number v-model="filters.user_id" :min="1" controls-position="right" placeholder="全部用户" />
        </label>
        <label>
          <span class="admin-label">风险等级</span>
          <el-select v-model="filters.risk_level" placeholder="全部风险等级">
            <el-option label="全部风险等级" value="" />
            <el-option label="低风险" value="low" />
            <el-option label="中风险" value="medium" />
            <el-option label="高风险" value="high" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">开始日期</span>
          <el-date-picker v-model="filters.start_date" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        </label>
        <label>
          <span class="admin-label">结束日期</span>
          <el-date-picker v-model="filters.end_date" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button :disabled="loading" @click="reset">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">评估记录列表</span>
        <span class="admin-count-pill">共 {{ total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="list" row-key="assessment_id" empty-text="暂无评估记录">
        <el-table-column label="评估" min-width="150">
          <template #default="{ row }">
            <strong class="admin-table-title">#{{ row.assessment_id }}</strong>
            <span class="admin-table-subtitle">{{ formatDateTime(row.create_time) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.username || `用户 #${row.user_id}` }}</strong>
            <span class="admin-table-subtitle">ID {{ row.user_id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.risk_level)" round>{{ riskLevelText(row.risk_level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="110">
          <template #default="{ row }">
            <strong class="score-text">{{ row.risk_score ?? '-' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="生成状态" width="120">
          <template #default="{ row }">
            <el-tag :type="callStatusType(row.call_status)" round>{{ callStatusText(row.call_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评估摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="summary-text">{{ row.summary || '暂无摘要' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/risk-assessments/${row.assessment_id}`)">详情</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { AlertTriangle, CheckCircle, ShieldAlert, TrendingUp } from 'lucide-vue-next'
import { adminListRiskAssessments } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const statsSource = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  user_id: undefined,
  risk_level: '',
  start_date: '',
  end_date: ''
})

const stats = computed(() => {
  const source = statsSource.value.length ? statsSource.value : list.value
  return [
    { label: '评估总数', value: total.value || source.length, icon: ShieldAlert, bg: 'rgba(92,142,248,0.12)', color: '#5C8EF8' },
    { label: '高风险记录', value: source.filter((item) => normalizeRisk(item.risk_level) === 'high').length, icon: AlertTriangle, bg: 'rgba(239,68,68,0.12)', color: '#EF4444' },
    { label: '中高风险', value: source.filter((item) => ['medium', 'high'].includes(normalizeRisk(item.risk_level))).length, icon: TrendingUp, bg: 'rgba(245,158,11,0.12)', color: '#F59E0B' },
    { label: '生成成功', value: source.filter((item) => item.call_status === 'success').length, icon: CheckCircle, bg: 'rgba(34,197,94,0.12)', color: '#22C55E' }
  ]
})

onMounted(() => {
  loadList()
  loadStats()
})

async function loadList() {
  loading.value = true
  try {
    const data = await adminListRiskAssessments(buildParams())
    list.value = data.list || []
    total.value = data.total || 0
    page.value = data.page || page.value
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '风险评估加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await adminListRiskAssessments({ page: 1, page_size: 100 })
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
    risk_level: filters.risk_level || undefined,
    start_date: filters.start_date || undefined,
    end_date: filters.end_date || undefined
  }
}

function search() {
  page.value = 1
  loadList()
}

function reset() {
  filters.user_id = undefined
  filters.risk_level = ''
  filters.start_date = ''
  filters.end_date = ''
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

function normalizeRisk(level) {
  const value = String(level || '').toLowerCase()
  if (value.includes('high') || value.includes('高')) return 'high'
  if (value.includes('medium') || value.includes('中')) return 'medium'
  if (value.includes('low') || value.includes('低')) return 'low'
  return ''
}

function riskLevelText(level) {
  return ({ high: '高风险', medium: '中风险', low: '低风险' })[normalizeRisk(level)] || level || '未知'
}

function riskTagType(level) {
  const normalized = normalizeRisk(level)
  if (normalized === 'high') return 'danger'
  if (normalized === 'medium') return 'warning'
  if (normalized === 'low') return 'success'
  return 'info'
}

function callStatusType(status) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  return 'info'
}

function callStatusText(status) {
  return ({ success: '成功', failed: '失败', running: '生成中' })[status] || status || '-'
}
</script>

<style scoped>
.risk-filter-grid {
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
}

.score-text {
  color: var(--admin-text-title);
  font-size: 18px;
  font-weight: 800;
}

.summary-text {
  color: var(--admin-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

:deep(.el-input-number),
:deep(.el-date-editor.el-input) {
  width: 100%;
}
</style>
