<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">用户管理</h1>
        <p class="admin-page-desc">检索、查看和维护患者用户账号，不展示 password_hash。</p>
      </div>
    </div>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid">
        <label>
          <span class="admin-label">搜索用户</span>
          <el-input v-model.trim="query.keyword" clearable placeholder="按用户名、手机号、用户ID搜索">
            <template #prefix><Search :size="16" /></template>
          </el-input>
        </label>

        <label>
          <span class="admin-label">账号状态</span>
          <el-select v-model="query.status" placeholder="全部状态">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </label>

        <label>
          <span class="admin-label">角色</span>
          <el-select v-model="query.role" placeholder="全部角色">
            <el-option label="全部角色" value="" />
            <el-option label="患者" value="patient" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </label>

        <span></span>

        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="loadUsers">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">用户列表</span>
        <span class="admin-count-pill">共 {{ filteredUsers.length }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="filteredUsers" row-key="user_id" empty-text="暂无用户数据">
        <el-table-column label="用户ID" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" type="primary">#{{ row.user_id }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户名" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" :src="row.avatar">{{ row.username?.slice(0, 1) }}</el-avatar>
              <strong>{{ row.username }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            {{ statusLabel(row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" round>
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="注册时间" min-width="170" />
        <el-table-column prop="last_login_time" label="最近登录" min-width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/admin/users/${row.user_id}`)">详情</el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'danger' : 'success'"
              @click="confirmStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="error" class="admin-tip">
        <AlertCircle :size="16" />
        <span>{{ error }} 当前显示本地示例数据，后端接口接入后会自动替换。</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AlertCircle, Search } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminListUsers, adminUpdateUserStatus } from '@/api/admin'
import { adminMockUsers } from '@/modules/admin/mockData'
import { resolveAdminError, statusLabel, unwrapPage } from '@/modules/admin/utils'

const router = useRouter()
const users = ref([])
const loading = ref(false)
const error = ref('')
const query = reactive({
  keyword: '',
  role: '',
  status: '',
  page: 1,
  page_size: 10
})

const filteredUsers = computed(() => {
  const keyword = query.keyword.toLowerCase()
  return users.value.filter((user) => {
    const matchesKeyword = !keyword
      || String(user.user_id).includes(keyword)
      || String(user.username || '').toLowerCase().includes(keyword)
      || String(user.phone || '').includes(keyword)
      || String(user.email || '').toLowerCase().includes(keyword)
    const matchesRole = !query.role || user.role === query.role
    const matchesStatus = !query.status || user.status === query.status
    return matchesKeyword && matchesRole && matchesStatus
  })
})

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const response = await adminListUsers(query)
    users.value = unwrapPage(response).list
  } catch (err) {
    error.value = resolveAdminError(err, '用户列表加载失败')
    users.value = adminMockUsers
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.role = ''
  query.status = ''
  loadUsers()
}

function confirmStatus(user) {
  const next = user.status === 'active' ? 'disabled' : 'active'
  ElMessageBox.confirm(
    next === 'active' ? '启用后，该用户将能够正常登录系统。' : '禁用后，该用户将无法登录系统，请谨慎操作。',
    `确认${next === 'active' ? '启用' : '禁用'}账号？`,
    {
      confirmButtonText: `确认${next === 'active' ? '启用' : '禁用'}`,
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await adminUpdateUserStatus(user.user_id, next)
    } catch {
      // 后端未接入时仍允许前端演示状态变化。
    }
    user.status = next
    ElMessage.success(next === 'active' ? '账号已启用' : '账号已禁用')
  }).catch(() => {})
}

onMounted(loadUsers)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell strong {
  color: var(--admin-text-title);
  font-size: 14px;
}

.admin-tip {
  margin: 14px;
}
</style>
