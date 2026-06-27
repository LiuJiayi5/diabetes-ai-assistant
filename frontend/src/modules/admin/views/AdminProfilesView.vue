<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>健康档案管理</h1>
    </div>

    <div class="admin-filters">
      <input v-model="filters.keyword" placeholder="关键词" />
      <select v-model="filters.gender">
        <option value="">全部性别</option>
        <option value="male">男</option>
        <option value="female">女</option>
        <option value="other">其他</option>
      </select>
      <input v-model.number="filters.min_age" type="number" placeholder="最小年龄" />
      <input v-model.number="filters.max_age" type="number" placeholder="最大年龄" />
      <button class="admin-btn" type="button" :disabled="loading" @click="search">查询</button>
      <button class="admin-btn admin-btn--ghost" type="button" :disabled="loading" @click="reset">重置</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="list.length === 0" class="admin-empty">暂无档案数据</div>
    <div v-else class="admin-table-wrap">
      <table class="admin-table">
        <thead>
          <tr>
            <th>用户ID</th>
            <th>用户名</th>
            <th>年龄</th>
            <th>性别</th>
            <th>身高(cm)</th>
            <th>基础体重(kg)</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in list"
            :key="item.profile_id"
            class="clickable"
            @click="router.push(`/admin/profiles/${item.user_id}`)"
          >
            <td>{{ item.user_id }}</td>
            <td>{{ item.username || '—' }}</td>
            <td>{{ item.age ?? '—' }}</td>
            <td>{{ formatGender(item.gender) }}</td>
            <td>{{ item.height_cm ?? '—' }}</td>
            <td>{{ item.base_weight_kg ?? '—' }}</td>
            <td>{{ formatDateTime(item.update_time) }}</td>
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
import { adminListProfiles } from '@/api/profile'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'
import { formatGender } from '@/utils/health'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10

const filters = reactive({
  keyword: '',
  gender: '',
  min_age: undefined,
  max_age: undefined
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      page_size: pageSize,
      keyword: filters.keyword || undefined,
      gender: filters.gender || undefined,
      min_age: filters.min_age || undefined,
      max_age: filters.max_age || undefined
    }
    const data = assertSuccess(await adminListProfiles(params))
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
</script>

<style scoped>
@import '@/styles/admin-page.css';
</style>
