/**
 * 按钮级权限指令 v-perm（与桌面端语义保持一致，fail-closed）
 *
 * 用法：
 *   <button v-perm="'sysAuthUser:addSysAuthUser'">新增用户</button>
 *   <button v-perm="['a:b','c:d']">满足其一即可显示</button>
 *
 * 判定依据：当前登录用户拥有且仅拥有后端下发的 permissions（菜单 + 按钮标识集合）。
 * 无权限的元素直接从 DOM 移除，因此未授权入口既不可见也不可点击。
 *
 * 安全语义（本轮加固）：
 *   1. 会话未加载完成时不渲染受控元素（避免"先显示后隐藏"的瞬间暴露）；
 *   2. 指令值为空（漏写标识）按"无权限"处理并在控制台报错，避免配置遗漏变成默认放行；
 *   3. 多标识数组为"任一命中即可显示"。
 */
import { useUserStore } from '@/stores/user'

/** 移除元素（连同其绑定的点击事件） */
function removeEl(el) {
  el.parentNode && el.parentNode.removeChild(el)
}

export default {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()

    // 会话未加载：fail-closed
    if (!userStore.loaded) {
      removeEl(el)
      return
    }

    // 未提供权限标识：属配置遗漏，报错并按无权限处理
    if (!value) {
      console.error('[v-perm] 指令缺少权限标识（permission_key），该元素已按无权限处理')
      removeEl(el)
      return
    }

    const keys = Array.isArray(value) ? value : [value]
    if (!keys.some((key) => userStore.hasPerm(key))) {
      removeEl(el)
    }
  },
}
