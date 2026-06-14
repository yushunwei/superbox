<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { knowledgeApi, type KnowledgeEntry } from '@/api/modules/knowledge'

const entries = ref<KnowledgeEntry[]>([])
const loading = ref(true)

onMounted(() => fetchEntries())

async function fetchEntries() {
  loading.value = true
  try {
    const res = await knowledgeApi.list({ page: 1, size: 30 })
    entries.value = res.data.records
  } finally {
    loading.value = false
  }
}

function getSourceIcon(type: string) {
  const map: Record<string, string> = { file_upload: '📄', manual: '✏️', imported_task: '🔄' }
  return map[type] || '📄'
}

function snippet(text: string | null, max = 80) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}
</script>

<template>
  <view class="knowledge-page">
    <view class="knowledge-list" v-if="!loading">
      <view
        v-for="entry in entries"
        :key="entry.id"
        class="knowledge-card"
      >
        <text class="knowledge-card__title">{{ entry.title }}</text>
        <text class="knowledge-card__snippet">{{ snippet(entry.contentText) }}</text>
        <view class="knowledge-card__meta">
          <text>{{ getSourceIcon(entry.sourceType) }}</text>
          <text v-if="entry.fileType">.{{ entry.fileType }}</text>
          <text v-for="tag in entry.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</text>
        </view>
      </view>
      <view v-if="entries.length === 0" class="empty-state">
        暂无知识条目
      </view>
    </view>
  </view>
</template>

<style scoped>
.knowledge-page {
  min-height: 100vh;
  background: #0d1117;
}
.knowledge-list {
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.knowledge-card {
  padding: 28rpx;
  background: #161b22;
  border: 1rpx solid rgba(255,255,255,0.06);
  border-radius: 16rpx;
}
.knowledge-card__title {
  font-size: 30rpx;
  font-weight: 600;
  color: #e6edf3;
  display: block;
  margin-bottom: 8rpx;
}
.knowledge-card__snippet {
  font-size: 24rpx;
  color: #6e7681;
  display: block;
  margin-bottom: 12rpx;
  line-height: 1.6;
}
.knowledge-card__meta {
  display: flex;
  gap: 12rpx;
  align-items: center;
  font-size: 22rpx;
  color: #8b949e;
}
.tag-chip {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(255,255,255,0.06);
  color: #8b949e;
}
.empty-state {
  text-align: center;
  padding: 80rpx 0;
  color: #6e7681;
  font-size: 28rpx;
}
</style>
