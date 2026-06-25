<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">用户管理</h1>
        <p class="admin-page-desc">检索、查看和维护患者用户账号、联系方式与账号状态。</p>
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
          <el-button class="admin-primary-btn" type="primary" @click="submitQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">用户列表</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 条记录</span>
      </div>

      <el-table v-loading="loading" :data="users" row-key="user_id" empty-text="暂无用户数据">
        <el-table-column label="ID" width="82">
          <template #default="{ row }">
            <el-tag effect="plain" type="primary">#{{ row.user_id }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="240">
          <template #default="{ row }">
            <div class="admin-table-main">
              <el-avatar :size="30" :src="asset(row.avatar)">{{ row.username?.slice(0, 1) }}</el-avatar>
              <span class="admin-table-main__body">
                <strong class="admin-table-title">{{ row.username || '未命名用户' }}</strong>
                <span class="admin-table-subtitle">{{ row.phone || '未填写手机号' }} · {{ row.email || '未填写邮箱' }}</span>
              </span>
            </div>
          </template>
        </el-table-column>
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
        <el-table-column prop="create_time" label="注册时间" width="160" show-overflow-tooltip />
        <el-table-column prop="last_login_time" label="最近登录" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="128" align="right">
          <template #default="{ row }">
            <span class="admin-actions">
              <el-button link type="primary" @click="router.push(`/admin/users/${row.user_id}`)">详情</el-button>
              <el-button
                link
                :type="row.status === 'active' ? 'danger' : 'success'"
                @click="confirmStatus(row)"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
            </span>
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
          @current-change="loadUsers"
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AlertCircle, Search } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminListUsers, adminUpdateUserStatus } from '@/api/admin'
import { assignPage, createPagination, pageParams, resolveAdminError, statusLabel, unwrapPage } from '@/modules/admin/utils'
import { resolveAssetUrl } from '@/utils/assets'

const router = useRouter()
const users = ref([])
const loading = ref(false)
const error = ref('')
const pagination = reactive(createPagination(10))
const query = reactive({
  keyword: '',
  role: '',
  status: ''
})

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const response = await adminListUsers({ ...query, ...pageParams(pagination) })
    const page = unwrapPage(response)
    users.value = page.list
    assignPage(pagination, page)
  } catch (err) {
    error.value = resolveAdminError(err, '用户列表加载失败')
    users.value = []
  } finally {
    loading.value = false
  }
}

function submitQuery() {
  pagination.page = 1
  loadUsers()
}

function resetQuery() {
  query.keyword = ''
  query.role = ''
  query.status = ''
  submitQuery()
}

function handleSizeChange() {
  pagination.page = 1
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
      user.status = next
      ElMessage.success(next === 'active' ? '账号已启用' : '账号已禁用')
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '账号状态更新失败')
    }
  }).catch(() => {})
}

onMounted(loadUsers)

function asset(value) {
  return resolveAssetUrl(value)
}
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
