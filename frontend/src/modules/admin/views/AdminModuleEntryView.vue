<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">{{ config.title }}</h1>
        <p class="admin-page-desc">{{ config.description }}</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="loadRows">
        刷新
      </el-button>
    </div>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid module-filter-grid">
        <label>
          <span class="admin-label">{{ config.keywordLabel }}</span>
          <el-input v-model.trim="query.keyword" clearable :placeholder="config.keywordPlaceholder" @keyup.enter="submitQuery" />
        </label>

        <label v-if="config.kind === 'profiles'">
          <span class="admin-label">性别</span>
          <el-select v-model="query.gender" placeholder="全部性别">
            <el-option label="全部性别" value="" />
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
            <el-option label="其他" value="other" />
          </el-select>
        </label>

        <label v-if="config.kind === 'metrics'">
          <span class="admin-label">异常指标</span>
          <el-select v-model="query.abnormal_only" placeholder="全部记录">
            <el-option label="全部记录" value="" />
            <el-option label="只看异常" value="true" />
          </el-select>
        </label>

        <label v-if="config.kind === 'risks'">
          <span class="admin-label">风险等级</span>
          <el-select v-model="query.risk_level" placeholder="全部风险">
            <el-option label="全部风险" value="" />
            <el-option label="低风险" value="low" />
            <el-option label="中风险" value="medium" />
            <el-option label="高风险" value="high" />
          </el-select>
        </label>

        <label v-if="config.hasDateRange">
          <span class="admin-label">开始日期</span>
          <el-input v-model="query.start_date" type="date" />
        </label>

        <label v-if="config.hasDateRange">
          <span class="admin-label">结束日期</span>
          <el-input v-model="query.end_date" type="date" />
        </label>

        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="submitQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">{{ config.listTitle }}</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="__row_key" :empty-text="`暂无${config.listTitle}`">
        <el-table-column v-for="column in config.columns" :key="column.key" :label="column.label" :width="column.width" :min-width="column.minWidth" show-overflow-tooltip>
          <template #default="{ row }">
            <component v-if="column.type === 'tag'" :is="ElTag" :type="tagType(row[column.key])" effect="plain" round>
              {{ formatCell(row, column) }}
            </component>
            <div v-else-if="column.type === 'main'" class="admin-table-main">
              <span class="admin-table-main__body">
                <strong class="admin-table-title">{{ formatCell(row, column) }}</strong>
                <span class="admin-table-subtitle">{{ subtitle(row, column) }}</span>
              </span>
            </div>
            <span v-else>{{ formatCell(row, column) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.page_size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadRows"
          @size-change="handleSizeChange"
        />
      </div>

      <div v-if="error" class="admin-tip">
        <span>{{ error }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElTag } from 'element-plus'
import { adminListHealthMetrics, adminListProfiles, adminListRiskAssessments } from '@/api/admin'
import { assignPage, createPagination, pageParams, resolveAdminError, statusLabel, unwrapPage } from '@/modules/admin/utils'
import { formatGender, formatRiskLevel } from '@/utils/health'

const route = useRoute()
const rows = ref([])
const loading = ref(false)
const error = ref('')
const pagination = reactive(createPagination(10))
const query = reactive({
  keyword: '',
  gender: '',
  abnormal_only: '',
  risk_level: '',
  start_date: '',
  end_date: ''
})

const configs = {
  AdminProfiles: {
    kind: 'profiles',
    title: '健康档案管理',
    description: '集中查看患者健康档案基础信息和资料完善情况。',
    listTitle: '健康档案列表',
    keywordLabel: '搜索患者',
    keywordPlaceholder: '按用户名或用户ID搜索',
    fetcher: adminListProfiles,
    columns: [
      { key: 'profile_id', label: '档案ID', width: 96, formatter: (v) => `#${v}` },
      { key: 'username', label: '患者', type: 'main', minWidth: 180, subtitle: (row) => `用户 ID ${row.user_id}` },
      { key: 'age', label: '年龄', width: 86, formatter: (v) => valueOrDash(v, ' 岁') },
      { key: 'gender', label: '性别', width: 86, formatter: formatGender },
      { key: 'height_cm', label: '身高', width: 96, formatter: (v) => valueOrDash(v, ' cm') },
      { key: 'base_weight_kg', label: '基础体重', width: 112, formatter: (v) => valueOrDash(v, ' kg') },
      { key: 'update_time', label: '更新时间', minWidth: 160, formatter: formatTime }
    ]
  },
  AdminHealthMetrics: {
    kind: 'metrics',
    title: '健康数据管理',
    description: '查看患者日常血糖、血压、体重、腰围等健康指标记录。',
    listTitle: '健康数据列表',
    keywordLabel: '用户ID',
    keywordPlaceholder: '输入用户ID筛选',
    hasDateRange: true,
    fetcher: adminListHealthMetrics,
    columns: [
      { key: 'metric_id', label: '记录ID', width: 96, formatter: (v) => `#${v}` },
      { key: 'username', label: '患者', type: 'main', minWidth: 170, subtitle: (row) => `用户 ID ${row.user_id}` },
      { key: 'recorded_at', label: '记录日期', width: 120 },
      { key: 'weight_kg', label: '体重', width: 90, formatter: (v) => valueOrDash(v, ' kg') },
      { key: 'fasting_glucose', label: '空腹血糖', width: 116, formatter: (v) => valueOrDash(v, ' mmol/L') },
      { key: 'postprandial_glucose', label: '餐后血糖', width: 116, formatter: (v) => valueOrDash(v, ' mmol/L') },
      { key: 'blood_pressure', label: '血压', width: 100, formatter: (_, row) => row.systolic_bp && row.diastolic_bp ? `${row.systolic_bp}/${row.diastolic_bp}` : '-' },
      { key: 'create_time', label: '创建时间', minWidth: 160, formatter: formatTime }
    ]
  },
  AdminRiskAssessments: {
    kind: 'risks',
    title: '风险评估记录',
    description: '查看糖尿病风险评估结果、风险分层、调用状态和生成摘要。',
    listTitle: '风险评估列表',
    keywordLabel: '用户ID',
    keywordPlaceholder: '输入用户ID筛选',
    hasDateRange: true,
    fetcher: adminListRiskAssessments,
    columns: [
      { key: 'assessment_id', label: '评估ID', width: 96, formatter: (v) => `#${v}` },
      { key: 'username', label: '患者', type: 'main', minWidth: 170, subtitle: (row) => `用户 ID ${row.user_id}` },
      { key: 'risk_level', label: '风险等级', type: 'tag', width: 112, formatter: formatRiskLevel },
      { key: 'risk_score', label: '评分', width: 86, formatter: (v) => v ?? '-' },
      { key: 'summary', label: '摘要', minWidth: 260 },
      { key: 'call_status', label: '调用状态', type: 'tag', width: 112, formatter: statusLabel },
      { key: 'create_time', label: '创建时间', minWidth: 160, formatter: formatTime }
    ]
  }
}

const config = computed(() => configs[route.name] || configs.AdminProfiles)

watch(() => route.name, () => {
  resetQuery()
})

onMounted(loadRows)

async function loadRows() {
  loading.value = true
  error.value = ''
  try {
    const response = await config.value.fetcher({ ...buildParams(), ...pageParams(pagination) })
    const page = unwrapPage(response)
    rows.value = page.list.map((item, index) => ({ ...item, __row_key: `${config.value.kind}-${item.id || item.profile_id || item.metric_id || item.assessment_id || index}` }))
    assignPage(pagination, page)
  } catch (err) {
    rows.value = []
    error.value = resolveAdminError(err, `${config.value.listTitle}加载失败`)
  } finally {
    loading.value = false
  }
}

function buildParams() {
  const params = {}
  if (config.value.kind === 'profiles') {
    if (query.keyword) params.keyword = query.keyword
    if (query.gender) params.gender = query.gender
    return params
  }
  if (/^\d+$/.test(query.keyword)) params.user_id = Number(query.keyword)
  if (query.start_date) params.start_date = query.start_date
  if (query.end_date) params.end_date = query.end_date
  if (config.value.kind === 'metrics' && query.abnormal_only) params.abnormal_only = query.abnormal_only
  if (config.value.kind === 'risks' && query.risk_level) params.risk_level = query.risk_level
  return params
}

function submitQuery() {
  pagination.page = 1
  loadRows()
}

function resetQuery() {
  query.keyword = ''
  query.gender = ''
  query.abnormal_only = ''
  query.risk_level = ''
  query.start_date = ''
  query.end_date = ''
  pagination.page = 1
  loadRows()
}

function handleSizeChange() {
  pagination.page = 1
  loadRows()
}

function formatCell(row, column) {
  const value = row[column.key]
  return column.formatter ? column.formatter(value, row) : (value ?? '-')
}

function subtitle(row, column) {
  return column.subtitle ? column.subtitle(row) : ''
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

function valueOrDash(value, suffix = '') {
  return value == null || value === '' ? '-' : `${value}${suffix}`
}

function tagType(value) {
  if (value === 'success' || value === 'low') return 'success'
  if (value === 'failed' || value === 'high') return 'danger'
  if (value === 'medium') return 'warning'
  return 'primary'
}
</script>

<style scoped>
.module-filter-grid {
  grid-template-columns: minmax(220px, 1fr) repeat(3, minmax(132px, 168px)) auto;
}

.admin-tip {
  margin: 14px 18px;
  color: var(--admin-text-muted);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .module-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
