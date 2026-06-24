<template>
  <div class="edit-page">
    <header class="edit-topbar">
      <button type="button" class="back-button" @click="router.push('/app/account')">
        <ArrowLeft />
      </button>
      <h1>编辑账号信息</h1>
      <span />
    </header>

    <main class="edit-content">
      <section class="edit-card">
        <label class="edit-field">
          <span>用户名</span>
          <input v-model.trim="form.username" type="text" placeholder="请输入用户名" />
        </label>

        <label class="edit-field">
          <span>手机号</span>
          <input v-model.trim="form.phone" type="tel" placeholder="请输入手机号" />
        </label>

        <label class="edit-field">
          <span>邮箱</span>
          <input v-model.trim="form.email" type="email" placeholder="请输入邮箱" />
        </label>

        <label class="edit-field">
          <span>头像地址</span>
          <input v-model.trim="form.avatar" type="text" placeholder="可填写头像 URL" />
        </label>
      </section>

      <section class="edit-tip">
        <h2>账号资料说明</h2>
        <p>本轮为前端模拟保存，正式联调时会通过 PUT /api/user/me 更新。</p>
      </section>
    </main>

    <footer class="edit-footer">
      <button type="button" class="save-button" :disabled="saving" @click="save">
        {{ saving ? '保存中…' : '保存账号信息' }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft } from 'lucide-vue-next'
import { getMockCurrentUser, updateMockCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const saving = ref(false)
const form = reactive({
  username: '',
  phone: '',
  email: '',
  avatar: ''
})

onMounted(async () => {
  const user = authStore.user || await getMockCurrentUser()
  form.username = user.username || ''
  form.phone = user.phone || ''
  form.email = user.email || ''
  form.avatar = user.avatar || ''
  authStore.setUser(user)
})

async function save() {
  if (!form.username) {
    showToast('请填写用户名')
    return
  }

  saving.value = true
  try {
    const updated = await updateMockCurrentUser({ ...form })
    authStore.setUser(updated)
    showToast('账号信息已保存')
    router.push('/app/account')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.edit-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--figma-bg-page);
}

.edit-topbar {
  flex-shrink: 0;
  height: 64px;
  padding: 0 20px;
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
  background: var(--figma-bg-page);
}

.edit-topbar h1 {
  margin: 0;
  text-align: center;
  color: var(--figma-text-strong);
  font-size: 16px;
  font-weight: 600;
}

.back-button {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(174, 232, 199, 0.28);
  color: #4A7A62;
}

.back-button svg {
  width: 18px;
  height: 18px;
}

.edit-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

.edit-card,
.edit-tip {
  border-radius: 24px;
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.edit-card {
  padding: 18px 20px;
}

.edit-field {
  display: block;
  margin-bottom: 16px;
}

.edit-field:last-child {
  margin-bottom: 0;
}

.edit-field span {
  display: block;
  margin-bottom: 8px;
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 600;
}

.edit-field input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1.5px solid rgba(174, 232, 199, 0.50);
  border-radius: 16px;
  outline: none;
  background: #F3FAF6;
  color: var(--figma-text-strong);
  font-size: 13px;
}

.edit-field input:focus {
  border-color: #9FDEB8;
  background: #FFFFFF;
}

.edit-tip {
  margin-top: 12px;
  padding: 16px 20px;
  background: var(--figma-info-soft);
  border: 1px solid rgba(174, 232, 199, 0.35);
}

.edit-tip h2 {
  margin: 0 0 6px;
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 600;
}

.edit-tip p {
  margin: 0;
  color: var(--figma-text-faint);
  font-size: 12px;
  line-height: 1.7;
}

.edit-footer {
  flex-shrink: 0;
  padding: 12px 16px;
  background: var(--figma-bg-page);
}

.save-button {
  width: 100%;
  height: 48px;
  border-radius: 999px;
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 500;
  box-shadow: var(--figma-shadow-button);
}

.save-button:disabled {
  opacity: 0.72;
}
</style>
