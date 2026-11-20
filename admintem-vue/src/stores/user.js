/**
 * 用户状态管理（Pinia）
 * - 持有令牌、用户基础信息、角色、菜单树、权限标识集合
 * - 登录后调用 fetchUserInfo 一次性拉齐会话数据
 */
import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, setRefreshToken, removeRefreshToken } from '@/utils/auth'
import * as authApi from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    /** 用户基础信息（来自后端 UserSessionVO） */
    user: null,
    /** 用户拥有的角色列表 */
    roles: [],
    /** 可访问菜单树（后端按权限过滤后返回） */
    menus: [],
    /** 全部权限标识（菜单 + 按钮 permission_key） */
    permissions: [],
    /** 会话数据是否已加载 */
    loaded: false,
  }),

  getters: {
    /** 是否已登录 */
    isLogin: (state) => !!state.token,
    /** 当前昵称/账号展示 */
    displayName: (state) =>
      (state.user && (state.user.nickName || state.user.userName)) || '',
    /** 权限判断：拥有指定 permission_key（未提供标识时按无权限处理，fail-closed） */
    hasPerm: (state) => (key) => {
      // 传空值属于配置遗漏：按"无权限"处理，避免因漏写标识导致越权渲染按钮
      if (!key) return false
      return state.permissions.includes(key)
    },
  },

  actions: {
    /**
     * 登录：请求令牌并写入本地
     * @param {Object} form 登录表单
     */
      async login(form) {
        const res = await authApi.login(form)
        const data = res.data || {}
        if (data.mfaRequired) {
          return data
        }
        this.applyLoginResult(data)
        return data
      },

      /** MFA 校验通过后写入正式令牌与会话 */
      async completeMfaLogin(data) {
        this.applyLoginResult(data)
        return data
      },

      async completeSsoLogin(data) {
        this.applyLoginResult(data)
        return data
      },

      applyLoginResult(data) {
        const { token, refreshToken, user } = data
        setToken(token)
        setRefreshToken(refreshToken)
        this.token = token
        this.applySession(user)
        this.loaded = true
      },

    /**
     * 拉取当前用户完整会话（刷新页面后恢复登录态时调用）
     */
    async fetchUserInfo() {
      const res = await authApi.getUserInfo()
      this.applySession(res.data)
      this.loaded = true
    },

    /**
     * 将会话视图写入本地状态
     * @param {Object} session UserSessionVO
     */
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

    /**
     * 退出登录
     */
    async logout() {
      try {
        // 通知服务端删除令牌白名单（失败也继续本地清理）
        await authApi.logout()
      } catch (e) {
        // 服务端注销失败（令牌已失效/网络异常）不阻断本地退出，但需留痕便于排查
        console.error('[user] 服务端注销失败，已继续清理本地会话：', e)
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
