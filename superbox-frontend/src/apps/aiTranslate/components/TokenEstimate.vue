<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  charCount: number
}>(), {
  charCount: 0,
})

const estimatedTokens = computed(() => {
  // Rough estimate: ~0.75 tokens per character for Chinese, ~0.3 for English
  // Use a blended estimate of 0.5
  return Math.ceil(props.charCount * 0.5)
})
</script>

<template>
  <span class="token-estimate" v-if="charCount > 0">
    {{ charCount }} 字符 / 约 {{ estimatedTokens }} token
  </span>
  <span class="token-estimate" v-else>0 字符</span>
</template>

<style scoped>
.token-estimate {
  font-size: 12px;
  color: #909399;
}
</style>
