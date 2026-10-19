<template>
  <!-- 首页（移动端）：欢迎信息 + 数据概览 + 权限入口 + 可访问菜单清单 -->
  <div class="lc-page home">
    <!-- 欢迎卡 -->
    <section class="home-hero">
      <div class="home-hero__avatar">{{ userStore.avatarText }}</div>
      <div class="home-hero__info">
        <p class="home-hero__hi">你好，{{ userStore.displayName }}</p>
        <p class="home-hero__sub">{{ userStore.roleText }}</p>
      </div>
    </section>

    <h1 class="lc-page__title">工作台</h1>
    <p class="lc-page__desc">
      当前登录账号的可访问范围由角色权限实时下发，移动端为轻量版本。
    </p>

    <!-- 概览 -->
    <section class="lc-card">
      <h2 class="lc-card__title">数据概览</h2>
      <ul class="home-stats">
        <li>
          <strong>{{ userStore.menus.length }}</strong>
          <span>可访问菜单</span>
        </li>
        <li>
          <strong>{{ userStore.permissions.length }}</strong>
          <span>权限标识</span>
        </li>
        <li>
          <strong>{{ userStore.roles.length }}</strong>
          <span>角色数量</span>
        </li>
      </ul>
    </section>

    <!-- 权限入口：按 permission_key 控制是否渲染 -->
    <section class="lc-card">
      <h2 class="lc-card__title">管理入口</h2>
      <div class="home-entries">
        <button
          v-for="entry in entries"
          :key="entry.perm"
          v-perm="entry.perm"
          class="home-entry"
          type="button"
          @click="notifyDesktop(entry.label)"
        >
          <van-icon :name="entry.icon" size="22" />
          <span>{{ entry.label }}</span>
        </button>
      </div>
      <p v-if="visibleEntryCount === 0" class="home-empty">暂无可用管理入口</p>
    </section>

    <!-- 可访问菜单（后端已按权限过滤） -->
    <section class="lc-card">
      <h2 class="lc-card__title">我的菜单</h2>
      <van-cell
        v-for="menu in flatMenus"
        :key="menu.id"
        :title="menu.name"
        :label="menu.menuUrl"
        is-link
        @click="notifyDesktop(menu.name)"
      />
      <p v-if="!flatMenus.length" class="home-empty">暂无可见菜单</p>
    </section>

    <p class="home-foot">admintem 移动端 v0.1.0 · 完整管理功能请使用桌面端</p>
  </div>
</template>

<script setup>
/**
 * 首页
 * - 数据全部来自 /auth/getUserInfo 返回的会话（用户、角色、菜单、权限）
 * - 管理入口使用 v-perm 指令按 permission_key 控制渲染
 */
import { computed } from 'vue'
import { Cell as VanCell, Icon as VanIcon, showToast } from 'vant'
import 'vant/es/cell/style/index'
import 'vant/es/icon/style/index'
import { useUserStore } from '@/stores/user'
import { flattenVisibleLeafMenus } from '@/utils/menu'

const userStore = useUserStore()

/**
 * 管理入口：permission_key 与后端 @PreAuthorize、菜单 permission_key 完全一致
 * 菜单级标识来自 sys_auth_permission(type=0)，按钮级标识来自 type=1
 */
const entries = [
  { label: '用户管理', perm: 'userInfo:userInfo', icon: 'friends-o' },
  { label: '角色管理', perm: 'userInfo:roleInfo', icon: 'manager-o' },
  { label: '权限管理', perm: 'userInfo:permissionInfo', icon: 'setting-o' },
  { label: '新增用户', perm: 'sysAuthUser:addSysAuthUser', icon: 'plus' },
]

/**
 * 「我的菜单」清单：仅保留需要展示的叶子菜单（过滤 show=1 的隐藏菜单，
 * 隐藏菜单仍可 URL 直达，只是不出现在清单里）。
 * 过滤逻辑抽到 utils/menu.js，与桌面端语义一致并有单测覆盖。
 */
const flatMenus = computed(() => flattenVisibleLeafMenus(userStore.menus))

/**
 * 实际可见的管理入口数量
 * 模板中由 v-perm 指令移除无权限按钮，这里用同一判定计算数量，
 * 仅用于「一个入口都没有」时给出提示（指令是命令式的，无法在模板中统计）。
 */
const visibleEntryCount = computed(() =>
  entries.filter((entry) => userStore.hasPerm(entry.perm)).length
)

/** 移动端暂不承载完整管理功能，给出明确提示 */
function notifyDesktop(name) {
  showToast({ message: `${name} 请在桌面端使用`, position: 'bottom' })
}
</script>

<style scoped>
.home-hero {
  display: flex;
  align-items: center;
  gap: var(--lc-sp-3);
  padding: 16px;
  margin-bottom: 14px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
}
.home-hero__avatar {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: 50%;
  background: var(--lc-brand);
  color: var(--lc-paper-card);
  font-size: 19px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.home-hero__hi {
  margin: 0 0 3px;
  font-size: var(--lc-fs-card);
  font-weight: 600;
  color: var(--lc-ink);
}
.home-hero__sub {
  margin: 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
}

.home-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 0;
  padding: 0;
  list-style: none;
}
.home-stats li {
  text-align: center;
}
.home-stats li + li {
  border-left: 1px solid var(--lc-line-soft);
}
.home-stats strong {
  display: block;
  font-size: var(--lc-fs-page);
  font-weight: 650;
  color: var(--lc-brand);
}
.home-stats span {
  display: block;
  margin-top: 4px;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

.home-entries {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.home-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 4px;
  font-size: var(--lc-fs-hint);
  font-family: inherit;
  color: var(--lc-ink-body);
  background: var(--lc-surface-soft);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.home-entry:active {
  background: var(--lc-surface-hover);
  border-color: var(--lc-line-hover);
}

.home-empty {
  margin: 6px 0 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

.home-foot {
  margin: 6px 0 0;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
  text-align: center;
}

/* 菜单单元格使用 Vant Cell 默认样式，这里仅收紧间距 */
.home :deep(.van-cell) {
  padding-left: 0;
  padding-right: 0;
}
.home :deep(.van-cell__label) {
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
}
</style>
