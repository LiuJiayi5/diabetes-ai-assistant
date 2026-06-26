<template>
  <div class="admin-page admin-profile-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">管理员个人中心</h1>
        <p class="admin-page-desc">维护管理端账号基础资料，头像与联系方式保存后会同步到顶部栏和账号表。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="saving" @click="saveProfile">
        <Save :size="16" /> 保存资料
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else>
      <section class="admin-card admin-profile-hero">
        <el-avatar :size="72" :src="avatarUrl" @error="useDefaultAvatar">{{ avatarInitial }}</el-avatar>
        <div>
          <h2>{{ form.username || '管理员' }}</h2>
          <p>{{ roleLabel }} · {{ statusLabel(form.status) }}</p>
        </div>
      </section>

      <div class="admin-profile-grid">
        <section class="admin-card admin-profile-card">
          <h3>头像</h3>
          <ImageUploader
            v-model="form.avatar"
            title="上传管理员头像"
            hint="点击选择或拖拽头像图片，可拖动调整显示位置"
            avatar-crop
            @error="ElMessage.error"
          />
        </section>

        <section class="admin-card admin-profile-card">
          <h3>账号资料</h3>
          <el-form label-position="top">
            <el-form-item label="用户名 / 显示名">
              <el-input v-model.trim="form.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-form>
        </section>
      </div>

      <section class="admin-card admin-profile-card">
        <h3>只读信息</h3>
        <div class="admin-detail-grid">
          <dl>
            <div class="admin-kv"><dt>用户 ID</dt><dd>#{{ form.user_id || '暂无' }}</dd></div>
            <div class="admin-kv"><dt>角色</dt><dd>{{ roleLabel }}</dd></div>
          </dl>
          <dl>
            <div class="admin-kv"><dt>账号状态</dt><dd>{{ statusLabel(form.status) }}</dd></div>
            <div class="admin-kv"><dt>最近登录</dt><dd>{{ form.last_login_time || '暂无记录' }}</dd></div>
          </dl>
          <dl>
            <div class="admin-kv"><dt>创建时间</dt><dd>{{ form.create_time || '暂无记录' }}</dd></div>
            <div class="admin-kv"><dt>更新时间</dt><dd>{{ form.update_time || '暂无记录' }}</dd></div>
          </dl>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Save } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { getCurrentUser, updateCurrentUser } from '@/api/auth'
import ImageUploader from '@/components/ImageUploader.vue'
import { useAuthStore } from '@/stores/auth'
import { resolveAvatarUrl, useDefaultAvatar } from '@/utils/assets'
import { statusLabel } from '@/modules/admin/utils'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  user_id: '',
  username: '',
  phone: '',
  email: '',
  avatar: '',
  role: '',
  status: '',
  create_time: '',
  update_time: '',
  last_login_time: ''
})

const avatarUrl = computed(() => resolveAvatarUrl(form.avatar))
const avatarInitial = computed(() => (form.username || '管').slice(0, 1).toUpperCase())
const roleLabel = computed(() => form.role === 'admin' ? '管理员' : statusLabel(form.role))

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const user = await getCurrentUser('admin')
    if (user?.role !== 'admin') {
      handleInvalidAdminSession()
      return
    }
    assignForm(user)
    authStore.setUser(user, 'admin')
    localStorage.setItem('diabetes_admin_user', JSON.stringify(user))
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '管理员资料加载失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!form.username) {
    ElMessage.warning('请填写用户名')
    return
  }

  saving.value = true
  try {
    const updated = await updateCurrentUser({
      username: form.username,
      phone: form.phone,
      email: form.email,
      avatar: form.avatar
    }, 'admin')
    if (updated?.role !== 'admin') {
      handleInvalidAdminSession()
      return
    }
    assignForm(updated)
    authStore.setUser(updated, 'admin')
    localStorage.setItem('diabetes_admin_user', JSON.stringify(updated))
    ElMessage.success('管理员资料已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '资料保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

function handleInvalidAdminSession() {
  authStore.clearSession('admin')
  localStorage.removeItem('diabetes_admin_user')
  ElMessage.error('管理员登录状态已失效，请重新登录')
  router.push('/admin/login')
}

function assignForm(user = {}) {
  Object.assign(form, {
    user_id: user.user_id || user.id || '',
    username: user.username || '',
    phone: user.phone || '',
    email: user.email || '',
    avatar: user.avatar || '',
    role: user.role || '',
    status: user.status || '',
    create_time: user.create_time || '',
    update_time: user.update_time || '',
    last_login_time: user.last_login_time || user.lastLoginTime || ''
  })
}
</script>

<style scoped>
.admin-profile-page {
  max-width: 1160px;
}

.admin-profile-hero,
.admin-profile-card {
  padding: 20px;
}

.admin-profile-hero {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #ffffff 0%, #eef3ff 62%, #e8f7ff 100%);
}

.admin-profile-hero h2,
.admin-profile-card h3 {
  margin: 0;
  color: var(--admin-text-title);
}

.admin-profile-hero h2 {
  margin-bottom: 4px;
  font-size: 22px;
}

.admin-profile-hero p {
  margin: 0;
  color: var(--admin-text-secondary);
}

.admin-profile-grid {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.admin-profile-card h3 {
  margin-bottom: 16px;
  font-size: 16px;
}

.admin-profile-card dl {
  margin: 0;
}

@media (max-width: 980px) {
  .admin-profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
