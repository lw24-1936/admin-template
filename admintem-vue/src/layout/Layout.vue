<template>
  <el-container class="layout-root">
    <!-- 左侧导航 -->
    <el-aside width="220px" class="layout-aside">
      <!-- 品牌区：编辑式双行锁定（标记 + 名称 + 副标题） -->
      <div class="brand">
        <!-- 品牌图标：与浏览器标签页 favicon 同一份 SVG，避免两套标识 -->
        <img class="brand__mark" src="/favicon.svg" alt="" width="30" height="30" />
        <span class="brand__text">
          <strong>admintem</strong>
          <em>权限管理系统</em>
        </span>
      </div>
      <el-scrollbar>
        <el-menu :default-active="activeMenu" router>
          <MenuItem
            v-for="menu in visibleMenus"
            :key="menu.id"
            :node="menu"
          />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-body">
      <!-- 顶部 -->
      <el-header class="layout-header">
        <!-- 面包屑：展示完整父级 -> 子级名称 -->
        <el-breadcrumb class="page-breadcrumb" separator="/">
          <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
            <span
              v-if="item.leaf && item.path && item.path !== route.path"
              class="crumb-link"
              @click="router.push(item.path)"
            >{{ item.name }}</span>
            <template v-else>{{ item.name }}</template>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown trigger="click">
          <span class="user-entry">
            <!-- 角色标签：编辑式细边胶囊，弱化但可读（过长时省略，悬停显示完整） -->
            <span class="user-role" :title="roleText">{{ roleText }}</span>
            <el-avatar :size="28" :src="avatarUrl" class="user-avatar">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ displayName }}</span>
            <el-icon class="user-caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="openProfile">个人资料</el-dropdown-item>
              <el-dropdown-item @click="openPwd">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容 -->
      <el-main class="layout-main">
        <!--
          路由容器：用 key 触发"换页重新挂载 + CSS 入场动画"。
          这里刻意**不用 <transition mode="out-in">**：路由组件是异步懒加载的，
          out-in 在异步组件解析期间会拿不到子节点，导致进入动画永不触发、内容区空白
          （2026-10-21 实测：点击菜单后 URL 变了但 layout-main 子元素为 0）。
          div + CSS animation 没有这层语义依赖，异步组件解析完自然渲染。
        -->
        <RouterView v-slot="{ Component, route: current }">
          <div :key="current.path" class="page-shell">
            <component :is="Component" />
          </div>
        </RouterView>
      </el-main>
    </el-container>

    <!-- 个人资料弹窗 -->
    <el-dialog v-model="profileVisible" title="个人资料" width="520px" append-to-body>
      <p class="dialog-desc">
        修改昵称、邮箱、手机号与个人介绍；账号本身是登录标识，不可修改。
      </p>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="90px">
        <el-form-item label="登录账号">
          <el-input :model-value="store.user?.userName" disabled />
        </el-form-item>
                <el-form-item label="头像">
          <div class="avatar-editor">
            <el-upload
              :show-file-list="false"
              :http-request="handleUploadAvatar"
              :before-upload="beforeAvatarUpload"
              accept="image/png,image/jpeg,image/webp,image/gif"
            >
              <el-avatar :size="56" :src="avatarUrl" class="avatar-editor__img">{{ avatarText }}</el-avatar>
            </el-upload>
            <span class="avatar-editor__hint">点击更换；支持 png / jpg / webp / gif，≤ 2MB</span>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickName" maxlength="32" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.sex">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="个人介绍">
          <el-input v-model="profileForm.introduce" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="460px" append-to-body>
      <p class="dialog-desc">
        修改成功后当前令牌会被吊销，需要用新密码重新登录。
      </p>
      <el-form ref="pwdFormRef" :model="pwdForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6~32位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="savePwd">确 定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
