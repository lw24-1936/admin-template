/**
 * 移动端路由
 * - 采用 hash 模式：静态托管或直接用手机打开时无需服务端重写配置
 * - 登录后进入带底部标签栏的布局（首页 / 我的）
 * - 全局前置守卫：未登录跳登录页，已登录但会话未加载时先拉取会话
 */
import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const routes = [
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/register/index.vue'),
      meta: { public: true, title: '注册' },
    },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    // 主布局：底部标签栏 + 子页面
    path: '/',
    component: () => import('@/views/layout/TabLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'mine',
        name: 'mine',
        component: () => import('@/views/mine/index.vue'),
        meta: { title: '我的' },
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理' },
      },
    ],
  },
  // 兜底：未知地址回到首页
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  /*
   * 登录态的唯一判定来源是 localStorage 中的令牌：
   * 接口返回 401 时 axios 拦截器只会清除本地令牌，内存中的 store 可能仍留有旧会话，
   * 若此处改用 store.token 判断，就会出现「跳登录页 -> 又被弹回首页」的重定向回环。
   */
  const token = getToken()

  // 公开页面：已登录时直接进入首页
  if (to.meta.public) {
    return token ? { path: '/home' } : true
  }

  // 未登录：跳转登录页并记录来源（同时清掉可能残留的内存会话）
  if (!token) {
    if (userStore.loaded) {
      userStore.clearSession()
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录但会话未加载（首次进入或刷新）：补拉一次会话，失败即视为登录态失效
  if (!userStore.loaded) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      userStore.clearSession()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · admintem 移动端` : 'admintem 移动端'
})

export default router
