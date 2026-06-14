<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await store.login(username.value, password.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e.message || '登录失败', icon: 'error' })
  } finally {
    loading.value = false
  }
}

async function handleWxLogin() {
  loading.value = true
  try {
    await store.wxLogin()
    uni.showToast({ title: '登录成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e.message || '微信登录失败', icon: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <view class="login-page">
    <view class="login-card">
      <view class="login-header">
        <text class="login-title">Superbox</text>
        <text class="login-subtitle">个人高效工作台</text>
      </view>

      <view class="login-form">
        <input
          v-model="username"
          class="login-input"
          placeholder="用户名"
          placeholder-style="color:#6e7681"
        />
        <input
          v-model="password"
          class="login-input"
          type="password"
          placeholder="密码"
          placeholder-style="color:#6e7681"
          @confirm="handleLogin"
        />
        <button
          class="login-btn"
          :loading="loading"
          :disabled="loading"
          @tap="handleLogin"
        >
          登录
        </button>

        <view class="login-divider">
          <view class="divider-line"></view>
          <text class="divider-text">或</text>
          <view class="divider-line"></view>
        </view>

        <button class="wx-login-btn" @tap="handleWxLogin">
          <text class="wx-icon">💚</text> 微信一键登录
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  background: #0d1117;
}
.login-card {
  width: 100%;
  max-width: 600rpx;
  padding: 60rpx 40rpx;
  background: #161b22;
  border-radius: 24rpx;
  border: 1rpx solid rgba(255,255,255,0.06);
}
.login-header {
  text-align: center;
  margin-bottom: 48rpx;
}
.login-title {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: #e6edf3;
  margin-bottom: 8rpx;
}
.login-subtitle {
  font-size: 26rpx;
  color: #8b949e;
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.login-input {
  width: 100%;
  padding: 24rpx;
  background: #0d1117;
  border: 1rpx solid rgba(255,255,255,0.1);
  border-radius: 16rpx;
  color: #e6edf3;
  font-size: 28rpx;
}
.login-btn {
  width: 100%;
  padding: 24rpx;
  background: #238636;
  color: #fff;
  border: none;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 12rpx;
}
.login-btn[disabled] {
  opacity: 0.5;
}
.login-divider {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 24rpx 0;
}
.divider-line {
  flex: 1;
  height: 1rpx;
  background: rgba(255,255,255,0.08);
}
.divider-text {
  font-size: 24rpx;
  color: #6e7681;
}
.wx-login-btn {
  width: 100%;
  padding: 24rpx;
  background: rgba(7,193,96,0.12);
  color: #07c160;
  border: 1rpx solid rgba(7,193,96,0.2);
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}
.wx-icon {
  font-size: 36rpx;
}
</style>
