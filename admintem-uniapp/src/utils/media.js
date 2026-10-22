/**
 * 头像/图片地址解析
 *
 * 后端返回的是相对路径（如 /uploads/avatar/202610/xxx.png）：
 * - H5：`/uploads` 已由 Vite（开发）或 Nginx（生产）反代到后端，直接用相对路径；
 * - 小程序：没有代理，必须拼上 API 基址（默认 http://localhost:8090）。
 */
import { API_BASE_URL } from '@/config'

export function resolveAssetUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  if (process.env.UNI_PLATFORM === 'h5') return path
  return API_BASE_URL + path
}
