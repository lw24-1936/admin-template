/**
 * 列表页查询状态 ⇄ URL 同步（组合式函数）
 *
 * 解决的问题：刷新、前进/后退、复制链接分享时，筛选条件与页码会丢，
 * 用户得重新点一遍。把状态写进 URL 后这些都是免费的。
 *
 * 设计取舍（刻意保持简单、可控）：
 *   1. **只做单向写回**：query 变化 → router.replace（不触发组件重挂载）；
 *      不从 URL 反向监听，避免"写 URL → 路由变化 → 再写 URL"的回环；
 *   2. **首屏水合**：进入页面时先把 URL 上的已知键合并进 query，再执行首次加载；
 *   3. **类型收敛**：页码/条数转数字，空串与 null 不写进 URL（保持地址干净）；
 *   4. **白名单键**：只同步调用方声明的字段，避免把内部状态（如 loading）带进地址栏。
 *
 * 用法：
 *   const query = reactive({ pageNum: 1, pageSize: 10, userName: '' })
 *   useQuerySync(query, { onHydrated: loadData })
 */
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 需要按数字处理的键 */
const NUMERIC_KEYS = new Set(['pageNum', 'pageSize', 'status'])

/** 把 URL 上的字符串还原成 query 需要的类型 */
function coerce(key, raw) {
  if (raw === undefined || raw === null || raw === '') return undefined
  if (NUMERIC_KEYS.has(key)) {
    const n = Number(raw)
    return Number.isFinite(n) ? n : undefined
  }
  return String(raw)
}

/** 把 query 收敛成干净的 URL 参数对象（去掉空值/默认值） */
function toParams(query) {
  const params = {}
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    // 页码 1 / 每页 10 属于默认值，不写进地址栏，保持可读
    if (key === 'pageNum' && Number(value) === 1) continue
    if (key === 'pageSize' && Number(value) === 10) continue
    params[key] = String(value)
  }
  return params
}

/**
 * @param {object} query 页面上的查询条件对象（reactive）
 * @param {object} [options]
 * @param {Function} [options.onHydrated] 从 URL 水合完成后的回调（通常用于首次加载）
 */
export function useQuerySync(query, options = {}) {
  const route = useRoute()
  const router = useRouter()

  // 1) 首屏水合：把 URL 上属于 query 的键写回 query
  let hydrated = false
  for (const key of Object.keys(query)) {
    const value = coerce(key, route.query[key])
    if (value !== undefined) {
      query[key] = value
      hydrated = true
    }
  }

  // 2) 单向写回：query 变化 -> 更新地址栏（replace，不产生历史记录，避免"后退"变成一步一筛）
  watch(
    () => ({ ...query }),
    (current) => {
      const params = toParams(current)
      // 与当前地址一致时不写，避免无意义的 replace
      const same =
        Object.keys(params).length === Object.keys(route.query).length &&
        Object.entries(params).every(([k, v]) => route.query[k] === v)
      if (same) return
      router.replace({ query: params }).catch(() => {
        // 路由跳转被取消（例如并发导航）时忽略：URL 同步失败不应影响列表使用
      })
    },
    { deep: true }
  )

  // 3) 水合后回调：由页面决定是否立即加载
  if (typeof options.onHydrated === 'function') {
    options.onHydrated()
  }

  return { hydrated }
}
