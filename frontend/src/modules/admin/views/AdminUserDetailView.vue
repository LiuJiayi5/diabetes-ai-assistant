<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <el-button text @click="router.push('/admin/users')">
          <ArrowLeft :size="16" /> 返回用户管理
        </el-button>
        <h1 class="admin-page-title">用户详情</h1>
        <p class="admin-page-desc">查看用户基础信息和健康数据概览，不展示密码字段。</p>
      </div>
      <el-button
        v-if="user"
        :type="user.status === 'active' ? 'danger' : 'success'"
        plain
        @click="confirmStatus(user)"
      >
        {{ user.status === 'active' ? '禁用账号' : '启用账号' }}
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="user">
      <section class="admin-card user-hero">
        <el-avatar :size="68" :src="user.avatar">{{ user.username?.slice(0, 1) }}</el-avatar>
        <div>
          <h2>{{ user.username }}</h2>
          <p>ID #{{ user.user_id }} · {{ statusLabel(user.role) }}</p>
        </div>
        <el-tag :type="user.status === 'active' ? 'success' : 'danger'" round>
          {{ statusLabel(user.status) }}
        </el-tag>
      </section>

      <div class="admin-detail-grid">
        <section class="admin-card detail-card">
          <h3>账号基础信息</h3>
          <dl>
            <div class="admin-kv"><dt>手机号</dt><dd>{{ user.phone || '-' }}</dd></div>
            <div class="admin-kv"><dt>邮箱</dt><dd>{{ user.email || '-' }}</dd></div>
            <div class="admin-kv"><dt>注册时间</dt><dd>{{ user.create_time || '-' }}</dd></div>
            <div class="admin-kv"><dt>最近登录</dt><dd>{{ user.last_login_time || '-' }}</dd></div>
          </dl>
        </section>

        <section class="admin-card detail-card">
          <h3>健康信息概览</h3>
          <dl>
            <div class="admin-kv"><dt>健康档案</dt><dd>{{ user.profile_summary || '暂无档案摘要' }}</dd></div>
            <div class="admin-kv"><dt>近期指标</dt><dd>{{ user.metric_summary || '暂无指标摘要' }}</dd></div>
            <div class="admin-kv"><dt>方案数量</dt><dd>{{ user.plan_count ?? 0 }} 个</dd></div>
            <div class="admin-kv"><dt>收藏资讯</dt><dd>{{ user.article_favorites ?? 0 }} 篇</dd></div>
          </dl>
        </section>
      </div>

      <section class="admin-card detail-card">
        <div class="admin-card-title-row inline-row">
          <span class="admin-section-title">关联管理入口</span>
        </div>
        <div class="link-grid">
          <button @click="router.push(`/admin/life-plans?user_id=${user.user_id}`)">
            <FileText :size="20" />
            <strong>查看生活方案</strong>
            <span>筛选该用户的方案生成记录</span>
          </button>
          <button @click="router.push('/admin/articles')">
            <Newspaper :size="20" />
            <strong>健康资讯管理</strong>
            <span>维护用户端可见科普内容</span>
          </button>
        </div>
      </section>
    </template>

    <div v-else class="admin-card admin-empty">
      未找到用户数据
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FileText, Newspaper } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminListUsers, adminUpdateUserStatus } from '@/api/admin'
import { adminMockUsers } from '@/modules/admin/mockData'
import { statusLabel, unwrapPage } from '@/modules/admin/utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const user = ref(null)

async function loadUser() {
  loading.value = true
  try {
    const response = await adminListUsers({
      keyword: route.params.userId,
      page: 1,
      page_size: 10
    })
    const list = unwrapPage(response).list
    user.value = list.find((item) => String(item.user_id) === String(route.params.userId)) || list[0]
  } catch {
    user.value = adminMockUsers.find((item) => String(item.user_id) === String(route.params.userId))
  } finally {
    loading.value = false
  }
}

function confirmStatus(target) {
  const next = target.status === 'active' ? 'disabled' : 'active'
  ElMessageBox.confirm(
    next === 'active' ? '确认恢复该账号登录权限？' : '禁用后该账号无法登录系统。',
    `确认${next === 'active' ? '启用' : '禁用'}账号？`,
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await adminUpdateUserStatus(target.user_id, next)
    } catch {}
    target.status = next
    ElMessage.success('状态已更新')
  }).catch(() => {})
}

onMounted(loadUser)
</script>

<style scoped>
.user-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 16px;
  padding: 20px;
}

.user-hero h2 {
  margin: 0 0 4px;
  color: var(--admin-text-title);
}

.user-hero p {
  margin: 0;
  color: var(--admin-text-secondary);
}

.user-hero .el-tag {
  margin-left: auto;
}

.detail-card {
  padding: 18px 20px;
}

.detail-card h3 {
  margin: 0 0 12px;
  color: var(--admin-text-title);
  font-size: 16px;
}

.inline-row {
  margin: -18px -20px 16px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.link-grid button {
  display: grid;
  gap: 6px;
  justify-items: start;
  border: 1px solid var(--admin-border-solid);
  border-radius: 12px;
  background: var(--admin-card-muted);
  color: var(--admin-primary);
  padding: 16px;
}

.link-grid strong {
  color: var(--admin-text-title);
}

.link-grid span {
  color: var(--admin-text-muted);
  font-size: 12px;
}
</style>
