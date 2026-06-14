<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const title = computed(() => (route.meta.title as string) || '')

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="app-shell">
    <header class="app-shell__topbar">
      <el-button text class="back-btn" @click="goHome">
        <el-icon><ArrowLeft /></el-icon>
        {{ t('common.back') }}
      </el-button>
      <span class="app-shell__title">{{ title }}</span>
      <div class="app-shell__actions">
        <slot name="actions" />
      </div>
    </header>
    <main class="app-shell__content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafc;
}
.app-shell__topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  height: 48px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.back-btn {
  color: #64748b;
  font-size: 14px;
}
.back-btn:hover {
  color: #1e293b;
}
.app-shell__title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  flex: 1;
}
.app-shell__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.app-shell__content {
  flex: 1;
  overflow: hidden;
}
</style>
