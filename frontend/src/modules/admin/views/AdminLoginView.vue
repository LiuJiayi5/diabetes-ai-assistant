<template>
  <div class="admin-login-page">
    <section class="admin-login-card">
      <div class="admin-login-brand">
        <div>
          <div class="admin-brand-logo">
            <span><Leaf :size="20" /></span>
            <div>
              <strong>糖尿病预治智能助手</strong>
              <small>管理控制台</small>
            </div>
          </div>
          <h1>健康数据<br />安全运营平台</h1>
          <p>为患者提供专业的糖尿病预防与治疗辅助，帮助管理员高效管理用户、内容和健康数据。</p>
        </div>

        <div class="admin-feature-list">
          <div v-for="item in features" :key="item.title" class="admin-feature-item">
            <span><component :is="item.icon" :size="16" /></span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </div>
          </div>
        </div>
      </div>

      <form class="admin-login-form" @submit.prevent="handleLogin">
        <div class="admin-login-inner">
          <h2>欢迎回来</h2>
          <p>请使用管理员账号登录后台</p>

          <label class="admin-login-field">
            <span>管理员账号</span>
            <el-input v-model.trim="form.account" size="large" autocomplete="username" placeholder="请输入管理员账号">
              <template #prefix><User :size="16" /></template>
            </el-input>
          </label>

          <label class="admin-login-field">
            <span>密码</span>
            <el-input v-model="form.password" size="large" type="password" show-password autocomplete="current-password" placeholder="请输入密码">
              <template #prefix><Lock :size="16" /></template>
            </el-input>
          </label>

          <div class="admin-login-options">
            <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
            <el-button link type="primary" @click="openResetDialog">忘记密码？</el-button>
          </div>

          <el-button class="admin-login-submit" native-type="submit" :loading="submitting">
            登录管理端
          </el-button>

          <div class="admin-login-note">仅限授权管理员使用 · 请勿共享账号信息</div>
        </div>
      </form>
    </section>

    <el-dialog v-model="resetDialogVisible" title="重置管理员密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="管理员账号">
          <el-input v-model.trim="resetForm.account" placeholder="用户名或手机号" />
        </el-form-item>
        <el-form-item label="注册邮箱">
          <el-input v-model.trim="resetForm.email" placeholder="请输入绑定邮箱" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="admin-code-field">
            <el-input v-model.trim="resetForm.emailCode" maxlength="6" placeholder="请输入6位验证码" />
            <el-button :disabled="codeSending || codeCountdown > 0" @click="sendResetEmailCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s` : (codeSending ? '发送中' : '发送验证码') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="6-32位新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Activity, Leaf, Lock, Shield, User } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { login, resetPassword, sendEmailCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import '@/modules/admin/styles/admin.css'

const router = useRouter()
const authStore = useAuthStore()
const REMEMBER_ACCOUNT_KEY = 'diabetes_admin_remember_account'
const submitting = ref(false)
const rememberMe = ref(false)
const resetDialogVisible = ref(false)
const resetSubmitting = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

const form = reactive({
  account: '',
  password: ''
})

const resetForm = reactive({
  account: '',
  email: '',
  emailCode: '',
  newPassword: ''
})

const features = [
  { title: '用户管理', desc: '患者账号维护与状态管理', icon: User },
  { title: '数据安全', desc: '健康数据安全运营管理', icon: Shield },
  { title: '数据概览', desc: '实时账号状态和统计分析', icon: Activity }
]

async function handleLogin() {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入管理员账号和密码')
    return
  }

  submitting.value = true
  try {
    const session = await login({
      account: form.account,
      password: form.password
    })

    if (session?.user?.role !== 'admin') {
      authStore.clearSession('admin')
      ElMessage.error('当前账号不是管理员')
      return
    }

    authStore.setUser(null)
    authStore.setSession({ ...session, role: 'admin', remember: rememberMe.value })
    localStorage.setItem('diabetes_admin_user', JSON.stringify(session.user))
    saveRememberedAccount()
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败，请检查账号密码')
  } finally {
    submitting.value = false
  }
}

