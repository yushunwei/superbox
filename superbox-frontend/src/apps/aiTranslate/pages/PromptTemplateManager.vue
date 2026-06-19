<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useTranslateStore } from '../stores/translateStore'
import type { PromptTemplate } from '../types'

const store = useTranslateStore()

const selectedCategory = ref('全部')
const selectedId = ref<number | null>(null)
const editingName = ref('')
const editingCategory = ref('通用')
const editingPrompt = ref('')
const isNew = ref(false)

const categories = ['全部', '通用', '技术', '文学', '口语', '自定义']

const filteredPrompts = computed(() => {
  if (selectedCategory.value === '全部') return store.prompts
  return store.prompts.filter(p => p.category === selectedCategory.value)
})

const selectedPrompt = computed(() => {
  if (isNew.value) return null
  return store.prompts.find(p => p.id === selectedId.value) || null
})

const isEditingPreset = computed(() => {
  return selectedPrompt.value?.isPreset === true
})

onMounted(async () => {
  await store.fetchPrompts()
  if (filteredPrompts.value.length > 0) {
    selectPrompt(filteredPrompts.value[0])
  }
})

function selectPrompt(p: PromptTemplate) {
  isNew.value = false
  selectedId.value = p.id
  editingName.value = p.name
  editingCategory.value = p.category
  editingPrompt.value = p.systemPrompt
}

function selectCategory(cat: string) {
  selectedCategory.value = cat
  const first = filteredPrompts.value[0]
  if (first) selectPrompt(first)
  else {
    selectedId.value = null
    isNew.value = false
  }
}

function startNew() {
  isNew.value = true
  selectedId.value = null
  editingName.value = ''
  editingCategory.value = '自定义'
  editingPrompt.value = ''
}

function copyPreset() {
  if (!selectedPrompt.value) return
  isNew.value = true
  selectedId.value = null
  editingName.value = selectedPrompt.value.name + ' (副本)'
  editingCategory.value = '自定义'
  editingPrompt.value = selectedPrompt.value.systemPrompt
}

async function handleSave() {
  if (!editingName.value.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!editingPrompt.value.trim()) {
    ElMessage.warning('请输入系统提示词')
    return
  }
  try {
    if (isNew.value) {
      const created = await store.createPrompt({
        name: editingName.value,
        category: editingCategory.value,
        systemPrompt: editingPrompt.value,
      })
      isNew.value = false
      selectedId.value = created.id
      ElMessage.success('模板已创建')
    } else if (selectedId.value && !isEditingPreset.value) {
      await store.updatePrompt(selectedId.value, {
        name: editingName.value,
        category: editingCategory.value,
        systemPrompt: editingPrompt.value,
      })
      ElMessage.success('模板已更新')
    }
    await store.fetchPrompts()
  } catch { ElMessage.error('保存失败') }
}

