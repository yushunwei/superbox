<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const store = useUserStore()

onMounted(async () => {
  if (store.checkAuth()) {
    await store.fetchUser()
  }
})

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) store.logout()
    },
  })
}
</script>

<template>
  <view class="mine-page">
    <!-- Profile -->
    <view class="profile-card">
      <view class="avatar">{{ store.user?.displayName?.charAt(0) || 'U' }}</view>
      <view class="profile-info">
        <text class="profile-name">{{ store.user?.displayName || '用户' }}</text>
        <text class="profile-username">@{{ store.user?.username || '---' }}</text>
      </view>
    </view>

    <!-- Menu -->
    <view class="menu-section">
      <view class="menu-item">
        <text>语言</text>
        <text class="menu-value">{{ store.user?.preferredLanguage || 'zh-CN' }}</text>
      </view>
      <view class="menu-item">
        <text>版本</text>
        <text class="menu-value">1.0.0</text>
      </view>
    </view>

    <!-- Logout -->
    <button class="logout-btn" @tap="handleLogout">退出登录</button>
  </view>
</template>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #0d1117;
  padding: 40rpx 32rpx;
}
.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 40rpx;
  background: #161b22;
  border-radius: 20rpx;
  margin-bottom: 32rpx;
}
.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #58a6ff, #3fb950);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}
.profile-info { flex: 1; }
.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #e6edf3;
  margin-bottom: 4rpx;
}
.profile-username {
  font-size: 26rpx;
  color: #8b949e;
}

.menu-section {
  background: #161b22;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 48rpx;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid rgba(255,255,255,0.04);
  font-size: 28rpx;
  color: #e6edf3;
}
.menu-value { color: #8b949e; font-size: 26rpx; }

.logout-btn {
  width: 100%;
  padding: 28rpx;
  background: rgba(248,81,73,0.1);
  color: #f85149;
  border: 1rpx solid rgba(248,81,73,0.2);
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
