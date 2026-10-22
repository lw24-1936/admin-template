/**
 * 按钮级权限指令 v-perm（fail-closed 语义）
 *
 * 用法：
 *   <el-button v-perm="'sysAuthUser:addSysAuthUser'">新增用户</el-button>
 *   <el-button v-perm="['a:b','c:d']">满足其一即可显示</el-button>
 *
 * 判定依据：当前登录用户拥有且仅拥有后端下发的 permissions（菜单 + 按钮标识集合）。
 *
 * 实现取舍：
 *   1. 用 display:none 隐藏而不是 removeChild —— 会话刷新/权限热更新后还能恢复显示，
 *      旧实现一旦移除就再也回不来（v-perm 只有 mounted，没有 updated）；
 *   2. 隐藏元素同时移出无障碍树与 Tab 顺序，安全语义不变；
 *   3. 真正的安全边界始终在后端 @PreAuthorize，前端隐藏只是体验。
 */
import { useUserStore } from '@/stores/user'

function hide(el) {
  el.style.display = 'none'
  el.setAttribute('data-perm-hidden', '1')
}

function show(el) {
  el.style.removeProperty('display')
  el.removeAttribute('data-perm-hidden')
}

function apply(el, binding) {
  const { value } = binding
  const store = useUserStore()

  // 会话尚未加载：fail-closed，等会话就绪后组件重渲染会再次进入 updated
  if (!store.loaded) {
    hide(el)
    return
  }

  // 未提供权限标识：属于配置遗漏，报错并按无权限处理
  if (!value) {
    console.error('[v-perm] 指令缺少权限标识（permission_key），该元素已按无权限处理')
    hide(el)
    return
  }

  const keys = Array.isArray(value) ? value : [value]
  const allowed = keys.some((key) => store.hasPerm(key))
  if (allowed) {
    show(el)
  } else {
    hide(el)
  }
}

export const permission = {
  mounted(el, binding) {
    apply(el, binding)
  },
  updated(el, binding) {
    apply(el, binding)
  },
}
