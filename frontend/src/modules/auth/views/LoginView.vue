<template>
  <MobileShell>
    <section class="auth-page">
      <div class="glow glow--top" />
      <div class="glow glow--bottom" />

      <div class="auth-page__content">
        <div class="auth-page__brand">
          <div class="brand-logo">
            <Leaf class="brand-logo__leaf" :stroke-width="2" />
            <span class="brand-logo__badge">
              <Droplet class="brand-logo__drop" fill="white" />
            </span>
          </div>
          <h1>{{ isAdmin ? '管理员登录' : '糖尿病预治智能助手' }}</h1>
          <p>{{ isAdmin ? '管理端账号登录' : '让控糖生活更清晰' }}</p>
        </div>

        <form class="auth-card" @submit.prevent="handleLogin">
          <label class="form-field">
            <span>账号</span>
            <input v-model.trim="form.username" type="text" autocomplete="username" placeholder="请输入手机号或用户名" />
          </label>

          <label class="form-field">
            <span>密码</span>
            <div class="password-field">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="current-password"
                placeholder="请输入密码"
              />
              <button type="button" class="icon-button" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" />
                <Eye v-else />
              </button>
            </div>
          </label>

          <div class="form-row">
            <label class="remember">
              <input v-model="rememberMe" type="checkbox" />
              <span>记住登录状态</span>
            </label>
            <button type="button" class="text-button" @click="openResetDialog">忘记密码？</button>
          </div>

          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? '登录中...' : '登录' }}
          </button>
        </form>

        <div v-if="!isAdmin" class="auth-switch">
          <span>还没有账号？</span>
          <button type="button" @click="router.push('/register')">立即注册</button>
        </div>
      </div>

      <div v-if="resetDialogVisible" class="reset-overlay" @click.self="closeResetDialog">
        <section class="reset-panel">
          <header class="reset-panel__head">
            <h2>重置密码</h2>
            <p>请输入账号，并通过绑定邮箱验证码完成身份校验。</p>
          </header>
          <div class="reset-dialog">
            <input v-model.trim="resetForm.account" type="text" placeholder="用户名或手机号" />
            <input v-model.trim="resetForm.email" type="email" placeholder="注册邮箱" />
            <div class="reset-code-field">
              <input v-model.trim="resetForm.emailCode" type="text" inputmode="numeric" maxlength="6" placeholder="邮箱验证码" />
              <button type="button" :disabled="codeSending || codeCountdown > 0" @click="sendResetEmailCode">
                {{ codeCountdown > 0 ? `${codeCountdown}s` : (codeSending ? '发送中' : '发送验证码') }}
              </button>
            </div>
            <input v-model="resetForm.newPassword" type="password" placeholder="新密码，6-32位" />
          </div>
          <footer class="reset-panel__actions">
            <button type="button" class="reset-panel__cancel" @click="closeResetDialog">取消</button>
            <button type="button" class="reset-panel__confirm" :disabled="resetSubmitting" @click="submitResetPassword">
              {{ resetSubmitting ? '重置中...' : '确认重置' }}
            </button>
          </footer>
        </section>
      </div>
    </section>
  </MobileShell>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { Droplet, Eye, EyeOff, Leaf } from 'lucide-vue-next'
