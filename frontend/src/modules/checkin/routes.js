export default [
  { path: 'checkin', name: 'CheckinToday', component: () => import('./views/CheckinEntryView.vue'), meta: { title: '今日打卡' } },
  { path: 'checkin/history', name: 'CheckinHistory', component: () => import('./views/CheckinHistoryView.vue'), meta: { title: '打卡历史' } },
  { path: 'checkin/statistics', name: 'CheckinStatistics', component: () => import('./views/CheckinStatisticsView.vue'), meta: { title: '打卡统计' } },
  { path: 'checkin/analysis', name: 'CheckinAnalysis', component: () => import('./views/CheckinAnalysisView.vue'), meta: { title: '行为分析' } }
]
