import AdminDashboardView from './views/AdminDashboardView.vue'
import AdminModuleEntryView from './views/AdminModuleEntryView.vue'
import AdminCheckinManagementView from '@/modules/checkin/views/AdminCheckinManagementView.vue'
import AdminAiChatLogsView from '@/modules/aichat/views/AdminAiChatLogsView.vue'

export default [
  { path: '', redirect: '/admin/dashboard' },
  { path: 'dashboard', name: 'AdminDashboard', component: AdminDashboardView, meta: { title: '管理端仪表盘', backend: '/api/health' } },
  { path: 'users', name: 'AdminUsers', component: AdminModuleEntryView, meta: { title: '用户管理', backend: '/api/user/*' } },
  { path: 'profiles', name: 'AdminProfiles', component: AdminModuleEntryView, meta: { title: '健康档案管理', backend: '/api/profile/*' } },
  { path: 'health-metrics', name: 'AdminHealthMetrics', component: AdminModuleEntryView, meta: { title: '健康数据管理', backend: '/api/health-metric/*' } },
  { path: 'risk-assessments', name: 'AdminRiskAssessments', component: AdminModuleEntryView, meta: { title: '风险评估管理', backend: '/api/risk/*', ai: 'diabetes_risk_prediction_workflow' } },
  { path: 'ai-chat', name: 'AdminAiChat', component: AdminAiChatLogsView, meta: { title: 'AI 医生咨询日志', backend: '/api/admin/ai-chat/*', ai: 'diabetes_ai_doctor_agent' } },
  { path: 'life-plans', name: 'AdminLifePlans', component: AdminModuleEntryView, meta: { title: '生活方案管理', backend: '/api/life-plan/*', ai: 'personalized_life_plan_workflow' } },
  { path: 'articles', name: 'AdminArticles', component: AdminModuleEntryView, meta: { title: '健康资讯管理', backend: '/api/content/*' } },
  { path: 'home-contents', name: 'AdminHomeContents', component: AdminModuleEntryView, meta: { title: '首页内容管理', backend: '/api/content/*' } },
  { path: 'checkins', name: 'AdminCheckins', component: AdminCheckinManagementView, meta: { title: '生活打卡分析管理', backend: '/api/admin/checkins/*' } },
  { path: 'checkin-analysis', name: 'AdminCheckinAnalysis', component: AdminCheckinManagementView, meta: { title: '打卡行为分析管理', backend: '/api/admin/checkins/analyses', ai: 'checkin_behavior_analysis_workflow' } }
]
