import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/GoodsList.vue'), meta: { title: '秒杀商品' } },
  { path: '/goods/:id', name: 'GoodsDetail', component: () => import('../views/GoodsDetail.vue'), meta: { title: '商品详情' } },
  { path: '/orders', name: 'MyOrders', component: () => import('../views/MyOrders.vue'), meta: { title: '我的订单' } },
  { path: '/metrics', name: 'Metrics', component: () => import('../views/Metrics.vue'), meta: { title: '秒杀指标' } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '双喜登录' } },
  { path: '/admin/goods', name: 'AdminGoods', component: () => import('../views/AdminGoods.vue'), meta: { title: '商品管理', requireAuth: true } },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  // 修改网页 Title
  document.title = to.meta.title ? `${to.meta.title} - 双喜秒杀` : '双喜秒杀'
  
  if (to.meta.requireAuth && !localStorage.getItem('adminToken')) {
    next({ name: 'Login', query: { role: 'admin', redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router