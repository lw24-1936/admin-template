/**
 * 路由配置与全局守卫（完全动态路由版）
 *
 * 设计说明：
 * - 固定路由仅保留：/login、/register、/403、根布局(Root)。
 * - 业务页面完全由后端菜单驱动：
 *   1) 登录后后端按角色下发 menus（树），其叶子节点携带 menuUrl 与 component；
 *   2) component 约定为 src/views 下相对路径（不含扩展名/前导斜杠，
 *      如 userInfo/UserInfo、HomeView），由 import.meta.glob 建立“路径→组件”映射；
 *   3) 守卫在会话加载后调用 addRoute 把“有权限的菜单”挂到 Root(Layout) 下，
 *      再以全路径 404 兜底。
 * - 效果：新增页面 = 放置 Vue 组件 + 在权限树登记菜单（含 component 路径）即可生效，
 *   无需修改前端路由表。
 * - 未登录访问受保护页 -> /login；无权限/不存在的路径 -> 404（含权限提示文案）。
 */
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

/* ------------------------------ 固定路由 ------------------------------ */
const Login = () => import('@/views/login/index.vue')
const Register = () => import('@/views/register/index.vue')
const Layout = () => import('@/layout/Layout.vue')
const Forbidden = () => import('@/views/error/Forbidden.vue')
const ServerError = () => import('@/views/error/ServerError.vue')
const NotFound = () => import('@/views/error/NotFound.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { public: true, title: '登录' } },
    { path: '/register', name: 'Register', component: Register, meta: { public: true, title: '注册' } },
    // 根布局：业务子路由由 ensureDynamicRoutes 动态挂载
    { path: '/', name: 'Root', component: Layout },
    // 无权限页（需要登录）
    { path: '/403', name: 'Forbidden', component: Forbidden, meta: { title: '无权限' } },
    // 渲染异常兜底页（需要登录）
    { path: '/500', name: 'ServerError', component: ServerError, meta: { title: '系统异常' } },
  ],
})

/* --------------------- 组件注册表（views 目录扫描） --------------------- */

/** 将 glob key 转换为 views 相对路径：'../views/userInfo/UserInfo.vue' -> 'userInfo/UserInfo' */
function toRel(key) {
  return key.split('/views/').pop().replace(/\.vue$/, '')
}

/** 扫描 src/views 下全部 .vue，懒加载 */
const viewModules = import.meta.glob('../views/**/*.vue')
const viewMap = {}
for (const [key, loader] of Object.entries(viewModules)) {
  viewMap[toRel(key)] = loader
}

/**
 * 解析后端 component 字段 → 懒加载组件
 * 兼容写法：userInfo/UserInfo、/userInfo/UserInfo、userInfo/UserInfo.vue
 */
function resolveView(componentPath) {
  if (!componentPath) return null
  const rel = String(componentPath).replace(/\\/g, '/').replace(/^\/+/, '').replace(/\.vue$/, '')
  return viewMap[rel] || null
}

/* ------------------------- 动态路由注册/清理 ------------------------- */

/** 是否已注册（会话级） */
let dynamicAdded = false
/** 记录已注册路由的移除函数 */
const removeFns = []

/**
 * 是否为“可注册路由的叶子菜单”：
 * 无子级 + 有路由地址 + 类型为菜单（type=0；历史数据可能缺失 type，按菜单处理）
 */
function isRoutableLeaf(node) {
  if (!node || !node.menuUrl) return false
  if (node.children && node.children.length) return false
  return node.type === undefined || node.type === null || node.type === 0
}

/** 从菜单树收集叶子节点（分组不注册页面，递归子级；叶子注册） */
function flattenLeaves(nodes) {
  const leaves = []
  const walk = (list) => {
    for (const n of list || []) {
      if (n.children && n.children.length) {
        walk(n.children)
      } else if (isRoutableLeaf(n)) {
        leaves.push(n)
      }
    }
  }
  walk(nodes)
  return leaves
}

/** 归一化路由地址（确保以 / 开头） */
function normalizePath(p) {
  if (!p) return '/'
  return p.startsWith('/') ? p : `/${p}`
}

