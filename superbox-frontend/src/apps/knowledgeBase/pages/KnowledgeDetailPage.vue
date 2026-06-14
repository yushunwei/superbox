<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useKnowledgeStore } from '@/stores/modules/knowledgeBaseStore'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const store = useKnowledgeStore()

const entryId = computed(() => Number(route.params.id))
const editing = ref(false)
const loading = ref(false)

const form = ref({
  title: '',
  content: '',
  folderId: null as number | null,
})

onMounted(loadEntry)

async function loadEntry() {
  loading.value = true
  try {
    const entry = await store.fetchEntry(entryId.value)
    form.value = {
      title: entry.title,
      content: entry.contentText || '',
      folderId: entry.folderId,
    }
  } finally {
    loading.value = false
  }
}

function startEdit() {
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  if (store.currentEntry) {
    form.value = {
      title: store.currentEntry.title,
      content: store.currentEntry.contentText || '',
      folderId: store.currentEntry.folderId,
    }
  }
}

async function saveEdit() {
  if (!form.value.title.trim()) return
  await store.updateEntry(entryId.value, {
    title: form.value.title,
    content: form.value.content,
    folderId: form.value.folderId,
  })
  editing.value = false
  ElMessage.success('知识已更新')
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条知识吗？', '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    await store.deleteEntry(entryId.value)
    router.push('/app/knowledge-base')
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

function getSourceLabel(type: string) {
  const map: Record<string, string> = { file_upload: '文件上传', manual: '手工录入', imported_task: '任务导入' }
  return map[type] || type
}

function formatDate(s: string) {
  if (!s) return ''
  return new Date(s).toLocaleDateString('zh-CN')
}

function formatFileSize(bytes: number | null) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <template v-if="store.currentEntry">
      <div class="detail-header">
        <div class="detail-header__left">
          <template v-if="!editing">
            <h1 class="detail-title">{{ store.currentEntry.title }}</h1>
          </template>
          <template v-else>
            <input v-model="form.title" class="edit-input-lg" placeholder="知识标题" />
          </template>
          <div class="detail-meta">
            <span class="meta-chip">{{ getSourceLabel(store.currentEntry.sourceType) }}</span>
            <span v-if="store.currentEntry.fileType" class="meta-chip">.{{ store.currentEntry.fileType }}</span>
            <span v-if="store.currentEntry.fileSize" class="meta-chip">{{ formatFileSize(store.currentEntry.fileSize) }}</span>
            <span v-if="store.currentEntry.chunkCount" class="meta-chip">{{ store.currentEntry.chunkCount }} 个分块</span>
            <span class="meta-chip">创建于 {{ formatDate(store.currentEntry.createdAt) }}</span>
          </div>
        </div>
        <div class="detail-header__actions">
          <template v-if="!editing">
            <el-button @click="startEdit">编辑</el-button>
            <el-button type="danger" plain @click="handleDelete">删除</el-button>
          </template>
          <template v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveEdit">保存</el-button>
          </template>
        </div>
      </div>

      <div class="detail-body">
        <div class="detail-section">
          <h3 class="section-label">内容</h3>
          <template v-if="!editing">
            <div v-if="store.currentEntry.contentText" class="content-text">{{ store.currentEntry.contentText }}</div>
            <p v-else class="empty-hint">暂无内容</p>
          </template>
          <template v-else>
            <textarea v-model="form.content" class="edit-textarea" rows="12" placeholder="知识内容（支持 Markdown）"></textarea>
            <div class="edit-props mt-3">
              <label class="prop-row">
                <span>目录</span>
                <select v-model="form.folderId" class="prop-select">
                  <option :value="null">无</option>
                  <option v-for="f in store.folders" :key="f.id" :value="f.id">{{ f.name }}</option>
                </select>
              </label>
            </div>
          </template>
        </div>

        <!-- Chunks -->
        <div v-if="store.currentEntry.chunks?.length" class="detail-section">
          <h3 class="section-label">知识分块 ({{ store.currentEntry.chunks.length }})</h3>
          <div class="chunk-list">
            <div v-for="chunk in store.currentEntry.chunks" :key="chunk.id" class="chunk-item">
              <div class="chunk-item__index">#{{ chunk.chunkIndex + 1 }}</div>
              <div class="chunk-item__content">{{ chunk.content }}</div>
              <div class="chunk-item__tokens">{{ chunk.tokenCount }} tokens</div>
            </div>
          </div>
        </div>

        <!-- Tags -->
        <div v-if="store.currentEntry.tags?.length" class="detail-section">
          <h3 class="section-label">标签</h3>
          <div class="tag-list">
            <span v-for="tag in store.currentEntry.tags" :key="tag.id" class="tag-chip" :style="{ borderColor: tag.color, color: tag.color }">
              {{ tag.name }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="!loading" class="not-found">
      <p>知识条目不存在</p>
      <el-button @click="router.push('/app/knowledge-base')">返回列表</el-button>
    </div>
  </div>
</template>

<style scoped>
.detail-page { padding:24px 32px; overflow-y:auto; height:100%; max-width:900px; }

.detail-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:32px; gap:16px; }
.detail-header__left { flex:1; }
.detail-title { font-size:22px; font-weight:600; color:#1e293b; margin:0 0 8px 0; }
.edit-input-lg {
  width:100%; padding:8px 12px; border-radius:8px; border:1px solid #3b82f6;
  background:#ffffff; color:#1e293b; font-size:22px; font-weight:600; outline:none; margin-bottom:8px;
  font-family:inherit;
}
.detail-meta { display:flex; gap:8px; flex-wrap:wrap; }
.meta-chip {
  font-size:12px; padding:2px 10px; border-radius:12px;
  background:#f1f5f9; color:#64748b;
}
.detail-header__actions { display:flex; gap:8px; flex-shrink:0; }

.detail-body { display:flex; flex-direction:column; gap:24px; }
.detail-section { }
.section-label { font-size:13px; font-weight:600; color:#94a3b8; margin:0 0 10px 0; text-transform:uppercase; letter-spacing:0.5px; }
.content-text { font-size:14px; line-height:1.9; color:#334155; white-space:pre-wrap; word-break:break-word; }
.empty-hint { color:#94a3b8; font-style:italic; }

.edit-textarea {
  width:100%; padding:12px; border-radius:8px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:14px; outline:none; resize:vertical;
  font-family:inherit; line-height:1.7;
}
.edit-textarea:focus { border-color:#3b82f6; }

.mt-3 { margin-top:12px; }
.edit-props { display:flex; flex-direction:column; gap:10px; }
.prop-row { display:flex; align-items:center; gap:12px; font-size:13px; color:#64748b; }
.prop-select {
  padding:6px 10px; border-radius:6px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:13px; outline:none;
}
.prop-select:focus { border-color:#3b82f6; }

.chunk-list { display:flex; flex-direction:column; gap:8px; }
.chunk-item {
  padding:12px; border-radius:8px; background:#f8fafc;
  border:1px solid #e2e8f0; display:flex; gap:12px; align-items:flex-start;
}
.chunk-item__index { font-size:11px; color:#94a3b8; flex-shrink:0; min-width:24px; }
.chunk-item__content { font-size:12px; color:#64748b; line-height:1.6; flex:1; word-break:break-word; max-height:100px; overflow-y:auto; }
.chunk-item__tokens { font-size:10px; color:#94a3b8; flex-shrink:0; }

.tag-list { display:flex; gap:6px; flex-wrap:wrap; }
.tag-chip {
  font-size:12px; padding:3px 10px; border-radius:12px;
  background:#f8fafc; border:1px solid #e2e8f0;
}

.not-found { text-align:center; padding:60px 0; color:#94a3b8; }
</style>
