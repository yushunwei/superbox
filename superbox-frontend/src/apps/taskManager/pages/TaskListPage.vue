<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useTaskStore } from '@/stores/modules/taskManagerStore'
import SpreadsheetView from '@/components/spreadsheet/SpreadsheetView.vue'
import type { ColumnDef } from '@/components/spreadsheet/SpreadsheetView.vue'
import type { Task } from '@/api/modules/taskManager'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, Upload, Operation } from '@element-plus/icons-vue'
import apiClient from '@/api'

const store = useTaskStore()
const quickKeyword = ref('')
const importing = ref(false)
const fileInput = ref<HTMLInputElement>()
const ioDialog = ref(false)
const exportMode = ref<'all' | 'filtered'>('all')
const dragOver = ref(false)

function downloadBlob(data: Blob, filename: string) {
  const url = URL.createObjectURL(data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function downloadTemplate() {
  try {
    const res = await apiClient.get('/task-manager/template', { responseType: 'blob' })
    downloadBlob(res.data as Blob, '任务导入模板.xlsx')
  } catch {
    ElMessage.error('下载模板失败')
  }
}

async function doExport() {
  try {
    const params: Record<string, string> = {}
    if (exportMode.value === 'filtered') {
      const f = store.filters
      if (f.keyword) params.keyword = String(f.keyword)
      if (f.status) params.status = f.status
      if (f.priority) params.priority = f.priority
      if (f.executor) params.executor = f.executor
      if (f.tagId) params.tagId = String(f.tagId)
    }
    const res = await apiClient.get('/task-manager/export', { params, responseType: 'blob' })
    downloadBlob(res.data as Blob, '任务列表.xlsx')
  } catch {
    ElMessage.error('导出失败')
  }
}

async function importTasks(file: File) {
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await apiClient.post('/task-manager/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    ElMessage.success(`导入完成：成功 ${res.data.created} 条，跳过 ${res.data.errors} 条`)
    store.fetchTasks()
    ioDialog.value = false
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importing.value = false
  }
}

function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    importTasks(input.files[0])
    input.value = ''
  }
}

function onDragOver(e: DragEvent) { e.preventDefault(); dragOver.value = true }
function onDragLeave() { dragOver.value = false }
function onDrop(e: DragEvent) {
  e.preventDefault(); dragOver.value = false
  if (e.dataTransfer?.files?.length) importTasks(e.dataTransfer.files[0])
}

const columns: ColumnDef[] = [
  { key: 'title',         label: '任务',       width: 220, type: 'text',   editable: true,  filterable: true },
  { key: 'description',   label: '描述',       width: 200, type: 'text',   editable: true,  filterable: false },
  { key: 'tags',          label: '标签',       width: 160, type: 'tags',   editable: false, filterable: true },
  { key: 'priority',      label: '重要等级',   width: 90,  type: 'select', editable: true,  filterable: true,
    options: [{ value: 'normal', label: '一般' }, { value: 'important', label: '重要' }] },
  { key: 'planStartDate', label: '计划开始',   width: 110, type: 'date',   editable: true,  filterable: false },
  { key: 'planEndDate',   label: '计划结束',   width: 110, type: 'date',   editable: true,  filterable: false },
  { key: 'executor',      label: '执行人',     width: 100, type: 'text',   editable: true,  filterable: true },
  { key: 'collaborators', label: '协同人',     width: 100, type: 'text',   editable: true,  filterable: false },
  { key: 'status',        label: '状态',       width: 90,  type: 'select', editable: true,  filterable: true,
    options: [
      { value: 'not_started', label: '未开始' },
      { value: 'in_progress', label: '进行中' },
      { value: 'completed',   label: '已完成' },
      { value: 'cancelled',    label: '已作废' },
    ]},
  { key: 'completedAt',   label: '完成时间',   width: 110, type: 'date',   editable: true,  filterable: false },
  { key: 'remarks',       label: '备注说明',   width: 180, type: 'text',   editable: true,  filterable: false },
]

onMounted(() => {
  store.fetchTasks()
})

