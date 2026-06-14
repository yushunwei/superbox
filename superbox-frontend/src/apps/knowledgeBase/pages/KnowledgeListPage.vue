<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useKnowledgeStore } from '@/stores/modules/knowledgeBaseStore'
import { ElMessageBox, ElMessage } from 'element-plus'

const router = useRouter()
const store = useKnowledgeStore()

const activeFolderId = ref<number | null>(null)
const newFolderName = ref('')
const showNewFolder = ref(false)
const creatingInFolderId = ref<number | null>(null)

onMounted(async () => {
  await store.fetchFolders()
  await store.fetchEntries()
})

function selectFolder(id: number | null) {
  activeFolderId.value = id
  store.fetchEntries({ folderId: id ?? undefined, page: 1 })
}

function goToDetail(id: number) {
  router.push(`/app/knowledge/${id}`)
}

function goToUpload() {
  router.push('/app/knowledge-base/upload')
}

async function createFolder(parentId: number | null) {
  if (!newFolderName.value.trim()) return
  await store.createFolder({ name: newFolderName.value.trim(), parentId })
  newFolderName.value = ''
  showNewFolder.value = false
  ElMessage.success('目录已创建')
}

async function handleDeleteEntry(id: number, title: string) {
  try {
    await ElMessageBox.confirm(`确定要删除「${title}」吗？`, '确认删除', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    await store.deleteEntry(id)
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

function getSourceIcon(type: string) {
  const map: Record<string, string> = { file_upload: '📄', manual: '✏️', imported_task: '🔄' }
  return map[type] || '📄'
}

function formatDate(s: string) {
  if (!s) return ''
  return new Date(s).toLocaleDateString('zh-CN')
}

function snippet(text: string | null, max = 120) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}
</script>

<template>
  <div class="knowledge-layout">
    <!-- Folder Tree -->
    <aside class="folder-panel">
      <div class="folder-panel__header">
        知识目录
        <button class="add-folder-btn" @click="showNewFolder = !showNewFolder; creatingInFolderId = null">＋</button>
      </div>
      <div class="folder-tree">
        <div :class="['folder-item', { active: activeFolderId === null }]" @click="selectFolder(null)">
          <span>📁</span> 全部知识
          <span class="folder-item__count">{{ store.total }}</span>
        </div>
        <template v-for="folder in store.folders" :key="folder.id">
          <div :class="['folder-item', { active: activeFolderId === folder.id }]" @click="selectFolder(folder.id)">
            <span>📁</span> {{ folder.name }}
            <span class="folder-item__count">{{ folder.entryCount || 0 }}</span>
            <button class="folder-item__add" @click.stop="creatingInFolderId = folder.id; newFolderName = ''; showNewFolder = true">＋</button>
          </div>
          <div
            v-for="child in folder.children"
            :key="child.id"
            :class="['folder-item folder-item--child', { active: activeFolderId === child.id }]"
            @click="selectFolder(child.id)"
          >
            <span>📁</span> {{ child.name }}
            <span class="folder-item__count">{{ child.entryCount || 0 }}</span>
          </div>
        </template>
      </div>
      <div v-if="showNewFolder" class="new-folder-row">
        <input
          v-model="newFolderName"
          placeholder="目录名称"
          class="new-folder-input"
          @keydown.enter="createFolder(creatingInFolderId)"
          @keydown.escape="showNewFolder = false"
          autofocus
        />
      </div>
    </aside>

    <!-- Content Area -->
    <div class="knowledge-content">
      <div class="content-toolbar">
        <span class="content-toolbar__count">{{ store.total }} 条知识</span>
        <el-button size="small" @click="goToUpload">＋ 新增知识</el-button>
      </div>

      <div v-if="store.loading" class="loading-state">加载中...</div>

      <div v-else-if="store.entries.length === 0" class="empty-state">
        暂无知识条目，点击「＋ 新增知识」开始
      </div>

      <div v-else class="knowledge-list">
        <div
          v-for="entry in store.entries"
          :key="entry.id"
          class="knowledge-card"
          @click="goToDetail(entry.id)"
        >
          <div class="knowledge-card__header">
            <span class="knowledge-card__title">{{ entry.title }}</span>
            <button class="knowledge-card__delete" @click.stop="handleDeleteEntry(entry.id, entry.title)">🗑</button>
          </div>
          <div class="knowledge-card__snippet">{{ snippet(entry.contentText) }}</div>
          <div class="knowledge-card__meta">
            <span>{{ getSourceIcon(entry.sourceType) }} {{ entry.sourceType === 'file_upload' ? '文件' : entry.sourceType === 'manual' ? '手动' : '导入' }}</span>
            <span v-if="entry.fileType" class="meta-chip">.{{ entry.fileType }}</span>
            <span v-for="tag in entry.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</span>
            <span>{{ formatDate(entry.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.knowledge-layout { display:flex; height:100%; }

/* Folder Panel */
.folder-panel {
  width:220px; flex-shrink:0; background:#f8fafc;
  border-right:1px solid #e2e8f0; padding:12px 8px;
  display:flex; flex-direction:column; overflow-y:auto;
}
.folder-panel__header {
  font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:0.06em;
  color:#94a3b8; padding:4px 8px 8px; display:flex; justify-content:space-between; align-items:center;
}
.add-folder-btn { background:none; border:none; color:#94a3b8; cursor:pointer; font-size:14px; }
.add-folder-btn:hover { color:#3b82f6; }

.folder-item {
  display:flex; align-items:center; gap:6px; padding:6px 10px;
  border-radius:6px; cursor:pointer; font-size:13px; color:#64748b;
  transition: background .15s, color .15s;
}
.folder-item:hover { background:#f1f5f9; color:#1e293b; }
.folder-item.active { background:#eff6ff; color:#3b82f6; }
.folder-item--child { padding-left:28px; }
.folder-item__count { margin-left:auto; font-size:11px; color:#94a3b8; }
.folder-item__add { background:none; border:none; color:#94a3b8; cursor:pointer; font-size:11px; opacity:0; transition:opacity .15s; }
.folder-item:hover .folder-item__add { opacity:1; }
.folder-item__add:hover { color:#3b82f6; }

.new-folder-row { padding:4px 10px; }
.new-folder-input {
  width:100%; padding:6px 8px; border-radius:6px; border:1px solid #3b82f6;
  background:#ffffff; color:#1e293b; font-size:12px; outline:none;
}

/* Content */
.knowledge-content { flex:1; overflow-y:auto; display:flex; flex-direction:column; }
.content-toolbar {
  display:flex; justify-content:space-between; align-items:center;
  padding:12px 20px; border-bottom:1px solid #e2e8f0; background:#ffffff;
}
.content-toolbar__count { font-size:13px; color:#94a3b8; }

.knowledge-list { padding:16px 20px; display:flex; flex-direction:column; gap:10px; flex:1; }
.knowledge-card {
  background:#ffffff; border:1px solid #e2e8f0;
  border-radius:10px; padding:14px 16px; cursor:pointer;
  transition: border-color .2s, box-shadow .2s;
  box-shadow:0 1px 2px rgba(0,0,0,0.04);
}
.knowledge-card:hover {
  border-color:#bfdbfe;
  box-shadow:0 4px 12px rgba(37,99,235,0.08);
}
.knowledge-card__header { display:flex; justify-content:space-between; align-items:center; margin-bottom:4px; }
.knowledge-card__title { font-size:14px; font-weight:600; color:#1e293b; }
.knowledge-card__delete {
  background:none; border:none; cursor:pointer; font-size:12px;
  opacity:0; transition:opacity .15s;
}
.knowledge-card:hover .knowledge-card__delete { opacity:1; }
.knowledge-card__snippet {
  font-size:12px; color:#94a3b8;
  display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;
  margin-bottom:8px;
}
.knowledge-card__meta { display:flex; align-items:center; gap:8px; font-size:11px; color:#64748b; flex-wrap:wrap; }
.tag-chip { font-size:10px; padding:1px 6px; border-radius:4px; background:#f1f5f9; color:#64748b; }
.meta-chip { font-size:10px; padding:1px 6px; border-radius:4px; background:#eff6ff; color:#3b82f6; }

.loading-state, .empty-state {
  flex:1; display:flex; align-items:center; justify-content:center;
  font-size:14px; color:#94a3b8;
}
</style>