/**
 * 登录后主框架：
 * - 左侧：基于后端菜单树（store.menus）渲染的导航
 * - 顶部：折叠状态 + 面包屑/标题 + 用户下拉（个人资料、修改密码、退出）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { animate, stagger } from 'animejs'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile, updatePassword, uploadAvatar } from '@/api/auth'
import { resolveAssetUrl } from '@/utils/media'
import MenuItem from './MenuItem.vue'
import { filterVisibleMenus } from '@/utils/menu'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

/**
 * 外壳入场动效（anime.js v4）。
 *
 * 只动 opacity / transform，且**尊重 reduced-motion**：
 * 用户开启"减少动效"时直接跳过，页面按最终态渲染（不做 CSS 隐藏再显示，
 * 避免 JS 失败时白屏）。
 */
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animate('.layout-aside', {
    opacity: [0, 1],
    x: [-14, 0],
    duration: 460,
    ease: 'outCubic',
  })
  animate('.layout-header', {
    opacity: [0, 1],
    y: [-10, 0],
    duration: 420,
    delay: 60,
    ease: 'outCubic',
  })
  animate('.layout-main > *', {
    opacity: [0, 1],
    y: [12, 0],
    duration: 520,
    delay: 120,
    ease: 'outCubic',
  })
  // 菜单项错峰淡入：让侧边栏"长出来"而不是整块出现
  animate('.layout-aside .el-menu-item, .layout-aside .el-sub-menu__title', {
    opacity: [0, 1],
    x: [-8, 0],
    duration: 360,
    delay: stagger(26, { start: 140 }),
    ease: 'outCubic',
  })
})

/** 当前激活菜单 = 路由路径（与后端 menu_url 一致） */
const activeMenu = computed(() => route.path)

/**
 * 左侧导航使用的菜单树：过滤掉 show=1（隐藏）的菜单。
 * 注意与路由注册区分——隐藏菜单仍会注册路由（可用 URL 直达），只是不出现在导航中。
 */
const visibleMenus = computed(() => filterVisibleMenus(store.menus))

/** 在菜单树中查找当前路径对应的节点链（自父级到当前节点） */
function findMenuChain(nodes, currentPath) {
  const target = String(currentPath).replace(/\/+$/, '')
  const walk = (list) => {
    for (const n of list || []) {
      if (n.menuUrl && n.menuUrl.replace(/\/+$/, '') === target) {
        return [n]
      }
      if (n.children && n.children.length) {
        const sub = walk(n.children)
        if (sub) {
          return [n, ...sub]
        }
      }
    }
    return null
  }
  return walk(nodes)
}

/**
 * 面包屑数据：由后端菜单树推导，包含全部父级与当前子级名称
 * （如 系统管理 / 用户管理）；无法匹配时回退路由标题兜底。
 */
const breadcrumbs = computed(() => {
  const chain = findMenuChain(store.menus, route.path)
  if (!chain || !chain.length) {
    const title = route.meta.title || ''
    return title ? [{ name: title, leaf: true, path: '' }] : []
  }
  return chain.map((node, index) => ({
    name: node.name,
    leaf: index === chain.length - 1,
    path: node.menuUrl || '',
  }))
})

/** 昵称展示 */
const displayName = computed(() => store.displayName || store.user?.userName || '未登录')

/* ------------------------- 下拉与弹窗控制 ------------------------- */
const profileVisible = ref(false)
const pwdVisible = ref(false)
const saving = ref(false)

// 个人资料表单
const profileForm = reactive({
  nickName: '',
  email: '',
  phone: '',
  sex: 1,
  introduce: '',
})
// 个人资料校验规则（与后端 UpdateProfileRequest 的 @Email/@Pattern 校验保持一致）
const profileRules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
}
const profileFormRef = ref()
// 修改密码表单
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const pwdFormRef = ref()

/** 打开个人资料弹窗 */
function openProfile() {
  const u = store.user || {}
  Object.assign(profileForm, {
    nickName: u.nickName || '',
    email: u.email || '',
    phone: u.phone || '',
    sex: u.sex ?? 1,
    introduce: u.introduce || '',
  })
  profileVisible.value = true
}

/** 保存个人资料 */
async function saveProfile() {
  // 邮箱/手机号格式先本地校验，避免直接触发后端 400 后才提示
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (saving.value) return
  saving.value = true
  try {
    await updateProfile({ ...profileForm })
    ElMessage.success('资料已更新')
    // 重新拉取会话刷新本地用户信息
    await store.fetchUserInfo()
    profileVisible.value = false
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕（401 除外：提示已由拦截器抑制）
    if (e?.response?.status !== 401) {
      console.error('[layout] 保存个人资料失败：', e)
    }
  } finally {
    saving.value = false
  }
}

/** 打开修改密码弹窗 */
function openPwd() {
  Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  pwdVisible.value = true
}

