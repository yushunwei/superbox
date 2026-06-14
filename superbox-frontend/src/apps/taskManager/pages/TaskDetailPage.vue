<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTaskStore } from '@/stores/modules/taskManagerStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Task } from '@/api/modules/taskManager'

const route = useRoute()
const router = useRouter()
const store = useTaskStore()

const taskId = computed(() => Number(route.params.id))
const editing = ref(false)
const loading = ref(false)

const form = ref({
  title: '',
  description: '',
  priority: 'normal' as string,
  status: 'not_started' as string,
  planStartDate: '',
  planEndDate: '',
  executor: '',
  collaborators: '',
  remarks: '',
})

const statusOptions = [
  { value: 'not_started', label: '未开始' },
  { value: 'in_progress', label: '进行中' },
  { value: 'completed', label: '已完成' },
  { value: 'cancelled', label: '已作废' },
]

const priorityOptions = [
  { value: 'normal', label: '一般' },
  { value: 'important', label: '重要' },
]

function getPriorityLabel(p: string) {
  return priorityOptions.find(o => o.value === p)?.label || p
}

function getStatusLabel(s: string) {
  return statusOptions.find(o => o.value === s)?.label || s
}

function isOverdue(dateStr: string | null) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date(new Date().toDateString())
}

function formatDate(s: string | null) {
  if (!s) return ''
  return new Date(s).toLocaleDateString('zh-CN')
}

