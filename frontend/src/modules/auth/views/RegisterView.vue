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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft, Eye, EyeOff } from 'lucide-vue-next'
import MobileShell from '@/components/mobile/MobileShell.vue'
import { login, register } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const agreed = ref(false)
const submitting = ref(false)

const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
})

async function handleRegister() {
  if (!form.username || !form.password || !form.confirmPassword) {
    showToast('请填写用户名和密码')
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
      email: form.email
    })
    const session = await login({
      account: form.username,
      password: form.password
    })
    authStore.setSession(session)
    showToast('注册成功')
    router.push('/app/account')
  } catch (error) {
    showToast(error?.response?.data?.message || '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import './auth-mobile.css';
</style>
