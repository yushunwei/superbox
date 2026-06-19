<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useTranslateStore } from '../stores/translateStore'
import type { LangCode } from '../types'

const props = withDefaults(defineProps<{
  modelValue: LangCode | string
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '选择语言',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const store = useTranslateStore()

onMounted(() => {
  if (!store.languages.length) store.fetchLanguages()
})

const selectedLang = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val),
})
</script>

<template>
  <el-select
    v-model="selectedLang"
    :disabled="disabled"
    :placeholder="placeholder"
    :loading="store.languagesLoading"
    size="default"
    style="width: 140px"
  >
    <el-option
      v-for="lang in store.languages"
      :key="lang.code"
      :label="lang.nativeName"
      :value="lang.code"
    >
      <span>{{ lang.nativeName }}</span>
      <span style="float: right; color: #8492a6; font-size: 12px">{{ lang.code.toUpperCase() }}</span>
    </el-option>
  </el-select>
</template>
