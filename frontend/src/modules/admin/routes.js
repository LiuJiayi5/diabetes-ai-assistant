import AdminDashboardView from './views/AdminDashboardView.vue'
import AdminUsersView from './views/AdminUsersView.vue'
import AdminUserDetailView from './views/AdminUserDetailView.vue'
import AdminLifePlansView from './views/AdminLifePlansView.vue'
import AdminLifePlanDetailView from './views/AdminLifePlanDetailView.vue'
import AdminLifePlanLogView from './views/AdminLifePlanLogView.vue'
import AdminInterventionReviewsView from './views/AdminInterventionReviewsView.vue'
import AdminArticlesView from './views/AdminArticlesView.vue'
import AdminArticleEditView from './views/AdminArticleEditView.vue'
import AdminHomeContentView from './views/AdminHomeContentView.vue'
import AdminExpertsView from './views/AdminExpertsView.vue'
import AdminProfileView from './views/AdminProfileView.vue'
import AdminProfilesView from './views/AdminProfilesView.vue'
import AdminProfileDetailView from './views/AdminProfileDetailView.vue'
import AdminHealthMetricsView from './views/AdminHealthMetricsView.vue'
import AdminRiskAssessmentsView from './views/AdminRiskAssessmentsView.vue'
import AdminRiskDetailView from './views/AdminRiskDetailView.vue'
import AdminAiChatLogsView from '@/modules/aichat/views/AdminAiChatLogsView.vue'
import AdminCheckinManagementView from '@/modules/checkin/views/AdminCheckinManagementView.vue'
import AdminCheckinLogDetailView from '@/modules/checkin/views/AdminCheckinLogDetailView.vue'

export default [
  { path: '', redirect: '/admin/dashboard' },
  { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboardView, meta: { title: '首页概览' } },
  { path: 'profile', name: 'AdminProfile', component: AdminProfileView, meta: { title: '管理员个人中心' } },
  { path: 'users', name: 'AdminUsers', component: AdminUsersView, meta: { title: '用户管理' } },
  { path: 'users/:userId', name: 'AdminUserDetail', component: AdminUserDetailView, meta: { title: '用户详情' } },
  { path: 'profiles', name: 'AdminProfiles', component: AdminProfilesView, meta: { title: '健康档案管理' } },
  { path: 'profiles/:userId', name: 'AdminProfileDetail', component: AdminProfileDetailView, meta: { title: '档案详情' } },
  { path: 'health-metrics', name: 'AdminHealthMetrics', component: AdminHealthMetricsView, meta: { title: '健康数据管理' } },
  { path: 'risk-assessments', name: 'AdminRiskAssessments', component: AdminRiskAssessmentsView, meta: { title: '风险评估管理' } },
  { path: 'risk-assessments/:assessmentId', name: 'AdminRiskDetail', component: AdminRiskDetailView, meta: { title: '评估详情' } },
  { path: 'ai-chat', name: 'AdminAiChat', component: AdminAiChatLogsView, meta: { title: 'AI 咨询日志' } },
  { path: 'life-plans', name: 'AdminLifePlans', component: AdminLifePlansView, meta: { title: '生活方案管理' } },
  { path: 'intervention-reviews', name: 'AdminInterventionReviews', component: AdminInterventionReviewsView, meta: { title: '自动干预复盘' } },
  { path: 'life-plans/:planId', name: 'AdminLifePlanDetail', component: AdminLifePlanDetailView, meta: { title: '生活方案详情' } },
  { path: 'life-plans/:planId/log', name: 'AdminLifePlanLog', component: AdminLifePlanLogView, meta: { title: '方案生成日志' } },
  { path: 'articles', name: 'AdminArticles', component: AdminArticlesView, meta: { title: '健康资讯管理' } },
  { path: 'articles/create', name: 'AdminArticleCreate', component: AdminArticleEditView, meta: { title: '新增资讯' } },
  { path: 'articles/:articleId/edit', name: 'AdminArticleEdit', component: AdminArticleEditView, meta: { title: '编辑资讯' } },
  { path: 'home-content', name: 'AdminHomeContent', component: AdminHomeContentView, meta: { title: '首页内容管理' } },
  { path: 'home-contents', redirect: '/admin/home-content' },
  { path: 'banners', redirect: '/admin/home-content' },
  { path: 'experts', name: 'AdminExperts', component: AdminExpertsView, meta: { title: '专家展示管理' } },
  { path: 'checkins', name: 'AdminCheckins', component: AdminCheckinManagementView, meta: { title: '打卡记录管理' } },
  { path: 'checkin-analysis', name: 'AdminCheckinAnalysis', component: AdminCheckinManagementView, meta: { title: '打卡行为分析管理' } },
  { path: 'checkin-analysis/logs/:logId', name: 'AdminCheckinLogDetail', component: AdminCheckinLogDetailView, meta: { title: '调用日志详情' } }
]
