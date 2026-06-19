<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection } from '@element-plus/icons-vue'
import { useModelManagerStore } from '../stores/modelManagerStore'
import type { ModelConfig } from '../types'

const store = useModelManagerStore()

const dialogVisible = ref(false)
const dialogTitle = ref('添加模型')
const form = ref<Partial<ModelConfig>>({})
const editingId = ref<number | null>(null)
const saveLoading = ref(false)
const testing = ref<Record<number, boolean>>({})

const apiFormatOptions = [
  { value: 'OPENAI_COMPATIBLE', label: 'OpenAI 兼容格式' },
  { value: 'ANTHROPIC_COMPATIBLE', label: 'Anthropic 兼容格式' },
  { value: 'OLLAMA', label: 'Ollama 格式' },
]

const formDefaults = (): Partial<ModelConfig> => ({
  providerName: 'openai',
  apiKey: '',
  baseUrl: '',
  modelName: '',
  displayName: '',
  apiFormat: '',
  isDefault: false,
  isActive: true,
})

function maskedKey(apiKey?: string) {
  if (!apiKey) return '使用系统配置'
  if (apiKey.length <= 4) return '****'
  return '****' + apiKey.slice(-4)
}

function showCreateDialog() {
  form.value = formDefaults()
  editingId.value = null
  dialogTitle.value = '添加模型'
  dialogVisible.value = true
}

function showEditDialog(row: ModelConfig) {
  form.value = { ...row }
  editingId.value = row.id!
  dialogTitle.value = '编辑模型'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.providerName || !form.value.modelName) {
    ElMessage.warning('请填写模型厂商和模型名称')
    return
  }
  saveLoading.value = true
  try {
    if (editingId.value) {
      await store.updateModel(editingId.value, form.value)
      ElMessage.success('模型已更新')
    } else {
      await store.createModel(form.value)
      ElMessage.success('模型已添加')
    }
    dialogVisible.value = false
  } catch (err: any) {
    const msg = err?.message || err?.msg || '保存失败'
    ElMessage.error(typeof msg === 'string' ? msg : '保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function handleDelete(row: ModelConfig) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型「${row.displayName || row.modelName}」吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await store.deleteModel(row.id!)
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

async function handleSetDefault(row: ModelConfig) {
  if (row.isDefault) return
  try {
    await ElMessageBox.confirm(
      `设为默认后将取消其他模型的默认状态，确定继续？`,
      '设为默认',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await store.setDefault(row.id!)
    ElMessage.success('已设为默认')
  } catch { /* cancelled */ }
}

async function handleTest(row: ModelConfig) {
  if (!row.apiKey) {
    ElMessage.warning('请先填写 API Key')
    return
  }
  testing.value[row.id!] = true
  try {
    const result = await store.testConnection({
      providerName: row.providerName,
      apiKey: row.apiKey,
      baseUrl: row.baseUrl || '',
      modelName: row.modelName,
    })
    if (result.success) {
      const suffix = result.message && result.message !== '连接成功' ? `（${result.message}）` : ''
      ElMessage.success(`连接成功 ${suffix}(${result.responseTimeMs}ms)`)
    } else {
      ElMessage.error(result.message)
    }
  } catch (err: any) {
    const msg = err?.message || err?.msg || '测试请求失败'
    ElMessage.error(typeof msg === 'string' ? msg : '测试请求失败')
  } finally {
    testing.value[row.id!] = false
  }
}

type EpTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function getProviderTag(provider: string): EpTagType {
  const map: Record<string, EpTagType> = {
    openai: 'success',
    deepseek: 'primary',
    anthropic: 'warning',
    ollama: 'info',
  }
  return map[provider] || 'primary'
}

onMounted(() => {
  store.fetchModels()
})
</script>

<template>
  <div class="model-manager-page">
    <div class="mm-toolbar">
      <h2 class="mm-title">AI 模型管理</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog">添加模型</el-button>
    </div>

    <div v-loading="store.loading" class="mm-card-grid">
      <div
        v-for="row in store.models"
        :key="row.id"
        class="mm-card"
      >
        <div class="mm-card__header">
          <div class="mm-card__name">
            {{ row.displayName || row.modelName }}
            <el-tag v-if="row.isDefault" size="small" type="success">默认</el-tag>
          </div>
          <el-tag :type="getProviderTag(row.providerName)" size="small">{{ row.providerName }}</el-tag>
        </div>
        <div class="mm-card__body">
          <div class="mm-card__field">
            <span class="mm-card__label">模型名</span>
            <span class="mm-card__value">{{ row.modelName }}</span>
          </div>
          <div class="mm-card__field">
            <span class="mm-card__label">API Key</span>
            <span class="mm-card__value mm-card__key">{{ maskedKey(row.apiKey) }}</span>
          </div>
          <div class="mm-card__field" v-if="row.apiFormat || row.baseUrl">
            <span class="mm-card__label">格式</span>
            <span class="mm-card__value">{{ row.apiFormat || '自动检测' }}</span>
          </div>
        </div>
        <div class="mm-card__actions">
          <el-button text size="small" @click="handleSetDefault(row)" :disabled="row.isDefault">默认</el-button>
          <el-button text size="small" :icon="Connection" :loading="testing[row.id]" @click="handleTest(row)">测试</el-button>
          <el-button text size="small" @click="showEditDialog(row)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-if="!store.loading && store.models.length === 0" class="mm-empty">
      暂无模型配置，点击"添加模型"开始
    </div>

    <!-- Form Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="form = formDefaults()">
      <el-form :model="form" label-width="100px" autocomplete="off">
        <el-form-item label="模型厂商" required>
          <el-input v-model="form.providerName" placeholder="如 openai、deepseek、minimaxi" />
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="form.modelName" placeholder="如 gpt-4o-mini" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.displayName" placeholder="自定义显示名称" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="留空使用系统默认" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="留空使用默认地址" />
        </el-form-item>
        <el-form-item label="API 格式">
          <el-select v-model="form.apiFormat" placeholder="自动检测" style="width: 100%" clearable>
            <el-option v-for="opt in apiFormatOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <span class="mm-hint">留空则根据 Base URL 自动检测</span>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
          <span class="mm-hint">设为默认后，翻译和问答将优先使用此模型</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.model-manager-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
}
.mm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.mm-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}
.mm-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}
.mm-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 24px;
  transition: box-shadow 0.2s;
}
.mm-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}
.mm-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.mm-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}
.mm-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.mm-card__field {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mm-card__label {
  font-size: 12px;
  color: #94a3b8;
  min-width: 56px;
}
.mm-card__value {
  font-size: 13px;
  color: #475569;
}
.mm-card__key {
  font-family: monospace;
}
.mm-card__actions {
  display: flex;
  gap: 4px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}
.mm-empty {
  text-align: center;
  padding: 60px 0;
  color: #94a3b8;
  font-size: 14px;
}
.mm-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
