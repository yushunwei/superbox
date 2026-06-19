<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTranslateStore } from '../stores/translateStore'
import LanguageSelector from '../components/LanguageSelector.vue'
import ModelSelector from '../components/ModelSelector.vue'
import type { LangCode } from '../types'

const store = useTranslateStore()

const form = ref({
  defaultModel: '',
  defaultSourceLang: 'zh' as string,
  defaultTargetLang: 'en' as string,
  temperature: 0.3,
  maxTokens: 4096,
  pdfFormat: 'overlay' as 'overlay' | 'dual_column',
  wordFormat: 'keep_style' as 'keep_style' | 'plain_text',
  doc2xEnabled: false,
})
const saving = ref(false)

onMounted(async () => {
  await Promise.all([
    store.fetchModels(),
    store.fetchLanguages(),
    store.fetchSettings(),
  ])
  if (store.settings) {
    form.value = {
      defaultModel: store.settings.defaultModel || '',
      defaultSourceLang: store.settings.defaultSourceLang || 'zh',
      defaultTargetLang: store.settings.defaultTargetLang || 'en',
      temperature: store.settings.temperature ?? 0.3,
      maxTokens: store.settings.maxTokens ?? 4096,
      pdfFormat: store.settings.pdfFormat || 'overlay',
      wordFormat: store.settings.wordFormat || 'keep_style',
      doc2xEnabled: store.settings.doc2xEnabled ?? false,
    }
  }
})

async function handleSave() {
  saving.value = true
  try {
    await store.saveSettings(form.value)
    ElMessage.success('设置已保存')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="ait-settings">
    <div class="ait-panel">
      <h3 class="ait-panel__title">翻译设置</h3>

      <el-form label-width="120px" class="ait-settings-form">
        <!-- Defaults -->
        <div class="ait-section">
          <h4 class="ait-section__title">默认设置</h4>
          <el-form-item label="默认源语言">
            <LanguageSelector v-model="form.defaultSourceLang" />
          </el-form-item>
          <el-form-item label="默认目标语言">
            <LanguageSelector v-model="form.defaultTargetLang" />
          </el-form-item>
          <el-form-item label="默认模型">
            <ModelSelector v-model="form.defaultModel" />
          </el-form-item>
        </div>

        <!-- LLM Params -->
        <div class="ait-section">
          <h4 class="ait-section__title">LLM 参数</h4>
          <el-form-item label="温度参数">
            <el-slider
              v-model="form.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              show-input
              style="width: 300px"
            />
          </el-form-item>
          <el-form-item label="Max Tokens">
            <el-input-number
              v-model="form.maxTokens"
              :min="256"
              :max="32768"
              :step="256"
            />
          </el-form-item>
        </div>

        <!-- Format options -->
        <div class="ait-section">
          <h4 class="ait-section__title">格式选项</h4>
          <el-form-item label="PDF 格式">
            <el-radio-group v-model="form.pdfFormat">
              <el-radio value="overlay">文字覆盖</el-radio>
              <el-radio value="dual_column">双栏对照</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Word 格式">
            <el-radio-group v-model="form.wordFormat">
              <el-radio value="keep_style">保留样式</el-radio>
              <el-radio value="plain_text">纯文本</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Doc2x">
            <el-switch v-model="form.doc2xEnabled" />
            <span class="ait-switch-hint">启用 Doc2x 文档解析引擎</span>
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存设置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.ait-settings {
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
.ait-settings-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.ait-section {
  margin-bottom: 24px;
}
.ait-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.ait-switch-hint {
  margin-left: 10px;
  font-size: 13px;
  color: #909399;
}
</style>
