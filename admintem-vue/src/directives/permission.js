/**
 * 按钮级权限指令 v-perm（fail-closed 语义）
 *
 * 用法：
 *   <el-button v-perm="'sysAuthUser:addSysAuthUser'">新增用户</el-button>
 *   <el-button v-perm="['a:b','c:d']">满足其一即可显示</el-button>
 *
 * 判定依据：当前登录用户拥有且仅拥有后端下发的 permissions（菜单 + 按钮标识集合）。
 * 无权限的元素会被直接从 DOM 中移除。
 *
 * 安全语义（本轮加固）：
 *   1. 会话未加载完成（permissions 尚为空）时不渲染受控元素，避免"先显示后隐藏"的瞬间暴露；
 *   2. 指令值为空（漏写标识）按"无权限"处理并在控制台报错，避免配置遗漏变成默认放行；
 *   3. 多标识数组为"任一命中即可显示"。
 * 前端隐藏仅是体验，真正的安全边界仍在后端 @PreAuthorize。
 */
import { useUserStore } from '@/stores/user'

/** 移除元素（连同其可能绑定的点击事件） */
function removeEl(el) {
  el.parentNode && el.parentNode.removeChild(el)
}

export const permission = {
  mounted(el, binding) {
    const { value } = binding
    const store = useUserStore()

    // 会话未加载：fail-closed，等待会话就绪后由页面重新渲染
    if (!store.loaded) {
      removeEl(el)
      return
    }

    // 未提供权限标识：属配置遗漏，报错并按无权限处理
    if (!value) {
      console.error('[v-perm] 指令缺少权限标识（permission_key），该元素已按无权限处理')
      removeEl(el)
      return
    }

    // 支持字符串或数组（数组任一命中即可）
    const keys = Array.isArray(value) ? value : [value]
    const allowed = keys.some((k) => store.hasPerm(k))
    if (!allowed) {
      removeEl(el)
    }
  },
}
