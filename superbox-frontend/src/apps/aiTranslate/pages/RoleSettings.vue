<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useTranslateStore } from '../stores/translateStore'
import type { TranslatorRole } from '../types'

const store = useTranslateStore()

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  name: '',
  description: '',
  defaultPromptId: undefined as number | undefined,
  model: '',
  temperature: 0.3,
  maxTokens: 4096,
})

onMounted(async () => {
  await Promise.all([store.fetchRoles(), store.fetchPrompts(), store.fetchModels()])
})

function openNew() {
  editingId.value = null
  form.value = {
    name: '',
    description: '',
    defaultPromptId: undefined,
    model: store.models[0]?.model || '',
    temperature: 0.3,
    maxTokens: 4096,
  }
  dialogVisible.value = true
}

function openEdit(role: TranslatorRole) {
  editingId.value = role.id
  form.value = {
    name: role.name,
    description: role.description,
    defaultPromptId: role.defaultPromptId,
    model: role.model,
    temperature: role.temperature,
    maxTokens: role.maxTokens,
  }
  dialogVisible.value = true
}

function copyPreset(role: TranslatorRole) {
  editingId.value = null
  form.value = {
    name: role.name + ' (副本)',
    description: role.description,
    defaultPromptId: role.defaultPromptId,
    model: role.model,
    temperature: role.temperature,
    maxTokens: role.maxTokens,
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入角色名称')
    return
  }
  try {
    if (editingId.value) {
      await store.updateRole(editingId.value, form.value)
      ElMessage.success('角色已更新')
    } else {
      await store.createRole(form.value)
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    await store.fetchRoles()
  } catch { ElMessage.error('保存失败') }
}

async function handleDelete(role: TranslatorRole) {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${role.name}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await store.deleteRole(role.id)
    await store.fetchRoles()
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
}
</script>

<template>
  <div class="ait-roles">
    <div class="ait-panel">
      <div class="ait-panel__header">
        <h3 class="ait-panel__title">角色设定</h3>
        <el-button type="primary" :icon="Plus" @click="openNew">新增角色</el-button>
      </div>

      <div class="ait-role-grid" v-loading="store.rolesLoading">
        <div v-for="role in store.roles" :key="role.id" class="ait-role-card">
          <div class="ait-role-card__header">
            <span class="ait-role-card__name">{{ role.name }}</span>
            <el-tag v-if="role.isPreset" size="small" type="info">预设</el-tag>
          </div>
          <p class="ait-role-card__desc">{{ role.description || '暂无描述' }}</p>
          <div class="ait-role-card__meta">
            <span class="ait-role-meta-item">
              <strong>模型:</strong> {{ role.model }}
            </span>
            <span class="ait-role-meta-item">
              <strong>温度:</strong> {{ role.temperature }}
            </span>
            <span class="ait-role-meta-item">
              <strong>Max Tokens:</strong> {{ role.maxTokens }}
            </span>
          </div>
          <div class="ait-role-card__footer">
            <el-button v-if="role.isPreset" size="small" @click="copyPreset(role)">复制为自定义</el-button>
            <template v-else>
              <el-button size="small" @click="openEdit(role)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(role)">删除</el-button>
            </template>
          </div>
        </div>
        <div v-if="!store.roles.length" class="ait-empty">暂无角色，点击"新增角色"创建</div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑角色' : '新增角色'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="如：专业译者" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="绑定提示词">
          <el-select v-model="form.defaultPromptId" placeholder="选择提示词模板" clearable style="width: 100%">
            <el-option
              v-for="p in store.prompts"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐模型">
          <el-select v-model="form.model" placeholder="选择模型" style="width: 100%">
            <el-option
              v-for="m in store.models"
              :key="m.model"
              :label="`${m.provider} / ${m.model}`"
              :value="m.model"
              :disabled="!m.available"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="温度参数">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="256" :max="32768" :step="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ait-roles {
  max-width: 1100px;
  margin: 0 auto;
}
.ait-panel {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  padding: 20px;
}
.ait-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.ait-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.ait-role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.ait-role-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  transition: box-shadow 0.15s ease;
}
.ait-role-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.ait-role-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.ait-role-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.ait-role-card__desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px 0;
  line-height: 1.5;
}
.ait-role-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.ait-role-meta-item {
  font-size: 12px;
  color: #606266;
}
.ait-role-card__footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.ait-empty {
  padding: 60px 20px;
  text-align: center;
  font-size: 14px;
  color: #c0c4cc;
  grid-column: 1 / -1;
}
</style>
