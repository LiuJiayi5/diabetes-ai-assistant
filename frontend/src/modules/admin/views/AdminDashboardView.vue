<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">首页概览</h1>
        <p class="admin-page-desc">查看用户增长、生活方案、健康资讯和首页内容的运营概览。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="loadDashboard">
        刷新数据
      </el-button>
    </div>

    <div class="admin-stat-grid dashboard-stat-grid">
      <section v-for="stat in stats" :key="stat.label" class="admin-card admin-stat-card">
        <span class="admin-stat-icon" :style="{ background: stat.bg, color: stat.color }">
          <component :is="stat.icon" :size="18" />
        </span>
        <div class="admin-stat-value">{{ stat.value }}</div>
        <div class="admin-stat-label">{{ stat.label }}</div>
      </section>
    </div>

    <div class="dashboard-grid">
      <section class="admin-card dashboard-card">
        <div class="admin-card-title-row">
          <span class="admin-section-title">用户与方案趋势</span>
          <span class="admin-count-pill">最近 7 天</span>
        </div>
        <div class="trend-chart">
          <div v-for="item in trendData" :key="item.date" class="trend-day">
            <div class="trend-bars">
              <span class="trend-bar user" :style="{ height: `${barHeight(item.users, maxTrend)}%` }"></span>
              <span class="trend-bar plan" :style="{ height: `${barHeight(item.plans, maxTrend)}%` }"></span>
            </div>
            <small>{{ item.label }}</small>
          </div>
        </div>
        <div class="chart-legend">
          <span><i class="legend-dot user"></i>新增用户</span>
          <span><i class="legend-dot plan"></i>方案生成</span>
        </div>
      </section>

      <section class="admin-card dashboard-card">
        <div class="admin-card-title-row">
          <span class="admin-section-title">方案调用状态</span>
          <span class="admin-count-pill">{{ lifePlans.length }} 条</span>
        </div>
        <div class="status-ring" :style="{ background: statusRingGradient }">
          <div>
            <strong>{{ lifePlans.length }}</strong>
            <span>方案记录</span>
          </div>
        </div>
        <div class="status-list">
          <span><i class="status-dot success"></i>成功 {{ callStatus.success }}</span>
          <span><i class="status-dot running"></i>生成中 {{ callStatus.running }}</span>
          <span><i class="status-dot failed"></i>失败 {{ callStatus.failed }}</span>
        </div>
      </section>

      <section class="admin-card dashboard-card">
        <div class="admin-card-title-row">
          <span class="admin-section-title">资讯分类分布</span>
          <span class="admin-count-pill">{{ articles.length }} 篇</span>
        </div>
        <div class="category-bars">
          <div v-for="item in categoryStats" :key="item.label" class="category-bar-row">
            <span>{{ item.label }}</span>
            <div class="category-track">
              <i :style="{ width: `${barHeight(item.value, maxCategory)}%` }"></i>
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <section class="admin-card dashboard-card">
        <div class="admin-card-title-row">
          <span class="admin-section-title">最近记录</span>
          <span class="admin-count-pill">运营动态</span>
        </div>
        <div class="recent-list" v-if="recentItems.length">
          <button v-for="item in recentItems" :key="item.key" type="button" @click="router.push(item.path)">
            <span :class="['recent-icon', item.type]"><component :is="item.icon" :size="16" /></span>
            <span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
          </button>
        </div>
        <div v-else class="admin-empty compact-empty">暂无运营记录</div>
      </section>
    </div>

    <section class="quick-grid">
      <button v-for="link in links" :key="link.path" class="admin-card quick-card" @click="router.push(link.path)">
        <span :style="{ background: link.gradient }"><component :is="link.icon" :size="22" /></span>
        <strong>{{ link.title }}</strong>
        <p>{{ link.desc }}</p>
        <small>进入管理</small>
      </button>
    </section>

    <div v-if="error" class="admin-tip dashboard-error">
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOpen,
  CheckCircle,
  FileText,
  Home,
  Newspaper,
  TrendingUp,
  Users,
  XCircle
} from 'lucide-vue-next'
import { adminGetContentManagement, adminListLifePlans, adminListUsers } from '@/api/admin'
import { articleCategories } from '@/modules/admin/constants'
import { resolveAdminError, unwrapPage } from '@/modules/admin/utils'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const users = ref([])
const lifePlans = ref([])
const articles = ref([])
const homeContents = ref([])

