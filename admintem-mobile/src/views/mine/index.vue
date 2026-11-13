<template>
  <!-- 我的（移动端）：个人资料 + 账号信息 + 修改密码 + 退出登录 -->
  <div class="lc-page mine">
    <!-- 资料头 -->
    <section class="mine-hero">
      <div class="mine-hero__top">
        <div class="mine-hero__avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ userStore.avatarText }}</span>
        </div>
        <div class="mine-hero__info">
          <p class="mine-hero__name">{{ userStore.displayName }}</p>
          <p class="mine-hero__account">@{{ userStore.user?.userName || '-' }}</p>
        </div>
        <span class="mine-hero__status">{{ statusText }}</span>
      </div>

      <div class="mine-hero__roles">
        <span v-for="name in userStore.roleNames" :key="name" class="mine-hero__chip">{{ name }}</span>
        <span v-if="!userStore.roleNames.length" class="mine-hero__chip">未分配角色</span>
      </div>

      <dl class="mine-hero__stats">
        <div>
          <dt>{{ userStore.menus.length }}</dt>
          <dd>菜单</dd>
        </div>
        <div>
          <dt>{{ userStore.permissions.length }}</dt>
          <dd>权限</dd>
        </div>
        <div>
          <dt>{{ userStore.roles.length }}</dt>
          <dd>角色</dd>
        </div>
      </dl>
    </section>

    <h1 class="lc-page__title">个人中心</h1>
    <p class="lc-page__desc">查看本人账号信息、修改登录密码或退出当前登录。</p>

    <!-- 账号信息 -->
    <section class="lc-card">
      <h2 class="lc-card__title">账号信息</h2>
      <van-cell-group :border="false">
        <van-cell title="登录账号" :value="userStore.user?.userName || '-'" />
        <van-cell title="昵称" :value="userStore.user?.nickName || '-'" />
        <van-cell title="邮箱" :value="userStore.user?.email || '-'" />
        <van-cell title="手机号" :value="userStore.user?.phone || '-'" />
        <van-cell title="账号状态" :value="statusText" />
      </van-cell-group>
    </section>

    <!-- 功能入口 -->
    <section class="lc-card">
      <h2 class="lc-card__title">账号操作</h2>
      <van-cell-group :border="false">
        <van-cell title="编辑资料" icon="edit" is-link @click="openProfile" />
        <van-cell title="修改密码" icon="lock" is-link @click="openPassword" />
        <van-cell title="关于移动端" icon="info-o" is-link @click="showAbout" />
      </van-cell-group>
    </section>

    <van-button block class="mine-logout" :loading="logoutLoading" @click="handleLogout">
      退出登录
    </van-button>

    <p class="mine-foot">admintem 移动端 v0.1.0 · 后端共享 admintem-backend</p>

    <!-- 编辑资料弹层 -->
    <van-popup v-model:show="profileVisible" position="bottom" round :style="{ paddingBottom: '20px' }">
      <div class="mine-popup">
        <h3 class="mine-popup__title">编辑资料</h3>
        <div class="profile-avatar-row">
          <!-- 用原生 file input + 真实 button：Vant 自定义插槽不会自动打开文件选择，
               且 div 包装对键盘不可用；保留 Vant 的视觉，交互用原生控件保证可访问性 -->
          <input
            ref="avatarInput"
            class="profile-avatar-input"
            type="file"
            tabindex="-1"
            aria-hidden="true"
            accept="image/png,image/jpeg,image/webp,image/gif"
            @change="onAvatarChange"
          />
          <button
            type="button"
            class="profile-avatar-btn"
            :disabled="avatarUploading"
            aria-label="更换头像"
            @click="openAvatarPicker"
          >
            <span class="profile-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="当前头像" />
              <span v-else>{{ userStore.avatarText }}</span>
            </span>
          </button>
          <div class="profile-avatar__hint">
            <strong>点击更换头像</strong>
            <span>支持 png / jpg / webp / gif，不超过 2MB</span>
            <span v-if="avatarUploading" class="profile-avatar__status" role="status">上传中...</span>
          </div>
        </div>
        <van-form @submit="submitProfile">
          <van-cell-group inset>
            <van-field
              v-model="profileForm.nickName"
              name="nickName"
              label="昵称"
              maxlength="32"
              placeholder="请输入昵称"
              :rules="[{ required: true, message: '请输入昵称' }]"
            />
            <van-field
              v-model="profileForm.email"
              name="email"
              label="邮箱"
              type="email"
              placeholder="选填，name@example.com"
              :rules="[{ validator: validateEmail, message: '邮箱格式不正确' }]"
            />
            <van-field
              v-model="profileForm.phone"
              name="phone"
              label="手机号"
              type="tel"
              maxlength="11"
              placeholder="选填，11 位手机号"
              :rules="[{ validator: validatePhone, message: '手机号格式不正确' }]"
            />
            <van-field label="性别">
              <template #input>
                <van-radio-group v-model="profileForm.sex" direction="horizontal">
                  <van-radio :name="1">男</van-radio>
                  <van-radio :name="0">女</van-radio>
                </van-radio-group>
              </template>
            </van-field>
            <van-field
              v-model="profileForm.introduce"
              name="introduce"
              label="个人简介"
              type="textarea"
              rows="2"
              maxlength="255"
              show-word-limit
              placeholder="选填，最多 255 个字符"
            />
          </van-cell-group>
          <div class="mine-popup__actions">
            <van-button block plain @click="profileVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="profileLoading">
              保存资料
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <!-- 修改密码弹层 -->
    <van-popup v-model:show="passwordVisible" position="bottom" round :style="{ paddingBottom: '20px' }">
      <div class="mine-popup">
        <h3 class="mine-popup__title">修改密码</h3>
        <van-form @submit="submitPassword">
          <van-cell-group inset>
            <van-field
              v-model="passwordForm.oldPassword"
              type="password"
              label="原密码"
              placeholder="请输入原密码"
              :rules="[{ required: true, message: '请输入原密码' }]"
            />
            <van-field
              v-model="passwordForm.newPassword"
              type="password"
              label="新密码"
              placeholder="6~32 位"
              :rules="[
                { required: true, message: '请输入新密码' },
                { validator: (v) => v.length >= 6 && v.length <= 32, message: '新密码长度须为6~32位' },
              ]"
            />
            <van-field
              v-model="passwordForm.confirmPassword"
              type="password"
              label="确认密码"
              placeholder="请再次输入新密码"
              :rules="[
                { required: true, message: '请再次输入新密码' },
                { validator: (v) => v === passwordForm.newPassword, message: '两次输入的密码不一致' },
              ]"
            />
          </van-cell-group>
          <div class="mine-popup__actions">
            <van-button block plain @click="passwordVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="passwordLoading">
              确认修改
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
/**
 * 我的（个人中心）
 * - 展示当前会话信息（复用 /auth/getUserInfo）
 * - 修改密码调用 /auth/updatePassword，成功后服务端会清理令牌，需重新登录
 * - 退出登录调用 /auth/logout 并清理本地登录态
 */
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Button as VanButton,
  Cell as VanCell,
  CellGroup as VanCellGroup,
  Field as VanField,
  Form as VanForm,
  Popup as VanPopup,
  Radio as VanRadio,
  RadioGroup as VanRadioGroup,
  showConfirmDialog,
  showSuccessToast,
  showToast,
} from 'vant'
import 'vant/es/button/style/index'
import 'vant/es/cell/style/index'
import 'vant/es/cell-group/style/index'
import 'vant/es/field/style/index'
import 'vant/es/form/style/index'
import 'vant/es/popup/style/index'
import 'vant/es/radio/style/index'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

