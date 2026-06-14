<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useKnowledgeStore } from '@/stores/modules/knowledgeBaseStore'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useKnowledgeStore()

const dragOver = ref(false)
const uploading = ref(false)

// Manual entry form
const manualTitle = ref('')
const manualContent = ref('')
const manualFolderId = ref<number | null>(null)
const saving = ref(false)

onMounted(() => {
  store.fetchFolders()
})

async function handleFileUpload(file: File) {
  uploading.value = true
  try {
    await store.uploadFile(file)
    ElMessage.success('文件上传成功，已自动解析并分块')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function onFileDrop(e: DragEvent) {
  dragOver.value = false
  const files = e.dataTransfer?.files
  if (files?.length) {
    for (const file of Array.from(files)) {
      handleFileUpload(file)
    }
  }
}

async function saveManual() {
  if (!manualTitle.value.trim() || !manualContent.value.trim()) return
  saving.value = true
  try {
    await store.createManual({
      title: manualTitle.value.trim(),
      content: manualContent.value.trim(),
      folderId: manualFolderId.value,
    })
    ElMessage.success('知识已保存')
    manualTitle.value = ''
    manualContent.value = ''
    router.push('/app/knowledge-base')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="upload-page">
    <div
      :class="['upload-zone', { 'upload-zone--drag': dragOver }]"
      @dragover.prevent="dragOver = true"
      @dragleave="dragOver = false"
      @drop.prevent="onFileDrop"
    >
      <div class="upload-zone__icon">📤</div>
      <div class="upload-zone__text">拖拽文件到此处上传，或点击选择文件</div>
      <div class="upload-zone__hint">支持 PDF / DOCX / TXT / Markdown / HTML / CSV · 最大 20MB</div>
      <input
        type="file"
        id="file-input"
        accept=".pdf,.docx,.txt,.md,.markdown,.html,.htm,.csv"
        class="file-input-hidden"
        @change="(e) => { const f = (e.target as HTMLInputElement).files?.[0]; if (f) handleFileUpload(f) }"
      />
      <label for="file-input" class="file-label">
        <el-button :loading="uploading" type="primary">选择文件</el-button>
      </label>
    </div>

    <div class="manual-entry">
      <h3 class="section-title">手工录入</h3>
      <el-input
        v-model="manualTitle"
        placeholder="知识标题"
        size="large"
        class="mb-3"
      />
      <div class="mb-3">
        <label class="prop-label">目录（可选）</label>
        <select v-model="manualFolderId" class="folder-select">
          <option :value="null">无</option>
          <option v-for="f in store.folders" :key="f.id" :value="f.id">{{ f.name }}</option>
        </select>
      </div>
      <textarea
        v-model="manualContent"
        placeholder="知识内容（支持 Markdown 格式）"
        class="content-textarea"
        rows="10"
      ></textarea>
      <el-button type="primary" :loading="saving" @click="saveManual">
        保存知识
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.upload-page { padding:20px; max-width:720px; margin:0 auto; overflow-y:auto; height:100%; }

.upload-zone {
  padding:40px; text-align:center;
  border:2px dashed #cbd5e1; border-radius:14px;
  background:#f8fafc;
  transition: border-color .2s, background .2s;
}
.upload-zone--drag { border-color:#3b82f6; background:#eff6ff; }
.upload-zone__icon { font-size:40px; margin-bottom:12px; }
.upload-zone__text { font-size:14px; color:#64748b; }
.upload-zone__hint { font-size:12px; color:#94a3b8; margin-top:6px; margin-bottom:16px; }
.file-input-hidden { display:none; }
.file-label { cursor:pointer; }

.manual-entry {
  margin-top:24px; padding:20px;
  background:#ffffff; border-radius:12px;
  border:1px solid #e2e8f0; box-shadow:0 1px 3px rgba(0,0,0,0.04);
}
.section-title { font-size:15px; font-weight:600; color:#1e293b; margin:0 0 16px 0; }
.mb-3 { margin-bottom:12px; }
.prop-label { display:block; font-size:12px; color:#94a3b8; margin-bottom:4px; }
.folder-select {
  width:100%; padding:8px 12px; border-radius:8px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:13px; outline:none;
}
.folder-select:focus { border-color:#3b82f6; }
.content-textarea {
  width:100%; padding:12px; border-radius:8px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:13px; outline:none; resize:vertical;
  font-family:inherit; line-height:1.7; margin-bottom:12px;
}
.content-textarea:focus { border-color:#3b82f6; }
</style>
