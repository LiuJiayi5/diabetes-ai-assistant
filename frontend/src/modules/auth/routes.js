export default [
  { path: '/login', name: 'Login', component: () => import('./views/LoginView.vue'), meta: { title: '患者登录' } },
  { path: '/register', name: 'Register', component: () => import('./views/RegisterView.vue'), meta: { title: '患者注册' } },
  { path: '/admin/login', name: 'AdminLogin', component: () => import('./views/LoginView.vue'), meta: { title: '管理员登录', role: 'admin' } }
]
