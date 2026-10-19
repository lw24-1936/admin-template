/**
 * 工具函数单测：src/utils/menu.js（菜单展示过滤）
 *
 * 运行：npm run test        （等价于 node --test tests/unit）
 * 说明：使用 Node 内置测试运行器，不引入任何第三方依赖。
 *       menu.js 为纯函数模块（无 Vue 依赖），可直接被 Node 以 ESM 方式导入。
 */
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { filterVisibleMenus, isHiddenMenu } from '../../src/utils/menu.js'

/** 构造一棵含隐藏项与嵌套分组的菜单树 */
function sampleTree() {
  return [
    { id: 1, name: '工作台', show: 0, children: [] },
    {
      id: 2,
      name: '系统管理',
      show: 0,
      children: [
        { id: 3, name: '用户管理', show: 0, children: [] },
        { id: 4, name: '角色管理', show: 1, children: [] }, // 隐藏
        { id: 5, name: '权限管理', show: null, children: [] }, // null 视为展示
      ],
    },
    { id: 6, name: '隐藏顶级', show: 1, children: [{ id: 7, name: '隐藏子级', show: 0, children: [] }] },
  ]
}

test('isHiddenMenu：仅 show=1 视为隐藏，null/undefined 视为展示', () => {
  assert.equal(isHiddenMenu({ show: 1 }), true)
  assert.equal(isHiddenMenu({ show: 0 }), false)
  assert.equal(isHiddenMenu({ show: null }), false)
  assert.equal(isHiddenMenu({}), false)
  assert.equal(isHiddenMenu(null), false)
})

test('filterVisibleMenus：过滤隐藏节点并保留展示节点', () => {
  const visible = filterVisibleMenus(sampleTree())
  assert.deepEqual(visible.map((n) => n.id), [1, 2], '应移除隐藏的顶级菜单')
  assert.deepEqual(visible[1].children.map((n) => n.id), [3, 5], '应移除隐藏子级，保留 show=null 的子级')
})

test('filterVisibleMenus：不修改入参（纯函数）', () => {
  const tree = sampleTree()
  const snapshot = JSON.stringify(tree)
  filterVisibleMenus(tree)
  assert.equal(JSON.stringify(tree), snapshot, '原始树结构不应被改写')
})

test('filterVisibleMenus：返回的是新对象，改子节点不影响源数据', () => {
  const tree = sampleTree()
  const visible = filterVisibleMenus(tree)
  visible[1].children[0].name = '被改名'
  assert.equal(tree[1].children[0].name, '用户管理', '源树中的节点名不应被牵连修改')
})

test('filterVisibleMenus：入参非数组时返回空数组', () => {
  assert.deepEqual(filterVisibleMenus(null), [])
  assert.deepEqual(filterVisibleMenus(undefined), [])
  assert.deepEqual(filterVisibleMenus({}), [])
})

test('filterVisibleMenus：父级隐藏时其子级一并不可见（导航语义）', () => {
  const visible = filterVisibleMenus(sampleTree())
  const ids = visible.map((n) => n.id)
  assert.ok(!ids.includes(6), '隐藏父级应被移除')
  const childIds = visible.flatMap((n) => (n.children || []).map((c) => c.id))
  assert.ok(!childIds.includes(7), '隐藏父级的子级不应作为独立节点出现')
})
