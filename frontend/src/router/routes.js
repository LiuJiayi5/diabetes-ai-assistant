import BlankLayout from '@/layouts/BlankLayout.vue'
import PatientLayout from '@/layouts/PatientLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

import authRoutes from '@/modules/auth/routes'
import profileRoutes from '@/modules/profile/routes'
import healthMetricRoutes from '@/modules/healthmetric/routes'
import riskRoutes from '@/modules/risk/routes'
import aiChatRoutes from '@/modules/aichat/routes'
import lifePlanRoutes from '@/modules/lifeplan/routes'
import articleRoutes from '@/modules/article/routes'
import checkinRoutes from '@/modules/checkin/routes'
import adminRoutes from '@/modules/admin/routes'

export const routes = [
  { path: '/', redirect: '/app/home' },
  { path: '/', component: BlankLayout, children: authRoutes },
  {
    path: '/app',
    component: PatientLayout,
    children: [
      ...articleRoutes,
      ...profileRoutes,
      ...healthMetricRoutes,
      ...riskRoutes,
      ...aiChatRoutes,
      ...lifePlanRoutes,
      ...checkinRoutes
    ]
  },
  { path: '/admin', component: AdminLayout, children: adminRoutes },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/modules/common/views/NotFoundView.vue') }
]
