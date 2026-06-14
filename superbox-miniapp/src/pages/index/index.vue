<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { taskApi } from '@/api/modules/task'
import { knowledgeApi } from '@/api/modules/knowledge'

const store = useUserStore()
const pendingCount = ref(0)
const entryCount = ref(0)

onMounted(async () => {
  if (!store.checkAuth()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  await store.fetchUser()
  try {
    const [taskRes, knowRes] = await Promise.all([
      taskApi.list({ status: 'not_started', size: 1 }),
      knowledgeApi.list({ size: 1 }),
    ])
    pendingCount.value = taskRes.data.total
    entryCount.value = knowRes.data.total
  } catch { /* ignore */ }
})

const apps = [
  { label: '任务管理', desc: '跟踪和管理日常任务', count: pendingCount, unit: '待办', url: '/pages/task/task', icon: '📋' },
  { label: '知识库', desc: '个人知识管理与检索', count: entryCount, unit: '条目', url: '/pages/knowledge/knowledge', icon: '📚' },
  { label: 'AI 问答', desc: '智能对话 · 深度推理 · 多模型', count: null, unit: '', url: '/pages/chat/chat', icon: '💬', prominent: true },
]

function navigate(url: string) {
  uni.navigateTo({ url })
}
</script>

<template>
  <view class="home-page">
    <!-- Header -->
    <view class="home-header">
      <text class="home-greeting">你好，{{ store.user?.displayName || '用户' }}</text>
      <text class="home-subtitle">今天想做些什么？</text>
    </view>

    <!-- App Cards -->
    <view class="app-grid">
      <view
        v-for="app in apps"
        :key="app.label"
        :class="['app-card', { 'app-card--prominent': app.prominent }]"
        @tap="navigate(app.url)"
      >
        <text class="app-card__icon">{{ app.icon }}</text>
        <view class="app-card__body">
          <text class="app-card__label">{{ app.label }}</text>
          <text class="app-card__desc">{{ app.desc }}</text>
        </view>
        <view v-if="app.count !== null" class="app-card__badge">
          {{ app.count }} {{ app.unit }}
        </view>
      </view>
    </view>

    <!-- Quick Actions -->
    <view class="quick-actions">
      <text class="section-title">快速操作</text>
      <view class="action-row">
        <button class="action-btn" @tap="navigate('/pages/task/task')">＋ 新建任务</button>
        <button class="action-btn" @tap="navigate('/pages/chat/chat')">💬 开始对话</button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #0d1117;
  padding: 40rpx 32rpx;
}
.home-header {
  margin-bottom: 48rpx;
}
.home-greeting {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  color: #e6edf3;
  margin-bottom: 8rpx;
}
.home-subtitle {
  font-size: 28rpx;
  color: #8b949e;
}

.app-grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 48rpx;
}
.app-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 32rpx;
  background: #161b22;
  border: 1rpx solid rgba(255,255,255,0.06);
  border-radius: 20rpx;
}
.app-card--prominent {
  background: linear-gradient(135deg, rgba(88,166,255,0.1), rgba(88,166,255,0.02));
  border-color: rgba(88,166,255,0.2);
}
.app-card__icon {
  font-size: 44rpx;
}
.app-card__body {
  flex: 1;
}
.app-card__label {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #e6edf3;
  margin-bottom: 4rpx;
}
.app-card__desc {
  font-size: 24rpx;
  color: #8b949e;
}
.app-card__badge {
  padding: 8rpx 20rpx;
  background: rgba(88,166,255,0.12);
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #58a6ff;
}

.quick-actions {
  margin-top: 20rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #8b949e;
  margin-bottom: 20rpx;
}
.action-row {
  display: flex;
  gap: 20rpx;
}
.action-btn {
  flex: 1;
  padding: 24rpx;
  background: transparent;
  border: 1rpx solid rgba(255,255,255,0.1);
  border-radius: 16rpx;
  color: #8b949e;
  font-size: 26rpx;
  text-align: center;
}
</style>
