<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink to="/admin/dashboard" class="brand">
        <span class="brand-icon"><Leaf :size="20" /></span>
        <span>
          <strong>糖尿病预治助手</strong>
          <small>管理控制台</small>
        </span>
      </RouterLink>

      <nav class="nav-groups">
        <section v-for="group in navGroups" :key="group.label" class="nav-group">
          <p>{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ disabled: item.disabled }"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
            <ChevronRight v-if="isActive(item.to)" :size="14" />
          </RouterLink>
        </section>
      </nav>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <p class="breadcrumb">管理端 / {{ currentTitle }}</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="topbar-tools">
          <div class="search-box">
            <Search :size="15" />
            <span>搜索...</span>
          </div>
          <button class="icon-btn" title="通知">
            <Bell :size="18" />
          </button>
          <div class="admin-avatar">A</div>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Activity,
  Bell,
  ChevronRight,
  FileText,
  Home,
  LayoutDashboard,
  Leaf,
  MessageCircle,
  Newspaper,
  Search,
  Users
} from 'lucide-vue-next'

const route = useRoute()
const currentTitle = computed(() => route.meta?.title || '管理端')

const navGroups = [
  {
    label: '核心功能',
    items: [
      { label: '首页概览', to: '/admin/dashboard', icon: LayoutDashboard },
      { label: '用户管理', to: '/admin/users', icon: Users },
      { label: '生活方案记录', to: '/admin/life-plans', icon: FileText },
      { label: '生活打卡分析', to: '/admin/checkins', icon: Activity }
    ]
  },
  {
    label: '内容与系统',
    items: [
      { label: '健康资讯管理', to: '/admin/articles', icon: Newspaper },
      { label: '首页内容管理', to: '/admin/home-contents', icon: Home },
      { label: 'AI 咨询配置', to: '/admin/ai-chat', icon: MessageCircle }
    ]
  }
]

function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<style scoped>
.admin-shell {
  height: 100vh;
  display: flex;
  background: #f5f8ff;
  overflow: hidden;
}

.admin-sidebar {
  width: 256px;
  height: 100vh;
  flex: 0 0 256px;
  display: flex;
  flex-direction: column;
  background: #0d1b36;
  color: #a8c8ee;
  overflow-y: auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  color: #fff;
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #5c8ef8, #7eb5ff);
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 14px;
  line-height: 1.2;
}

.brand small {
  margin-top: 4px;
  color: #7eb5ff;
  font-size: 12px;
}

.nav-groups {
  flex: 1;
  padding: 14px 12px;
}

.nav-group {
  margin-bottom: 18px;
}

.nav-group p {
  margin: 0 0 8px;
  padding: 0 10px;
  color: #4a7aaa;
  font-size: 12px;
  font-weight: 700;
}

.nav-link {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 8px;
  color: #a8c8ee;
  transition: 0.18s ease;
}

.nav-link span {
  flex: 1;
  font-size: 14px;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.07);
}

.nav-link.router-link-active {
  color: #fff;
  background: linear-gradient(135deg, #5c8ef8, #4a7bf5);
  box-shadow: 0 4px 14px rgba(92, 142, 248, 0.35);
}

.admin-main {
  flex: 1;
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-topbar {
  height: 62px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid rgba(92, 142, 248, 0.1);
  box-shadow: 0 1px 4px rgba(92, 142, 248, 0.06);
}

.breadcrumb {
  margin: 0 0 3px;
  color: #6b82a4;
  font-size: 12px;
}

.admin-topbar h1 {
  margin: 0;
  color: #1a2e4a;
  font-size: 18px;
}

.topbar-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  width: 180px;
  height: 34px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border-radius: 8px;
  background: #f4f7fe;
  color: #94a3b8;
  font-size: 13px;
}

.icon-btn,
.admin-avatar {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 8px;
}

.icon-btn {
  background: #fff;
  color: #6b82a4;
}

.icon-btn:hover {
  background: #eef3ff;
}

.admin-avatar {
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, #5c8ef8, #4a7bf5);
}

.admin-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
  scroll-behavior: auto;
}
</style>