const stats = computed(() => [
  { label: '用户总数', value: users.value.length, icon: Users, bg: 'rgba(92,142,248,0.12)', color: '#5C8EF8' },
  { label: '今日新增用户', value: countToday(users.value, 'create_time'), icon: TrendingUp, bg: 'rgba(52,211,153,0.12)', color: '#34D399' },
  { label: '生活方案记录', value: lifePlans.value.length, icon: FileText, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
  { label: '方案生成成功', value: callStatus.value.success, icon: CheckCircle, bg: 'rgba(34,197,94,0.10)', color: '#22C55E' },
  { label: '生成失败记录', value: callStatus.value.failed, icon: XCircle, bg: 'rgba(239,68,68,0.10)', color: '#EF4444' },
  { label: '健康资讯文章', value: articles.value.length, icon: BookOpen, bg: 'rgba(245,158,11,0.10)', color: '#F59E0B' },
  { label: '首页展示内容', value: homeContents.value.length, icon: Home, bg: 'rgba(99,102,241,0.10)', color: '#6366F1' }
])


const callStatus = computed(() => ({
  success: lifePlans.value.filter((item) => item.call_status === 'success').length,
  running: lifePlans.value.filter((item) => item.call_status === 'running').length,
  failed: lifePlans.value.filter((item) => item.call_status === 'failed').length
}))

const trendData = computed(() => {
  const days = lastDays(7)
  return days.map((day) => ({
    ...day,
    users: users.value.filter((item) => sameDate(item.create_time, day.date)).length,
    plans: lifePlans.value.filter((item) => sameDate(item.create_time, day.date)).length
  }))
})

const maxTrend = computed(() => Math.max(1, ...trendData.value.flatMap((item) => [item.users, item.plans])))

const categoryStats = computed(() => articleCategories.map((category) => ({
  label: category.label,
  value: articles.value.filter((article) => article.category === category.value).length
})))

const maxCategory = computed(() => Math.max(1, ...categoryStats.value.map((item) => item.value)))

const statusRingGradient = computed(() => {
  const total = Math.max(1, lifePlans.value.length)
  const success = Math.round((callStatus.value.success / total) * 360)
  const running = Math.round((callStatus.value.running / total) * 360)
  return `conic-gradient(#22C55E 0deg ${success}deg, #5C8EF8 ${success}deg ${success + running}deg, #EF4444 ${success + running}deg 360deg)`
})

const recentItems = computed(() => {
  const articleItems = articles.value.slice(0, 3).map((item) => ({
    key: `article-${item.article_id}`,
    type: 'article',
    icon: Newspaper,
    title: item.title || '未命名资讯',
    desc: `资讯更新 · ${item.update_time || item.create_time || '暂无时间'}`,
    path: item.article_id ? `/admin/articles/${item.article_id}/edit` : '/admin/articles'
  }))
  const planItems = lifePlans.value.slice(0, 3).map((item) => ({
    key: `plan-${item.plan_id}`,
    type: item.call_status === 'failed' ? 'failed' : 'plan',
    icon: FileText,
    title: item.plan_title || '生活方案记录',
    desc: `用户 ${item.username || item.user_id || '未命名'} · ${item.create_time || '暂无时间'}`,
    path: item.plan_id ? `/admin/life-plans/${item.plan_id}` : '/admin/life-plans'
  }))
  return [...articleItems, ...planItems]
    .sort((a, b) => String(b.desc).localeCompare(String(a.desc)))
    .slice(0, 6)
})

const links = [
  { title: '用户管理', desc: '检索、查看和维护患者用户账号', path: '/admin/users', icon: Users, gradient: 'linear-gradient(135deg,#5C8EF8,#7EB5FF)' },
  { title: '生活方案记录', desc: '查看方案生成记录、状态和失败原因', path: '/admin/life-plans', icon: TrendingUp, gradient: 'linear-gradient(135deg,#34D399,#6EE7C0)' },
  { title: '健康资讯管理', desc: '发布和维护糖尿病科普内容', path: '/admin/articles', icon: Newspaper, gradient: 'linear-gradient(135deg,#2563EB,#38BDF8)' },
  { title: '首页内容管理', desc: '配置轮播图和 AI 医师展示卡片', path: '/admin/home-content', icon: Home, gradient: 'linear-gradient(135deg,#F59E0B,#FCD34D)' }
]

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [userResponse, planResponse, contentResponse] = await Promise.all([
      adminListUsers({ page: 1, page_size: 100 }),
      adminListLifePlans({ page: 1, page_size: 100 }),
      adminGetContentManagement({ page: 1, page_size: 100 })
    ])
    users.value = unwrapPage(userResponse).list
    lifePlans.value = unwrapPage(planResponse).list
    articles.value = unwrapPage(contentResponse, 'articles').list
    homeContents.value = contentResponse?.home_contents || contentResponse?.data?.home_contents || []
  } catch (err) {
    error.value = resolveAdminError(err, '首页概览数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function countToday(list, field) {
  const today = new Date()
  return list.filter((item) => sameDate(item[field], today)).length
}

function sameDate(value, date) {
  if (!value) return false
  const parsed = new Date(String(value).replace(/-/g, '/'))
  return parsed.getFullYear() === date.getFullYear()
    && parsed.getMonth() === date.getMonth()
    && parsed.getDate() === date.getDate()
}

function lastDays(count) {
  return Array.from({ length: count }).map((_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (count - 1 - index))
    return {
      date,
      label: `${date.getMonth() + 1}/${date.getDate()}`
    }
  })
}