import MobileShell from '@/components/mobile/MobileShell.vue'
import { login, resetPassword, sendEmailCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { prewarmPatientSmartRecommendations } from '@/modules/article/utils/smartRecommendationCache'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const REMEMBER_ACCOUNT_KEY = 'diabetes_patient_remember_account'

const isAdmin = computed(() => route.meta?.role === 'admin')
const showPassword = ref(false)
const rememberMe = ref(false)
const submitting = ref(false)
const form = reactive({
  username: '',
  password: ''
})
const resetDialogVisible = ref(false)
const resetSubmitting = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer = null
const resetForm = reactive({
  account: '',
  email: '',
  emailCode: '',
  newPassword: ''
})

async function handleLogin() {
  if (!form.username || !form.password) {
    showToast('请输入账号和密码')
    return
  }

  submitting.value = true
  try {
    const session = await login({
      account: form.username,
      password: form.password
    })
    if (isAdmin.value && session.user?.role !== 'admin') {
      showToast('当前账号不是管理员')
      authStore.clearSession('admin')
      return
    }
    if (!isAdmin.value && session.user?.role === 'admin') {
      showToast('请使用患者账号登录')
      authStore.clearSession('patient')
      return
    }
    authStore.setUser(null)
    authStore.setSession({ ...session, remember: rememberMe.value })
    saveRememberedAccount()
    if (!isAdmin.value) {
      prewarmPatientSmartRecommendations()
    }
    showToast('登录成功')
    router.push(isAdmin.value ? '/admin/dashboard' : '/app/home')
  } catch (error) {
    showToast(error?.response?.data?.message || '登录失败')
  } finally {
    submitting.value = false
  }
}

function saveRememberedAccount() {
  if (rememberMe.value) {
    localStorage.setItem(REMEMBER_ACCOUNT_KEY, form.username)
  } else {
    localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
  }
}

function openResetDialog() {
  resetForm.account = form.username || ''
  resetForm.email = ''
  resetForm.emailCode = ''
  resetForm.newPassword = ''
  resetDialogVisible.value = true
}

function closeResetDialog() {
  if (resetSubmitting.value) return
  resetDialogVisible.value = false
}

async function submitResetPassword() {
  if (!resetForm.account || !resetForm.email || !resetForm.emailCode || !resetForm.newPassword) {
    showToast('请填写账号、邮箱、验证码和新密码')
    return
  }
  if (resetForm.newPassword.length < 6 || resetForm.newPassword.length > 32) {
    showToast('新密码长度需为6-32位')
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
    form.username = resetForm.account
    showToast('密码已重置，请使用新密码登录')
    resetDialogVisible.value = false
  } catch (error) {
    showToast(error?.response?.data?.message || '密码重置失败')
  } finally {
    resetSubmitting.value = false
  }
}

async function sendResetEmailCode() {
  if (!resetForm.email) {
    showToast('请先填写注册邮箱')
    return
  }
  codeSending.value = true
  try {
    const result = await sendEmailCode({ email: resetForm.email, purpose: 'reset_password' })
    startCodeCountdown()
    showToast(result?.debug_code ? `验证码：${result.debug_code}` : '验证码已发送，请查收邮箱')
  } catch (error) {
    showToast(error?.response?.data?.message || '验证码发送失败')
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
    form.username = rememberedAccount
    rememberMe.value = true
  }
})

watch(rememberMe, (checked) => {
  if (!checked) localStorage.removeItem(REMEMBER_ACCOUNT_KEY)
})
</script>

<style scoped>
@import './auth-mobile.css';

.reset-overlay {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 18px 18px calc(22px + env(safe-area-inset-bottom));
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(8px);
}

.reset-panel {
  width: 100%;
  max-width: 100%;
  max-height: calc(100% - 36px);
  overflow: hidden;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 60px rgba(31, 41, 55, 0.18);
}

.reset-panel__head {
  padding: 24px 24px 10px;
}

.reset-panel__head h2 {
  margin: 0 0 8px;
  color: var(--figma-text-primary);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.reset-panel__head p {
  margin: 0;
  color: var(--figma-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.reset-dialog {
  display: grid;
  gap: 10px;
  padding: 10px 24px 18px;
}

.reset-dialog input {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  border: 1px solid rgba(174, 232, 199, 0.9);
  border-radius: 16px;
  outline: none;
  background: #F7FCF9;
  color: var(--figma-text-primary);
  font-size: 14px;
  transition: 0.18s ease;
}

.reset-code-field {
  display: grid;
  grid-template-columns: 1fr 104px;
  gap: 10px;
}

.reset-code-field button {
  height: 46px;
  border-radius: 16px;
  background: #DDF7E9;
  color: var(--figma-primary-green-dark);
  font-size: 13px;
  font-weight: 600;
}

.reset-code-field button:disabled {
  opacity: 0.68;
}

.reset-dialog input:focus {
  border-color: var(--figma-primary-green);
  background: #FFFFFF;
  box-shadow: 0 0 0 3px rgba(111, 207, 151, 0.14);
}

.reset-panel__actions {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 10px;
  padding: 0 24px 24px;
}

.reset-panel__actions button {
  height: 44px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
}

.reset-panel__cancel {
  background: #EEF8F2;
  color: var(--figma-text-secondary);
}

.reset-panel__confirm {
  background: var(--figma-green-button);
  color: #FFFFFF;
  box-shadow: var(--figma-shadow-button);
}

.reset-panel__confirm:disabled {
  opacity: 0.58;
}
</style>
