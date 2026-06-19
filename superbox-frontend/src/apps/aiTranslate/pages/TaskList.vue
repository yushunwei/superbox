<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, RefreshRight, Delete } from '@element-plus/icons-vue'
import { useTranslateStore } from '../stores/translateStore'

const store = useTranslateStore()

const page = ref(1)
const pageSize = ref(20)
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await store.fetchTasks(page.value, pageSize.value)
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

function startAutoRefresh() {
  refreshTimer = setInterval(() => {
    const hasProcessing = store.tasks.some(t => t.status === 'processing' || t.status === 'queued')
    if (hasProcessing) {
      store.fetchTasks(page.value, pageSize.value)
    }
  }, 3000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    queued: 'info',
    processing: 'warning',
    completed: 'success',
    failed: 'danger',
  }
  return map[status] || 'info'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    queued: '排队中',
    processing: '翻译中',
    completed: '已完成',
    failed: '失败',
  }
  return map[status] || status
}

async function handleDelete(task: { id: number; fileName: string }) {
  try {
    await ElMessageBox.confirm(`确定要删除「${task.fileName}」的翻译任务吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await store.deleteTask(task.id)
    await store.fetchTasks(page.value, pageSize.value)
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function handleRetry(task: { id: number }) {
  try {
    await store.retryTask(task.id)
    ElMessage.success('已重新加入队列')
    await store.fetchTasks(page.value, pageSize.value)
  } catch {
    ElMessage.error('重试失败')
  }
}

async function handleDownload(task: { id: number; fileName: string }) {
  try {
    const res = await store.downloadTask(task.id)
    const blob = (res as any).data
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = task.fileName.replace(/\.[^.]+$/, '') + '_translated' + task.fileName.match(/\.[^.]+$/)?.[0]
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function onPageChange(p: number) {
  page.value = p
  store.fetchTasks(p, pageSize.value)
}

function onSizeChange(s: number) {
  pageSize.value = s
  page.value = 1
  store.fetchTasks(1, s)
}
</script>

<template>
  <div class="ait-task-list">
    <div class="ait-panel">
      <div class="ait-panel__header">
        <h3 class="ait-panel__title">翻译任务</h3>
        <el-button :icon="RefreshRight" text @click="store.fetchTasks(page, pageSize)">
          刷新
        </el-button>
      </div>

      <el-table
        :data="store.tasks"
        v-loading="store.tasksLoading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="70">
          <template #default="{ row }">
            <span class="ait-file-type">{{ row.fileType?.toUpperCase() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="120">
          <template #default="{ row }">
            {{ row.sourceLang?.toUpperCase() }} → {{ row.targetLang?.toUpperCase() }}
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress || 0"
              :status="row.status === 'failed' ? 'exception' : (row.status === 'completed' ? 'success' : undefined)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'completed'"
              :icon="Download"
              text
              size="small"
              type="primary"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
            <el-button
              v-if="row.status === 'failed'"
              :icon="RefreshRight"
              text
              size="small"
              type="warning"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
            <el-button
              :icon="Delete"
              text
              size="small"
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="ait-pagination" v-if="store.tasksTotal > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="store.tasksTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.ait-task-list {
  max-width: 1100px;
  margin: 0 auto;
}
.ait-panel {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  padding: 20px;
}
.ait-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.ait-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.ait-file-type {
  color: #909399;
  font-size: 12px;
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
}
.ait-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
