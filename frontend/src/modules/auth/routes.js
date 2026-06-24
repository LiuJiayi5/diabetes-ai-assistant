export default [
  { path: '/welcome', name: 'Welcome', component: () => import('./views/WelcomeView.vue'), meta: { title: '欢迎使用' } },
  { path: '/login', name: 'Login', component: () => import('./views/LoginView.vue'), meta: { title: '患者登录' } },
  { path: '/register', name: 'Register', component: () => import('./views/RegisterView.vue'), meta: { title: '患者注册' } },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('@/modules/admin/views/AdminLoginView.vue'), meta: { title: '管理员登录', role: 'admin' } }
]

export const accountRoutes = [
  { path: 'account', name: 'AccountCenter', component: () => import('./views/AccountCenterView.vue'), meta: { title: '个人中心' } },
  { path: 'account/edit', name: 'EditAccount', component: () => import('./views/EditAccountView.vue'), meta: { title: '编辑账号信息' } },
  { path: 'account/privacy', name: 'AccountPrivacy', component: () => import('./views/PrivacyView.vue'), meta: { title: '隐私说明' } },
  { path: 'account/about', name: 'AccountAbout', component: () => import('./views/AboutView.vue'), meta: { title: '关于系统' } }
]