async function loadTask() {
  loading.value = true
  try {
    const task = await store.fetchTask(taskId.value)
    form.value = {
      title: task.title,
      description: task.description || '',
      priority: task.priority || 'normal',
      status: task.status || 'not_started',
      planStartDate: task.planStartDate || '',
      planEndDate: task.planEndDate || '',
      executor: task.executor || '',
      collaborators: task.collaborators || '',
      remarks: task.remarks || '',
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadTask)

function startEdit() {
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  if (store.currentTask) {
    form.value = {
      title: store.currentTask.title,
      description: store.currentTask.description || '',
      priority: store.currentTask.priority || 'normal',
      status: store.currentTask.status || 'not_started',
      planStartDate: store.currentTask.planStartDate || '',
      planEndDate: store.currentTask.planEndDate || '',
      executor: store.currentTask.executor || '',
      collaborators: store.currentTask.collaborators || '',
      remarks: store.currentTask.remarks || '',
    }
  }
}

async function saveEdit() {
  if (!form.value.title.trim()) return
  try {
    await store.updateTask(taskId.value, {
      title: form.value.title,
      description: form.value.description,
      priority: form.value.priority,
      status: form.value.status,
      planStartDate: form.value.planStartDate || undefined,
      planEndDate: form.value.planEndDate || undefined,
      executor: form.value.executor || undefined,
      collaborators: form.value.collaborators || undefined,
      remarks: form.value.remarks || undefined,
    } as any)
    editing.value = false
    ElMessage.success('任务已更新')
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleStatusChange(newStatus: string) {
  try {
    await store.changeStatus(taskId.value, newStatus)
    form.value.status = newStatus
    ElMessage.success('状态已更新')
  } catch {
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这个任务吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await store.deleteTask(taskId.value)
    router.push('/app/task-manager')
    ElMessage.success('任务已删除')
  } catch { /* cancelled */ }
}

async function handlePromote() {
  try {
    await ElMessageBox.confirm(
      '将任务内容提升为知识条目？任务标题和描述将被保存到知识库。',
      '提升到知识库',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  try {
    await store.promoteToKnowledge(taskId.value)
    ElMessage.success('已提升到知识库')
  } catch {
    ElMessage.error('提升到知识库失败')
  }
}
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <template v-if="store.currentTask">
      <!-- Header -->
      <div class="detail-header">
        <div class="detail-header__left">
          <div>
            <template v-if="!editing">
              <h1 class="detail-title">{{ store.currentTask.title }}</h1>
            </template>
            <template v-else>
              <input v-model="form.title" class="edit-input-lg" placeholder="任务标题" />
            </template>
            <div class="detail-meta">
              <span class="meta-chip status-chip">{{ getStatusLabel(store.currentTask.status) }}</span>
              <span class="meta-chip">{{ getPriorityLabel(store.currentTask.priority) }}</span>
              <span v-if="store.currentTask.planEndDate" :class="['meta-chip', { 'meta-chip--overdue': isOverdue(store.currentTask.planEndDate) }]">
                &#128197; {{ store.currentTask.planEndDate }}
              </span>
              <span class="meta-chip">创建于 {{ formatDate(store.currentTask.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-header__actions">
          <template v-if="!editing">
            <el-button @click="startEdit">编辑</el-button>
            <el-dropdown @command="handleStatusChange">
              <el-button type="primary">变更状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="s in statusOptions.filter(s => s.value !== store.currentTask?.status)"
                    :key="s.value"
                    :command="s.value"
                  >{{ s.label }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button @click="handlePromote">提升到知识库</el-button>
            <el-button type="danger" plain @click="handleDelete">删除</el-button>
          </template>
          <template v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveEdit">保存</el-button>
          </template>
        </div>
      </div>

      <!-- Content -->
      <div class="detail-body">
        <div class="detail-section">
          <h3 class="section-label">描述</h3>
          <template v-if="!editing">
            <p v-if="store.currentTask.description" class="detail-desc">{{ store.currentTask.description }}</p>
            <p v-else class="detail-desc empty">暂无描述</p>
          </template>
          <template v-else>
            <textarea v-model="form.description" class="edit-textarea" placeholder="任务描述（支持 Markdown）" rows="8"></textarea>
          </template>
        </div>

        <div class="detail-section" v-if="editing">
          <h3 class="section-label">属性</h3>
          <div class="edit-props">
            <label class="prop-row">
              <span>优先级</span>
              <select v-model="form.priority" class="prop-select">
                <option v-for="p in priorityOptions" :key="p.value" :value="p.value">{{ p.label }}</option>
              </select>
            </label>
            <label class="prop-row">
              <span>计划开始</span>
              <input v-model="form.planStartDate" type="date" class="prop-input" />
            </label>
            <label class="prop-row">
              <span>计划结束</span>
              <input v-model="form.planEndDate" type="date" class="prop-input" />
            </label>
            <label class="prop-row">
              <span>执行人</span>
              <input v-model="form.executor" class="prop-input" placeholder="执行人" />
            </label>
            <label class="prop-row">
              <span>协同人</span>
              <input v-model="form.collaborators" class="prop-input" placeholder="多个用逗号分隔" />
            </label>
            <label class="prop-row">
              <span>备注说明</span>
              <textarea v-model="form.remarks" class="prop-textarea" rows="3" placeholder="备注说明"></textarea>
            </label>
          </div>
        </div>

        <div v-if="!editing" class="detail-section">
          <h3 class="section-label">详细信息</h3>
          <div class="info-grid">
            <div class="info-item" v-if="store.currentTask.executor">
              <span class="info-label">执行人</span>
              <span>{{ store.currentTask.executor }}</span>
            </div>
            <div class="info-item" v-if="store.currentTask.collaborators">
              <span class="info-label">协同人</span>
              <span>{{ store.currentTask.collaborators }}</span>
            </div>
            <div class="info-item" v-if="store.currentTask.planStartDate">
              <span class="info-label">计划开始</span>
              <span>{{ store.currentTask.planStartDate }}</span>
            </div>
            <div class="info-item" v-if="store.currentTask.completedAt">
              <span class="info-label">完成时间</span>
              <span>{{ formatDate(store.currentTask.completedAt) }}</span>
            </div>
            <div class="info-item info-item--full" v-if="store.currentTask.remarks">
              <span class="info-label">备注说明</span>
              <span>{{ store.currentTask.remarks }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section" v-if="store.currentTask.tags?.length">
          <h3 class="section-label">标签</h3>
          <div class="tag-list">
            <span v-for="tag in store.currentTask.tags" :key="tag.id" class="tag-chip" :style="{ borderColor: tag.color, color: tag.color }">
              {{ tag.name }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="!loading" class="not-found">
      <p>任务不存在</p>
      <el-button @click="router.push('/app/task-manager')">返回列表</el-button>
    </div>
  </div>
</template>

<style scoped>
.detail-page { padding:24px 32px; overflow-y:auto; height:100%; max-width:900px; }

.detail-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:32px; gap:16px; }
.detail-header__left { display:flex; gap:12px; align-items:flex-start; }
.detail-title { font-size:22px; font-weight:600; color:#1e293b; margin:0 0 8px 0; line-height:1.3; }
.edit-input-lg {
  width:100%; padding:8px 12px; border-radius:8px; border:1px solid #3b82f6;
  background:#ffffff; color:#1e293b; font-size:22px; font-weight:600; outline:none; margin-bottom:8px;
  font-family:inherit;
}
.detail-meta { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
.meta-chip {
  font-size:12px; padding:2px 10px; border-radius:12px;
  background:#f1f5f9; color:#64748b;
}
.meta-chip--overdue { color:#ef4444; background:#fef2f2; }
.status-chip { background:#eff6ff; color:#3b82f6; }
.detail-header__actions { display:flex; gap:8px; flex-shrink:0; }

.detail-body { display:flex; flex-direction:column; gap:24px; }
.detail-section { }
.section-label { font-size:13px; font-weight:600; color:#94a3b8; margin:0 0 10px 0; text-transform:uppercase; letter-spacing:0.5px; }
.detail-desc { font-size:14px; line-height:1.8; color:#334155; margin:0; white-space:pre-wrap; }
.detail-desc.empty { color:#94a3b8; font-style:italic; }

.edit-textarea {
  width:100%; padding:12px; border-radius:8px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:14px; outline:none; resize:vertical;
  font-family:inherit; line-height:1.7;
}
.edit-textarea:focus { border-color:#3b82f6; }

.edit-props { display:flex; flex-direction:column; gap:10px; }
.prop-row {
  display:flex; align-items:center; gap:12px; font-size:13px; color:#64748b;
}
.prop-select, .prop-input {
  padding:6px 10px; border-radius:6px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:13px; outline:none;
  flex:1;
}
.prop-textarea {
  padding:6px 10px; border-radius:6px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:13px; outline:none;
  font-family:inherit; resize:vertical; flex:1;
}
.prop-select:focus, .prop-input:focus, .prop-textarea:focus { border-color:#3b82f6; }

/* Info grid for view mode */
.info-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
.info-item { display:flex; gap:8px; font-size:13px; }
.info-item--full { grid-column:span 2; }
.info-label { color:#94a3b8; min-width:60px; flex-shrink:0; }

.tag-list { display:flex; gap:6px; flex-wrap:wrap; }
.tag-chip {
  font-size:12px; padding:3px 10px; border-radius:12px;
  background:#f8fafc; border:1px solid #e2e8f0;
}

.not-found { text-align:center; padding:60px 0; color:#94a3b8; }
</style>
