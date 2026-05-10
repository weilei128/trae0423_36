import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'assets',
        name: 'Assets',
        component: () => import('../views/Assets.vue'),
        meta: { title: '资产管理' }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('../views/Category.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'allocation',
        name: 'Allocation',
        component: () => import('../views/Allocation.vue'),
        meta: { title: '资产分配' }
      },
      {
        path: 'transfer',
        name: 'Transfer',
        component: () => import('../views/Transfer.vue'),
        meta: { title: '资产变动' }
      },
      {
        path: 'maintenance',
        name: 'Maintenance',
        component: () => import('../views/Maintenance.vue'),
        meta: { title: '维修管理' }
      },
      {
        path: 'depreciation',
        name: 'Depreciation',
        component: () => import('../views/Depreciation.vue'),
        meta: { title: '折旧管理' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('../views/Report.vue'),
        meta: { title: '报表统计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - 固定资产管理系统' : '固定资产管理系统'
  
  if (to.meta.public) {
    next()
  } else {
    const token = localStorage.getItem('token')
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
