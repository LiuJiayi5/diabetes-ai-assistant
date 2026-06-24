<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">首页概览</h1>
        <p class="admin-page-desc">查看系统用户、生活方案和健康资讯管理概览。</p>
      </div>
    </div>

    <div class="admin-stat-grid">
      <section v-for="stat in stats" :key="stat.label" class="admin-card admin-stat-card">
        <span class="admin-stat-icon" :style="{ background: stat.bg, color: stat.color }">
          <component :is="stat.icon" :size="18" />
        </span>
        <div class="admin-stat-value">{{ stat.value }}</div>
        <div class="admin-stat-label">{{ stat.label }}</div>
      </section>
    </div>

    <section class="quick-grid">
      <button v-for="link in links" :key="link.path" class="admin-card quick-card" @click="router.push(link.path)">
        <span :style="{ background: link.gradient }"><component :is="link.icon" :size="22" /></span>
        <strong>{{ link.title }}</strong>
        <p>{{ link.desc }}</p>
        <small>立即进入</small>
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, CheckCircle, FileText, Home, Newspaper, TrendingUp, Users, XCircle } from 'lucide-vue-next'
import { adminMockArticles, adminMockLifePlans, adminMockUsers } from '@/modules/admin/mockData'

const router = useRouter()

const stats = computed(() => [
  { label: '患者用户总数', value: adminMockUsers.filter((u) => u.role === 'patient').length, icon: Users, bg: 'rgba(92,142,248,0.12)', color: '#5C8EF8' },
  { label: '正常账号', value: adminMockUsers.filter((u) => u.status === 'active').length, icon: CheckCircle, bg: 'rgba(52,211,153,0.12)', color: '#34D399' },
  { label: '生成成功方案', value: adminMockLifePlans.filter((p) => p.call_status === 'success').length, icon: FileText, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
  { label: '已上架资讯', value: adminMockArticles.filter((a) => a.status === 'published').length, icon: BookOpen, bg: 'rgba(245,158,11,0.10)', color: '#F59E0B' }
])

const links = [
  { title: '用户管理', desc: '检索、查看和维护患者用户账号', path: '/admin/users', icon: Users, gradient: 'linear-gradient(135deg,#5C8EF8,#7EB5FF)' },
  { title: '生活方案记录', desc: '查看方案生成记录、状态和失败原因', path: '/admin/life-plans', icon: TrendingUp, gradient: 'linear-gradient(135deg,#34D399,#6EE7C0)' },
  { title: '健康资讯管理', desc: '发布和维护糖尿病科普内容', path: '/admin/articles', icon: Newspaper, gradient: 'linear-gradient(135deg,#2563EB,#38BDF8)' },
  { title: '首页内容管理', desc: '配置轮播图和 AI 医师展示卡片', path: '/admin/home-content', icon: Home, gradient: 'linear-gradient(135deg,#FBBF24,#FDE68A)' }
]
</script>

<style scoped>
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.quick-card {
  display: grid;
  justify-items: start;
  gap: 10px;
  padding: 20px;
  text-align: left;
}

.quick-card > span {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #fff;
}

.quick-card strong {
  color: var(--admin-text-title);
  font-size: 15px;
}

.quick-card p {
  min-height: 40px;
  margin: 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.quick-card small {
  color: var(--admin-primary);
  font-size: 13px;
  font-weight: 600;
}
</style>
