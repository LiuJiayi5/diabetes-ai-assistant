import AdminDashboardView from './views/AdminDashboardView.vue'
import AdminModuleEntryView from './views/AdminModuleEntryView.vue'
import AdminUsersView from './views/AdminUsersView.vue'
import AdminUserDetailView from './views/AdminUserDetailView.vue'
import AdminLifePlansView from './views/AdminLifePlansView.vue'
import AdminLifePlanDetailView from './views/AdminLifePlanDetailView.vue'
import AdminLifePlanLogView from './views/AdminLifePlanLogView.vue'
import AdminArticlesView from './views/AdminArticlesView.vue'
import AdminArticleEditView from './views/AdminArticleEditView.vue'
import AdminHomeContentView from './views/AdminHomeContentView.vue'
import AdminBannersView from './views/AdminBannersView.vue'
import AdminExpertsView from './views/AdminExpertsView.vue'
import AdminProfileView from './views/AdminProfileView.vue'

export default [
  { path: '', redirect: '/admin/dashboard' },
  { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboardView, meta: { title: '管理端仪表盘' } },
  { path: 'profile', name: 'AdminProfile', component: AdminProfileView, meta: { title: '管理员个人中心' } },
  { path: 'users', name: 'AdminUsers', component: AdminUsersView, meta: { title: '用户管理' } },
  { path: 'users/:userId', name: 'AdminUserDetail', component: AdminUserDetailView, meta: { title: '用户详情' } },
  { path: 'profiles', name: 'AdminProfiles', component: AdminModuleEntryView, meta: { title: '健康档案管理' } },
  { path: 'health-metrics', name: 'AdminHealthMetrics', component: AdminModuleEntryView, meta: { title: '健康数据管理' } },
  { path: 'risk-assessments', name: 'AdminRiskAssessments', component: AdminModuleEntryView, meta: { title: '风险评估管理' } },
  { path: 'ai-chat', name: 'AdminAiChat', component: AdminModuleEntryView, meta: { title: 'AI 医生咨询配置' } },
  { path: 'life-plans', name: 'AdminLifePlans', component: AdminLifePlansView, meta: { title: '生活方案管理' } },
  { path: 'life-plans/:planId', name: 'AdminLifePlanDetail', component: AdminLifePlanDetailView, meta: { title: '生活方案详情' } },
  { path: 'life-plans/:planId/log', name: 'AdminLifePlanLog', component: AdminLifePlanLogView, meta: { title: '方案生成日志' } },
  { path: 'articles', name: 'AdminArticles', component: AdminArticlesView, meta: { title: '健康资讯管理' } },
  { path: 'articles/create', name: 'AdminArticleCreate', component: AdminArticleEditView, meta: { title: '新增资讯' } },
  { path: 'articles/:articleId/edit', name: 'AdminArticleEdit', component: AdminArticleEditView, meta: { title: '编辑资讯' } },
  { path: 'home-content', name: 'AdminHomeContent', component: AdminHomeContentView, meta: { title: '首页内容管理' } },
  { path: 'home-contents', redirect: '/admin/home-content' },
  { path: 'banners', name: 'AdminBanners', component: AdminBannersView, meta: { title: '轮播图管理' } },
  { path: 'experts', name: 'AdminExperts', component: AdminExpertsView, meta: { title: '专家展示管理' } },
  { path: 'checkins', name: 'AdminCheckins', component: AdminModuleEntryView, meta: { title: '打卡记录管理' } },
  { path: 'checkin-analysis', name: 'AdminCheckinAnalysis', component: AdminModuleEntryView, meta: { title: '打卡行为分析管理' } }
]