/** 保存新密码 */
async function savePwd() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  // 与后端 UpdatePasswordRequest 的 6~32 位校验保持一致，避免无谓请求
  const len = pwdForm.newPassword.length
  if (len < 6 || len > 32) {
    ElMessage.warning('新密码长度须为 6~32 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    store.clearSession()
    router.replace('/login')
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出，此处仅留痕（401 除外：提示已由拦截器抑制）
    if (e?.response?.status !== 401) {
      console.error('[layout] 修改密码失败：', e)
    }
  } finally {
    saving.value = false
  }
}

/** 退出登录 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    // 用户取消确认：ElMessageBox 以 reject 结束，属正常流程
    return
  }
  await store.logout()
  router.replace('/login')
}

/** 头像兜底：无头像显示账号首字符 */
const avatarText = computed(() => (displayName.value || '?').charAt(0).toUpperCase())

/**
 * 头像地址：后端存的是相对路径（/uploads/avatar/...），这里按 API 基址拼完整 URL。
 * 没有头像时返回空串，el-avatar 会回退到插槽里的首字母。
 */
const avatarUrl = computed(() => resolveAssetUrl(store.user?.avatar))

/** 上传前的前端预校验（大小/类型）：服务端也会校验，这里只是早点给出提示 */
function beforeAvatarUpload(file) {
  const okType = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type)
  if (!okType) {
    ElMessage.error('仅支持 png / jpg / webp / gif 格式的图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片不能超过 2MB')
    return false
  }
  return true
}

/**
 * 自定义上传：走统一的 axios 实例（自动带令牌、统一错误提示）。
 *
 * @param {{file: File}} options el-upload 的 http-request 参数
 */
async function handleUploadAvatar(options) {
  try {
    const res = await uploadAvatar(options.file)
    // 直接写回本地会话，顶部头像立即变化，无需重新登录
    if (store.user) store.user.avatar = res.data
    ElMessage.success('头像已更新')
  } catch (e) {
    console.error('[layout] 头像上传失败：', e)
  }
}

/**
 * 当前用户角色展示串（顶栏胶囊标签用）
 * 后端下发的是 RoleVO 对象数组（{ id, roleName, roleKey }），需按 roleName 取值
 */
const roleText = computed(() => {
  const names = (store.roles || []).map((role) => role.roleName || role.roleKey).filter(Boolean)
  return names.length ? names.join('、') : '普通用户'
})
</script>

<style scoped>
/*
 * 后台外壳（V2「层叠纸面」）
 * - 侧边栏：白卡 + 细分隔线；菜单用浅底胶囊 + 左侧指示条表达激活
 * - 顶栏：半透明 + 背景虚化（滚动时内容从下方"透"出来），保持浅色不压暗
 * - 内容区：统一 16px 外边距（页面卡片内部另有 20px），避免"双层留白"过厚
 * - 全部数值取自 tokens，页面内不写裸色值
 */
.layout-root {
  height: 100%;
}

/* ==================== 左侧导航 ==================== */
.layout-aside {
  background: var(--lc-paper-card);
  border-right: 1px solid var(--lc-line);
  overflow: hidden;
  /* 极淡的侧栏渐变，让白色侧栏与纸面内容区产生层次（不抢内容） */
  background-image: linear-gradient(180deg, var(--lc-paper-card) 0%, var(--lc-surface-soft) 100%);
}

/* 品牌区：渐变方块 + 双行锁定（名称 + 副标题） */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 var(--lc-sp-5);
  border-bottom: 1px solid var(--lc-line-soft);
}
.brand__mark {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: block;
  border-radius: 9px;
  /* 图标自身已含渐变底与白色字母，这里只保留统一的柔和投影 */
  box-shadow: 0 8px 18px -12px var(--lc-brand-glow);
}
.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}
.brand__text strong {
  font-size: var(--lc-fs-lead);
  font-weight: 650;
  letter-spacing: -0.01em;
  color: var(--lc-ink);
}
.brand__text em {
  font-style: normal;
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.14em;
  color: var(--lc-ink-faint);
}

