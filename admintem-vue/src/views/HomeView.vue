<template>
  <!-- 工作台：编辑式信息卡——一个卡片承载"我是谁 / 我能做什么 / 权限怎么生效" -->
  <el-card shadow="never" class="home">
    <!-- 顶部：欢迎 + 身份 -->
    <header class="home-head">
      <p class="home-kicker">DASHBOARD · 概览</p>
      <h1 class="home-title">工作台</h1>
      <p class="home-lead">
        {{ greeting }}，{{ store.displayName }}。当前角色为 <strong>{{ roleText }}</strong>，
        可访问 {{ menuCount }} 个菜单、持有 {{ permCount }} 项权限标识。
      </p>
    </header>

    <!-- 数据条：四列，列间以细线分隔（替代四张独立卡片） -->
    <ul class="home-stats">
      <li>
        <span class="home-stats__num">{{ menuCount }}</span>
        <span class="home-stats__label">可访问菜单</span>
      </li>
      <li>
        <span class="home-stats__num">{{ permCount }}</span>
        <span class="home-stats__label">权限标识</span>
      </li>
      <li>
        <span class="home-stats__num">{{ store.roles.length }}</span>
        <span class="home-stats__label">角色数量</span>
      </li>
      <li>
        <span class="home-stats__num">{{ leafMenuCount }}</span>
        <span class="home-stats__label">可进入页面</span>
      </li>
    </ul>

    <!-- 权限口径：三条说明替代默认提示框 -->
    <section class="home-notes">
      <h2 class="home-notes__title">权限如何生效</h2>
      <dl class="home-notes__list">
        <dt>菜单权限（type=0）</dt>
        <dd>决定页面是否可见：未授权的菜单不会下发，路由也不会注册。</dd>
        <dt>按钮权限（type=1）</dt>
        <dd>决定操作是否可用：前端 v-perm 不渲染按钮，后端 @PreAuthorize 二次校验。</dd>
        <dt>动态路由</dt>
        <dd>页面由数据库菜单注册，新增页面只需放组件、登记菜单并授权角色。</dd>
      </dl>
    </section>
  </el-card>
</template>

<script setup>
/**
 * 工作台首页
 * - 展示当前登录用户的身份、可访问范围与权限生效口径
 * - 数据全部来自 /auth/getUserInfo 返回的会话（不额外请求接口）
 */
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const store = useUserStore()

const roleText = computed(() => {
  const names = (store.roles || []).map((r) => r.roleName || r.roleKey).filter(Boolean)
  return names.length ? names.join('、') : '未分配角色'
})

const menuCount = computed(() => (store.menus || []).length)
const permCount = computed(() => (store.permissions || []).length)

/** 可进入的页面数：菜单树中的叶子节点（分组本身不是页面） */
const leafMenuCount = computed(() => {
  let count = 0
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (node.children && node.children.length) walk(node.children)
      else if (node.show !== 1) count++
    }
  }
  walk(store.menus)
  return count
})

/** 当前时间问候语 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
</script>

<style scoped>
.home-head {
  padding-bottom: var(--lc-sp-5);
  border-bottom: 1px solid var(--lc-line-soft);
}
.home-kicker {
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-kicker);
  letter-spacing: 2.4px;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.home-title {
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-page);
  font-weight: 650;
  letter-spacing: 0.4px;
  color: var(--lc-ink);
}
.home-lead {
  margin: 0;
  max-width: 720px;
  font-size: var(--lc-fs-body);
  line-height: 1.9;
  color: var(--lc-ink-muted);
}
.home-lead strong {
  color: var(--lc-ink);
  font-weight: 600;
}

/* 四列数据条：仅用细线分隔，避免"卡片堆卡片" */
.home-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 0;
  padding: var(--lc-sp-5) 0;
  list-style: none;
  border-bottom: 1px solid var(--lc-line-soft);
}
.home-stats li {
  padding: 0 var(--lc-sp-4);
  border-left: 1px solid var(--lc-line-soft);
}
.home-stats li:first-child {
  padding-left: 0;
  border-left: none;
}
.home-stats__num {
  display: block;
  font-size: 28px;
  font-weight: 650;
  line-height: 1.1;
  color: var(--lc-brand);
  font-variant-numeric: tabular-nums;
}
.home-stats__label {
  display: block;
  margin-top: var(--lc-sp-2);
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-muted);
}

.home-notes {
  padding-top: var(--lc-sp-5);
}
.home-notes__title {
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-card);
  font-weight: 650;
  color: var(--lc-ink);
}
.home-notes__list {
  margin: 0;
  display: grid;
  grid-template-columns: 180px 1fr;
  row-gap: var(--lc-sp-3);
  column-gap: var(--lc-sp-4);
  font-size: var(--lc-fs-label);
  line-height: 1.8;
}
.home-notes__list dt {
  color: var(--lc-ink);
  font-weight: 600;
}
.home-notes__list dd {
  margin: 0;
  color: var(--lc-ink-muted);
}

@media (max-width: 900px) {
  .home-stats {
    grid-template-columns: repeat(2, 1fr);
    row-gap: var(--lc-sp-5);
  }
  .home-stats li:nth-child(3) {
    padding-left: 0;
    border-left: none;
  }
  .home-notes__list {
    grid-template-columns: 1fr;
    row-gap: var(--lc-sp-1);
  }
  .home-notes__list dd {
    margin-bottom: var(--lc-sp-3);
  }
}
</style>
