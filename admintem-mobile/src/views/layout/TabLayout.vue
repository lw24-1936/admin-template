<template>
  <!-- 登录后的主布局：内容区 + 底部标签栏（首页 / 我的） -->
  <div class="tab-layout">
    <div class="tab-layout__body">
      <router-view />
    </div>

    <van-tabbar route fixed safe-area-inset-bottom>
      <van-tabbar-item replace to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/mine" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
/**
 * 主布局
 * - van-tabbar 使用 route 模式：高亮项自动跟随当前路由
 * - 标签栏固定于视口底部；内容区由 .lc-page 的底部内边距预留空间，避免被遮挡
 * - Vant 组件按需引入（别名 VanXxx 以便模板沿用 <van-xxx> 标签）
 */
import {
  Tabbar as VanTabbar,
  TabbarItem as VanTabbarItem,
} from 'vant'
import 'vant/es/tabbar/style/index'
import 'vant/es/tabbar-item/style/index'
</script>

<style scoped>
.tab-layout {
  min-height: 100vh;
  min-height: 100dvh;
  background: transparent;
}

.tab-layout__body {
  min-height: 100vh;
  min-height: 100dvh;
}

.tab-layout :deep(.van-tabbar) {
  /* 半透明白 + 背景虚化：内容滚到底部时会从栏下透出，比纯白更"轻" */
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px) saturate(1.1);
  border-top: 1px solid var(--lc-line);
  box-shadow: 0 -8px 24px -20px rgba(15, 23, 42, 0.3);
  /* 固定在视口底部，并在宽屏下与 480px 内容容器对齐 */
  max-width: 480px;
  margin: 0 auto;
}

/* 选中项：主色 + 文字加粗 + 图标轻微上浮（不动布局尺寸） */
.tab-layout :deep(.van-tabbar-item) {
  color: var(--lc-ink-muted);
  transition: color var(--lc-motion-fast) var(--lc-ease-out);
}
.tab-layout :deep(.van-tabbar-item--active) {
  color: var(--lc-brand);
  font-weight: 600;
}
.tab-layout :deep(.van-tabbar-item__icon) {
  font-size: 20px;
  transition: transform var(--lc-motion-fast) var(--lc-ease-out);
}
.tab-layout :deep(.van-tabbar-item--active .van-tabbar-item__icon) {
  transform: translateY(-1px);
}
.tab-layout :deep(.van-tabbar-item__text) {
  font-size: var(--lc-fs-hint);
  letter-spacing: 0.02em;
}
</style>
