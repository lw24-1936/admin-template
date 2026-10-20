<template>
  <!-- 工作台：一屏内回答"我是谁 / 我能去哪 / 权限怎么生效" -->
  <el-card shadow="never" class="home">
    <!-- 顶部：问候 + 日期 -->
    <header class="home-head">
      <div class="home-head__main">
        <p class="home-kicker">DASHBOARD · 概览</p>
        <h1 class="home-title">工作台</h1>
        <p class="home-lead">
          {{ greeting }}，{{ store.displayName }}。当前角色为 <strong>{{ roleText }}</strong>，
          可访问 {{ menuCount }} 个菜单、持有 {{ permCount }} 项权限标识。
        </p>
      </div>
      <p class="home-date">{{ todayText }}</p>
    </header>

    <!-- 指标：四块浅底小卡（数值用 anime.js 做一次 count-up） -->
    <ul class="home-stats">
      <li v-for="item in stats" :key="item.label" class="stat">
        <span class="stat__num" :data-value="item.value">{{ item.value }}</span>
        <span class="stat__label">{{ item.label }}</span>
        <span class="stat__hint">{{ item.hint }}</span>
      </li>
    </ul>

    <div class="home-body">
      <!-- 快捷入口：只列当前账号真正有权限进入的页面 -->
      <section class="home-quick">
        <h2 class="home-sec-title">快捷入口</h2>
        <div v-if="quickMenus.length" class="quick-grid">
          <button
            v-for="menu in quickMenus"
            :key="menu.path"
            type="button"
            class="quick"
            @click="router.push(menu.path)"
          >
            <span class="quick__name">{{ menu.name }}</span>
            <span class="quick__arrow" aria-hidden="true">→</span>
          </button>
        </div>
        <p v-else class="quick-empty">当前账号没有可进入的管理页面，请联系管理员授权。</p>
      </section>

      <!-- 权限口径：三条说明替代默认提示框 -->
      <section class="home-notes">
        <h2 class="home-sec-title">权限如何生效</h2>
        <dl class="home-notes__list">
          <dt>菜单权限（type=0）</dt>
          <dd>决定页面是否可见：未授权的菜单不会下发，路由也不会注册。</dd>
          <dt>按钮权限（type=1）</dt>
          <dd>决定操作是否可用：前端 v-perm 不渲染按钮，后端 @PreAuthorize 二次校验。</dd>
          <dt>动态路由</dt>
          <dd>页面由数据库菜单注册，新增页面只需放组件、登记菜单并授权角色。</dd>
        </dl>
      </section>
    </div>
  </el-card>
</template>

<script setup>
/**
 * 工作台首页
 * - 展示当前登录用户的身份、可访问范围与权限生效口径
 * - 数据全部来自 /auth/getUserInfo 返回的会话（不额外请求接口）
 * - 动效：指标数字 count-up（anime.js v4），尊重 reduced-motion；
 *   首屏 HTML 先渲染真实数值，JS 到位后才从 0 开始动画（避免白屏/闪 0）。
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { animate } from 'animejs'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const router = useRouter()

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

/** 指标卡配置（hint 解释这个数字从哪来，避免"看不懂的仪表盘"） */
const stats = computed(() => [
  { label: '可访问菜单', value: menuCount.value, hint: '当前会话下发的菜单' },
  { label: '权限标识', value: permCount.value, hint: '菜单 + 按钮合计' },
  { label: '角色数量', value: (store.roles || []).length, hint: '已分配的角色' },
  { label: '可进入页面', value: leafMenuCount.value, hint: '叶子菜单（不含分组）' },
])

