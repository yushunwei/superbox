<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile, UploadInstance } from 'element-plus'
import { useTranslateStore } from '../stores/translateStore'
import { translateApi } from '../api/translateApi'
import ModelSelector from '../components/ModelSelector.vue'
import LanguageSelector from '../components/LanguageSelector.vue'
import type { LangCode } from '../types'

const router = useRouter()
const store = useTranslateStore()

const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadFile[]>([])
const sourceLang = ref<LangCode>('zh')
const targetLang = ref<LangCode>('en')
const model = ref('')
const uploading = ref(false)
const taskId = ref<number | null>(null)

const allowedExtensions = '.pdf,.docx,.xlsx,.txt'
const maxFileSize = 200

onMounted(async () => {
  await Promise.all([
    store.fetchLanguages(),
    store.fetchModels(),
    store.fetchSettings(),
  ])
  if (store.models.length > 0 && !model.value) {
    const def = store.models.find(m => m.isDefault)
    model.value = def?.model || store.models[0].model
  }
  if (store.settings) {
    if (store.settings.defaultSourceLang) sourceLang.value = store.settings.defaultSourceLang as LangCode
    if (store.settings.defaultTargetLang) targetLang.value = store.settings.defaultTargetLang as LangCode
  }
})

function beforeUpload(file: File) {
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  const allowed = allowedExtensions.split(',').map(s => s.trim().toLowerCase())
  if (!allowed.includes(ext)) {
    ElMessage.error(`不支持的文件类型，仅支持: ${allowedExtensions}`)
    return false
  }
  if (file.size > maxFileSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${maxFileSize}MB`)
    return false
  }
  return true
}

async function handleUpload() {
  if (!fileList.value.length) {
    ElMessage.warning('请先选择文件')
    return
  }
  const file = fileList.value[0].raw
  if (!file) return

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('sourceLang', sourceLang.value)
    formData.append('targetLang', targetLang.value)
    formData.append('model', model.value)

    const res = await translateApi.uploadDocument(formData)
    const data = (res as any).data
    taskId.value = data.taskId
    ElMessage.success(`文件上传成功，任务ID: ${data.taskId}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

function goToTasks() {
  router.push('/app/ai-translate/tasks')
}
</script>

<template>
  <div class="ait-doc-translate">
    <div class="ait-panel">
      <h3 class="ait-panel__title">文档翻译</h3>

      <!-- Upload area -->
      <el-upload
        ref="uploadRef"
        v-model:file-list="fileList"
        drag
        :auto-upload="false"
        :before-upload="beforeUpload"
        :limit="1"
        :accept="allowedExtensions"
      >
        <el-icon class="ait-upload-icon"><UploadFilled /></el-icon>
        <div class="ait-upload-text">
          <p>将文件拖到此处，或<em>点击上传</em></p>
          <p class="ait-upload-hint">
            支持 PDF / DOCX / XLSX / TXT 格式，最大 {{ maxFileSize }}MB
          </p>
        </div>
      </el-upload>

      <!-- Settings -->
      <div class="ait-doc-settings" v-if="fileList.length > 0">
        <div class="ait-doc-settings__row">
          <label>源语言</label>
          <LanguageSelector v-model="sourceLang" />
        </div>
        <div class="ait-doc-settings__row">
          <label>目标语言</label>
          <LanguageSelector v-model="targetLang" />
        </div>
        <div class="ait-doc-settings__row">
          <label>翻译模型</label>
          <ModelSelector v-model="model" />
        </div>
        <div class="ait-doc-settings__row">
          <el-button
            type="primary"
            @click="handleUpload"
            :loading="uploading"
            :disabled="!fileList.length || !model"
          >
            开始翻译
          </el-button>
        </div>
      </div>

      <!-- Success message -->
      <div v-if="taskId" class="ait-doc-success">
        <p>翻译任务已创建，任务ID: <strong>{{ taskId }}</strong></p>
        <el-button type="primary" link @click="goToTasks">查看任务列表</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ait-doc-translate {
  max-width: 640px;
  margin: 0 auto;
}
.ait-panel {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  padding: 24px;
}
.ait-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}
.ait-upload-icon {
  font-size: 48px;
  color: #c0c4cc;
}
.ait-upload-text {
  font-size: 14px;
  color: #606266;
}
.ait-upload-text em {
  color: #409EFF;
  font-style: normal;
}
.ait-upload-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}
.ait-doc-settings {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ait-doc-settings__row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ait-doc-settings__row label {
  width: 80px;
  flex-shrink: 0;
  font-size: 14px;
  color: #606266;
}
.ait-doc-success {
  margin-top: 24px;
  padding: 16px;
  background: #f0f9eb;
  border-radius: 6px;
  text-align: center;
}
.ait-doc-success p {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #67C23A;
}
</style>
