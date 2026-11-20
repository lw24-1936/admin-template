/**
 * 当前用户会话（轻量全局状态）
 *
 * 为什么不用 Pinia：本项目只需要"一个用户 + 一组派生值"，
 * 用 Vue 的 reactive 单例即可，少一个依赖、小程序包体更小。
 * 权限判断与桌面端同源：后端下发 permissions（菜单 + 按钮的 permission_key）。
 */
import { reactive, computed } from 'vue'
import { getCachedUser, setCachedUser, clearSession, setToken, setRefreshToken } from '@/utils/storage'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

const state = reactive({
  user: getCachedUser(),
  loading: false,
})

/** 用户信息（可能为 null） */
export const user = computed(() => state.user)

/** 是否登录（有会话即可；令牌是否存在由请求层兜底） */
export const isLoggedIn = computed(() => !!state.user)

/** 角色名串：优先 roleName，回退 roleKey */
export const roleText = computed(() => {
  const roles = (state.user && state.user.roles) || []
  const names = roles.map((r) => r.roleName || r.roleKey).filter(Boolean)
  return names.length ? names.join('、') : '普通用户'
})

/** 头像文字（无头像时显示昵称/账号首字） */
export const avatarText = computed(() => {
  const u = state.user || {}
  const base = u.nickName || u.userName || 'A'
  return String(base).slice(0, 1).toUpperCase()
})

/** 权限判断：与桌面端 v-perm 使用同一份 permission_key */
export function hasPerm(key) {
  if (!key) return true
  const list = (state.user && state.user.permissions) || []
  return list.includes(key)
}

/** 菜单树拍平（只保留能跳转的叶子；本项目小程序端仅展示，不承载桌面端页面） */
export function flattenMenus() {
  const out = []
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (node.children && node.children.length) {
        walk(node.children)
        continue
      }
      out.push({ id: node.id, name: node.name, url: node.menuUrl || node.path || '' })
    }
  }
  walk((state.user && state.user.menus) || [])
  return out
}

export function setUser(nextUser) {
  state.user = nextUser || null
  setCachedUser(state.user)
}

/** 登录：保存令牌与会话 */
export async function login(payload) {
  const res = await loginApi(payload)
  const data = (res && res.data) || {}
  setToken(data.token)
    setRefreshToken(data.refreshToken)
  setUser(data.user)
  return data
}

/** 刷新会话：登录后/下拉刷新/资料修改后调用 */
export async function refreshUser() {
  state.loading = true
  try {
    const res = await getUserInfo()
    setUser((res && res.data) || null)
    return state.user
  } finally {
    state.loading = false
  }
}

/** 退出：先通知服务端吊销令牌，失败也要清本地（网络差时不能把用户困住） */
export async function logout() {
  try {
    await logoutApi()
  } catch (e) {
    // 忽略：本地登录态必须清掉
  }
  clearSession()
  state.user = null
}

/** 仅清本地（修改密码后强制重新登录） */
export function clearLocal() {
  clearSession()
  state.user = null
}