.layout-aside :deep(.el-menu) {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--lc-ink-body);
  --el-menu-active-color: var(--lc-brand);
  --el-menu-hover-bg-color: var(--lc-surface-hover);
  --el-menu-item-height: 42px;
  --el-menu-sub-item-height: 40px;
  border-right: none;
  padding: var(--lc-sp-2);
}
.layout-aside :deep(.el-menu-item),
.layout-aside :deep(.el-sub-menu__title) {
  position: relative;
  margin: 2px 0;
  border-radius: var(--lc-radius-ctl);
  font-size: var(--lc-fs-body);
  transition: background-color var(--lc-motion-fast) var(--lc-ease-out),
    color var(--lc-motion-fast) var(--lc-ease-out);
}
.layout-aside :deep(.el-sub-menu__title) {
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}
.layout-aside :deep(.el-menu-item:hover),
.layout-aside :deep(.el-sub-menu__title:hover) {
  color: var(--lc-ink);
  background: var(--lc-surface-hover);
}
/* 激活项：主色浅底 + 主色文字 + 左侧 3px 指示条（不用深色反白，保持浅色主题） */
.layout-aside :deep(.el-menu-item.is-active) {
  color: var(--lc-brand);
  font-weight: 600;
  background: var(--lc-brand-soft);
}
.layout-aside :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  background-image: var(--lc-gradient-brand);
  border-radius: 0 3px 3px 0;
}
/* 子菜单展开时，缩进线与父级标题对齐 */
.layout-aside :deep(.el-menu .el-menu) {
  padding-left: 0;
}

/* ==================== 顶栏 ==================== */
.layout-body {
  min-width: 0;
}
.layout-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 var(--lc-sp-6);
  /* 半透明白 + 背景虚化：滚动时内容从下方透出，比纯白更"轻" */
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(12px) saturate(1.1);
  border-bottom: 1px solid var(--lc-line-soft);
}
/* 面包屑：父级弱化、当前为墨色加粗 */
.page-breadcrumb {
  font-size: var(--lc-fs-label);
}
.page-breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--lc-ink-muted);
  font-weight: 400;
}
.page-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--lc-ink);
  font-weight: 600;
  font-size: var(--lc-fs-body);
}
.page-breadcrumb :deep(.el-breadcrumb__separator) {
  color: var(--lc-ink-faint);
}
.crumb-link {
  color: var(--lc-brand);
  cursor: pointer;
  transition: opacity var(--lc-motion-fast);
}
.crumb-link:hover {
  opacity: 0.75;
}

/* 用户区：角色胶囊 + 头像 + 名称 + 箭头 */
.user-entry {
  display: inline-flex;
  align-items: center;
  gap: var(--lc-sp-2);
  padding: 4px 10px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--lc-radius-pill);
  cursor: pointer;
  outline: none;
  transition: background var(--lc-motion-fast), border-color var(--lc-motion-fast);
}
.user-entry:hover {
  background: var(--lc-paper-card);
  border-color: var(--lc-line);
}
.user-role {
  padding: 2px 8px;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-pill);
  background: var(--lc-paper-card);
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.04em;
  color: var(--lc-ink-muted);
  /* 多角色时避免胶囊撑宽顶栏；完整内容通过 title 属性可见 */
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-avatar {
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  font-size: var(--lc-fs-label);
}
.user-name {
  font-size: var(--lc-fs-label);
  color: var(--lc-ink);
  font-weight: 500;
}
.user-caret {
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

/* ==================== 主内容 ==================== */
.layout-main {
  background: transparent;
  overflow-y: auto;
  /*
   * 内容区与「左侧导航 / 顶栏 / 窗口左右边」的间距（外壳统一控制，所有后台页面一致）。
   *
   * 历史：曾按需求收紧到 1px（页面卡片自身有 20px 内边距），V2 视觉升级放宽到 16px。
   * 2026-10-22 按需求统一为 10px（--lc-sp-shell-gap）：四边等距，卡片贴着顶栏与窗口边但不糊在一起。
   *
   * 只改这一个值即可；不要在页面里分别加 padding，否则会重新变成"每页间距不一致"。
   */
  padding: var(--lc-sp-shell-gap);
}

/* 路由容器与换页入场：只做一次 8px 上移淡入，不与路由异步解析耦合 */
.page-shell {
  animation: page-in var(--lc-motion-enter) var(--lc-ease-out) both;
}
@keyframes page-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@media (prefers-reduced-motion: reduce) {
  .page-shell {
    animation: none;
  }
}

/* ==================== 窄屏适配 ==================== */
@media (max-width: 1280px) {
  .layout-header {
    padding: 0 var(--lc-sp-4);
  }
}

/* 头像编辑（个人资料弹窗）：头像 + 一旁的说明文字 */
.avatar-editor {
  display: flex;
  align-items: center;
  gap: var(--lc-sp-3);
}
.avatar-editor__img {
  cursor: pointer;
  border: 1px solid var(--lc-line);
}
.avatar-editor__hint {
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}
</style>
