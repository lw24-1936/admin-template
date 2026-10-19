/**
 * 媒体资源地址处理。
 *
 * 后端返回的头像路径是**相对路径**（如 `/uploads/avatar/202610/x.png`），
 * 因为同一个后端可能被不同域名的前端访问，写死绝对地址会在换环境时失效。
 * 前端按 API 基址拼完整 URL：
 *   - dev：VITE_API_BASE_URL = http://localhost:8090
 *   - 生产：通常与前端同域（基址为空），此时直接用相对路径
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 把后端返回的相对资源路径转成浏览器可用的 URL。
 *
 * @param {string} path 相对路径或已是完整 URL
 * @returns {string} 可直接用于 img src 的地址；空值返回空串
 */
export function resolveAssetUrl(path) {
  if (!path) return ''
  // 已经是完整地址 / data URI / blob，原样返回
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  return API_BASE + (path.startsWith('/') ? path : '/' + path)
}
