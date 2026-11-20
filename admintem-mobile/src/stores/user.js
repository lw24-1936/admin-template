/**
 * 用户状态管理（Pinia，移动端）
 * 与桌面端同源：登录后一次性拉齐用户、角色、菜单、权限标识
 */
import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, setRefreshToken, removeRefreshToken } from '@/utils/auth'
import * as authApi from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    /** 用户基础信息 */
    user: null,
    /** 角色列表 */
    roles: [],
    /** 可访问菜单树（后端已按权限过滤） */
    menus: [],
    /** 全部权限标识（菜单 + 按钮） */
    permissions: [],
    /** 会话是否已加载 */
    loaded: false,
  }),

  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.token,
    /** 昵称优先，其次账号 */
    displayName: (state) =>
      (state.user && (state.user.nickName || state.user.userName)) || '未登录',
    /** 头像占位字符：取昵称/账号首字 */
    avatarText: (state) => {
      const name =
        (state.user && (state.user.nickName || state.user.userName)) || '?'
      return String(name).trim().charAt(0).toUpperCase()
    },
    /** 角色名称数组（后端 RoleVO：{ id, roleName, roleKey }） */
    roleNames: (state) => (state.roles || []).map((role) => role.roleName || role.roleKey).filter(Boolean),
    /** 角色展示串 */
    roleText: (state) => {
      const names = (state.roles || [])
        .map((role) => role.roleName || role.roleKey)
        .filter(Boolean)
      return names.length ? names.join('、') : '普通用户'
    },
    /** 权限判断：是否拥有指定 permission_key（未提供标识时按无权限处理，fail-closed） */
    hasPerm: (state) => (key) => {
      // 传空值属于配置遗漏：按"无权限"处理，避免因漏写标识导致越权渲染入口
      if (!key) return false
      return state.permissions.includes(key)
    },
  },

  actions: {
    /** 登录：请求令牌并写入本地 */
    async login(form) {
      const res = await authApi.login(form)
      const { token, refreshToken, user } = res.data
      setToken(token)
      setRefreshToken(refreshToken)
      this.token = token
      this.applySession(user)
      this.loaded = true
    },

    /** 拉取当前用户完整会话（刷新页面恢复登录态时调用） */
    async fetchUserInfo() {
      const res = await authApi.getUserInfo()
      this.applySession(res.data)
      this.loaded = true
    },

    /** 将会话视图写入本地状态 */
    applySession(session) {
      if (!session) return
      this.user = {
        userId: session.userId,
        userName: session.userName,
        nickName: session.nickName,
        avatar: session.avatar,
        email: session.email,
        phone: session.phone,
        sex: session.sex,
        introduce: session.introduce,
        status: session.status,
      }
      this.roles = session.roles || []
      this.menus = session.menus || []
      this.permissions = session.permissions || []
    },

    /** 退出登录 */
    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 网络异常不阻塞本地清理
      } finally {
        this.clearSession()
      }
    },

    /** 清空本地会话状态 */
    clearSession() {
      removeToken()
      removeRefreshToken()
      this.token = ''
      this.user = null
      this.roles = []
      this.menus = []
      this.permissions = []
      this.loaded = false
    },
  },
})
