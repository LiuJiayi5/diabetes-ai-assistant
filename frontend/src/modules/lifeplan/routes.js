export default [
  { path: 'life-plan', name: 'LifePlan', component: () => import('./views/LifePlanEntryView.vue'), meta: { title: '个性化生活方案' } },
  { path: 'life-plan/history', name: 'LifePlanHistory', component: () => import('./views/LifePlanHistoryView.vue'), meta: { title: '历史方案' } },
  { path: 'life-plan/:planId', name: 'LifePlanDetail', component: () => import('./views/LifePlanDetailView.vue'), meta: { title: '方案详情' } }
]
