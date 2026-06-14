<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { taskApi, type Task } from '@/api/modules/task'

const tasks = ref<Task[]>([])
const loading = ref(true)
const activeTab = ref<'not_started' | 'in_progress' | 'completed'>('not_started')

const tabs = [
  { key: 'not_started' as const, label: '待办' },
  { key: 'in_progress' as const, label: '进行中' },
  { key: 'completed' as const, label: '已完成' },
]

onMounted(() => fetchTasks())

async function fetchTasks() {
  loading.value = true
  try {
    const res = await taskApi.list({ page: 1, size: 50 })
    tasks.value = res.data.records
  } finally {
    loading.value = false
  }
}

const filteredTasks = () => tasks.value.filter(t => t.status === activeTab.value)

function getPriorityClass(p: string) {
  const map: Record<string, string> = { normal: 'pri-normal', important: 'pri-important' }
  return map[p] || ''
}

async function changeStatus(id: number, status: string) {
  await taskApi.updateStatus(id, status)
  await fetchTasks()
  uni.showToast({ title: '状态已更新', icon: 'success' })
}
</script>

<template>
  <view class="task-page">
    <!-- Tab Bar -->
    <view class="task-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        :class="['task-tab', { active: activeTab === tab.key }]"
        @tap="activeTab = tab.key"
      >
        {{ tab.label }}
      </view>
    </view>

    <!-- Task List -->
    <view class="task-list" v-if="!loading">
      <view
        v-for="task in filteredTasks()"
        :key="task.id"
        class="task-item"
      >
        <view class="task-item__left">
          <view :class="['priority-dot', getPriorityClass(task.priority)]"></view>
          <view class="task-item__body">
            <text class="task-item__title">{{ task.title }}</text>
            <view class="task-item__meta">
              <text v-if="task.planEndDate" class="meta-text">📅 {{ task.planEndDate }}</text>
              <text v-for="tag in task.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</text>
            </view>
          </view>
        </view>
        <view class="task-item__actions">
          <button
            v-if="activeTab === 'not_started'"
            class="status-btn start-btn"
            @tap="changeStatus(task.id, 'in_progress')"
          >开始</button>
          <button
            v-if="activeTab === 'in_progress'"
            class="status-btn done-btn"
            @tap="changeStatus(task.id, 'completed')"
          >完成</button>
        </view>
      </view>
      <view v-if="filteredTasks().length === 0" class="empty-state">
        暂无任务
      </view>
    </view>
  </view>
</template>

<style scoped>
.task-page {
  min-height: 100vh;
  background: #0d1117;
}
.task-tabs {
  display: flex;
  background: #161b22;
  border-bottom: 1rpx solid rgba(255,255,255,0.06);
  position: sticky;
  top: 0;
  z-index: 10;
}
.task-tab {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  font-size: 28rpx;
  color: #8b949e;
  border-bottom: 3rpx solid transparent;
  transition: all .2s;
}
.task-tab.active {
  color: #58a6ff;
  border-bottom-color: #58a6ff;
}

.task-list {
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx;
  background: #161b22;
  border: 1rpx solid rgba(255,255,255,0.06);
  border-radius: 16rpx;
}
.task-item__left {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
  flex: 1;
  overflow: hidden;
}
.priority-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-top: 8rpx;
  flex-shrink: 0;
}
.pri-normal { background: #3fb950; }
.pri-important { background: #f85149; }
.task-item__body {
  flex: 1;
  overflow: hidden;
}
.task-item__title {
  font-size: 28rpx;
  color: #e6edf3;
  display: block;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-item__meta {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}
.meta-text {
  font-size: 22rpx;
  color: #6e7681;
}
.tag-chip {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(255,255,255,0.06);
  color: #8b949e;
}
.task-item__actions {
  flex-shrink: 0;
  margin-left: 16rpx;
}
.status-btn {
  padding: 10rpx 24rpx;
  border-radius: 12rpx;
  font-size: 24rpx;
  border: none;
}
.start-btn {
  background: rgba(88,166,255,0.15);
  color: #58a6ff;
}
.done-btn {
  background: rgba(63,185,80,0.15);
  color: #3fb950;
}

.empty-state {
  text-align: center;
  padding: 80rpx 0;
  color: #6e7681;
  font-size: 28rpx;
}
</style>
