import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '监控总览', component: () => import('@/views/Dashboard.vue') },
      { path: 'customers', name: '客户与租户', component: () => import('@/views/CustomerList.vue') },
      { path: 'rules', name: '检查规则', component: () => import('@/views/RuleList.vue') },
      { path: 'rules/edit', name: '规则编辑', component: () => import('@/views/RuleEdit.vue') },
      { path: 'templates', name: '规则模板库', component: () => import('@/views/RuleTemplates.vue') },
      { path: 'template-diff', name: '模板差异同步', component: () => import('@/views/TemplateDiff.vue') },
      { path: 'datasources', name: '数据源', component: () => import('@/views/DataSourceList.vue') },
      { path: 'tasks', name: '检测任务', component: () => import('@/views/TaskList.vue') },
      { path: 'runs', name: '检测结果', component: () => import('@/views/RunList.vue') },
      { path: 'runs/:id', name: '运行详情', component: () => import('@/views/RunDetail.vue') },
      { path: 'analytics', name: '趋势与统计', component: () => import('@/views/Analytics.vue') },
      { path: 'rollouts', name: '灰度发布', component: () => import('@/views/RolloutList.vue') },
      { path: 'alerts', name: '告警中心', component: () => import('@/views/AlertCenter.vue') },
      { path: 'wecom', name: '企业微信通知', component: () => import('@/views/WecomSettings.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
