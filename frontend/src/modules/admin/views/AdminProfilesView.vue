<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">健康档案管理</h1>
        <p class="admin-page-desc">集中查看患者基础档案、身体指标和最近更新情况，支持按用户、性别与年龄段筛选。</p>
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
      <div class="admin-filter-grid profile-filter-grid">
        <label>
          <span class="admin-label">搜索</span>
          <el-input v-model.trim="filters.keyword" clearable placeholder="用户名、手机号或用户 ID">
            <template #prefix><Search :size="16" /></template>
          </el-input>
        </label>
        <label>
          <span class="admin-label">性别</span>
          <el-select v-model="filters.gender" placeholder="全部性别">
            <el-option label="全部性别" value="" />
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
            <el-option label="其他" value="other" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">最小年龄</span>
          <el-input-number v-model="filters.min_age" :min="0" :max="120" controls-position="right" placeholder="不限" />
        </label>
        <label>
          <span class="admin-label">最大年龄</span>
          <el-input-number v-model="filters.max_age" :min="0" :max="120" controls-position="right" placeholder="不限" />
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button :disabled="loading" @click="reset">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">档案列表</span>
        <span class="admin-count-pill">共 {{ total }} 条档案</span>
      </div>

      <el-table v-loading="loading" :data="list" row-key="profile_id" empty-text="暂无档案数据">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="admin-table-main">
              <span class="profile-avatar">{{ avatarText(row) }}</span>
              <span class="admin-table-main__body">
                <strong class="admin-table-title">{{ row.username || `用户 #${row.user_id}` }}</strong>
                <span class="admin-table-subtitle">ID {{ row.user_id }}</span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="年龄/性别" width="130">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.age ?? '-' }} 岁</strong>
            <span class="admin-table-subtitle">{{ formatGender(row.gender) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="身高" width="110">
          <template #default="{ row }">{{ displayNumber(row.height_cm, 'cm') }}</template>
        </el-table-column>
        <el-table-column label="基础体重" width="120">
          <template #default="{ row }">{{ displayNumber(row.base_weight_kg, 'kg') }}</template>
        </el-table-column>
        <el-table-column label="BMI" width="110">
          <template #default="{ row }">
            <el-tag :type="bmiTagType(row)" round>{{ bmiText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.update_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="96" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/profiles/${row.user_id}`)">详情</el-button>
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
import { Search, UserCheck, Users, VenusAndMars, Weight } from 'lucide-vue-next'
import { adminListProfiles } from '@/api/admin'
import { formatDateTime } from '@/utils/format'
import { formatGender } from '@/utils/health'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const statsSource = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  gender: '',
  min_age: undefined,
  max_age: undefined
})

const stats = computed(() => {
  const source = statsSource.value.length ? statsSource.value : list.value
  const ages = source.map((item) => Number(item.age)).filter((age) => Number.isFinite(age) && age > 0)
  const avgAge = ages.length ? Math.round(ages.reduce((sum, age) => sum + age, 0) / ages.length) : 0
  const completeCount = source.filter((item) => item.height_cm && item.base_weight_kg).length
  return [
    { label: '档案总数', value: total.value || source.length, icon: Users, bg: 'rgba(92,142,248,0.12)', color: '#5C8EF8' },
    { label: '资料完整', value: completeCount, icon: UserCheck, bg: 'rgba(34,197,94,0.12)', color: '#22C55E' },
    { label: '平均年龄', value: avgAge ? `${avgAge}岁` : '-', icon: VenusAndMars, bg: 'rgba(14,165,233,0.12)', color: '#0EA5E9' },
    { label: '体重已记录', value: source.filter((item) => item.base_weight_kg).length, icon: Weight, bg: 'rgba(245,158,11,0.12)', color: '#F59E0B' }
  ]
})

onMounted(() => {
  loadList()
  loadStats()
})

async function loadList() {
  loading.value = true
  try {
    const data = await adminListProfiles(buildParams())
    list.value = data.list || []
    total.value = data.total || 0
    page.value = data.page || page.value
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '健康档案加载失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data = await adminListProfiles({ page: 1, page_size: 100 })
    statsSource.value = data.list || []
  } catch {
    statsSource.value = []
  }
}

function buildParams() {
  return {
    page: page.value,
    page_size: pageSize.value,
    keyword: filters.keyword || undefined,
    gender: filters.gender || undefined,
    min_age: filters.min_age ?? undefined,
    max_age: filters.max_age ?? undefined
  }
}

function search() {
  page.value = 1
  loadList()
}

function reset() {
  filters.keyword = ''
  filters.gender = ''
  filters.min_age = undefined
  filters.max_age = undefined
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

function avatarText(row) {
  return String(row.username || row.user_id || '?').slice(0, 1).toUpperCase()
}

function displayNumber(value, unit) {
  return value == null || value === '' ? '-' : `${value} ${unit}`
}

function bmi(row) {
  const height = Number(row.height_cm)
  const weight = Number(row.base_weight_kg)
  if (!height || !weight) return null
  return weight / ((height / 100) ** 2)
}

function bmiText(row) {
  const value = bmi(row)
  return value ? value.toFixed(1) : '未计算'
}

function bmiTagType(row) {
  const value = bmi(row)
  if (!value) return 'info'
  if (value >= 28) return 'danger'
  if (value >= 24) return 'warning'
  return 'success'
}
</script>

<style scoped>
.profile-filter-grid {
  grid-template-columns: minmax(220px, 1fr) repeat(3, minmax(132px, 168px)) auto;
}

.profile-avatar {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #5C8EF8, #7EB5FF);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
