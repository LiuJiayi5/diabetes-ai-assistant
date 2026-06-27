<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>风险评估管理</h1>
    </div>

    <div class="admin-filters">
      <input v-model.number="filters.user_id" type="number" placeholder="用户ID" />
      <select v-model="filters.risk_level">
        <option value="">全部风险等级</option>
        <option value="low">低风险</option>
        <option value="medium">中风险</option>
        <option value="high">高风险</option>
      </select>
      <input v-model="filters.start_date" type="date" />
      <input v-model="filters.end_date" type="date" />
      <button class="admin-btn" type="button" :disabled="loading" @click="search">查询</button>
      <button class="admin-btn admin-btn--ghost" type="button" :disabled="loading" @click="reset">重置</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="list.length === 0" class="admin-empty">暂无评估记录</div>
    <div v-else class="admin-table-wrap">
      <table class="admin-table">
        <thead>
          <tr>
            <th>评估ID</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>风险等级</th>
            <th>评分</th>
            <th>状态</th>
            <th>摘要</th>
            <th>评估时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in list"
            :key="item.assessment_id"
            class="clickable"
            @click="router.push(`/admin/risk-assessments/${item.assessment_id}`)"
          >
            <td>{{ item.assessment_id }}</td>
            <td>{{ item.user_id }}</td>
            <td>{{ item.username || '—' }}</td>
            <td><RiskLevelTag :level="item.risk_level" /></td>
            <td>{{ item.risk_score ?? '—' }}</td>
            <td>
              <span class="admin-status" :class="item.call_status === 'success' ? 'admin-status--success' : 'admin-status--failed'">
                {{ item.call_status === 'success' ? '成功' : '失败' }}
              </span>
            </td>
            <td class="wrap">{{ item.summary || '—' }}</td>
            <td>{{ formatDateTime(item.create_time) }}</td>
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
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { adminListAssessments } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const filters = reactive({
  user_id: undefined,
  risk_level: '',
  start_date: '',
  end_date: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      page_size: pageSize,
      user_id: filters.user_id || undefined,
      risk_level: filters.risk_level || undefined,
      start_date: filters.start_date || undefined,
      end_date: filters.end_date || undefined
    }
    const data = assertSuccess(await adminListAssessments(params))
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
  filters.risk_level = ''
  filters.start_date = ''
  filters.end_date = ''
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
