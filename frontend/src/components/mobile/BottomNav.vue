<template>
  <nav class="bottom-nav">
    <button
      v-for="item in navItems"
      :key="item.id"
      class="bottom-nav__item"
      :class="{ 'bottom-nav__item--active': isActive(item) }"
      type="button"
      @click="handleClick(item)"
    >
      <component
        :is="item.icon"
        class="bottom-nav__icon"
        :stroke-width="isActive(item) ? 2.5 : 1.8"
      />
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>

<script setup>
import { Home, FileText, Newspaper, Bot, User } from 'lucide-vue-next'
import { showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const navItems = [
  { id: 'home', label: '首页', icon: Home, path: '/app/home', match: ['/app/home'] },
  { id: 'plan', label: '方案定制', icon: FileText, path: '/app/life-plan', match: ['/app/life-plan'] },
  { id: 'news', label: '健康资讯', icon: Newspaper, path: '/app/articles', match: ['/app/articles'] },
  { id: 'ai', label: 'AI助手', icon: Bot, disabled: true },
  { id: 'account', label: '个人中心', icon: User, path: '/app/account', match: ['/app/account', '/app/account/edit'] }
]

function isActive(item) {
  return item.match?.some((prefix) => route.path === prefix || route.path.startsWith(`${prefix}/`))
}

function handleClick(item) {
  if (item.disabled) {
    showToast('该模块后续接入')
    return
  }
  if (item.path && route.path !== item.path) {
    router.push(item.path)
  }
}
</script>

<style scoped>
.bottom-nav {
  flex-shrink: 0;
  height: var(--figma-tabbar-height);
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  background: #FFFFFF;
  border-top: 1px solid var(--figma-border-green-30);
}

.bottom-nav__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--figma-tabbar-inactive);
  background: transparent;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.5;
  transition: color 0.2s ease;
}

.bottom-nav__item--active {
  color: var(--figma-tabbar-active);
}

.bottom-nav__icon {
  width: 20px;
  height: 20px;
}
</style>
