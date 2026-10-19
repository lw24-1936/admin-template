/**
 * 菜单展示工具（移动端，与桌面端 `admintem-vue/src/utils/menu.js` 语义保持一致）
 *
 * 后端菜单节点字段：
 *   - status：0 启用 / 1 禁用(null 按启用) —— 禁用的菜单后端不再下发；
 *   - show  ：0 展示 / 1 隐藏            —— 隐藏的菜单仍会下发（可 URL 直达），
 *             仅不应出现在导航与清单中。
 *
 * 桌面端用 `filterVisibleMenus` 过滤整棵树；移动端首页只需"要展示的叶子菜单清单"，
 * 因此这里提供 `flattenVisibleLeafMenus`，但隐藏判定共用 `isHiddenMenu`。
 */

/** 是否为需要在界面中隐藏的菜单（show=1；null/undefined 视为展示） */
export function isHiddenMenu(node) {
  return !!node && node.show === 1
}

/**
 * 过滤出需要展示的菜单树（不修改入参，返回新对象）
 * 与桌面端 `filterVisibleMenus` 行为一致，便于两端语义对齐。
 *
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

/**
 * 拍平为"需要展示的叶子菜单"清单（移动端首页「我的菜单」用）
 *
 * 规则：隐藏节点（show=1）整棵子树跳过；有可见子节点的取子节点；
 * 无子节点的自身即为叶子；父级被隐藏时其子级不作为独立项出现。
 *
 * @param {Array} nodes 菜单树
 * @returns {Array} 叶子菜单列表（顺序与入参一致）
 */
export function flattenVisibleLeafMenus(nodes) {
  const result = []
  const walk = (list) => {
    for (const node of list || []) {
      if (isHiddenMenu(node)) continue
      const children = (node.children || []).filter((child) => !isHiddenMenu(child))
      if (children.length) {
        walk(children)
      } else {
        result.push(node)
      }
    }
  }
  walk(nodes)
  return result
}
