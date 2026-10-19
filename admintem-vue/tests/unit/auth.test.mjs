/**
 * 工具函数单测：src/utils/auth.js（令牌本地存取）
 *
 * 运行：npm run test
 * 说明：使用 Node 内置测试运行器；auth.js 仅依赖 localStorage，
 *       这里用一个最小内存实现替代浏览器环境（测试结束恢复全局）。
 */
import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'

/** 最小 localStorage 替身 */
function createStorageStub() {
  const map = new Map()
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
    clear: () => map.clear(),
  }
}

let auth

before(async () => {
  globalThis.localStorage = createStorageStub()
  auth = await import('../../src/utils/auth.js')
})

after(() => {
  delete globalThis.localStorage
})

test('未设置时 getToken 返回空字符串（而非 null）', () => {
  localStorage.clear()
  assert.equal(auth.getToken(), '')
})

test('setToken/getToken：写入后可读回', () => {
  auth.setToken('abc.def.ghi')
  assert.equal(auth.getToken(), 'abc.def.ghi')
})

test('令牌 key 与后端约定一致（admintem_token）', () => {
  auth.setToken('t1')
  assert.equal(localStorage.getItem('admintem_token'), 't1', '桌面端与移动端、后端都使用 admintem_token')
})

test('removeToken：清除后 getToken 为空字符串，且不抛异常', () => {
  auth.setToken('t2')
  auth.removeToken()
  assert.equal(auth.getToken(), '')
  // 重复清除应幂等
  auth.removeToken()
  assert.equal(auth.getToken(), '')
})