async function handleDelete() {
  if (!selectedId.value) return
  try {
    await ElMessageBox.confirm('确定要删除此提示词模板吗？', '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await store.deletePrompt(selectedId.value)
    ElMessage.success('已删除')
    selectedId.value = null
    isNew.value = false
    await store.fetchPrompts()
  } catch { ElMessage.error('删除失败') }
}

function insertVariable(variable: string) {
  editingPrompt.value += variable
}
</script>

<template>
  <div class="ait-prompts">
    <div class="ait-panel ait-prompts-layout">
      <!-- Left: Category + List -->
      <div class="ait-prompts-left">
        <div class="ait-prompts-left__header">
          <span class="ait-prompts-left__title">模板分类</span>
          <el-button :icon="Plus" text size="small" @click="startNew">新建</el-button>
        </div>
        <div class="ait-cat-list">
          <div
            v-for="cat in categories"
            :key="cat"
            :class="['ait-cat-item', { active: selectedCategory === cat }]"
            @click="selectCategory(cat)"
          >
            {{ cat }}
          </div>
        </div>
        <div class="ait-prompts-list" v-loading="store.promptsLoading">
          <div
            v-for="p in filteredPrompts"
            :key="p.id"
            :class="['ait-prompt-item', { active: selectedId === p.id && !isNew }]"
            @click="selectPrompt(p)"
          >
            <span class="ait-prompt-item__name">{{ p.name }}</span>
            <el-tag v-if="p.isPreset" size="small" type="info">预设</el-tag>
          </div>
          <div v-if="!filteredPrompts.length" class="ait-empty">暂无模板</div>
        </div>
      </div>

      <!-- Right: Editor -->
      <div class="ait-prompts-right">
        <div class="ait-editor">
          <div class="ait-editor__header">
            <span class="ait-editor__title">{{ isNew ? '新建模板' : (selectedPrompt?.name || '选择模板') }}</span>
            <div class="ait-editor__actions">
              <el-button
                v-if="isEditingPreset && !isNew"
                size="small"
                @click="copyPreset"
              >
                复制为自定义
              </el-button>
              <el-button
                v-if="!isNew && !isEditingPreset"
                size="small"
                type="danger"
                @click="handleDelete"
              >
                删除
              </el-button>
            </div>
          </div>

          <div v-if="selectedId || isNew" class="ait-editor__form">
            <el-form label-width="70px">
              <el-form-item label="名称">
                <el-input
                  v-model="editingName"
                  placeholder="模板名称"
                  :disabled="isEditingPreset && !isNew"
                />
              </el-form-item>
              <el-form-item label="分类">
                <el-select
                  v-model="editingCategory"
                  placeholder="选择分类"
                  style="width: 100%"
                  :disabled="isEditingPreset && !isNew"
                >
                  <el-option v-for="c in categories.filter(x => x !== '全部')" :key="c" :label="c" :value="c" />
                </el-select>
              </el-form-item>
              <el-form-item label="提示词">
                <el-input
                  v-model="editingPrompt"
                  type="textarea"
                  :rows="12"
                  placeholder="输入系统提示词，支持变量..."
                  :disabled="isEditingPreset && !isNew"
                />
              </el-form-item>
            </el-form>

            <!-- Variable hints -->
            <div class="ait-variables">
              <span class="ait-variables__title">可用变量：</span>
              <el-tag
                v-for="v in ['{source_lang}', '{target_lang}', '{text}']"
                :key="v"
                class="ait-var-tag"
                @click="insertVariable(v)"
              >
                {{ v }}
              </el-tag>
            </div>

            <div class="ait-editor__footer">
              <el-button type="primary" @click="handleSave" :disabled="isEditingPreset && !isNew">
                保存
              </el-button>
            </div>
          </div>

          <div v-else class="ait-empty ait-editor__placeholder">
            请从左侧选择模板，或点击"新建"创建
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ait-prompts {
  height: 100%;
}
.ait-prompts-layout {
  display: flex;
  height: 100%;
  gap: 0;
  padding: 0;
  overflow: hidden;
}
.ait-prompts-left {
  width: 240px;
  flex-shrink: 0;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}
.ait-prompts-left__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeef5;
}
.ait-prompts-left__title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.ait-cat-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
}
.ait-cat-item {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  color: #606266;
  background: #f5f7fa;
  transition: all 0.15s ease;
}
.ait-cat-item:hover { background: #ecf5ff; color: #409EFF; }
.ait-cat-item.active { background: #409EFF; color: #ffffff; }
.ait-prompts-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}
.ait-prompt-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all 0.15s ease;
}
.ait-prompt-item:hover { background: #f5f7fa; }
.ait-prompt-item.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.ait-prompt-item__name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ait-prompts-right {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.ait-editor {
  max-width: 700px;
}
.ait-editor__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.ait-editor__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.ait-editor__actions {
  display: flex;
  gap: 8px;
}
.ait-editor__form {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  padding: 20px;
}
.ait-editor__footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.ait-editor__placeholder {
  margin-top: 40px;
}
.ait-variables {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ait-variables__title {
  font-size: 12px;
  color: #909399;
}
.ait-var-tag {
  cursor: pointer;
}
.ait-var-tag:hover {
  background: #409EFF;
  color: #ffffff;
  border-color: #409EFF;
}
.ait-empty {
  text-align: center;
  padding: 40px 20px;
  font-size: 13px;
  color: #c0c4cc;
}
.ait-panel {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}
</style>
