<script>
/**
 * 应用根组件
 *
 * 只做三件事：
 *   1. 引入设计令牌与全局样式（页面里不再重复定义色值）；
 *   2. 启动时记录平台与登录态（便于排查"H5 能登录、小程序不行"这类环境差异）；
 *   3. 不在这里做路由守卫 —— 各页面 onShow/onLoad 里判断令牌更可靠
 *      （App.onLaunch 早于首页渲染，reLaunch 容易和首屏竞争）。
 */
import { getToken } from '@/utils/storage'

export default {
  onError(error) {
    // 小程序/H5 的全局错误钩子：至少留下可检索的日志，避免"白屏无线索"
    console.error('[admintem] uncaught error:', error)
  },
  onLaunch() {
    // 平台与令牌状态只打一条，避免刷屏；不打令牌内容
    console.log('[admintem] launch platform=' + process.env.UNI_PLATFORM + ' hasToken=' + !!getToken())
  },
}
</script>

<style>
/* 设计令牌：色板/圆角/投影，与桌面端同一套（尺寸用 rpx） */
@import './styles/tokens.css';

/* ==================== 基础重置 ==================== */
page {
  background-color: var(--c-bg);
  color: var(--c-body);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}

view,
text,
input,
textarea,
button,
image {
  box-sizing: border-box;
}

/* 去掉 uni-app button 默认边框与内边距，交由 .btn 系列接管 */
button {
  margin: 0;
  padding: 0;
  line-height: normal;
  background: transparent;
  border-radius: 0;
  font-size: inherit;
  color: inherit;
  overflow: visible;
}
button::after {
  border: none;
}

/* ==================== 布局工具 ==================== */
.page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.page--tabbar {
  /* tabBar 是 fixed 定位，页面底部要显式预留它的高度（H5 与小程序一致） */
  padding-bottom: calc(200rpx + env(safe-area-inset-bottom));
}

.row {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.between {
  justify-content: space-between;
}

.col {
  display: flex;
  flex-direction: column;
}

.grow {
  flex: 1;
  min-width: 0;
}

.gap-8 {
  gap: 8rpx;
}
.gap-16 {
  gap: 16rpx;
}
.gap-24 {
  gap: 24rpx;
}

.mt-8 { margin-top: 8rpx; }
.mt-16 { margin-top: 16rpx; }
.mt-24 { margin-top: 24rpx; }
.mt-32 { margin-top: 32rpx; }

/* ==================== 文字 ==================== */
.kicker {
  font-size: 22rpx;
  letter-spacing: 4rpx;
  text-transform: uppercase;
  color: var(--c-faint);
}

.title {
  font-size: 44rpx;
  font-weight: 700;
  letter-spacing: -0.5rpx;
  color: var(--c-ink);
}

.subtitle {
  font-size: 32rpx;
  font-weight: 650;
  color: var(--c-ink);
}

.body {
  font-size: 28rpx;
  color: var(--c-body);
}

.muted {
  font-size: 26rpx;
  color: var(--c-muted);
}

.faint {
  font-size: 24rpx;
  color: var(--c-faint);
}

.brand-text {
  color: var(--c-brand);
}

.strong {
  font-weight: 650;
  color: var(--c-ink);
}

/* ==================== 卡片 ==================== */
.card {
  background: var(--c-card);
  border: 1rpx solid var(--c-line);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
  padding: 32rpx;
}

.card + .card {
  margin-top: 24rpx;
}

.card__title {
  display: block;
  font-size: 30rpx;
  font-weight: 650;
  color: var(--c-ink);
  margin-bottom: 8rpx;
}

.card__desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: var(--c-muted);
  margin-bottom: 24rpx;
}

/* ==================== 按钮 ==================== */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 92rpx;
  padding: 0 32rpx;
  border-radius: var(--r-ctl);
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
  transition: transform 0.16s ease, opacity 0.16s ease, box-shadow 0.16s ease;
}

