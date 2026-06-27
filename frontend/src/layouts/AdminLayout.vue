<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <div class="admin-logo-mark">
          <Leaf :size="20" />
          <span class="admin-logo-badge"><Shield :size="12" /></span>
        </div>
        <div>
          <strong>糖尿病预治助手</strong>
          <span>管理控制台</span>
        </div>
      </div>

      <nav class="admin-nav">
        <section v-for="group in navGroups" :key="group.label" class="admin-nav-group">
          <p>{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="admin-nav-link"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
            <ChevronRight class="active-arrow" :size="14" />
          </RouterLink>
        </section>
      </nav>

      <button class="admin-logout" type="button" @click="logout">
        <LogOut :size="18" />
        <span>退出登录</span>
      </button>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div class="admin-breadcrumb">
          <span>后台管理</span>
          <ChevronRight :size="14" />
          <strong>{{ route.meta?.title || '管理端' }}</strong>
        </div>

        <div class="admin-topbar-right">
          <div class="admin-search">
            <Search :size="14" />
            <input type="text" placeholder="搜索..." />
          </div>
          <button class="admin-user-chip" type="button" @click="router.push('/admin/profile')">
            <span class="admin-user-avatar">
              <img :src="adminAvatar" alt="管理员头像" @error="useDefaultAvatar" />
            </span>
            <strong>{{ adminName }}</strong>
          </button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChevronRight,
  Activity,
  Bot,
  ClipboardList,
  FileText,
  Home,
  LayoutDashboard,
  Leaf,
  LogOut,
  Newspaper,
  Search,
  Shield,
  UserCheck,
  Users,
  CheckSquare
} from 'lucide-vue-next'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { resolveAvatarUrl, useDefaultAvatar } from '@/utils/assets'
import '@/modules/admin/styles/admin.css'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navGroups = [
  {
    label: '核心功能',
    items: [
      { label: '首页概览', path: '/admin/dashboard', icon: LayoutDashboard },
      { label: '用户管理', path: '/admin/users', icon: Users },
      { label: '生活方案记录', path: '/admin/life-plans', icon: FileText }
    ]
  },
  {
    label: '健康运营',
    items: [
      { label: '健康档案管理', path: '/admin/profiles', icon: ClipboardList },
      { label: '健康数据管理', path: '/admin/health-metrics', icon: Activity },
      { label: '风险评估记录', path: '/admin/risk-assessments', icon: CheckSquare },
      { label: 'AI 咨询日志', path: '/admin/ai-chat', icon: Bot },
      { label: '打卡记录', path: '/admin/checkins', icon: CheckSquare }
    ]
  },
  {
    label: '内容管理',
    items: [
      { label: '健康资讯管理', path: '/admin/articles', icon: Newspaper },
      { label: '首页内容管理', path: '/admin/home-content', icon: Home },
      { label: '专家展示管理', path: '/admin/experts', icon: UserCheck }
    ]
  }
]

const adminUser = computed(() => authStore.user?.role === 'admin' ? authStore.user : null)
const adminName = computed(() => adminUser.value?.username || adminUser.value?.name || '管理员')
const adminAvatar = computed(() => resolveAvatarUrl(adminUser.value?.avatar))

onMounted(ensureAdminUser)

watch(() => authStore.user?.role, ensureAdminUser)

function ensureAdminUser() {
  const token = authStore.restoreSession('admin')
  if (!token) {
    router.push('/admin/login')
    return
  }

  if (authStore.user && authStore.user.role !== 'admin') {
    authStore.setUser(null)
  }
}

function logout() {
  ElMessageBox.confirm('退出后需要重新登录管理端，确认退出吗？', '确认退出登录', {
    confirmButtonText: '确认退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.clearSession('admin')
    localStorage.removeItem('diabetes_admin_user')
    router.push('/admin/login')
  }).catch(() => {})
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: grid;
  grid-template-columns: var(--admin-sidebar-width) 1fr;
  background: var(--admin-bg);
  font-family: var(--admin-font-family);
  overflow: hidden;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: var(--admin-sidebar-bg);
  color: rgba(255, 255, 255, 0.86);
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
}

.admin-logo-mark {
  position: relative;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg,#5c8ef8,#7eb5ff);
}

.admin-logo-badge {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #4ade80;
  color: #fff;
  box-shadow: 0 3px 8px rgba(0,0,0,0.18);
}

.admin-logo strong {
  display: block;
  color: #fff;
  font-size: 14px;
  line-height: 1.35;
}

.admin-logo span {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  margin-top: 2px;
}

.admin-nav {
  flex: 1;
  padding: 12px;
}

.admin-nav-group {
  margin-bottom: 18px;
}

.admin-nav-group p {
  margin: 0 8px 8px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  border-radius: 12px;
  padding: 0 12px;
  color: rgba(255, 255, 255, 0.82);
  transition: background 0.2s ease, color 0.2s ease;
}

.admin-nav-link:hover {
  background: rgba(255,255,255,0.07);
  color: #fff;
}

.admin-nav-link.router-link-active {
  color: #fff;
  background: var(--admin-sidebar-active);
  box-shadow: 0 4px 14px rgba(92,142,248,0.35);
}

.admin-nav-link span {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.active-arrow {
  opacity: 0;
}

.router-link-active .active-arrow {
  opacity: 0.65;
}

.admin-logout {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 12px 16px;
  min-height: 42px;
  border-radius: 12px;
  padding: 0 12px;
  color: rgba(255, 255, 255, 0.78);
  background: transparent;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
}

.admin-logout:hover {
  color: #f87171;
  background: rgba(239,68,68,0.12);
}

.admin-main {
  min-width: 0;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
}

.admin-main > :last-child {
  min-height: calc(100vh - var(--admin-topbar-height));
}

.admin-topbar {
  height: var(--admin-topbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid rgba(92,142,248,0.10);
  background: #fff;
  box-shadow: 0 1px 4px rgba(92,142,248,0.06);
}

.admin-breadcrumb,
.admin-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-breadcrumb {
  color: var(--admin-text-secondary);
  font-size: 14px;
}

.admin-breadcrumb strong {
  color: var(--admin-text-strong);
}

.admin-search {
  width: 224px;
  height: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(92,142,248,0.15);
  border-radius: 8px;
  background: #f4f7fe;
  padding: 0 10px;
  color: var(--admin-text-muted);
}

.admin-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--admin-text-strong);
  font-size: 14px;
}

.admin-user-chip {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  padding: 4px 10px 4px 4px;
  color: var(--admin-text-strong);
  background: #f8faff;
  border: 1px solid rgba(92,142,248,0.12);
  cursor: pointer;
}

.admin-user-chip:hover {
  background: #eef3ff;
}

.admin-user-avatar {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 999px;
  background: var(--admin-primary-gradient);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.admin-user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-user-chip strong {
  font-size: 14px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 76px 1fr;
  }

  .admin-logo div:last-child,
  .admin-nav-group p,
  .admin-nav-link span,
  .admin-logout span,
  .active-arrow {
    display: none;
  }

  .admin-logo {
    justify-content: center;
    padding: 18px 0;
  }

  .admin-search {
    display: none;
  }
}
</style>
