<script setup lang="ts">
defineProps<{
  icon: string
  label: string
  desc: string
  badge?: string
  prominent?: boolean
}>()

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <view
    :class="['app-card', { 'app-card--prominent': prominent }]"
    @tap="emit('click')"
  >
    <text class="app-card__icon">{{ icon }}</text>
    <view class="app-card__body">
      <text class="app-card__label">{{ label }}</text>
      <text class="app-card__desc">{{ desc }}</text>
    </view>
    <view v-if="badge" class="app-card__badge">{{ badge }}</view>
  </view>
</template>

<style scoped>
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
.app-card__icon { font-size: 44rpx; }
.app-card__body { flex: 1; }
.app-card__label { display: block; font-size: 30rpx; font-weight: 600; color: #e6edf3; margin-bottom: 4rpx; }
.app-card__desc { font-size: 24rpx; color: #8b949e; }
.app-card__badge {
  padding: 8rpx 20rpx;
  background: rgba(88,166,255,0.12);
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #58a6ff;
}
</style>
