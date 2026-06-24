export default [
  { path: 'risk', name: 'RiskAssessment', component: () => import('./views/RiskEntryView.vue'), meta: { title: '糖尿病风险预测' } },
  { path: 'risk/history', name: 'RiskHistory', component: () => import('./views/RiskHistoryView.vue'), meta: { title: '评估记录' } },
  { path: 'risk/:assessmentId', name: 'RiskDetail', component: () => import('./views/RiskDetailView.vue'), meta: { title: '评估详情' } }
]