function saveRememberedAccount() {
  if (rememberMe.value) {
    localStorage.setItem(REMEMBER_ACCOUNT_KEY, form.account)
  } else {
    localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
  }
}

function openResetDialog() {
  resetForm.account = form.account || ''
  resetForm.email = ''
  resetForm.emailCode = ''
  resetForm.newPassword = ''
  resetDialogVisible.value = true
}

async function submitResetPassword() {
  if (!resetForm.account || !resetForm.email || !resetForm.emailCode || !resetForm.newPassword) {
    ElMessage.warning('请填写账号、邮箱、验证码和新密码')
    return
  }
  if (resetForm.newPassword.length < 6 || resetForm.newPassword.length > 32) {
    ElMessage.warning('新密码长度需为6-32位')
    return
  }
  resetSubmitting.value = true
  try {
    await resetPassword({
      account: resetForm.account,
      email: resetForm.email,
      email_code: resetForm.emailCode,
      new_password: resetForm.newPassword
    })
    form.account = resetForm.account
    resetDialogVisible.value = false
    ElMessage.success('密码已重置，请使用新密码登录')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '密码重置失败')
  } finally {
    resetSubmitting.value = false
  }
}

async function sendResetEmailCode() {
  if (!resetForm.email) {
    ElMessage.warning('请先填写绑定邮箱')
    return
  }
  codeSending.value = true
  try {
    const result = await sendEmailCode({ email: resetForm.email, purpose: 'reset_password' })
    startCodeCountdown()
    ElMessage.success(result?.debug_code ? `验证码：${result.debug_code}` : '验证码已发送，请查收邮箱')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

function startCodeCountdown() {
  codeCountdown.value = 60
  if (codeTimer) window.clearInterval(codeTimer)
  codeTimer = window.setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      window.clearInterval(codeTimer)
      codeTimer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (codeTimer) window.clearInterval(codeTimer)
})

onMounted(() => {
  const rememberedAccount = localStorage.getItem(REMEMBER_ACCOUNT_KEY)
  if (rememberedAccount) {
    form.account = rememberedAccount
    rememberMe.value = true
  }
})

watch(rememberMe, (checked) => {
  if (!checked) localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
})
</script>

<style scoped>
.admin-brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.admin-brand-logo > span {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #5c8ef8, #7eb5ff);
  box-shadow: 0 4px 12px rgba(92,142,248,0.45);
}

.admin-brand-logo strong,
.admin-feature-item strong {
  display: block;
  color: #fff;
  font-size: 14px;
}

.admin-brand-logo small {
  color: #7eb5ff;
  font-size: 12px;
}

.admin-login-brand h1 {
  margin: 0 0 16px;
  color: #fff;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.25;
}

.admin-login-brand p {
  margin: 0;
  color: #cfe0ff;
  font-size: 14px;
  line-height: 1.8;
}

.admin-feature-list {
  display: grid;
  gap: 12px;
}

.admin-code-field {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 112px;
  gap: 10px;
}

.admin-feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 12px;
  background: rgba(255,255,255,0.09);
  padding: 14px 16px;
}

.admin-feature-item > span {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  background: rgba(92,142,248,0.25);
}

.admin-feature-item small {
  color: #cfe0ff;
  font-size: 12px;
}

.admin-login-inner h2 {
  margin: 0 0 8px;
  color: var(--admin-text-strong);
  font-size: 24px;
}

.admin-login-inner > p {
  margin: 0 0 28px;
  color: #4b6285;
  font-size: 14px;
}

.admin-login-field {
  display: grid;
  gap: 7px;
  margin-bottom: 18px;
}

.admin-login-field span {
  color: var(--admin-text-strong);
  font-size: 14px;
  font-weight: 500;
}

.admin-login-submit {
  width: 100%;
  height: 44px;
  margin-top: 18px;
  border: 0;
  border-radius: 12px;
  color: #fff;
  font-weight: 600;
  background: var(--admin-primary-gradient);
  box-shadow: 0 3px 10px rgba(92,142,248,0.32);
}

.admin-login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 2px;
}

.admin-login-note {
  margin-top: 16px;
  color: #7a90ad;
  text-align: center;
  font-size: 12px;
}
</style>
