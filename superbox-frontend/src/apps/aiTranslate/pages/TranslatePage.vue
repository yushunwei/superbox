<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Document, List, Notebook, Setting, UserFilled,
  Memo, Collection,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { path: '/app/ai-translate/text', label: '文本翻译', icon: Memo },
  { path: '/app/ai-translate/document', label: '文档翻译', icon: Document },
  { path: '/app/ai-translate/tasks', label: '任务列表', icon: List },
  { path: '/app/ai-translate/glossary', label: '术语表', icon: Notebook },
  { path: '/app/ai-translate/prompts', label: '提示词', icon: Collection },
  { path: '/app/ai-translate/roles', label: '角色设定', icon: UserFilled },
  { path: '/app/ai-translate/settings', label: '翻译设置', icon: Setting },
]

const activeMenu = computed(() => route.path)

function onMenuSelect(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="ait-layout">
    <aside class="ait-sidebar">
      <div class="ait-sidebar__title">AI 智能翻译</div>
      <el-menu
        :default-active="activeMenu"
        class="ait-menu"
        @select="onMenuSelect"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="ait-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.ait-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}
.ait-sidebar {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid #e4e7ed;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}
.ait-sidebar__title {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}
.ait-menu {
  border-right: none;
  flex: 1;
}
.ait-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}
</style>
