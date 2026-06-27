import AdminDashboardView from './views/AdminDashboardView.vue'
import AdminModuleEntryView from './views/AdminModuleEntryView.vue'
import AdminProfilesView from './views/AdminProfilesView.vue'
import AdminProfileDetailView from './views/AdminProfileDetailView.vue'
import AdminHealthMetricsView from './views/AdminHealthMetricsView.vue'
import AdminRiskAssessmentsView from './views/AdminRiskAssessmentsView.vue'
import AdminRiskDetailView from './views/AdminRiskDetailView.vue'

export default [
  { path: '', redirect: 'dashboard' },
  { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboardView, meta: { title: '管理端仪表盘', backend: '/api/health' } },
  { path: 'users', name: 'AdminUsers', component: AdminModuleEntryView, meta: { title: '用户管理', backend: '/api/user/*' } },
  { path: 'profiles', name: 'AdminProfiles', component: AdminProfilesView, meta: { title: '健康档案管理', backend: '/api/profile/*' } },
  { path: 'profiles/:userId', name: 'AdminProfileDetail', component: AdminProfileDetailView, meta: { title: '档案详情', backend: '/api/profile/admin/{user_id}' } },
  { path: 'health-metrics', name: 'AdminHealthMetrics', component: AdminHealthMetricsView, meta: { title: '健康数据管理', backend: '/api/health-metric/*' } },
  { path: 'risk-assessments', name: 'AdminRiskAssessments', component: AdminRiskAssessmentsView, meta: { title: '风险评估管理', backend: '/api/risk/*', ai: 'diabetes_risk_prediction_workflow' } },
  { path: 'risk-assessments/:assessmentId', name: 'AdminRiskDetail', component: AdminRiskDetailView, meta: { title: '评估详情', backend: '/api/risk/admin/{assessment_id}' } },
  { path: 'ai-chat', name: 'AdminAiChat', component: AdminModuleEntryView, meta: { title: 'AI 医生咨询配置', backend: '/api/ai-chat/*', ai: 'diabetes_ai_doctor_agent' } },
  { path: 'life-plans', name: 'AdminLifePlans', component: AdminModuleEntryView, meta: { title: '生活方案管理', backend: '/api/life-plan/*', ai: 'personalized_life_plan_workflow' } },
  { path: 'articles', name: 'AdminArticles', component: AdminModuleEntryView, meta: { title: '健康资讯管理', backend: '/api/content/*' } },
  { path: 'home-contents', name: 'AdminHomeContents', component: AdminModuleEntryView, meta: { title: '首页内容管理', backend: '/api/content/*' } },
  { path: 'checkins', name: 'AdminCheckins', component: AdminModuleEntryView, meta: { title: '打卡记录管理', backend: '/api/checkin/*' } },
  { path: 'checkin-analysis', name: 'AdminCheckinAnalysis', component: AdminModuleEntryView, meta: { title: '打卡行为分析管理', backend: '/api/checkin/analysis/*', ai: 'checkin_behavior_analysis_workflow' } }
]
