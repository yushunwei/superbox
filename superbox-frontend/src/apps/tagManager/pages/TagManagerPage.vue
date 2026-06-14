<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { tagCategoryApi, tagApi, type TagCategory, type Tag } from '@/api/modules/tagManager'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Folder } from '@element-plus/icons-vue'

// ── Categories ──
const categories = ref<TagCategory[]>([])
const selectedCategoryId = ref<number | null>(null)
const categoryLoading = ref(false)

// Category dialog
const categoryDialog = ref(false)
const categoryForm = ref({ name: '', color: '#409EFF' })
const editingCategoryId = ref<number | null>(null)

async function loadCategories() {
  categoryLoading.value = true
  try {
    const res = await tagCategoryApi.list()
    categories.value = (res as any).data
    if (categories.value.length && !selectedCategoryId.value) {
      selectedCategoryId.value = categories.value[0].id!
    }
  } finally { categoryLoading.value = false }
}

function openNewCategory() {
  editingCategoryId.value = null
  categoryForm.value = { name: '', color: '#409EFF' }
  categoryDialog.value = true
}

function openEditCategory(cat: TagCategory) {
  editingCategoryId.value = cat.id!
  categoryForm.value = { name: cat.name, color: cat.color }
  categoryDialog.value = true
}

async function saveCategory() {
  if (!categoryForm.value.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (editingCategoryId.value) {
      await tagCategoryApi.update(editingCategoryId.value, categoryForm.value)
      ElMessage.success('分类已更新')
    } else {
      await tagCategoryApi.create(categoryForm.value)
      ElMessage.success('分类已创建')
    }
    categoryDialog.value = false
    await loadCategories()
  } catch { ElMessage.error('操作失败') }
}

