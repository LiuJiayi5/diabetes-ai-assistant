<template>
  <section class="ai-chat-page">
    <header class="ai-chat-topbar">
      <button class="icon-button" type="button" aria-label="返回首页" @click="router.push('/app/home')">
        <ArrowLeft :size="18" :stroke-width="2" />
      </button>
      <div class="topbar-copy">
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <button class="icon-button" type="button" aria-label="刷新" @click="$emit('refresh')">
        <RefreshCw :size="17" :stroke-width="2" />
      </button>
    </header>

    <nav class="module-tabs" aria-label="AI 医生模块导航">
      <RouterLink
        v-for="item in tabs"
        :key="item.path"
        class="module-tab"
        :class="{ 'module-tab--active': route.path === item.path }"
        :to="item.path"
      >
        <component :is="item.icon" :size="15" :stroke-width="2" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="ai-chat-scroll mobile-scroll" :class="{ 'ai-chat-scroll--flush': flush }">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Bot, History, MessageCircle, RefreshCw } from 'lucide-vue-next'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  flush: {
    type: Boolean,
    default: false
  }
})

defineEmits(['refresh'])

const route = useRoute()
const router = useRouter()

const tabs = [
  { label: '助手', path: '/app/ai-chat', icon: Bot },
  { label: '咨询', path: '/app/ai-chat/chat', icon: MessageCircle },
  { label: '历史', path: '/app/ai-chat/history', icon: History }
]
</script>

<style scoped>
.ai-chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--figma-bg-page);
  color: var(--figma-text-primary);
}

.ai-chat-topbar {
  flex-shrink: 0;
  min-height: 64px;
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  align-items: center;
  gap: 10px;
  padding: 14px 20px 10px;
  background: var(--figma-bg-page);
}

.icon-button {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--figma-radius-pill);
  background: rgba(174, 232, 199, 0.28);
  color: #4A7A62;
}

.topbar-copy {
  min-width: 0;
  text-align: center;
}

.topbar-copy h1 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.45;
}

.topbar-copy p {
  margin: 1px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-tabs {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 0 16px 12px;
  background: var(--figma-bg-page);
}

.module-tab {
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: var(--figma-radius-pill);
  background: #FFFFFF;
  border: 1px solid rgba(174, 232, 199, 0.35);
  color: var(--figma-text-muted);
  font-size: 12px;
  font-weight: 500;
  box-shadow: var(--figma-shadow-card);
}

.module-tab--active {
  background: var(--figma-secondary-green);
  border-color: rgba(111, 207, 151, 0.45);
  color: var(--figma-tabbar-active);
}

.ai-chat-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 18px;
}

.ai-chat-scroll--flush {
  padding: 0;
}
</style>
