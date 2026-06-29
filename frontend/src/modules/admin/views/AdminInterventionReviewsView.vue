<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">自动干预复盘</h1>
        <p class="admin-page-desc">查看系统在用户打卡、健康数据和风险变化后自动触发的复盘决策，以及是否自动优化生活方案。</p>
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
      <div class="admin-filter-grid intervention-filter-grid">
        <label>
          <span class="admin-label">用户ID</span>
          <el-input v-model.trim="query.user_id" clearable placeholder="输入用户 ID" />
        </label>
        <label>
          <span class="admin-label">干预等级</span>
          <el-select v-model="query.intervention_level" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="继续观察" value="observe" />
            <el-option label="轻微调整" value="minor_adjustment" />
            <el-option label="中度调整" value="moderate_adjustment" />
            <el-option label="高风险提醒" value="high_risk_alert" />
          </el-select>
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="submitQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">复盘记录</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="reviews" row-key="review_id" empty-text="暂无自动复盘记录">
        <el-table-column label="ID" width="82">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.review_id }}</el-tag></template>
        </el-table-column>
        <el-table-column label="用户/方案" width="140">
          <template #default="{ row }">
            <strong class="admin-table-title">用户 #{{ row.user_id }}</strong>
            <span class="admin-table-subtitle">方案 #{{ row.plan_id || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="触发来源" width="150">
          <template #default="{ row }">
            <el-tag round>{{ triggerText(row.trigger_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="等级/评分" width="150">
          <template #default="{ row }">
            <el-tag :type="levelType(row.intervention_level)" round>{{ levelText(row.intervention_level) }}</el-tag>
            <span class="review-score">{{ row.adherence_score ?? '-' }} 分</span>
          </template>
        </el-table-column>
        <el-table-column label="决策说明" min-width="300">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.patient_notice || '系统已完成自动复盘' }}</strong>
            <span class="admin-table-subtitle">{{ row.explanation || row.adjustment_strategy || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="自动调整" width="120">
          <template #default="{ row }">
            <el-tag :type="row.should_update_plan ? 'success' : 'info'" round>
              {{ row.should_update_plan ? '已调整' : '未调整' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成方案" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.generated_plan_id"
              link
              type="primary"
              @click="router.push(`/admin/life-plans/${row.generated_plan_id}`)"
            >
              #{{ row.generated_plan_id }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="时间" width="170" show-overflow-tooltip />
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.page_size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadReviews"
          @size-change="handleSizeChange"
        />
      </div>

      <div v-if="error" class="admin-tip">
        <AlertCircle :size="16" />
        <span>{{ error }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AlertCircle, BellRing, CheckCircle, RefreshCw, ShieldAlert } from 'lucide-vue-next'
import { getAdminInterventionReviews } from '@/api/interventionReview'
import { assignPage, createPagination, pageParams, resolveAdminError, unwrapPage } from '@/modules/admin/utils'

const router = useRouter()
const reviews = ref([])
const loading = ref(false)
const error = ref('')
const pagination = reactive(createPagination(10))

const query = reactive({
  user_id: '',
  intervention_level: ''
})

const stats = computed(() => [
  { label: '复盘总数', value: pagination.total || reviews.value.length, icon: RefreshCw, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
  { label: '自动调整', value: reviews.value.filter((item) => item.should_update_plan).length, icon: CheckCircle, bg: 'rgba(34,197,94,0.10)', color: '#22C55E' },
  { label: '高风险提醒', value: reviews.value.filter((item) => item.intervention_level === 'high_risk_alert').length, icon: ShieldAlert, bg: 'rgba(239,68,68,0.10)', color: '#EF4444' },
  { label: '继续观察', value: reviews.value.filter((item) => item.intervention_level === 'observe').length, icon: BellRing, bg: 'rgba(245,158,11,0.10)', color: '#F59E0B' }
])

async function loadReviews() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      ...pageParams(pagination),
      user_id: query.user_id || undefined,
      intervention_level: query.intervention_level || undefined
    }
    const response = await getAdminInterventionReviews(params)
    const page = unwrapPage(response)
    reviews.value = page.list
    assignPage(pagination, page)
  } catch (err) {
    error.value = resolveAdminError(err, '自动复盘记录加载失败')
    reviews.value = []
  } finally {
    loading.value = false
  }
}

function submitQuery() {
  pagination.page = 1
  loadReviews()
}

function resetQuery() {
  query.user_id = ''
  query.intervention_level = ''
  submitQuery()
}

function handleSizeChange() {
  pagination.page = 1
  loadReviews()
}

function levelText(level) {
  return {
    observe: '继续观察',
    minor_adjustment: '轻微调整',
    moderate_adjustment: '中度调整',
    high_risk_alert: '高风险提醒'
  }[level] || level || '-'
}

function levelType(level) {
  if (level === 'high_risk_alert') return 'danger'
  if (level === 'moderate_adjustment') return 'warning'
  if (level === 'minor_adjustment') return 'success'
  return 'info'
}

function triggerText(type) {
  return {
    checkin_submit: '提交打卡',
    health_metric_save: '录入健康数据',
    risk_assessment_save: '风险评估更新',
    plan_progress: '方案周期推进'
  }[type] || type || '-'
}

onMounted(loadReviews)
</script>

<style scoped>
.intervention-filter-grid {
  grid-template-columns: minmax(180px, 240px) minmax(180px, 240px) 1fr;
}

.review-score {
  display: block;
  margin-top: 6px;
  color: var(--admin-text-muted);
  font-size: 12px;
}
</style>
