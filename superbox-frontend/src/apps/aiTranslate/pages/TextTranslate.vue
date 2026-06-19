<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Sort } from '@element-plus/icons-vue'
import { useTranslateStore } from '../stores/translateStore'
import { translateApi } from '../api/translateApi'
import ModelSelector from '../components/ModelSelector.vue'
import LanguageSelector from '../components/LanguageSelector.vue'
import TokenEstimate from '../components/TokenEstimate.vue'
import type { LangCode } from '../types'

const store = useTranslateStore()

const sourceText = ref('')
const translatedText = ref('')
const sourceLang = ref<LangCode>('zh')
const targetLang = ref<LangCode>('en')
const model = ref('')
const roleId = ref<number | undefined>(undefined)
const promptTemplateId = ref<number | undefined>(undefined)
const translating = ref(false)

onMounted(async () => {
  await Promise.all([
    store.fetchLanguages(),
    store.fetchModels(),
    store.fetchRoles(),
    store.fetchPrompts(),
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

function swapLangs() {
  const tmp = sourceLang.value
  sourceLang.value = targetLang.value
  targetLang.value = tmp
}

async function doTranslate() {
  if (!sourceText.value.trim()) {
    ElMessage.warning('请输入待翻译文本')
    return
  }
  translating.value = true
  try {
    const res = await translateApi.text({
      text: sourceText.value,
      sourceLang: sourceLang.value,
      targetLang: targetLang.value,
      model: model.value,
      promptTemplateId: promptTemplateId.value,
      roleId: roleId.value,
    })
    const data = (res as any).data
    translatedText.value = data.translatedText
  } catch (e: any) {
    ElMessage.error(e?.message || '翻译失败，请重试')
  } finally {
    translating.value = false
  }
}

function copySource() {
  if (!sourceText.value) return
  navigator.clipboard.writeText(sourceText.value).then(() => {
    ElMessage.success('原文已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function copyResult() {
  if (!translatedText.value) return
  navigator.clipboard.writeText(translatedText.value).then(() => {
    ElMessage.success('译文已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function clearSource() {
  sourceText.value = ''
}
</script>

<template>
  <div class="ait-text-translate">
    <!-- Toolbar -->
    <div class="ait-toolbar">
      <div class="ait-toolbar__left">
        <LanguageSelector v-model="sourceLang" placeholder="源语言" />
        <el-button :icon="Sort" circle size="small" @click="swapLangs" title="交换语言" />
        <LanguageSelector v-model="targetLang" placeholder="目标语言" />
        <ModelSelector v-model="model" />
        <el-select
          v-model="promptTemplateId"
          placeholder="提示词模板"
          clearable
          size="default"
          style="width: 140px"
        >
          <el-option
            v-for="p in store.prompts"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
        <el-select
          v-model="roleId"
          placeholder="选择角色"
          clearable
          size="default"
          style="width: 140px"
        >
          <el-option
            v-for="r in store.roles"
            :key="r.id"
            :label="r.name"
            :value="r.id"
          />
        </el-select>
      </div>
      <div class="ait-toolbar__right">
        <el-button type="primary" @click="doTranslate" :loading="translating" :disabled="!model">
          翻译
        </el-button>
      </div>
    </div>

    <!-- Two-column layout -->
    <div class="ait-columns">
      <div class="ait-column">
        <div class="ait-column__header">
          原文
          <div class="ait-column__actions">
            <el-button text size="small" :disabled="!sourceText" @click="copySource">复制</el-button>
            <el-button text size="small" :disabled="!sourceText" @click="clearSource">清除</el-button>
          </div>
        </div>
        <el-input
          v-model="sourceText"
          type="textarea"
          :rows="16"
          placeholder="请输入要翻译的文本..."
          class="ait-textarea"
        />
        <div class="ait-column__footer">
          <TokenEstimate :char-count="sourceText.length" />
        </div>
      </div>
      <div class="ait-column">
        <div class="ait-column__header">
          译文
          <el-button text size="small" :disabled="!translatedText" @click="copyResult">复制</el-button>
        </div>
        <div class="ait-output" v-if="translatedText">
          <div class="ait-output__text">{{ translatedText }}</div>
        </div>
        <div class="ait-output--empty" v-else>
          <span v-if="translating">翻译中...</span>
          <span v-else>翻译结果将显示在这里</span>
        </div>
        <div class="ait-column__footer" v-if="translatedText">
          <span class="ait-token">模型: {{ model }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ait-text-translate {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}
.ait-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.ait-toolbar__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ait-columns {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
}
.ait-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}
.ait-column__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}
.ait-column__actions {
  display: flex;
  gap: 2px;
}
.ait-column__footer {
  padding: 8px 14px;
  border-top: 1px solid #ebeef5;
}
.ait-textarea {
  flex: 1;
}
.ait-textarea :deep(.el-textarea__inner) {
  border: none !important;
  border-radius: 0;
  box-shadow: none !important;
  resize: none;
  height: 100% !important;
  font-size: 14px;
  line-height: 1.8;
}
.ait-output {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
}
.ait-output__text {
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
.ait-output--empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 14px;
}
.ait-token {
  font-size: 12px;
  color: #909399;
}
</style>