function barHeight(value, max) {
  if (!value) return 8
  return Math.max(12, Math.round((value / Math.max(1, max)) * 100))
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.9fr);
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-card {
  padding: 18px;
  min-height: 260px;
}

.trend-chart {
  height: 170px;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
  padding: 12px 6px 0;
}

.trend-day {
  min-width: 0;
  display: grid;
  gap: 8px;
  justify-items: center;
}

.trend-bars {
  height: 132px;
  display: flex;
  align-items: end;
  gap: 5px;
}

.trend-bar {
  width: 12px;
  min-height: 8px;
  border-radius: 8px 8px 2px 2px;
}

.trend-bar.user,
.legend-dot.user {
  background: #5C8EF8;
}

.trend-bar.plan,
.legend-dot.plan {
  background: #34D399;
}

.trend-day small {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.chart-legend,
.status-list {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--admin-text-secondary);
  font-size: 13px;
}

.legend-dot,
.status-dot {
  width: 8px;
  height: 8px;
  display: inline-block;
  border-radius: 999px;
  margin-right: 6px;
}

.status-ring {
  width: 156px;
  height: 156px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  margin: 20px auto;
}

.status-ring > div {
  width: 108px;
  height: 108px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 999px;
  background: #fff;
  box-shadow: inset 0 0 0 1px var(--admin-border);
}

.status-ring strong {
  color: var(--admin-text-title);
  font-size: 28px;
}

.status-ring span {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.status-dot.success { background: #22C55E; }
.status-dot.running { background: #5C8EF8; }
.status-dot.failed { background: #EF4444; }

.category-bars {
  display: grid;
  gap: 14px;
  padding-top: 8px;
}

.category-bar-row {
  display: grid;
  grid-template-columns: 92px 1fr 36px;
  align-items: center;
  gap: 10px;
  color: var(--admin-text-secondary);
  font-size: 13px;
}

.category-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf2fb;
}

.category-track i {
  height: 100%;
  display: block;
  border-radius: inherit;
  background: linear-gradient(135deg,#5C8EF8,#34D399);
}

.category-bar-row strong {
  color: var(--admin-text-title);
  text-align: right;
}

.recent-list {
  display: grid;
  gap: 10px;
}

.recent-list button {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 10px;
  background: #f8faff;
  text-align: left;
}

.recent-list button:hover {
  border-color: rgba(92,142,248,0.35);
  background: #eef3ff;
}

.recent-icon {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 10px;
  color: #fff;
  background: #5C8EF8;
}

.recent-icon.article { background: #0EA5E9; }
.recent-icon.plan { background: #22C55E; }
.recent-icon.failed { background: #EF4444; }

.recent-list strong,
.recent-list small {
  display: block;
}

.recent-list strong {
  color: var(--admin-text-title);
  font-size: 13px;
}

.recent-list small {
  color: var(--admin-text-muted);
  font-size: 12px;
  margin-top: 2px;
}

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

.compact-empty {
  min-height: 170px;
}

.dashboard-error {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .dashboard-stat-grid,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
