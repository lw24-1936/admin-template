import { createRouter, createWebHistory } from 'vue-router'
import { site } from '@/content/site'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '让团队协作回归简单' } },
  { path: '/features', name: 'features', component: () => import('@/views/FeaturesView.vue'), meta: { title: '功能' } },
  { path: '/pricing', name: 'pricing', component: () => import('@/views/PricingView.vue'), meta: { title: '价格' } },
  { path: '/about', name: 'about', component: () => import('@/views/AboutView.vue'), meta: { title: '关于我们' } },
  { path: '/contact', name: 'contact', component: () => import('@/views/ContactView.vue'), meta: { title: '联系我们' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth', top: 96 }
    return { top: 0 }
  },
})

// 每页独立标题，便于 SEO 与浏览器标签识别
router.afterEach((to) => {
  const title = to.meta.title ? `${to.meta.title} · ${site.name}` : site.name
  document.title = title
})

export default router
