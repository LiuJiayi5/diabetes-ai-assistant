<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">生活方案记录</h1>
        <p class="admin-page-desc">查看用户个性化生活方案生成记录、风险等级、调用状态和失败原因。</p>
      </div>
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
      <div class="admin-filter-grid">
        <label>
          <span class="admin-label">搜索</span>
          <el-input v-model.trim="query.keyword" clearable placeholder="按用户、手机号、用户ID、方案ID搜索">
            <template #prefix><Search :size="16" /></template>
          </el-input>
        </label>
        <label>
          <span class="admin-label">风险等级</span>
          <el-select v-model="query.risk_level" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="低风险" value="低风险" />
            <el-option label="中风险" value="中风险" />
            <el-option label="高风险" value="高风险" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">生成状态</span>
          <el-select v-model="query.call_status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="生成成功" value="success" />
            <el-option label="生成失败" value="failed" />
            <el-option label="生成中" value="running" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">方案状态</span>
          <el-select v-model="query.status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="当前方案" value="active" />
            <el-option label="历史方案" value="history" />
            <el-option label="失败记录" value="failed" />
          </el-select>
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="loadPlans">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">方案列表</span>
        <span class="admin-count-pill">共 {{ filteredPlans.length }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="filteredPlans" row-key="plan_id" empty-text="暂无生活方案记录">
        <el-table-column label="方案ID" width="110">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.plan_id }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.risk_level)" round>{{ row.risk_level || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="plan_title" label="方案标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="plan_goal" label="目标" min-width="130" show-overflow-tooltip />
        <el-table-column label="调用状态" width="120">
          <template #default="{ row }">
            <el-tag :type="callStatusType(row.call_status)" round>{{ callStatusText(row.call_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="生成时间" min-width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/life-plans/${row.plan_id}`)">详情</el-button>
            <el-button link @click="router.push(`/admin/life-plans/${row.plan_id}/log`)">日志</el-button>
            <el-button v-if="row.call_status === 'failed'" link type="danger" @click="showError(row)">失败原因</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="error" class="admin-tip">
        <AlertCircle :size="16" />
        <span>{{ error }} 当前显示本地示例数据。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlertCircle, CheckCircle, FileText, Search, TrendingUp, XCircle } from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { adminListLifePlans } from '@/api/admin'
import { adminMockLifePlans } from '@/modules/admin/mockData'
import { resolveAdminError, unwrapPage } from '@/modules/admin/utils'

const route = useRoute()
const router = useRouter()
const plans = ref([])
const loading = ref(false)
const error = ref('')

const query = reactive({
  keyword: route.query.user_id || '',
  risk_level: '',
  call_status: '',
  status: '',
  page: 1,
  page_size: 10
})

const filteredPlans = computed(() => {
  const keyword = String(query.keyword || '').toLowerCase()
  return plans.value.filter((plan) => {
    const matchesKeyword = !keyword
      || String(plan.plan_id).includes(keyword)
      || String(plan.user_id).includes(keyword)
      || String(plan.username || '').toLowerCase().includes(keyword)
      || String(plan.phone || '').includes(keyword)
    const matchesRisk = !query.risk_level || plan.risk_level === query.risk_level
    const matchesCall = !query.call_status || plan.call_status === query.call_status
    const matchesStatus = !query.status || plan.status === query.status
    return matchesKeyword && matchesRisk && matchesCall && matchesStatus
  })
})

const stats = computed(() => {
  const list = plans.value
  return [
    { label: '总方案数', value: list.length, icon: FileText, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
    { label: '生成成功', value: list.filter((p) => p.call_status === 'success').length, icon: CheckCircle, bg: 'rgba(34,197,94,0.10)', color: '#22C55E' },
    { label: '生成失败', value: list.filter((p) => p.call_status === 'failed').length, icon: XCircle, bg: 'rgba(239,68,68,0.10)', color: '#EF4444' },
    { label: '中高风险方案', value: list.filter((p) => ['中风险', '高风险'].includes(p.risk_level)).length, icon: TrendingUp, bg: 'rgba(245,158,11,0.10)', color: '#F59E0B' }
  ]
})

async function loadPlans() {
  loading.value = true
  error.value = ''
  try {
    const response = await adminListLifePlans(query)
    plans.value = unwrapPage(response).list
  } catch (err) {
    error.value = resolveAdminError(err, '生活方案记录加载失败')
    plans.value = adminMockLifePlans
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.risk_level = ''
  query.call_status = ''
  query.status = ''
  loadPlans()
}

function riskTagType(level) {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  if (level === '低风险') return 'success'
  return 'info'
}

function callStatusType(status) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'running') return 'primary'
  return 'info'
}

function callStatusText(status) {
  return ({ success: '生成成功', failed: '生成失败', running: '生成中' })[status] || status || '-'
}

function showError(plan) {
  ElMessageBox.alert(plan.error_message || '暂无失败原因', '生成失败原因', {
    confirmButtonText: '知道了',
    type: 'error'
  })
}

onMounted(loadPlans)
</script>

<style scoped>
.admin-tip {
  margin: 14px;
}
</style>