/** 按后端菜单注册业务路由 + 末尾 404 兜底 */
function ensureDynamicRoutes(menus) {
  if (dynamicAdded) return
  const leaves = flattenLeaves(menus)
  for (const menu of leaves) {
    // component 未登记 / 组件文件不存在时回退 404 页，避免整棵路由注册中断
    const component = resolveView(menu.component)
    if (!component) {
      console.error(`[router] 菜单「${menu.name}」的组件路径无法解析：${menu.component}`)
    }
    const remove = router.addRoute('Root', {
      // 以相对路径注册为 Root(Layout) 的子路由（'userInfo' -> /userInfo），
      // 保证匹配链为 [Root, leaf]，页面渲染在 Layout 的 RouterView 内
      path: normalizePath(menu.menuUrl).replace(/^\//, ''),
      name: `dyn-${menu.permissionKey || menu.id}`,
      component: component || NotFound,
      meta: { title: menu.name, perm: menu.permissionKey },
    })
    removeFns.push(remove)
  }
  // 兜底 404（放在动态路由之后注册，保证已注册页面优先匹配）
  // 注意：不能标记 public，否则未登录访问任意地址都会落到 404 而不是登录页
  const removeCatch = router.addRoute({
    path: '/:pathMatch(.*)*',
    name: 'DynamicCatchAll',
    component: NotFound,
    meta: { title: '404' },
  })
  removeFns.push(removeCatch)
  dynamicAdded = true
}

/** 清空全部动态路由（换账号/退出后由守卫按需调用） */
function resetDynamicRoutes() {
  while (removeFns.length) {
    removeFns.pop()()
  }
  dynamicAdded = false
}

/** 取第一个可访问叶子路径（登录后默认首页） */
function firstLeafPath(nodes) {
  const walk = (list) => {
    for (const n of list || []) {
      if (n.children && n.children.length) {
        const found = walk(n.children)
        if (found) return found
      } else if (isRoutableLeaf(n)) {
        return normalizePath(n.menuUrl)
      }
    }
    return null
  }
  return walk(nodes)
}

/* ------------------------------ 全局守卫 ------------------------------ */

/** 记录最近一次动态路由对应的用户，用户变化时重建路由表 */
let lastRouteUserId = null

router.beforeEach(async (to) => {
  const store = useUserStore()
  const isPublic = to.meta.public === true

  // 未登录
  if (!getToken()) {
    if (isPublic) return true
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录访问登录/注册页 -> 回默认首页（守卫内再解析）
  if (to.path === '/login' || to.path === '/register') return '/'

  // 恢复/刷新会话
  if (!store.loaded) {
    try {
      await store.fetchUserInfo()
    } catch {
      store.clearSession()
      return { path: '/login' }
    }
  }

  // 用户变化（重新登录/切换账号）时重建动态路由
  // 注意：仅在拿到有效 userId 时记录，否则会把“未知用户”当成当前用户，
  // 导致下一个账号直接复用上一账号的动态路由表
  const userId = store.user?.userId
  if (userId !== undefined && userId !== null && lastRouteUserId !== userId) {
    resetDynamicRoutes()
    lastRouteUserId = userId
  }
  ensureDynamicRoutes(store.menus)

  // 根路径 -> 首个有权限菜单；无任何菜单 -> 无权限页
  if (to.path === '/') {
    const first = firstLeafPath(store.menus)
    return first ? first : '/403'
  }

  // 首次导航到动态路由时，路由表刚完成注册，重新导航一次以正确匹配。
  // 注意：必须把 path / query / hash 分开传递；此前的写法 `path: to.fullPath`
  // 会把 "?a=1" 当成路径的一部分，query 在这一次重放中丢失，
  // 表现为"带筛选参数的深链打开后条件没了"（列表页 URL 同步时发现）。
  if (to.matched.length === 0) {
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  }

  // 页面级权限兜底（动态注册的页面均已带权限，此处兼容手动跳转）
  const perm = to.meta.perm
  if (perm && !store.hasPerm(perm)) {
    return { path: '/403' }
  }

  return true
})

// 更新浏览器标签标题
router.afterEach((to) => {
  const title = to.meta.title || ''
  document.title = title ? `${title} - admintem 权限管理系统` : 'admintem 权限管理系统'
})

export { resetDynamicRoutes }
export default router
