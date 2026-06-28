export default [
  { path: 'reports', name: 'HealthReports', component: () => import('./views/ReportEntryView.vue'), meta: { title: '健康报告' } },
  { path: 'reports/:reportId', name: 'HealthReportDetail', component: () => import('./views/ReportDetailView.vue'), meta: { title: '报告详情' } }
]