/** 快捷入口：叶子菜单里能直接跳转的页面，最多 6 个 */
const quickMenus = computed(() => {
  const out = []
  const walk = (nodes) => {
    for (const node of nodes || []) {
      if (node.children && node.children.length) {
        walk(node.children)
        continue
      }
      const path = node.menuUrl || node.path
      if (!path || node.show === 1) continue
      if (path === '/HomeView' || path === '/') continue
      out.push({ name: node.name, path })
    }
  }
  walk(store.menus)
  return out.slice(0, 6)
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

/** 日期行：2026 年 10 月 20 日 · 星期二 */
const todayText = computed(() => {
  const now = new Date()
  const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${now.getFullYear()} 年 ${now.getMonth() + 1} 月 ${now.getDate()} 日 · ${week[now.getDay()]}`
})

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  // 数字滚动：只动 textContent，不动布局尺寸，不引发重排
  document.querySelectorAll('.stat__num').forEach((el) => {
    const target = Number(el.dataset.value || 0)
    if (!Number.isFinite(target) || target === 0) return
    const proxy = { value: 0 }
    el.textContent = '0'
    animate(proxy, {
      value: target,
      duration: 880,
      ease: 'outCubic',
      onUpdate: () => {
        el.textContent = String(Math.round(proxy.value))
      },
    })
  })
})
</script>

<style scoped>
/* ==================== 顶部 ==================== */
.home-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--lc-sp-6);
  padding-bottom: var(--lc-sp-5);
  border-bottom: 1px solid var(--lc-line-soft);
}
.home-head__main {
  min-width: 0;
}
.home-kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-kicker);
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--lc-ink-faint);
}
.home-kicker::before {
  content: '';
  width: 22px;
  height: 1px;
  background: var(--lc-brand);
}
.home-title {
  margin: 0 0 var(--lc-sp-3);
  font-size: var(--lc-fs-page);
  font-weight: 680;
  letter-spacing: -0.02em;
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
.home-date {
  flex: 0 0 auto;
  margin: 4px 0 0;
  padding: 5px 12px;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-pill);
  background: var(--lc-surface-soft);
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

/* ==================== 指标卡 ==================== */
.home-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--lc-sp-3);
  margin: 0;
  padding: var(--lc-sp-5) 0;
  list-style: none;
}
.stat {
  position: relative;
  padding: var(--lc-sp-4) var(--lc-sp-4) var(--lc-sp-3);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-paper-card);
  overflow: hidden;
  transition: border-color var(--lc-motion-fast) var(--lc-ease-out),
    box-shadow var(--lc-motion-fast) var(--lc-ease-out),
    transform var(--lc-motion-fast) var(--lc-ease-out);
}
/* 顶部一条主色渐变细线：hover 时整条点亮，作为"这块可交互"的提示 */
.stat::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 2px;
  background-image: var(--lc-gradient-brand);
  opacity: 0.28;
  transition: opacity var(--lc-motion-fast);
}
.stat:hover {
  border-color: var(--lc-line-hover);
  box-shadow: var(--lc-shadow-card);
  transform: translateY(-2px);
}
.stat:hover::before {
  opacity: 1;
}
.stat__num {
  display: block;
  font-size: 32px;
  font-weight: 680;
  line-height: 1.05;
  letter-spacing: -0.03em;
  color: var(--lc-brand);
  font-variant-numeric: tabular-nums;
}
.stat__label {
  display: block;
  margin-top: var(--lc-sp-2);
  font-size: var(--lc-fs-label);
  font-weight: 500;
  color: var(--lc-ink);
}
.stat__hint {
  display: block;
  margin-top: 2px;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

/* ==================== 两栏主体 ==================== */
.home-body {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: var(--lc-sp-7);
  padding-top: var(--lc-sp-5);
}
.home-sec-title {
  margin: 0 0 var(--lc-sp-4);
  font-size: var(--lc-fs-card);
  font-weight: 650;
  letter-spacing: -0.01em;
  color: var(--lc-ink);
}

/* 快捷入口：浅底磁贴，整块可点（含键盘焦点） */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--lc-sp-2);
}
.quick {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--lc-sp-2);
  padding: 11px 13px;
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-surface-soft);
  color: var(--lc-ink-body);
  font-family: inherit;
  font-size: var(--lc-fs-label);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--lc-motion-fast) var(--lc-ease-out),
    background-color var(--lc-motion-fast) var(--lc-ease-out),
    color var(--lc-motion-fast), transform var(--lc-motion-fast) var(--lc-ease-out);
}
.quick:hover {
  border-color: var(--lc-brand);
  background: var(--lc-brand-soft);
  color: var(--lc-brand);
  transform: translateX(2px);
}
.quick__arrow {
  color: var(--lc-ink-faint);
  transition: transform var(--lc-motion-fast) var(--lc-ease-out), color var(--lc-motion-fast);
}
.quick:hover .quick__arrow {
  color: var(--lc-brand);
  transform: translateX(3px);
}
.quick-empty {
  margin: 0;
  padding: var(--lc-sp-4);
  border: 1px dashed var(--lc-line);
  border-radius: var(--lc-radius-ctl);
  font-size: var(--lc-fs-label);
  color: var(--lc-ink-faint);
}

/* 权限口径 */
.home-notes__list {
  margin: 0;
  display: grid;
  grid-template-columns: 150px 1fr;
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

/* ==================== 响应式 ==================== */
@media (max-width: 1100px) {
  .home-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .home-body {
    grid-template-columns: 1fr;
    gap: var(--lc-sp-6);
  }
}
@media (max-width: 760px) {
  .home-head {
    flex-direction: column;
    gap: var(--lc-sp-3);
  }
  .home-notes__list {
    grid-template-columns: 1fr;
    row-gap: var(--lc-sp-1);
  }
  .home-notes__list dd {
    margin-bottom: var(--lc-sp-3);
  }
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
