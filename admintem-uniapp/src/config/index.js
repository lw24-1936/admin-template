/**
 * 后端地址与运行时常量
 *
 * - H5：走 Vite 开发代理 `/api`（见 vite.config.js），生产同源部署时也成立；
 *   如需直连其他地址，构建时设 `VITE_API_BASE_URL` 覆盖。
 * - 小程序：没有代理，必须用绝对地址。默认本机 8090，真机调试请改成局域网 IP，
 *   并在微信开发者工具里勾选「不校验合法域名」。
 */
const isH5 = process.env.UNI_PLATFORM === 'h5'

export const API_BASE_URL = isH5
  ? import.meta.env.VITE_API_BASE_URL || '/api'
  : 'http://localhost:8090'

/** 令牌请求头名称（与后端 AuthTokenFilter 约定一致） */
export const TOKEN_HEADER = 'admintem_token'

/** 请求头前缀：后端只认 `LAUNCH <token>` 形式 */
export const TOKEN_PREFIX = 'LAUNCH '

/** 本地缓存键（与桌面端/移动端同名，便于排查） */
export const STORAGE_KEYS = {
  token: 'admintem_token',
  user: 'admintem_user',
  remember: 'admintem_remember_username',
}