async function handleCellChange(id: number, field: string, value: unknown) {
  try {
    await store.updateCell(id, field, value)
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleInsertRow(afterId: number | null) {
  try {
    await store.insertRow(afterId)
    ElMessage.success('已插入行')
  } catch {
    ElMessage.error('插入失败')
  }
}

async function handleDeleteRows(ids: number[]) {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 行吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await store.batchDeleteTasks(ids)
    ElMessage.success(`已删除 ${ids.length} 行`)
  } catch {
    /* cancelled or failed */
  }
}

async function handlePasteRows(afterId: number | null, data: Record<string, unknown>[]) {
  try {
    await store.batchPaste(afterId, data as Partial<Task>[])
    ElMessage.success(`已粘贴 ${data.length} 行`)
  } catch {
    ElMessage.error('粘贴失败')
  }
}

const CLEAR_VALUES: Record<string, unknown> = {
  title: '', description: '', priority: 'normal', status: 'not_started',
  planStartDate: '', planEndDate: '', executor: '', collaborators: '',
  completedAt: '', remarks: '',
}

async function handlePasteOverCells(targetId: number, sourceData: Record<string, unknown>, sourceId: number) {
  try {
    // Overwrite target row cells with clipboard data
    for (const [field, value] of Object.entries(sourceData)) {
      await store.updateCell(targetId, field, value)
    }
    // Clear source row cells
    for (const [field, value] of Object.entries(CLEAR_VALUES)) {
      await store.updateCell(sourceId, field, value)
    }
    ElMessage.success('已粘贴（覆盖）')
  } catch {
    ElMessage.error('粘贴失败')
  }
}

async function handlePasteInsertCut(targetId: number, sourceData: Record<string, unknown>, sourceId: number) {
  try {
    // Find the row BEFORE target to get afterId for insertion
    const rows = store.tasks
    const targetIdx = rows.findIndex(r => r.id === targetId)
    const afterId = targetIdx > 0 ? rows[targetIdx - 1].id : null
    // Insert new row with clipboard data before target
    await store.batchPaste(afterId, [sourceData as Partial<Task>])
    // Delete source row
    await store.batchDeleteTasks([sourceId])
    ElMessage.success('已插入剪切单元格')
  } catch {
    ElMessage.error('操作失败')
  }
}

const lastColumnFilters = ref<Record<string, string>>({})

function buildQuery(extra?: Record<string, string>) {
  const f = { ...lastColumnFilters.value, ...extra }
  return {
    keyword: quickKeyword.value || f.title || undefined,
    status: f.status || undefined,
    priority: f.priority || undefined,
    executor: f.executor || undefined,
    tagId: f.tags || undefined,
  }
}

function doSearch() {
  store.fetchTasks(buildQuery())
}

function clearSearch() {
  quickKeyword.value = ''
  store.fetchTasks(buildQuery())
}

function handleRefresh(filters?: Record<string, string>) {
  lastColumnFilters.value = filters || {}
  store.fetchTasks(buildQuery())
}
</script>

<template>
  <div class="task-list-page">
    <!-- Top toolbar: search + import/export -->
    <div class="top-toolbar">
      <div class="search-bar">
        <el-input
          v-model="quickKeyword"
          placeholder="输入关键词回车检索"
          clearable
          @keydown.enter="doSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :icon="Operation" @click="ioDialog = true">导入/导出</el-button>
      </div>
    </div>

    <!-- Import/Export Dialog -->
    <el-dialog v-model="ioDialog" title="导入 / 导出" width="420px" destroy-on-close>
      <!-- Export section -->
      <div class="io-section">
        <div class="io-section__title">导出任务</div>
        <div class="io-export-row">
          <el-radio-group v-model="exportMode" size="small">
            <el-radio value="all">导出全部任务</el-radio>
            <el-radio value="filtered">仅导出当前筛选结果</el-radio>
          </el-radio-group>
          <el-button size="small" :icon="Download" @click="doExport">导出 Excel</el-button>
        </div>
      </div>
      <el-divider/>
      <!-- Template section -->
      <div class="io-section">
        <div class="io-section__title">导入任务</div>
        <el-button size="small" :icon="Download" @click="downloadTemplate">下载导入模板</el-button>
      </div>
      <div
        :class="['io-drop', { 'io-drop--over': dragOver }]"
        @dragover="onDragOver"
        @dragleave="onDragLeave"
        @drop.prevent="onDrop"
        @click="fileInput?.click()"
      >
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="handleFileChange"/>
        <div v-if="!importing" style="text-align:center">
          <el-icon :size="28"><Upload /></el-icon>
          <p>{{ dragOver ? '释放文件上传' : '拖拽文件到此处，或点击上传' }}</p>
        </div>
        <div v-else style="text-align:center"><p>正在导入...</p></div>
      </div>
    </el-dialog>

    <SpreadsheetView
      :columns="columns"
      :rows="store.tasks"
      :loading="store.loading"
      page-key="task-manager"
      @cell-change="handleCellChange"
      @insert-row="handleInsertRow"
      @delete-rows="handleDeleteRows"
      @paste-rows="handlePasteRows"
      @paste-over-cells="handlePasteOverCells"
      @paste-insert-cut="handlePasteInsertCut"
      @refresh="handleRefresh"
    />
  </div>
</template>

<style scoped>
.task-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}
.top-toolbar {
  position: fixed;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 55%;
  min-width: 520px;
  z-index: 101;
  display: flex;
  gap: 10px;
  align-items: center;
}
.search-bar {
  flex: 1;
}
.search-bar :deep(.el-input__wrapper) {
  border-radius: 10px;
}
.toolbar-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.io-section { margin-bottom: 8px; }
.io-section__title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.io-export-row { display: flex; align-items: center; gap: 16px; }
.io-drop {
  display: block;
  border: 2px dashed #dcdfe6; border-radius: 8px;
  padding: 24px; margin-top: 12px; cursor: pointer;
  transition: all 0.15s ease; text-align: center;
  font-size: 12px; color: #909399;
}
.io-drop:hover, .io-drop--over { border-color: #409EFF; background: #ecf5ff; color: #409EFF; }
.io-drop p { margin: 8px 0 0; }
</style>
