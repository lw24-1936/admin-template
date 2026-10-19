/**
 * 菜单展示工具（桌面端）
 *
 * 后端菜单节点字段：
 *   - status：0 启用 / 1 禁用(null 按启用) —— 禁用的菜单后端不再下发；
 *   - show  ：0 展示 / 1 隐藏            —— 隐藏的菜单仍会下发，用于"可直达但不显示在导航中"的场景
 *             （例如详情页、仅在特定入口跳转的页面）。
 *
 * 因此：路由注册使用完整菜单树（隐藏页仍可按 URL 直达），
 *       左导航/面包屑等"展示"场景使用本函数过滤后的树。
 */

/** 是否为需要在导航中隐藏的菜单（show=1；null/undefined 视为展示） */
export function isHiddenMenu(node) {
  return !!node && node.show === 1
}

/**
 * 递归过滤出需要展示的菜单树（不修改入参，返回新对象）
 * @param {Array} nodes 菜单树
 * @returns {Array} 过滤后的菜单树
 */
export function filterVisibleMenus(nodes) {
  if (!Array.isArray(nodes)) return []
  return nodes
    .filter((node) => !isHiddenMenu(node))
    .map((node) =>
      Array.isArray(node.children) && node.children.length
        ? { ...node, children: filterVisibleMenus(node.children) }
        : { ...node }
    )
}