.btn--block {
  width: 100%;
}

.btn--primary {
  color: #ffffff;
  background-color: var(--c-brand);
  background-image: var(--grad-brand);
  border: 1rpx solid var(--c-brand-deep);
  box-shadow: 0 16rpx 36rpx -24rpx rgba(30, 58, 110, 0.55);
}

.btn--primary:active {
  transform: translateY(1rpx);
  opacity: 0.92;
}

.btn--ghost {
  color: var(--c-body);
  background: var(--c-card);
  border: 1rpx solid var(--c-line-input);
}

.btn--ghost:active {
  background: var(--c-hover);
}

.btn--danger-ghost {
  color: var(--c-danger);
  background: var(--c-card);
  border: 1rpx solid #f0d9d9;
}

.btn--disabled {
  opacity: 0.55;
  pointer-events: none;
}

.btn--sm {
  height: 64rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
  border-radius: 16rpx;
  letter-spacing: 0;
}

/* ==================== 表单 ==================== */
.field {
  margin-bottom: 28rpx;
}

.field__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  font-weight: 500;
  color: var(--c-body);
}

.input {
  width: 100%;
  height: 92rpx;
  padding: 0 24rpx;
  background: var(--c-card);
  border: 1rpx solid var(--c-line-input);
  border-radius: var(--r-ctl);
  font-size: 28rpx;
  color: var(--c-ink);
}

.input--focus {
  border-color: var(--c-brand);
}

.textarea {
  width: 100%;
  min-height: 180rpx;
  padding: 20rpx 24rpx;
  background: var(--c-card);
  border: 1rpx solid var(--c-line-input);
  border-radius: var(--r-ctl);
  font-size: 28rpx;
  line-height: 1.6;
  color: var(--c-ink);
}

.input-placeholder {
  color: var(--c-faint);
  font-size: 26rpx;
}

/* 拾取器（性别等）：外观与 input 一致，右侧加箭头 */
.picker-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 92rpx;
  padding: 0 24rpx;
  background: var(--c-card);
  border: 1rpx solid var(--c-line-input);
  border-radius: var(--r-ctl);
  font-size: 28rpx;
  color: var(--c-ink);
}

.chevron {
  width: 14rpx;
  height: 14rpx;
  border-right: 2rpx solid var(--c-faint);
  border-bottom: 2rpx solid var(--c-faint);
  transform: rotate(45deg);
  margin-left: 12rpx;
}

/* ==================== 列表单元 ==================== */
.cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid var(--c-line-soft);
}

.cell:last-child {
  border-bottom: none;
}

.cell__label {
  font-size: 28rpx;
  color: var(--c-body);
}

.cell__value {
  font-size: 26rpx;
  color: var(--c-muted);
  max-width: 420rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ==================== 胶囊 / 标签 ==================== */
.chip {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 18rpx;
  border-radius: var(--r-pill);
  background: var(--c-brand-soft);
  color: var(--c-brand);
  font-size: 22rpx;
  line-height: 1.6;
}

.chip--soft {
  background: var(--c-soft);
  color: var(--c-muted);
  border: 1rpx solid var(--c-line);
}

.chip--success {
  background: var(--c-success-soft);
  color: var(--c-success);
}

/* ==================== 提示条 ==================== */
.notice {
  display: flex;
  gap: 16rpx;
  padding: 22rpx 24rpx;
  border-radius: 20rpx;
  background: var(--c-brand-soft);
  border: 1rpx solid #dfe8f7;
  font-size: 24rpx;
  line-height: 1.7;
  color: var(--c-brand);
}

.notice--muted {
  background: var(--c-soft);
  border-color: var(--c-line);
  color: var(--c-muted);
}

/* ==================== 空态 ==================== */
.empty {
  padding: 48rpx 24rpx;
  text-align: center;
  font-size: 26rpx;
  color: var(--c-faint);
}
</style>