/** 头像：兼容本地相对路径与 MinIO 绝对地址 */
const avatarUrl = computed(() => {
  const path = userStore.user?.avatar
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  return API_BASE + (path.startsWith('/') ? path : '/' + path)
})

const logoutLoading = ref(false)
const passwordVisible = ref(false)
const passwordLoading = ref(false)
const profileVisible = ref(false)
const profileLoading = ref(false)
const avatarUploading = ref(false)
const avatarInput = ref(null)

const profileForm = reactive({
  nickName: '',
  email: '',
  phone: '',
  sex: 1,
  introduce: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

/** 账号状态：0 启用，1 禁用 */
const statusText = computed(() => {
  const status = userStore.user?.status
  if (status === 1) return '已禁用'
  return '正常'
})

/** 打开编辑资料弹层，回填当前用户资料 */
function openProfile() {
  const user = userStore.user || {}
  profileForm.nickName = user.nickName || ''
  profileForm.email = user.email || ''
  profileForm.phone = user.phone || ''
  profileForm.sex = user.sex === 0 ? 0 : 1
  profileForm.introduce = user.introduce || ''
  profileForm.avatar = user.avatar || ''
  profileVisible.value = true
}

/** 选图前校验：类型与大小（后端同样会校验） */
function isValidAvatar(file) {
  if (!file || !String(file.type || '').startsWith('image/')) {
    showToast('请选择图片文件')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    showToast('头像不能超过 2MB')
    return false
  }
  return true
}

function openAvatarPicker() {
  if (!avatarUploading.value) avatarInput.value?.click()
}

/** 头像上传成功即写入用户表；这里同步本地会话，避免需要重新登录 */
async function onAvatarChange(event) {
  const input = event.target
  const file = input?.files?.[0]
  if (input) input.value = ''
  if (!file || !isValidAvatar(file) || avatarUploading.value) return
  avatarUploading.value = true
  try {
    const res = await authApi.uploadAvatar(file)
    profileForm.avatar = res.data || ''
    if (userStore.user) userStore.user.avatar = res.data || ''
    showSuccessToast('头像已更新')
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    avatarUploading.value = false
  }
}

/** 邮箱选填：为空放行，非空按后端 @Email 规则校验 */
function validateEmail(value) {
  if (!value) return true
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

/** 手机号选填：为空放行，非空按后端 ^1\d{10}$ 校验 */
function validatePhone(value) {
  if (!value) return true
  return /^1\d{10}$/.test(value)
}

/** 保存资料：与桌面端共用 /auth/updateProfile */
async function submitProfile() {
  profileLoading.value = true
  try {
    await authApi.updateProfile({
      nickName: profileForm.nickName,
      email: profileForm.email,
      phone: profileForm.phone,
      sex: profileForm.sex,
      introduce: profileForm.introduce,
      avatar: profileForm.avatar,
    })
    await userStore.fetchUserInfo()
    profileVisible.value = false
    showSuccessToast('资料已保存')
  } catch (e) {
    // 错误提示由 request 拦截器统一处理
  } finally {
    profileLoading.value = false
  }
}

/** 打开修改密码弹层并重置表单 */
function openPassword() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

/** 提交修改密码 */
async function submitPassword() {
  passwordLoading.value = true
  try {
    await authApi.updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordVisible.value = false
    showSuccessToast('密码修改成功，请重新登录')
    userStore.clearSession()
    router.replace('/login')
  } catch (e) {
    // 错误提示已由 request 拦截器统一弹出
  } finally {
    passwordLoading.value = false
  }
}

/** 关于信息 */
function showAbout() {
  showToast({ message: 'admintem 移动端 v0.1.0（基础版）', position: 'bottom' })
}

/** 退出登录（二次确认） */
async function handleLogout() {
  try {
    await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？' })
  } catch (e) {
    // 用户取消
    return
  }
  logoutLoading.value = true
  try {
    await userStore.logout()
    showSuccessToast('已退出登录')
    router.replace('/login')
  } finally {
    logoutLoading.value = false
  }
}
</script>

<style scoped>
.mine-hero {
  display: flex;
  align-items: center;
  gap: var(--lc-sp-3);
  padding: 18px 16px;
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-line);
  border-radius: var(--lc-radius-card);
}
.mine-hero__avatar {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 50%;
  background: var(--lc-brand);
  color: var(--lc-paper-card);
  font-size: 21px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.mine-hero__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.mine-hero__top {
  display: flex;
  align-items: center;
  gap: var(--lc-sp-3);
}
.mine-hero__status {
  flex: 0 0 auto;
  padding: 4px 9px;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-paper-card);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 999px;
}
.mine-hero__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}
.mine-hero__chip {
  padding: 3px 9px;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-paper-card);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
}
.mine-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 14px 0 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}
.mine-hero__stats div {
  text-align: center;
}
.mine-hero__stats dt {
  font-size: 22px;
  font-weight: 680;
  color: var(--lc-paper-card);
  font-variant-numeric: tabular-nums;
}
.mine-hero__stats dd {
  margin: 3px 0 0;
  font-size: var(--lc-fs-hint);
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.78);
}
.mine .lc-page__title {
  margin-top: 18px;
}
.mine-hero__info {
  flex: 1;
  min-width: 0;
}
.mine-hero__name {
  margin: 0 0 4px;
  font-size: var(--lc-fs-title);
  font-weight: 600;
  color: var(--lc-ink);
}
.mine-hero__account {
  margin: 0;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

.mine-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--lc-sp-2);
}
.mine-count {
  margin: 12px 0 0;
  padding-top: 10px;
  border-top: 1px solid var(--lc-line-soft);
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}
.mine-empty {
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-faint);
}

