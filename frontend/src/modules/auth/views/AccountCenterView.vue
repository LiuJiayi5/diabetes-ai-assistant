<template>
  <div class="account-page">
    <section class="profile-card">
      <div class="profile-card__glow profile-card__glow--top" />
      <div class="profile-card__glow profile-card__glow--bottom" />
      <div class="profile-card__body">
        <div class="avatar">
          <User />
        </div>
        <div class="profile-card__info">
          <h1>{{ currentUser.username }}</h1>
          <div class="badges">
            <span>患者用户</span>
            <span class="badges__status">正常</span>
          </div>
        </div>
      </div>
      <p>持续记录健康数据，获得更适合你的建议</p>
    </section>

    <section class="info-card">
      <h2>账号信息</h2>
      <div class="info-list">
        <div v-for="(row, index) in accountRows" :key="row.label" class="info-row">
          <div class="info-row__line">
            <span>{{ row.label }}</span>
            <strong>{{ row.value }}</strong>
          </div>
          <div v-if="index < accountRows.length - 1" class="divider" />
        </div>
      </div>
    </section>

    <section class="info-card">
      <h2>健康记录</h2>
      <div class="record-grid">
        <button
          v-for="item in healthRecords"
          :key="item.label"
          class="record-tile"
          :style="{ background: item.bg }"
          type="button"
          @click="handleRecordClick(item)"
        >
          <span class="record-tile__icon">
            <component :is="item.icon" :style="{ color: item.iconColor }" :stroke-width="1.8" />
          </span>
          <strong>{{ item.label }}</strong>
        </button>
      </div>
    </section>

    <section class="settings-card">
      <div v-for="(item, index) in settingsItems" :key="item.label">
        <button
          class="setting-row"
          :class="{ 'setting-row--danger': item.danger }"
          type="button"
          @click="item.action"
        >
          <span class="setting-row__left">
            <span class="setting-row__icon">
              <component :is="item.icon" :stroke-width="1.8" />
            </span>
            <span>{{ item.label }}</span>
          </span>
          <ChevronRight class="setting-row__arrow" :stroke-width="1.5" />
        </button>
        <div v-if="index < settingsItems.length - 1" class="settings-divider" />
      </div>
    </section>

    <p class="medical-note">AI 建议仅供参考，不能替代线下诊疗</p>

    <div v-if="showLogoutDialog" class="logout-overlay" @click.self="showLogoutDialog = false">
      <div class="logout-dialog">
        <h3>确认退出登录？</h3>
        <p>退出后需要重新登录才能查看个人信息</p>
        <div class="logout-dialog__actions">
          <button type="button" class="dialog-button dialog-button--cancel" @click="showLogoutDialog = false">
            取消
          </button>
          <button type="button" class="dialog-button dialog-button--danger" @click="confirmLogout">
            确认退出
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  Activity,
  CalendarCheck,
  ChevronRight,
  ClipboardList,
  Edit,
  FileText,
  Info,
  LogOut,
  MessageSquare,
  Shield,
  User,
  Utensils
} from 'lucide-vue-next'
import { getMockCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const showLogoutDialog = ref(false)

const currentUser = computed(() => authStore.user || {
  id: 10001,
  username: '小代',
  phone: '138****2026',
  email: 'xiaodai@example.com',
  role: 'patient',
  status: 'active',
  lastLogin: '今天 09:30'
})

const accountRows = computed(() => [
  { label: '用户 ID', value: currentUser.value.id || currentUser.value.user_id || '10001' },
  { label: '手机号', value: currentUser.value.phone || '138****2026' },
  { label: '邮箱', value: currentUser.value.email || 'xiaodai@example.com' },
  { label: '最近登录', value: currentUser.value.lastLogin || currentUser.value.last_login_time || '今天 09:30' }
])

const healthRecords = [
  { label: '健康档案', icon: Activity, bg: '#E8F7EE', iconColor: '#5BBF8A', path: '/app/profile' },
  { label: '评估记录', icon: ClipboardList, bg: '#E2F3FA', iconColor: '#4FAAC4', path: '/app/risk' },
  { label: '生活方案', icon: Utensils, bg: '#EBF8F2', iconColor: '#5BBF8A', path: '/app/life-plan' },
  { label: '咨询记录', icon: MessageSquare, bg: '#E4F4FB', iconColor: '#4FAAC4', path: '/app/ai-chat' }
]

const settingsItems = [
  { label: '打卡记录', icon: CalendarCheck, action: () => router.push('/app/checkin') },
  { label: '编辑账号信息', icon: Edit, action: () => router.push('/app/account/edit') },
  { label: '登录状态与安全', icon: Shield, action: () => showToast('登录状态与安全后续接入') },
  { label: '隐私说明', icon: FileText, action: () => router.push('/app/account/privacy') },
  { label: '关于系统', icon: Info, action: () => router.push('/app/account/about') },
  { label: '退出登录', icon: LogOut, danger: true, action: () => { showLogoutDialog.value = true } }
]

onMounted(async () => {
  if (!authStore.user) {
    const user = await getMockCurrentUser()
    authStore.setUser(user)
  }
})

function handleRecordClick(item) {
  if (item.path) {
    router.push(item.path)
  } else {
    showToast('后续模块接入')
  }
}

function confirmLogout() {
  authStore.clearSession()
  showLogoutDialog.value = false
  showToast('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.account-page {
  min-height: 100%;
  padding: 24px 20px 16px;
  background: var(--figma-bg-page);
}

.profile-card {
  position: relative;
  overflow: hidden;
  padding: 20px;
  border-radius: 28px;
  background: var(--figma-profile-card);
  box-shadow: var(--figma-shadow-hero);
}

.profile-card__glow {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.profile-card__glow--top {
  top: -32px;
  right: -32px;
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.22);
  filter: blur(24px);
}

.profile-card__glow--bottom {
  left: -24px;
  bottom: -24px;
  width: 128px;
  height: 128px;
  background: rgba(190, 240, 242, 0.30);
  filter: blur(20px);
}

.profile-card__body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.70);
  backdrop-filter: blur(8px);
}

