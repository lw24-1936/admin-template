/**
 * 移动端轻量日志
 *
 * 为什么单独建一个：移动端跑在手机上，**没有控制台可看**——
 * 出问题时用户只会说"点了没反应"。因此约定：
 *   1. 开发态（import.meta.env.DEV）输出 info/warn/debug，便于本机与真机远程调试；
 *   2. error 在**生产也输出**：它量少、价值高，便于用户接远程调试工具时抓取；
 *   3. 统一前缀 `[admintem-m]`，与桌面端日志、后端 traceId 日志区分开，便于过滤；
 *   4. 绝不记录令牌、密码等敏感值（调用方只传方法名/路径/状态码）。
 */

const PREFIX = '[admintem-m]'

/** 是否输出调试类日志（打包后自动为 false，由 Vite 在构建期静态替换） */
const devEnabled = Boolean(import.meta.env && import.meta.env.DEV)

/** 开发态调试日志 */
export function debug(...args) {
  if (devEnabled) console.debug(PREFIX, ...args)
}

/** 开发态常规日志 */
export function info(...args) {
  if (devEnabled) console.info(PREFIX, ...args)
}

/** 开发态告警（业务错误、鉴权失败等预期内异常） */
export function warn(...args) {
  if (devEnabled) console.warn(PREFIX, ...args)
}

/** 错误日志：生产也输出（组件异常、未处理异常、接口 5xx/网络失败） */
export function error(...args) {
  console.error(PREFIX, ...args)
}