.mine :deep(.van-cell) {
  padding-left: 0;
  padding-right: 0;
  font-size: var(--lc-fs-body);
}
.mine :deep(.van-cell__value) {
  color: var(--lc-ink-muted);
}

.mine-logout {
  margin-top: 4px;
  height: var(--lc-control-h);
  font-size: var(--lc-fs-lead);
  color: var(--lc-danger);
  background: var(--lc-paper-card);
  border: 1px solid var(--lc-danger-line);
  border-radius: var(--lc-radius-ctl);
}
.mine-logout:active {
  background: var(--lc-danger-soft);
}
.mine-foot {
  margin: 14px 0 0;
  text-align: center;
  font-size: var(--lc-fs-kicker);
  color: var(--lc-ink-faint);
}

.profile-avatar-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}
.profile-avatar-btn {
  display: block;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}
.profile-avatar-btn:disabled {
  cursor: wait;
  opacity: 0.6;
}
.profile-avatar__status {
  color: var(--lc-brand);
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 16px 16px;
}
.profile-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  font-size: 24px;
  font-weight: 600;
  color: var(--lc-paper-card);
  background: var(--lc-brand);
  border: 1px solid var(--lc-line);
  border-radius: 50%;
}
.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.profile-avatar__hint {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: var(--lc-fs-hint);
  color: var(--lc-ink-muted);
}
.profile-avatar__hint strong {
  font-size: var(--lc-fs-label);
  font-weight: 600;
  color: var(--lc-ink);
}

