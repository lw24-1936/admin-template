/**
 * 工具函数单测：src/utils/menu.js（菜单展示过滤）
 *
 * 运行：npm run test      （等价于 node --test tests/unit/menu.test.mjs）
 * 说明：使用 Node 内置测试运行器，不引入任何第三方依赖。
 */
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  filterVisibleMenus,
  flattenVisibleLeafMenus,
  isHiddenMenu,
} from '../../src/utils/menu.js'

/** 与后端下发结构一致的示例菜单树 */
function sampleTree() {
  return [
    { id: 1, name: '工作台', show: 0, children: [] },
    {
      id: 2,
      name: '系统管理',
      show: 0,
      children: [
        { id: 3, name: '用户管理', show: 0, children: [] },
        { id: 4, name: '角色管理', show: 1, children: [] },
        { id: 5, name: '权限管理', show: null, children: [] },
      ],
    },
    { id: 6, name: '隐藏顶级', show: 1, children: [{ id: 7, name: '隐藏子级', show: 0, children: [] }] },
  ]
}

test('isHiddenMenu：仅 show=1 视为隐藏', () => {
  assert.equal(isHiddenMenu({ show: 1 }), true)
  assert.equal(isHiddenMenu({ show: 0 }), false)
  assert.equal(isHiddenMenu({ show: null }), false)
  assert.equal(isHiddenMenu({}), false)
  assert.equal(isHiddenMenu(null), false)
})

test('flattenVisibleLeafMenus：返回应展示的叶子并跳过隐藏项', () => {
  const leaves = flattenVisibleLeafMenus(sampleTree())
  assert.deepEqual(leaves.map((n) => n.id), [1, 3, 5])
})

test('flattenVisibleLeafMenus：父级隐藏时其子级不作为独立项出现', () => {
  const leaves = flattenVisibleLeafMenus(sampleTree())
  assert.ok(!leaves.some((n) => n.id === 6 || n.id === 7))
})

test('flattenVisibleLeafMenus：空入参返回空数组', () => {
  assert.deepEqual(flattenVisibleLeafMenus(null), [])
  assert.deepEqual(flattenVisibleLeafMenus(undefined), [])
  assert.deepEqual(flattenVisibleLeafMenus([]), [])
})

test('flattenVisibleLeafMenus：不修改入参（纯函数）', () => {
  const tree = sampleTree()
  const snapshot = JSON.stringify(tree)
  flattenVisibleLeafMenus(tree)
  assert.equal(JSON.stringify(tree), snapshot)
})

test('filterVisibleMenus：与桌面端语义一致（隐藏过滤 + 非破坏性）', () => {
  const visible = filterVisibleMenus(sampleTree())
  assert.deepEqual(visible.map((n) => n.id), [1, 2])
  assert.deepEqual(visible[1].children.map((n) => n.id), [3, 5])
  assert.deepEqual(filterVisibleMenus(null), [])
})
