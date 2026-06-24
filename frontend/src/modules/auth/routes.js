export default [
  { path: '/welcome', name: 'Welcome', component: () => import('@/modules/common/views/PlaceholderView.vue'), meta: { title: '欢迎使用' } },
  { path: '/login', name: 'Login', component: () => import('./views/LoginView.vue'), meta: { title: '患者登录' } },
  { path: '/register', name: 'Register', component: () => import('./views/RegisterView.vue'), meta: { title: '患者注册' } },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('./views/LoginView.vue'), meta: { title: '管理员登录', role: 'admin' } }
]

export const accountRoutes = [
  { path: 'account', name: 'AccountCenter', component: () => import('@/modules/common/views/PlaceholderView.vue'), meta: { title: '个人中心' } },
  { path: 'account/edit', name: 'EditAccount', component: () => import('@/modules/common/views/PlaceholderView.vue'), meta: { title: '编辑账号信息' } }
]