async function deleteCategory(cat: TagCategory) {
  try {
    // Count tags under this category to show in the confirmation message
    let tagCount = 0
    try {
      const res = await tagApi.list(cat.id!)
      tagCount = ((res as any).data || []).length
    } catch { /* proceed with count=0 */ }

    const message = tagCount > 0
      ? `确定要删除分类「${cat.name}」吗？该分类下的 ${tagCount} 个标签将变为"未分类"状态，标签本身不会被删除。`
      : `确定要删除分类「${cat.name}」吗？该分类下没有标签，可安全删除。`

    await ElMessageBox.confirm(message, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
    await tagCategoryApi.delete(cat.id!)
    if (selectedCategoryId.value === cat.id) selectedCategoryId.value = null
    await loadCategories()
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

function selectCategory(id: number) {
  selectedCategoryId.value = id
}

// ── Tags ──
const tags = ref<Tag[]>([])
const tagsLoading = ref(false)

// Tag dialog
const tagDialog = ref(false)
const tagForm = ref({ name: '', color: '#409EFF' })
const editingTagId = ref<number | null>(null)

async function loadTags() {
  tagsLoading.value = true
  try {
    const res = await tagApi.list(selectedCategoryId.value ?? undefined)
    tags.value = (res as any).data
  } finally { tagsLoading.value = false }
}

function openNewTag() {
  editingTagId.value = null
  tagForm.value = { name: '', color: '#409EFF' }
  tagDialog.value = true
}

function openEditTag(tag: Tag) {
  editingTagId.value = tag.id!
  tagForm.value = { name: tag.name, color: tag.color }
  tagDialog.value = true
}

async function saveTag() {
  if (!tagForm.value.name.trim()) { ElMessage.warning('请输入标签名称'); return }
  try {
    const data = { ...tagForm.value, categoryId: selectedCategoryId.value }
    if (editingTagId.value) {
      await tagApi.update(editingTagId.value, data)
      ElMessage.success('标签已更新')
    } else {
      await tagApi.create(data)
      ElMessage.success('标签已创建')
    }
    tagDialog.value = false
    await loadTags()
  } catch { ElMessage.error('操作失败') }
}

async function deleteTag(tag: Tag) {
  try {
    await ElMessageBox.confirm(`确定要删除标签「${tag.name}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch { return }
  try {
    await tagApi.delete(tag.id!)
    await loadTags()
    ElMessage.success('已删除')
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

const colorOptions = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8B5CF6', '#EC4899', '#14B8A6']

onMounted(async () => {
  await loadCategories()
  if (selectedCategoryId.value) await loadTags()
})

// Reload tags when category changes
async function onCategoryChange(id: number) {
  selectCategory(id)
  await loadTags()
}
</script>

<template>
  <div class="tag-manager">
    <!-- Left: Categories -->
    <aside class="tm-sidebar">
      <div class="tm-sidebar__header">
        <span class="tm-sidebar__title">标签分类</span>
        <el-button :icon="Plus" size="small" circle @click="openNewCategory" title="新增分类"/>
      </div>
      <div class="tm-sidebar__list" v-loading="categoryLoading">
        <div
          v-for="cat in categories"
          :key="cat.id"
          :class="['tm-cat-item', { active: selectedCategoryId === cat.id }]"
          @click="onCategoryChange(cat.id!)"
        >
          <span class="tm-cat-item__dot" :style="{ background: cat.color }"></span>
          <span class="tm-cat-item__name">{{ cat.name }}</span>
          <span class="tm-cat-item__actions">
            <el-button :icon="Edit" size="small" text @click.stop="openEditCategory(cat)" title="编辑"/>
            <el-button :icon="Delete" size="small" text @click.stop="deleteCategory(cat)" title="删除"/>
          </span>
        </div>
        <div v-if="!categories.length" class="tm-empty">暂无分类，点击 + 新增</div>
      </div>
    </aside>

    <!-- Right: Tags -->
    <main class="tm-main">
      <div class="tm-main__header">
        <span class="tm-main__title">
          <el-icon><Folder /></el-icon>
          {{ categories.find(c => c.id === selectedCategoryId)?.name || '标签列表' }}
        </span>
        <el-button type="primary" :icon="Plus" size="small" @click="openNewTag" :disabled="!selectedCategoryId">
          新增标签
        </el-button>
      </div>
      <div class="tm-tags" v-loading="tagsLoading">
        <div v-if="!selectedCategoryId" class="tm-empty">请先选择一个分类</div>
        <template v-else>
          <div v-for="tag in tags" :key="tag.id" class="tm-tag-item">
            <span class="tm-tag-item__badge" :style="{ background: tag.color }">{{ tag.name }}</span>
            <span class="tm-tag-item__actions">
              <el-button :icon="Edit" size="small" text @click="openEditTag(tag)" title="编辑"/>
              <el-button :icon="Delete" size="small" text @click="deleteTag(tag)" title="删除"/>
            </span>
          </div>
          <div v-if="!tags.length" class="tm-empty">此分类下暂无标签</div>
        </template>
      </div>
    </main>

    <!-- Category Edit Dialog -->
    <el-dialog v-model="categoryDialog" :title="editingCategoryId ? '编辑分类' : '新增分类'" width="380px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="categoryForm.name" placeholder="分类名称" maxlength="32"/>
        </el-form-item>
        <el-form-item label="颜色">
          <div class="tm-color-picker">
            <span
              v-for="c in colorOptions"
              :key="c"
              :class="['tm-color-dot', { active: categoryForm.color === c }]"
              :style="{ background: c }"
              @click="categoryForm.color = c"
            ></span>
            <el-color-picker v-model="categoryForm.color" size="small" :predefine="colorOptions"/>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- Tag Edit Dialog -->
    <el-dialog v-model="tagDialog" :title="editingTagId ? '编辑标签' : '新增标签'" width="380px" destroy-on-close>
      <el-form label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="tagForm.name" placeholder="标签名称" maxlength="32"/>
        </el-form-item>
        <el-form-item label="颜色">
          <div class="tm-color-picker">
            <span
              v-for="c in colorOptions"
              :key="c"
              :class="['tm-color-dot', { active: tagForm.color === c }]"
              :style="{ background: c }"
              @click="tagForm.color = c"
            ></span>
            <el-color-picker v-model="tagForm.color" size="small" :predefine="colorOptions"/>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTag">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tag-manager {
  display: flex; height: 100%; background: #ffffff; border-radius: 4px;
  border: 1px solid #e4e7ed; overflow: hidden;
}
/* ── Sidebar ── */
.tm-sidebar { width: 220px; border-right: 1px solid #ebeef5; display: flex; flex-direction: column; flex-shrink: 0; }
.tm-sidebar__header { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid #ebeef5; }
.tm-sidebar__title { font-size: 13px; font-weight: 600; color: #303133; }
.tm-sidebar__list { flex: 1; overflow-y: auto; padding: 4px 0; }
.tm-cat-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 14px; cursor: pointer;
  transition: all 0.15s ease; font-size: 13px; color: #606266;
}
.tm-cat-item:hover { background: #f5f7fa; }
.tm-cat-item.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.tm-cat-item__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.tm-cat-item__name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tm-cat-item__actions { display: none; }
.tm-cat-item:hover .tm-cat-item__actions { display: flex; gap: 2px; }
/* ── Main ── */
.tm-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.tm-main__header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #ebeef5; }
.tm-main__title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #303133; }
.tm-tags { flex: 1; overflow-y: auto; padding: 12px 16px; display: flex; flex-wrap: wrap; align-content: flex-start; gap: 10px; }
.tm-tag-item {
  display: flex; align-items: center; gap: 6px; padding: 6px 12px; border: 1px solid #ebeef5;
  border-radius: 4px; transition: all 0.15s ease;
}
.tm-tag-item:hover { border-color: #c6e2ff; background: #ecf5ff; }
.tm-tag-item:hover .tm-tag-item__actions { display: flex; }
.tm-tag-item__actions { display: none; gap: 2px; }
.tm-tag-item__badge {
  display: inline-flex; align-items: center; padding: 3px 12px; border-radius: 3px;
  font-size: 12px; color: #ffffff; font-weight: 500; white-space: nowrap;
}
.tm-empty { padding: 40px 20px; text-align: center; font-size: 13px; color: #c0c4cc; width: 100%; }
/* ── Color picker ── */
.tm-color-picker { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.tm-color-dot { width: 22px; height: 22px; border-radius: 3px; cursor: pointer; border: 2px solid transparent; transition: transform 0.12s ease; }
.tm-color-dot:hover { transform: scale(1.15); }
.tm-color-dot.active { border-color: #303133; transform: scale(1.15); }
</style>
