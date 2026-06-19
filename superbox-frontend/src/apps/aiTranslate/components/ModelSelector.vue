<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useTranslateStore } from '../stores/translateStore'
import type { ModelInfo } from '../types'

const props = withDefaults(defineProps<{
  modelValue: string
  disabled?: boolean
}>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const store = useTranslateStore()

onMounted(() => {
  if (!store.models.length) store.fetchModels()
})

const selectedModel = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val),
})

function providerTagType(provider: string) {
  const map: Record<string, string> = {
    openai: 'success',
    deepseek: '',
    anthropic: 'warning',
    ollama: 'info',
  }
  return map[provider.toLowerCase()] || 'info'
}

const placeholderText = computed(() => {
  if (store.modelsLoading) return '加载中...'
  if (store.models.length === 0) return '请先在模型管理中配置模型'
  return '选择模型'
})
</script>

<template>
  <el-select
    v-model="selectedModel"
    :disabled="disabled"
    :placeholder="placeholderText"
    :loading="store.modelsLoading"
    size="default"
    style="width: 240px"
  >
    <el-option
      v-for="m in store.models"
      :key="m.provider + '/' + m.model"
      :label="m.name"
      :value="m.model"
      :disabled="!m.available"
    >
      <div class="mo-option">
        <span class="mo-option__name">{{ m.name }}</span>
        <span class="mo-option__right">
          <el-tag v-if="m.isDefault" size="small" type="success">默认</el-tag>
          <el-tag :type="providerTagType(m.provider)" size="small">{{ m.provider }}</el-tag>
        </span>
      </div>
    </el-option>
  </el-select>
</template>

<style scoped>
.mo-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.mo-option__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mo-option__right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}
</style>
