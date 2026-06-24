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
            <input v-model.trim="form.username" type="text" placeholder="请输入手机号或用户名" />
          </label>

          <label class="form-field">
            <span>密码</span>
            <div class="password-field">
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
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
            <button type="button" class="text-button" @click="showToast('找回密码功能后续接入')">忘记密码？</button>
          </div>

          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? '登录中…' : '登录' }}
          </button>
        </form>

        <div v-if="!isAdmin" class="auth-switch">
          <span>还没有账号？</span>
          <button type="button" @click="router.push('/register')">立即注册</button>
        </div>

        <div v-if="!isAdmin" class="demo-card">
          <p>演示账号（直接复制使用）</p>
          <span>账号：<strong>demo</strong>　密码：<strong>123456</strong></span>
        </div>
      </div>
    </section>
  </MobileShell>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { Droplet, Eye, EyeOff, Leaf } from 'lucide-vue-next'
import MobileShell from '@/components/mobile/MobileShell.vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAdmin = computed(() => route.meta?.role === 'admin')
const showPassword = ref(false)
const rememberMe = ref(false)
const submitting = ref(false)
const form = reactive({
  username: '',
  password: ''
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
      authStore.clearSession()
      return
    }
    authStore.setSession(session)
    showToast('登录成功')
    router.push(isAdmin.value ? '/admin/dashboard' : '/app/account')
  } catch (error) {
    showToast(error?.response?.data?.message || '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import './auth-mobile.css';
</style>
