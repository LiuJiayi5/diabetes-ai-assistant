import BlankLayout from '@/layouts/BlankLayout.vue'
import PatientLayout from '@/layouts/PatientLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

import authRoutes, { accountRoutes } from '@/modules/auth/routes'
import profileRoutes from '@/modules/profile/routes'
import healthMetricRoutes from '@/modules/healthmetric/routes'
import riskRoutes from '@/modules/risk/routes'
import aiChatRoutes from '@/modules/aichat/routes'
import lifePlanRoutes from '@/modules/lifeplan/routes'
import articleRoutes from '@/modules/article/routes'
import checkinRoutes from '@/modules/checkin/routes'
import reportRoutes from '@/modules/report/routes'
import adminRoutes from '@/modules/admin/routes'

export const routes = [
  { path: '/', redirect: '/welcome' },
  { path: '/', component: BlankLayout, children: authRoutes },
  {
    path: '/app',
    component: PatientLayout,
    children: [
      ...articleRoutes,
      ...accountRoutes,
      ...profileRoutes,
      ...healthMetricRoutes,
      ...riskRoutes,
      ...aiChatRoutes,
      ...lifePlanRoutes,
      ...checkinRoutes,
      ...reportRoutes
    ]
  },
  { path: '/admin', component: AdminLayout, children: adminRoutes },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/modules/common/views/NotFoundView.vue') }
]
