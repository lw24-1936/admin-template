<template>
  <view class="page">
    <!-- 头像 -->
    <view class="card avatar-card">
      <image v-if="form.avatar" class="avatar" :src="avatarSrc" mode="aspectFill" @tap="chooseAvatar" />
      <view v-else class="avatar avatar--text" @tap="chooseAvatar">{{ avatarText }}</view>
      <view class="avatar-card__info grow">
        <text class="strong">头像</text>
        <text class="faint mt-8">支持 png / jpg / webp / gif</text>
      </view>
      <button class="btn btn--ghost btn--sm" @tap="chooseAvatar">
        {{ uploading ? '上传中…' : '更换' }}
      </button>
    </view>

    <!-- 基本资料 -->
    <view class="card">
      <text class="card__title">基本资料</text>
      <text class="card__desc">昵称会显示在顶栏与操作日志中；邮箱/手机号用于联系与找回。</text>

      <view class="field">
        <text class="field__label">昵称</text>
        <input
          v-model="form.nickName"
          class="input"
          :maxlength="32"
          placeholder="请输入昵称"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field">
        <text class="field__label">邮箱</text>
        <input
          v-model="form.email"
          class="input"
          type="text"
          :maxlength="64"
          placeholder="name@example.com"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field">
        <text class="field__label">手机号</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          :maxlength="11"
          placeholder="11 位手机号"
          placeholder-class="input-placeholder"
        />
      </view>

      <view class="field">
        <text class="field__label">性别</text>
        <picker :range="sexOptions" :value="sexIndex" @change="onSexChange">
          <view class="picker-value">
            <text>{{ sexOptions[sexIndex] }}</text>
            <view class="chevron" />
          </view>
        </picker>
      </view>

      <view class="field">
        <text class="field__label">个人介绍</text>
        <textarea
          v-model="form.introduce"
          class="textarea"
          :maxlength="255"
          placeholder="一句话介绍自己（选填）"
          placeholder-class="input-placeholder"
        />
      </view>

      <button class="btn btn--primary btn--block" :class="{ 'btn--disabled': saving }" @tap="handleSave">
        {{ saving ? '保存中…' : '保存修改' }}
      </button>
    </view>

    <view class="notice notice--muted mt-24">
      <text>账号与角色由管理员在桌面端维护，小程序内只能修改自己的资料。</text>
    </view>
  </view>
</template>

<script setup>
/**
 * 个人资料
 *
 * 头像走 /auth/uploadAvatar（multipart，字段名 file），成功后把返回的相对路径
 * 一并写入 updateProfile，保证刷新后仍是新头像。
 */
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { updateProfile, uploadAvatar } from '@/api/auth'
import { user, avatarText, refreshUser } from '@/stores/user'
import { getToken } from '@/utils/storage'
import { resolveAssetUrl } from '@/utils/media'

const sexOptions = ['男', '女']
const sexIndex = ref(0)
const saving = ref(false)
const uploading = ref(false)
const avatarSrc = ref('')

const form = reactive({
  nickName: '',
  email: '',
  phone: '',
  sex: 1,
  introduce: '',
  avatar: '',
})

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function fillFromUser(u) {
  if (!u) return
  form.nickName = u.nickName || ''
  form.email = u.email || ''
  form.phone = u.phone || ''
  form.sex = typeof u.sex === 'number' ? u.sex : 1
  form.introduce = u.introduce || ''
  form.avatar = u.avatar || ''
  avatarSrc.value = resolveAssetUrl(form.avatar)
  sexIndex.value = form.sex === 0 ? 1 : 0
}

onLoad(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  fillFromUser(user.value)
  // 缓存可能是旧的：拉一次最新会话再回填（失败则沿用缓存）
  try {
    const fresh = await refreshUser()
    fillFromUser(fresh)
  } catch (e) {
    // ignore
  }
})

function onSexChange(e) {
  sexIndex.value = Number(e.detail.value)
  form.sex = sexIndex.value === 0 ? 1 : 0
}

function chooseAvatar() {
  if (uploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths && res.tempFilePaths[0]
      if (!filePath) return
      uploading.value = true
      try {
        const resp = await uploadAvatar(filePath)
        const url = (resp && resp.data) || ''
        form.avatar = url
        avatarSrc.value = resolveAssetUrl(url)
        toast('头像已上传，记得保存')
      } catch (e) {
        // 请求层已提示
      } finally {
        uploading.value = false
      }
    },
  })
}

async function handleSave() {
  if (saving.value) return
  if (!form.nickName.trim()) {
    toast('请输入昵称')
    return
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    toast('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickName: form.nickName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      sex: form.sex,
      introduce: form.introduce.trim(),
      avatar: form.avatar,
    })
    await refreshUser()
    uni.showToast({ title: '保存成功', icon: 'success', duration: 900 })
    setTimeout(() => uni.navigateBack(), 700)
  } catch (e) {
    // 请求层已提示
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.avatar-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 24rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  flex: 0 0 120rpx;
  border-radius: 50%;
  background: var(--c-brand-soft);
  border: 1rpx solid var(--c-line);
}

.avatar--text {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-brand);
  font-size: 44rpx;
  font-weight: 700;
}

.avatar-card__info {
  display: flex;
  flex-direction: column;
}
</style>