.mine-popup {
  padding: 20px 0 12px;
}
.mine-popup__title {
  margin: 0 16px 14px;
  font-size: var(--lc-fs-card);
  font-weight: 600;
  color: var(--lc-ink);
}
.mine-popup__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--lc-sp-3);
  margin: 18px 16px 0;
}

/* ===== V2 视觉覆盖（后置规则，优先级高于前面同名选择器） ===== */
/* 资料头：与首页英雄卡同一渐变语言 */
/* 新的资料头是"上中下"三段：顶部信息 / 角色 / 指标，不能再沿用横向 flex */
.mine-hero {
  display: block;
  border: 1px solid var(--lc-brand-deep);
  background-color: var(--lc-brand);
  background-image: var(--lc-gradient-brand);
  box-shadow: 0 16px 32px -22px var(--lc-brand-glow);
}
.mine-hero__avatar {
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.34);
  color: var(--lc-paper-card);
}
.mine-hero__name {
  color: var(--lc-paper-card);
  font-weight: 650;
}
.mine-hero__account {
  color: rgba(255, 255, 255, 0.78);
}
.mine :deep(.van-cell__left-icon) {
  color: var(--lc-brand);
  margin-right: 8px;
}
.mine-hero :deep(.van-tag--primary.van-tag--plain) {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.42);
  color: var(--lc-paper-card);
}

/* 退出登录：白底危险色文字，避免整屏唯一的实心红按钮 */
.mine-logout {
  height: 46px;
  border: 1px solid var(--lc-danger-line);
  border-radius: var(--lc-radius-ctl);
  background: var(--lc-paper-card);
  color: var(--lc-danger);
  font-weight: 600;
}
</style>
