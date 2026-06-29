<template>
  <MobileShell>
    <section class="auth-page auth-page--scroll">
      <div class="glow glow--top-register" />
      <div class="glow glow--bottom-register" />

      <div class="auth-topbar">
        <button type="button" class="back-button" @click="router.push('/login')">
          <ArrowLeft />
        </button>
        <h1>创建账号</h1>
        <span />
      </div>

      <div class="auth-page__scroll mobile-scroll">
        <form class="auth-card" @submit.prevent="handleRegister">
          <label class="form-field">
            <span>用户名</span>
            <input v-model.trim="form.username" type="text" placeholder="请输入用户名" />
          </label>

          <label class="form-field">
            <span>手机号</span>
            <input v-model.trim="form.phone" type="tel" placeholder="请输入手机号" />
          </label>

          <label class="form-field">
            <span>邮箱</span>
            <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" />
          </label>

          <label class="form-field">
            <span>邮箱验证码</span>
            <div class="code-field">
              <input v-model.trim="form.emailCode" type="text" inputmode="numeric" maxlength="6" placeholder="请输入6位验证码" />
              <button type="button" :disabled="codeSending || codeCountdown > 0" @click="sendRegisterEmailCode">
                {{ codeCountdown > 0 ? `${codeCountdown}s` : (codeSending ? '发送中' : '发送验证码') }}
              </button>
            </div>
          </label>

          <label class="form-field">
            <span>密码</span>
            <div class="password-field">
              <input v-model="form.password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" />
              <button type="button" class="icon-button" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" />
                <Eye v-else />
              </button>
            </div>
          </label>

          <label class="form-field form-field--last">
            <span>确认密码</span>
            <div class="password-field">
              <input
                v-model="form.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="请再次输入密码"
              />
              <button type="button" class="icon-button" @click="showConfirmPassword = !showConfirmPassword">
                <EyeOff v-if="showConfirmPassword" />
                <Eye v-else />
              </button>
            </div>
          </label>

          <label class="terms">
            <input v-model="agreed" type="checkbox" />
            <span>
              我已阅读并同意
              <button type="button" @click.stop="showToast('用户协议请以实际注册页面提示为准')">用户协议</button>
              与
              <button type="button" @click.stop="router.push('/app/account/privacy')">隐私说明</button>
            </span>
          </label>

          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? '创建中…' : '创建账号' }}
          </button>
        </form>

        <div class="auth-switch auth-switch--bottom">
          <span>已有账号？</span>
          <button type="button" @click="router.push('/login')">去登录</button>
        </div>
      </div>
    </section>
  </MobileShell>
</template>

<script setup>
import { onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft, Eye, EyeOff } from 'lucide-vue-next'
import MobileShell from '@/components/mobile/MobileShell.vue'
import { login, register, sendEmailCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const agreed = ref(false)
const submitting = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

const form = reactive({
  username: '',
  phone: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: ''
})

async function handleRegister() {
  if (!form.username || !form.email || !form.emailCode || !form.password || !form.confirmPassword) {
    showToast('请填写用户名、邮箱、验证码和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    showToast('两次密码输入不一致')
    return
  }
  if (!agreed.value) {
    showToast('请阅读并同意用户协议与隐私说明')
    return
  }

  submitting.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone,
      email: form.email,
      email_code: form.emailCode
    })
    const session = await login({
      account: form.username,
      password: form.password
    })
    authStore.setSession(session)
    showToast('注册成功')
    router.push('/app/home')
  } catch (error) {
    showToast(error?.response?.data?.message || '注册失败')
  } finally {
    submitting.value = false
  }
}

async function sendRegisterEmailCode() {
  if (!form.email) {
    showToast('请先填写邮箱')
    return
  }
  codeSending.value = true
  try {
    const result = await sendEmailCode({ email: form.email, purpose: 'register' })
    startCountdown()
    showToast(result?.debug_code ? `验证码：${result.debug_code}` : '验证码已发送，请查收邮箱')
  } catch (error) {
    showToast(error?.response?.data?.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
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
</script>

<style scoped>
@import './auth-mobile.css';

.code-field {
  display: grid;
  grid-template-columns: 1fr 104px;
  gap: 10px;
}

.code-field input {
  width: 100%;
  height: 48px;
  border: 1px solid transparent;
  border-radius: var(--figma-radius-input);
  outline: none;
  padding: 0 16px;
  background: var(--figma-bg-main);
  color: var(--figma-text-primary);
  font-size: 16px;
}

.code-field button {
  height: 48px;
  border-radius: var(--figma-radius-pill);
  background: #DDF7E9;
  color: var(--figma-primary-green-dark);
  font-size: 13px;
  font-weight: 600;
}

.code-field button:disabled {
  opacity: 0.68;
}
</style>
