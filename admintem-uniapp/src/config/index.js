/**
 * 后端地址与运行时常量
 *
 * - H5：走 Vite 开发代理 `/api`（见 vite.config.js），生产同源部署时也成立；
 * - 小程序 / App：没有代理，必须用绝对地址。默认本机 8090；
 * - 三端都支持用构建变量覆盖：
 *     VITE_API_BASE_URL=https://api.example.com pnpm build:h5
 *     VITE_API_BASE_URL=http://192.168.1.214:8090 pnpm build:app
 *
 * 真机注意：
 *   1. 手机上的 localhost 是手机自己，必须换成局域网 IP 或域名；
 *   2. Android 9+ / iOS ATS 对明文 HTTP 有限制，正式包建议用 HTTPS 域名。
 */
const isH5 = process.env.UNI_PLATFORM === 'h5'

/**
 * 构建时注入的地址（见 vite.config.js 的 define）。
 * 为什么不用 import.meta.env：uni-app 的 app/小程序构建只内联 BASE_URL/MODE 等少量字段，
 * VITE_* 不会被注入（实测构建产物里取不到），所以这里用自定义常量。
 */
const injectedBase = typeof __ADMINTEM_API_BASE__ === 'string' ? __ADMINTEM_API_BASE__ : ''

export const API_BASE_URL = injectedBase || (isH5 ? '/api' : 'http://localhost:8090')

/** 令牌请求头名称（与后端 AuthTokenFilter 约定一致） */
export const TOKEN_HEADER = 'admintem_token'

/** 请求头前缀：后端只认 `LAUNCH <token>` 形式 */
export const TOKEN_PREFIX = 'LAUNCH '

/** 本地缓存键（与桌面端/移动端同名，便于排查） */
export const STORAGE_KEYS = {
  token: 'admintem_token',
    refreshToken: 'admintem_refresh_token',
  user: 'admintem_user',
  remember: 'admintem_remember_username',
}
