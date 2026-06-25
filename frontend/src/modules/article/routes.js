export default [
  { path: 'home', name: 'PatientHome', component: () => import('./views/HomeEntryView.vue'), meta: { title: '首页内容' } },
  { path: 'articles', name: 'Articles', component: () => import('./views/ArticleEntryView.vue'), meta: { title: '健康资讯' } },
  { path: 'articles/all', name: 'ArticleList', component: () => import('./views/ArticleListView.vue'), meta: { title: '全部资讯' } },
  { path: 'articles/category/:category', name: 'ArticleCategory', component: () => import('./views/ArticleCategoryView.vue'), meta: { title: '分类资讯' } },
  { path: 'articles/:articleId', name: 'ArticleDetail', component: () => import('./views/ArticleDetailView.vue'), meta: { title: '资讯详情' } }
]