.avatar svg {
  width: 32px;
  height: 32px;
  color: #5BBF8A;
}

.profile-card__info h1 {
  margin: 0 0 6px;
  color: var(--figma-text-strong);
  font-size: 18px;
  font-weight: 700;
}

.badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badges span {
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  color: #4A7A62;
  font-size: 11px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.badges__status {
  color: #4A8A9C !important;
}

.profile-card p {
  position: relative;
  z-index: 1;
  margin: 12px 0 0;
  color: rgba(36, 50, 61, 0.55);
  font-size: 11px;
  line-height: 1.7;
}

.info-card,
.settings-card {
  margin-top: 16px;
  border-radius: 24px;
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-profile);
}

.info-card {
  padding: 16px 20px;
}

.info-card h2 {
  margin: 0 0 12px;
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row__line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 0;
}

.info-row span {
  color: var(--figma-text-muted);
  font-size: 12px;
}

.info-row strong {
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 500;
}

.divider {
  height: 1px;
  margin-top: 12px;
  background: rgba(120, 160, 150, 0.10);
}

.record-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.record-tile {
  min-height: 104px;
  padding: 16px;
  text-align: left;
  border-radius: 20px;
}

.record-tile__icon {
  width: 36px;
  height: 36px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #FFFFFF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.record-tile__icon svg {
  width: 18px;
  height: 18px;
}

.record-tile strong {
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 500;
}

.settings-card {
  padding: 8px;
}

.setting-row {
  width: 100%;
  padding: 14px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 18px;
  background: transparent;
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 500;
}

.setting-row:active {
  background: rgba(174, 232, 199, 0.15);
}

.setting-row__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-row__icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: rgba(174, 232, 199, 0.30);
  color: #5BBF8A;
}

.setting-row__icon svg,
.setting-row__arrow {
  width: 16px;
  height: 16px;
}

.setting-row__arrow {
  color: #AABCB5;
}

.setting-row--danger {
  color: var(--figma-error);
}

.setting-row--danger .setting-row__icon {
  background: rgba(239, 143, 143, 0.12);
  color: var(--figma-error);
}

.setting-row--danger .setting-row__arrow {
  color: var(--figma-error);
}

.setting-row--danger:active {
  background: rgba(239, 143, 143, 0.07);
}

.settings-divider {
  height: 1px;
  margin: 0 12px;
  background: rgba(120, 160, 150, 0.08);
}

.medical-note {
  margin: 16px 0 0;
  padding-bottom: 4px;
  text-align: center;
  color: rgba(120, 135, 148, 0.50);
  font-size: 10px;
}

.logout-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(36, 50, 61, 0.28);
  backdrop-filter: blur(6px);
}

.logout-dialog {
  width: min(320px, 100%);
  padding: 26px 20px 20px;
  border-radius: 28px;
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-dialog);
  text-align: center;
}

.logout-dialog h3 {
  margin: 0 0 8px;
  color: var(--figma-text-strong);
  font-size: 16px;
  font-weight: 600;
}

.logout-dialog p {
  margin: 0;
  color: var(--figma-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.logout-dialog__actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.dialog-button {
  flex: 1;
  height: 44px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.dialog-button--cancel {
  background: rgba(174, 232, 199, 0.25);
  color: #5BBF8A;
}

.dialog-button--danger {
  background: var(--figma-danger-button);
  color: #FFFFFF;
}
</style>
