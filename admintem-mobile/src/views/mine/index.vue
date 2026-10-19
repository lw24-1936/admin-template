<template>
  <!-- 我的（移动端）：个人资料 + 账号信息 + 修改密码 + 退出登录 -->
  <div class="lc-page mine">
    <!-- 资料头 -->
    <section class="mine-hero">
      <div class="mine-hero__avatar">{{ userStore.avatarText }}</div>
      <div class="mine-hero__info">
        <p class="mine-hero__name">{{ userStore.displayName }}</p>
        <p class="mine-hero__account">账号：{{ userStore.user?.userName || '-' }}</p>
      </div>
      <van-tag type="primary" plain>{{ userStore.roleNames[0] || '未分配角色' }}</van-tag>
    </section>

    <h1 class="lc-page__title">个人中心</h1>
    <p class="lc-page__desc">查看本人账号信息、修改登录密码或退出当前登录。</p>

    <!-- 角色与权限 -->
    <section class="lc-card">
      <h2 class="lc-card__title">角色与权限</h2>
      <div class="mine-tags">
        <van-tag v-for="role in userStore.roles" :key="role.id" type="primary" plain>
          {{ role.roleName || role.roleKey }}
        </van-tag>
        <span v-if="!userStore.roles.length" class="mine-empty">未分配角色</span>
      </div>
      <p class="mine-count">
        共 {{ userStore.permissions.length }} 项权限标识、{{ userStore.menus.length }} 个菜单
      </p>
    </section>

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
        <van-cell title="修改密码" is-link @click="openPassword" />
        <van-cell title="关于移动端" is-link @click="showAbout" />
      </van-cell-group>
    </section>

    <van-button block class="mine-logout" :loading="logoutLoading" @click="handleLogout">
      退出登录
    </van-button>

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
  Tag as VanTag,
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
import 'vant/es/tag/style/index'
import { useUserStore } from '@/stores/user'
import * as authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const logoutLoading = ref(false)
const passwordVisible = ref(false)
const passwordLoading = ref(false)

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

.mine-popup {
  padding: 20px 0 4px;
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
</style>
