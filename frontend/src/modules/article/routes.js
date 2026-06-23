export default [
  { path: 'home', name: 'PatientHome', component: () => import('./views/HomeEntryView.vue'), meta: { title: '首页内容' } },
  { path: 'articles', name: 'Articles', component: () => import('./views/ArticleEntryView.vue'), meta: { title: '健康资讯' } }
]
