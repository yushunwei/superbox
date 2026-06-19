<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Edit, Delete } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import { useTranslateStore } from '../stores/translateStore'
import LanguageSelector from '../components/LanguageSelector.vue'
import type { GlossaryEntry, LangCode } from '../types'

const store = useTranslateStore()

const keyword = ref('')
const filterSourceLang = ref('')
const filterTargetLang = ref('')
const filterCategory = ref('')
const page = ref(1)
const pageSize = ref(50)

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  sourceLang: 'zh' as string,
  targetLang: 'en' as string,
  sourceTerm: '',
  targetTerm: '',
  category: '',
  note: '',
})

// Import dialog
const importVisible = ref(false)
const importFile = ref<UploadFile[]>([])
const importing = ref(false)

const categoryOptions = ['技术', '法律', '医学', '商务', '文学', '口语', '其他']

onMounted(async () => {
  await Promise.all([store.fetchLanguages(), fetchData()])
})

async function fetchData() {
  await store.fetchGlossary({
    page: page.value,
    size: pageSize.value,
    keyword: keyword.value || undefined,
    sourceLang: filterSourceLang.value || undefined,
    targetLang: filterTargetLang.value || undefined,
  })
}

function openNew() {
  editingId.value = null
  form.value = { sourceLang: 'zh', targetLang: 'en', sourceTerm: '', targetTerm: '', category: '', note: '' }
  dialogVisible.value = true
}

function openEdit(row: GlossaryEntry) {
  editingId.value = row.id
  form.value = {
    sourceLang: row.sourceLang,
    targetLang: row.targetLang,
    sourceTerm: row.sourceTerm,
    targetTerm: row.targetTerm,
    category: row.category || '',
    note: row.note || '',
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.sourceTerm.trim() || !form.value.targetTerm.trim()) {
    ElMessage.warning('请填写源术语和目标术语')
    return
  }
  try {
    if (editingId.value) {
      await store.updateGlossaryEntry(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await store.createGlossaryEntry(form.value)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    await fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: GlossaryEntry) {
  try {
    await ElMessageBox.confirm(`确定要删除术语「${row.sourceTerm} → ${row.targetTerm}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await store.deleteGlossaryEntry(row.id)
    await fetchData()
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
}

async function handleImport() {
  if (!importFile.value.length) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    const file = importFile.value[0].raw
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    const result = await store.importGlossaryCsv(formData)
    ElMessage.success(`成功导入 ${result.imported} 条术语`)
    importVisible.value = false
    importFile.value = []
    await fetchData()
  } catch { ElMessage.error('导入失败') }
  finally { importing.value = false }
}

async function handleExport() {
  try {
    const res = await store.exportGlossaryCsv({
      sourceLang: filterSourceLang.value || undefined,
      targetLang: filterTargetLang.value || undefined,
    })
    const blob = (res as any).data
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'glossary_export.csv'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') }
}

function onSearch() {
  page.value = 1
  fetchData()
}
</script>

<template>
  <div class="ait-glossary">
    <div class="ait-panel">
      <h3 class="ait-panel__title">术语表管理</h3>

      <!-- Toolbar -->
      <div class="ait-glossary-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索术语..."
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <LanguageSelector v-model="filterSourceLang" placeholder="源语言" />
        <LanguageSelector v-model="filterTargetLang" placeholder="目标语言" />
        <el-select v-model="filterCategory" placeholder="分类" clearable style="width: 120px" @change="onSearch">
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <div class="ait-toolbar-spacer" />
        <el-button :icon="Upload" @click="importVisible = true">导入CSV</el-button>
        <el-button :icon="Download" @click="handleExport">导出CSV</el-button>
        <el-button type="primary" :icon="Plus" @click="openNew">新增术语</el-button>
      </div>

      <!-- Table -->
      <el-table :data="store.glossary" v-loading="store.glossaryLoading" stripe style="width: 100%">
        <el-table-column prop="sourceTerm" label="源术语" min-width="160" show-overflow-tooltip />
        <el-table-column prop="targetTerm" label="目标术语" min-width="160" show-overflow-tooltip />
        <el-table-column label="语言对" width="110">
          <template #default="{ row }">
            {{ row.sourceLang?.toUpperCase() }} → {{ row.targetLang?.toUpperCase() }}
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Edit" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button :icon="Delete" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="ait-pagination" v-if="store.glossaryTotal > pageSize">
        <el-pagination
          v-model:current-page="page"
          :total="store.glossaryTotal"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑术语' : '新增术语'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="源语言">
          <LanguageSelector v-model="form.sourceLang" />
        </el-form-item>
        <el-form-item label="目标语言">
          <LanguageSelector v-model="form.targetLang" />
        </el-form-item>
        <el-form-item label="源术语">
          <el-input v-model="form.sourceTerm" placeholder="源语言术语" />
        </el-form-item>
        <el-form-item label="目标术语">
          <el-input v-model="form.targetTerm" placeholder="目标语言术语" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="备注说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- Import Dialog -->
    <el-dialog v-model="importVisible" title="导入术语表" width="420px" destroy-on-close>
      <el-upload
        v-model:file-list="importFile"
        :auto-upload="false"
        :limit="1"
        accept=".csv"
        drag
      >
        <p>将 CSV 文件拖到此处，或<em>点击上传</em></p>
        <p style="font-size:12px;color:#c0c4cc;margin-top:4px">格式：source_term, target_term, category, note</p>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!importFile.length">
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ait-glossary {
  max-width: 1100px;
  margin: 0 auto;
}
.ait-panel {
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  padding: 20px;
}
.ait-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}
.ait-glossary-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.ait-toolbar-spacer {
  flex: 1;
}
.ait-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
