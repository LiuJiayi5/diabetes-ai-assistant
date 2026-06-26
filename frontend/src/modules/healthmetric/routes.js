export default [
  { path: 'health-metric', name: 'HealthMetric', component: () => import('./views/HealthMetricEntryView.vue'), meta: { title: 'Health Metrics' } },
  { path: 'health-metric/history', name: 'HealthMetricHistory', component: () => import('./views/HealthMetricHistoryView.vue'), meta: { title: 'Health Metric History' } }
]
