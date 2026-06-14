<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import type { Task } from '@/api/modules/taskManager'
import { spreadsheetSettingsApi } from '@/api/modules/spreadsheetSettings'
import { tagApi, type Tag } from '@/api/modules/tagManager'
import { ElMessageBox } from 'element-plus'

// ── Column definition ──
export interface ColumnDef {
  key: string
  label: string
  width: number
  type: 'display' | 'text' | 'select' | 'date' | 'tags'
  options?: { value: string; label: string }[]
  editable: boolean
  filterable: boolean
}

const props = defineProps<{
  columns: ColumnDef[]
  rows: Task[]
  loading?: boolean
  pageKey?: string
}>()

const emit = defineEmits<{
  cellChange: [id: number, field: string, value: unknown]
  insertRow: [afterId: number | null]
  deleteRows: [ids: number[]]
  pasteRows: [afterId: number | null, data: Record<string, unknown>[]]
  pasteOverCells: [targetId: number, sourceData: Record<string, unknown>, sourceId: number]
  pasteInsertCut: [targetId: number, sourceData: Record<string, unknown>, sourceId: number]
  refresh: [filters: Record<string, string>]
}>()

// ── Persistence (localStorage + backend) ──
const COL_STORAGE_KEY = 'superbox-sp-col-widths'
const ALIGN_STORAGE_KEY = 'superbox-sp-col-aligns'
const WRAP_STORAGE_KEY = 'superbox-sp-col-wraps'
const ROW_H_STORAGE_KEY = 'superbox-sp-row-heights'

function loadJson(key: string): Record<string, unknown> {
  try { const r = localStorage.getItem(key); return r ? JSON.parse(r) : {} } catch { return {} }
}

// Persist ALL current settings to localStorage + schedule backend sync
function persistAll() {
  saveJson(COL_STORAGE_KEY, { ...colWidths })
  saveJson(ALIGN_STORAGE_KEY, { ...colAligns })
  saveJson(WRAP_STORAGE_KEY, { ...colWraps })
  saveJson(ROW_H_STORAGE_KEY, { ...rowHeights })
  scheduleBackendSave()
}

function saveJson(key: string, v: Record<string, unknown>) {
  try { localStorage.setItem(key, JSON.stringify(v)) } catch { /* quota */ }
}

// Debounced backend save — sends full current state of all settings
let saveTimer: ReturnType<typeof setTimeout> | null = null
function scheduleBackendSave() {
  if (!props.pageKey) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    spreadsheetSettingsApi.save({
      pageKey: props.pageKey!,
      colWidths: { ...colWidths },
      colAligns: { ...colAligns },
      colWraps: { ...colWraps },
      rowHeights: { ...rowHeights },
    }).catch(() => { /* silently ignore */ })
  }, 500)
}

// Fetch backend settings on mount, merge over localStorage defaults
async function loadBackendAndMerge() {
  if (!props.pageKey) return
  try {
    const res = await spreadsheetSettingsApi.get(props.pageKey)
    const s: any = (res as any).data
    if (s) {
      const parseIfString = (v: any) => typeof v === 'string' ? JSON.parse(v) : v
      if (s.colWidths)  Object.assign(colWidths, parseIfString(s.colWidths))
      if (s.colAligns)  Object.assign(colAligns, parseIfString(s.colAligns))
      if (s.colWraps)   Object.assign(colWraps, parseIfString(s.colWraps))
      if (s.rowHeights) Object.assign(rowHeights, parseIfString(s.rowHeights))
      persistAll() // sync merged state back to localStorage
    }
  } catch { /* ignore fetch errors */ }
}

// Initialize: columns always get all current keys with defaults
const savedWidths = loadJson(COL_STORAGE_KEY) as Record<string, number>
const colWidths = reactive<Record<string, number>>(
  Object.fromEntries(props.columns.map(c => [c.key, savedWidths[c.key] || c.width]))
)

// Table total width = rownum(36) + sum of all data columns
const tableWidth = computed(() => {
  const sum = 36 + Object.values(colWidths).reduce((a, b) => a + b, 0)
  return sum + 'px'
})

// ── Column resize drag ──
const resizing = ref<{ key: string; startX: number; startW: number } | null>(null)

function onResizeDown(e: MouseEvent, key: string) {
  e.preventDefault(); e.stopPropagation()
  resizing.value = { key, startX: e.clientX, startW: colWidths[key] }
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
function onResizeMove(e: MouseEvent) {
  if (!resizing.value) return
  colWidths[resizing.value.key] = Math.max(40, resizing.value.startW + e.clientX - resizing.value.startX)
}
function onResizeUp() {
  if (!resizing.value) return
  persistAll()
  resizing.value = null
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeUp)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// ── Column text alignment ──
const savedAligns = loadJson(ALIGN_STORAGE_KEY) as Record<string, string>
const colAligns = reactive<Record<string, string>>(
  Object.fromEntries(props.columns.map(c => [c.key, savedAligns[c.key] || 'left']))
)
function setColAlign(key: string, align: string) {
  colAligns[key] = align
  persistAll()
  closeContextMenu()
}

// ── Column text wrapping ──
const savedWraps = loadJson(WRAP_STORAGE_KEY) as Record<string, boolean>
const colWraps = reactive<Record<string, boolean>>(
  Object.fromEntries(props.columns.map(c => [c.key, !!savedWraps[c.key]]))
)
function toggleColWrap(key: string) {
  colWraps[key] = !colWraps[key]
  persistAll()
  closeContextMenu()
}

// ── Row height persistence & drag ──
const savedRowHeights = loadJson(ROW_H_STORAGE_KEY) as Record<string, number>
const rowHeights = reactive<Record<string, number>>({ ...savedRowHeights })
const DEFAULT_ROW_H = 36

function getRowHeight(rowId: number): string {
  const h = rowHeights[rowId]
  return h ? h + 'px' : ''
}

const rowResizing = ref<{ rowId: number; startY: number; startH: number } | null>(null)

function onRowResizeDown(e: MouseEvent, rowId: number) {
  e.preventDefault(); e.stopPropagation()
  const el = (e.target as HTMLElement).closest('tr')
  const h = el ? el.getBoundingClientRect().height : (rowHeights[rowId] || DEFAULT_ROW_H)
  rowResizing.value = { rowId, startY: e.clientY, startH: h }
  document.addEventListener('mousemove', onRowResizeMove)
  document.addEventListener('mouseup', onRowResizeUp)
  document.body.style.cursor = 'row-resize'
  document.body.style.userSelect = 'none'
}
function onRowResizeMove(e: MouseEvent) {
  if (!rowResizing.value) return
  const newH = Math.max(22, rowResizing.value.startH + e.clientY - rowResizing.value.startY)
  rowHeights[rowResizing.value.rowId] = newH
}
function onRowResizeUp() {
  if (!rowResizing.value) return
  persistAll()
  rowResizing.value = null
  document.removeEventListener('mousemove', onRowResizeMove)
  document.removeEventListener('mouseup', onRowResizeUp)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

// ── Editing ──
const editingCell = ref<{ id: number; field: string } | null>(null)
const editValue = ref('')
const editInputRef = ref<any>(null)

function startEdit(id: number, field: string, currentValue: unknown) {
  if (editingCell.value) commitEdit()
  editingCell.value = { id, field }
  const col = props.columns.find(c => c.key === field)
  if (col?.type === 'date' && currentValue) {
    editValue.value = String(currentValue).slice(0, 10)
  } else {
    editValue.value = currentValue != null ? String(currentValue) : ''
  }
  nextTick(() => {
    const el = editInputRef.value
    if (el) {
      if (Array.isArray(el)) el[0]?.focus(); else if (el.focus) el.focus()
      // Auto-open date picker on double-click
      if (col?.type === 'date' && el.showPicker) el.showPicker()
    }
  })
}
function commitEdit() {
  if (!editingCell.value) return
  const { id, field } = editingCell.value
  const col = props.columns.find(c => c.key === field)
  let value: unknown = editValue.value
  if (col?.type === 'date') value = editValue.value || null
  emit('cellChange', id, field, value)
  editingCell.value = null
}
function cancelEdit() { editingCell.value = null }
function handleEditKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') { e.preventDefault(); commitEdit() }
  else if (e.key === 'Escape') { e.preventDefault(); cancelEdit() }
  else if (e.key === 'Tab') { e.preventDefault(); commitEdit() }
}
function onDoubleClick(id: number, field: string, currentValue: unknown) {
  const col = props.columns.find(c => c.key === field)
  if (col?.editable) startEdit(id, field, currentValue)
}

// ── Filtering ──
const activeFilters = ref<Record<string, string>>({})
const filterDropdown = ref<string | null>(null)
const filterInput = ref('')
const filterBackup = ref<Record<string, string>>({})

function toggleFilter(key: string, colType?: string) {
  if (filterDropdown.value === key) {
    filterDropdown.value = null
  } else {
    filterInput.value = activeFilters.value[key] || ''
    filterBackup.value = { ...activeFilters.value }
    filterDropdown.value = key
    tagSearch.value = ''
    if (colType === 'tags') loadAllTags()
  }
}
function applyFilter(key: string, value: string) {
  if (value) activeFilters.value = { ...activeFilters.value, [key]: value }
  else { const n = { ...activeFilters.value }; delete n[key]; activeFilters.value = n }
  filterDropdown.value = null
  filterInput.value = ''
  emit('refresh', { ...activeFilters.value })
}
// Toggle a single value in a comma-separated multi-select filter
function toggleMultiFilter(key: string, value: string) {
  const current = activeFilters.value[key] || ''
  const parts = current ? current.split(',').filter(Boolean) : []
  const idx = parts.indexOf(value)
  if (idx >= 0) parts.splice(idx, 1)
  else parts.push(value)
  activeFilters.value = { ...activeFilters.value, [key]: parts.join(',') }
}
function isMultiSelected(key: string, value: string): boolean {
  const current = activeFilters.value[key] || ''
  return current.split(',').includes(value)
}
function toggleAllFilter(key: string, allValues: string[]) {
  const current = activeFilters.value[key] || ''
  const parts = current ? current.split(',').filter(Boolean) : []
  if (parts.length === allValues.length) {
    activeFilters.value = { ...activeFilters.value, [key]: '' }
  } else {
    activeFilters.value = { ...activeFilters.value, [key]: allValues.join(',') }
  }
}
function isAllSelected(key: string, allValues: string[]): boolean {
  const current = activeFilters.value[key] || ''
  const parts = current ? current.split(',').filter(Boolean) : []
  return parts.length === allValues.length
}
function confirmMultiFilter() {
  filterDropdown.value = null
  emit('refresh', { ...activeFilters.value })
}
function cancelMultiFilter() {
  activeFilters.value = { ...filterBackup.value }
  filterDropdown.value = null
}
function confirmTextFilter(key: string) {
  applyFilter(key, filterInput.value)
}
function resetTextFilter() {
  filterInput.value = ''
}
function cancelTextFilter() {
  filterDropdown.value = null
  filterInput.value = ''
}
function clearAllFilters() {
  activeFilters.value = {}
  filterDropdown.value = null
  filterInput.value = ''
  emit('refresh', {})
}

// Fuzzy match: collect distinct cell values for a column, filtered by input
function filteredColumnValues(colKey: string): string[] {
  const input = filterInput.value.toLowerCase().trim()
  const seen = new Set<string>()
  const matches: string[] = []
  for (const row of props.rows) {
    const val = cellDisplay(row, colKey)
    if (!val) continue
    const lowered = val.toLowerCase()
    if (!input || lowered.includes(input)) {
      if (!seen.has(lowered)) {
        seen.add(lowered)
        matches.push(val)
      }
    }
  }
  return matches.slice(0, 20)
}

// ── Context menu (unified for rows AND column headers) ──
const ctxMenu = ref<{ x: number; y: number; type: 'row' | 'header'; rowId?: number; colKey?: string } | null>(null)

function onRowContext(e: MouseEvent, rowId: number) {
  e.preventDefault()
  ctxMenu.value = { x: e.clientX, y: e.clientY, type: 'row', rowId }
}
function onHeaderContext(e: MouseEvent, colKey: string) {
  e.preventDefault()
  ctxMenu.value = { x: e.clientX, y: e.clientY, type: 'header', colKey }
}
function closeContextMenu() { ctxMenu.value = null }

function ctxInsertAbove() {
  if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
  const idx = props.rows.findIndex(r => r.id === ctxMenu.value!.rowId)
  emit('insertRow', idx > 0 ? props.rows[idx - 1].id : null)
  closeContextMenu()
}
function ctxInsertBelow() {
  if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
  emit('insertRow', ctxMenu.value.rowId!)
  closeContextMenu()
}
async function ctxDeleteRow() {
  if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
  try {
    await ElMessageBox.confirm('确定要删除该行吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    emit('deleteRows', [ctxMenu.value.rowId!])
  } catch { /* cancelled */ }
  closeContextMenu()
}

// ── Clipboard (copy / cut) ──
const PASTE_FIELDS = ['title', 'description', 'priority', 'planStartDate', 'planEndDate',
  'executor', 'collaborators', 'status', 'completedAt', 'remarks']

const clipboard = ref<Record<string, unknown>[]>([])
const clipboardMode = ref<'copy' | 'cut'>('copy')
const cutSourceIds = ref<number[]>([])

function extractRowData(row: Task): Record<string, unknown> {
  const d: Record<string, unknown> = {}
  for (const f of PASTE_FIELDS) d[f] = (row as any)[f]
  return d
}

function ctxCopy() {
  if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
  clipboard.value = props.rows.filter(r => r.id === ctxMenu.value!.rowId).map(extractRowData)
  clipboardMode.value = 'copy'
  closeContextMenu()
}

function ctxCut() {
  if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
  const row = props.rows.find(r => r.id === ctxMenu.value!.rowId)
  if (!row) return
  clipboard.value = [extractRowData(row)]
  clipboardMode.value = 'cut'
  cutSourceIds.value = [row.id]
  closeContextMenu()
}

function ctxPaste() {
  if (!clipboard.value.length || !ctxMenu.value || ctxMenu.value.type !== 'row') return
  if (clipboardMode.value === 'cut') {
    // Cut-mode paste: overwrite target cells, clear source cells, keep both rows
    const targetId = ctxMenu.value.rowId!
    const sourceData = clipboard.value[0]
    const sourceId = cutSourceIds.value[0]
    emit('pasteOverCells', targetId, sourceData, sourceId)
  } else {
    // Copy-mode paste: insert new rows with data
    emit('pasteRows', ctxMenu.value.rowId!, [...clipboard.value])
  }
  closeContextMenu()
}

function ctxPasteInsert() {
  // Only available in cut mode: insert row before target + delete source
  if (!clipboard.value.length || clipboardMode.value !== 'cut' || !ctxMenu.value || ctxMenu.value.type !== 'row') return
  const targetId = ctxMenu.value.rowId!
  const sourceData = clipboard.value[0]
  const sourceId = cutSourceIds.value[0]
  emit('pasteInsertCut', targetId, sourceData, sourceId)
  closeContextMenu()
}

// Keyboard shortcuts
function onKeydown(e: KeyboardEvent) {
  if (editingCell.value) return
  // Need a row selected for copy/cut - use last right-clicked row
  if ((e.ctrlKey && e.key === 'c') || (e.ctrlKey && e.key === 'x') || (e.ctrlKey && e.key === 'v') || e.key === 'Delete') {
    if (!ctxMenu.value || ctxMenu.value.type !== 'row') return
    if (e.ctrlKey && e.key === 'c') ctxCopy()
    else if (e.ctrlKey && e.key === 'x') ctxCut()
    else if (e.ctrlKey && e.key === 'v') ctxPaste()
    else if (e.key === 'Delete') ctxDeleteRow()
  }
}

// ── Display helpers ──
function cellDisplay(row: Task, key: string): string {
  const val = (row as Record<string, unknown>)[key]
  if (val == null) return ''
  const col = props.columns.find(c => c.key === key)
  if (col?.type === 'select') return col.options?.find(o => o.value === val)?.label || String(val)
  // completedAt: date-only display, truncate ISO timestamp
  if (key === 'completedAt' && typeof val === 'string' && val.includes('T')) {
    return val.slice(0, 10)
  }
  return String(val)
}
function getStatusClass(status: string) {
  const m: Record<string, string> = {
    not_started: 'status--not-started', in_progress: 'status--in-progress',
    completed: 'status--completed', cancelled: 'status--cancelled',
  }
  return m[status] || ''
}
function getPriorityLabel(p: string) { return p === 'important' ? '重要' : '一般' }

// ── Tag management ──
const allTags = ref<Tag[]>([])
const tagPopover = ref<{ task: Task; left: number; top: number } | null>(null)
const tagSearch = ref('')

const filteredTags = computed(() => {
  const q = tagSearch.value.toLowerCase().trim()
  if (!q) return allTags.value
  return allTags.value.filter(t => t.name.toLowerCase().includes(q))
})

async function loadAllTags() {
  try {
    const res = await tagApi.list()
    const data = (res as any).data || []
    allTags.value = Array.isArray(data) ? data : []
  } catch { allTags.value = [] }
}

function getTaskTags(task: Task): Tag[] {
  return (task as any).tags || []
}

function isTagSelected(task: Task, tagId: number): boolean {
  return getTaskTags(task).some(t => t.id === tagId)
}

async function toggleTaskTag(task: Task, tag: Tag) {
  const taskId = (task as any).id
  const selected = isTagSelected(task, tag.id!)
  try {
    if (selected) {
      const { useTaskStore } = await import('@/stores/modules/taskManagerStore')
      await useTaskStore().removeTaskTag(taskId, tag.id!)
    } else {
      const currentIds = getTaskTags(task).map(t => t.id!)
      const { useTaskStore } = await import('@/stores/modules/taskManagerStore')
      await useTaskStore().setTaskTags(taskId, [...currentIds, tag.id!])
    }
  } catch { /* ignore */ }
}

function openTagPopover(task: Task, event: MouseEvent) {
  const btn = event.target as HTMLElement
  const rect = btn.getBoundingClientRect()
  tagPopover.value = { task, left: rect.left, top: rect.bottom + 2 }
  tagSearch.value = ''
  loadAllTags()
}
function closeTagPopover() { tagPopover.value = null; tagSearch.value = '' }

// Close menus on outside click
function onDocumentClick() { closeContextMenu(); filterDropdown.value = null; closeTagPopover() }
onMounted(() => { document.addEventListener('click', onDocumentClick); loadBackendAndMerge() })
onUnmounted(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <div class="spreadsheet" @keydown="onKeydown" tabindex="0">
    <!-- Filter badge (top-right) -->
    <div v-if="Object.keys(activeFilters).length" class="sp-filter-badge">
      <span class="sp-filter-badge__text">已过滤 {{ rows.length }} 条</span>
      <button class="sp-filter-badge__clear" @click="clearAllFilters">清除</button>
    </div>

    <!-- Table -->
    <div class="sp-table-wrap">
      <table class="sp-table" :style="{ width: tableWidth }">
        <thead>
          <tr>
            <th class="sp-th sp-th--rownum" style="width:36px">#</th>
            <th
              v-for="col in columns"
              :key="col.key"
              class="sp-th sp-th--data"
              :style="{ width: colWidths[col.key] + 'px' }"
              @contextmenu="onHeaderContext($event, col.key)"
            >
              <div class="sp-th__content">
                <span>{{ col.label }}</span>
                <button
                  v-if="col.filterable"
                  class="sp-th__filter-btn"
                  :class="{ active: activeFilters[col.key] }"
                  @click.stop="toggleFilter(col.key, col.type)"
                  title="筛选"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
                </button>
              </div>
              <div class="sp-th__resize" @mousedown="onResizeDown($event, col.key)"></div>
              <!-- Filter dropdown -->
              <div v-if="filterDropdown === col.key" class="sp-filter-dd" @click.stop>
                <template v-if="col.type === 'select' && col.options">
                  <label class="sp-filter-dd__item sp-filter-dd__item--all">
                    <input type="checkbox" :checked="isAllSelected(col.key, col.options!.map(o => o.value))" @change="toggleAllFilter(col.key, col.options!.map(o => o.value))"/>
                    全部
                  </label>
                  <label v-for="opt in col.options" :key="opt.value" class="sp-filter-dd__item">
                    <input type="checkbox" :checked="isMultiSelected(col.key, opt.value)" @change="toggleMultiFilter(col.key, opt.value)"/>
                    {{ opt.label }}
                  </label>
                  <div class="sp-filter-dd__actions">
                    <button class="sp-filter-dd__btn sp-filter-dd__btn--confirm" @click="confirmMultiFilter">确定</button>
                    <button class="sp-filter-dd__btn" @click="cancelMultiFilter">取消</button>
                  </div>
                </template>
                <template v-else-if="col.type === 'tags'">
                  <div class="sp-filter-dd__search">
                    <input v-model="tagSearch" class="sp-filter-dd__input" placeholder="搜索标签..." @keydown.escape="filterDropdown = null"/>
                  </div>
                  <label class="sp-filter-dd__item">
                    <input type="radio" :name="'f-'+col.key" :checked="!activeFilters[col.key]" @change="applyFilter(col.key, '')"/> 全部
                  </label>
                  <label v-for="tag in filteredTags" :key="tag.id" class="sp-filter-dd__item">
                    <input type="radio" :name="'f-'+col.key" :checked="activeFilters[col.key]===String(tag.id)" @change="applyFilter(col.key, String(tag.id))"/>
                    <span class="sp-tag-badge" :style="{ background: tag.color }" style="font-size:10px;padding:1px 6px;">{{ tag.name }}</span>
                  </label>
                </template>
                <template v-else>
                  <div class="sp-filter-panel">
                    <input class="sp-filter-panel__input" v-model="filterInput" placeholder="输入关键词筛选..." @keydown.enter="confirmTextFilter(col.key)" autofocus/>
                    <div class="sp-filter-panel__list" v-if="filteredColumnValues(col.key).length">
                      <div
                        v-for="val in filteredColumnValues(col.key)"
                        :key="val"
                        class="sp-filter-panel__item"
                        :class="{ active: filterInput === val }"
                        @click="filterInput = val"
                      >{{ val }}</div>
                    </div>
                    <div class="sp-filter-panel__list" v-else-if="filterInput">
                      <div class="sp-filter-panel__empty">无匹配结果</div>
                    </div>
                    <div class="sp-filter-panel__actions">
                      <button class="sp-filter-panel__btn sp-filter-panel__btn--confirm" @click="confirmTextFilter(col.key)">确定</button>
                      <button class="sp-filter-panel__btn" @click="resetTextFilter">重置</button>
                      <button class="sp-filter-panel__btn" @click="cancelTextFilter">取消</button>
                    </div>
                  </div>
                </template>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, idx) in rows"
            :key="row.id"
            :class="['sp-row', { 'sp-row--cancelled': row.status === 'cancelled', 'sp-row--resized': rowHeights[row.id] }]"
            :style="getRowHeight(row.id) ? { height: getRowHeight(row.id), minHeight: getRowHeight(row.id) } : undefined"
            @contextmenu="onRowContext($event, row.id)"
          >
            <td class="sp-td sp-td--rownum" :class="{ 'sp-td--has-resize': true }" :style="rowHeights[row.id] ? { height: rowHeights[row.id] + 'px' } : undefined"><div class="sp-cell-wrap" :style="rowHeights[row.id] ? { maxHeight: (rowHeights[row.id] - 13) + 'px' } : undefined">{{ idx + 1 }}</div><div class="sp-row__resize" @mousedown="onRowResizeDown($event, row.id)"></div></td>
            <td
              v-for="col in columns"
              :key="col.key"
              :class="['sp-td', `sp-td--${col.type}`, { 'sp-td--wrap': colWraps[col.key] }]"
              :style="{ width: colWidths[col.key] + 'px', textAlign: (colAligns[col.key] || 'left') as any, ...(rowHeights[row.id] ? { height: rowHeights[row.id] + 'px' } : {}) }"
              @dblclick="onDoubleClick(row.id, col.key, (row as any)[col.key])"
            >
              <template v-if="editingCell?.id === row.id && editingCell?.field === col.key">
                <input v-if="col.type === 'text'" ref="editInputRef" v-model="editValue" class="sp-cell-input" @keydown="handleEditKeydown" @blur="commitEdit"/>
                <select v-else-if="col.type === 'select'" ref="editInputRef" v-model="editValue" class="sp-cell-select" @keydown="handleEditKeydown" @blur="commitEdit" @change="commitEdit">
                  <option v-for="opt in col.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <input v-else-if="col.type === 'date'" ref="editInputRef" v-model="editValue" type="date" class="sp-cell-date" @keydown="handleEditKeydown" @blur="commitEdit" @change="commitEdit"/>
              </template>
              <div v-else class="sp-cell-wrap" :style="rowHeights[row.id] ? { maxHeight: (rowHeights[row.id] - 13) + 'px' } : undefined">
                <span v-if="col.key === 'status'" :class="['status-tag', getStatusClass(row.status)]">{{ cellDisplay(row, col.key) }}</span>
                <span v-else-if="col.key === 'priority'" :class="['priority-tag', row.priority === 'important' ? 'priority--important' : '']">{{ getPriorityLabel(row.priority) }}</span>
                <template v-else-if="col.type === 'tags'">
                  <span
                    v-for="tag in getTaskTags(row)"
                    :key="tag.id"
                    class="sp-tag-badge"
                    :style="{ background: tag.color }"
                  >{{ tag.name }}<span class="sp-tag-badge__x" @click.stop="toggleTaskTag(row, tag)" title="删除此标签">×</span></span>
                  <span class="sp-tag-add" @click.stop="openTagPopover(row, $event)">+</span>
                </template>
                <span v-else class="sp-cell-text" :title="cellDisplay(row, col.key)">{{ cellDisplay(row, col.key) }}</span>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0 && !loading">
            <td :colspan="columns.length + 1" class="sp-empty">暂无数据，右键插入行开始</td>
          </tr>
          <tr v-if="loading">
            <td :colspan="columns.length + 1" class="sp-empty">加载中...</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Tag popover (Teleported to body) -->
    <Teleport to="body" v-if="tagPopover">
      <div
        class="sp-tag-popover"
        :style="{ left: tagPopover.left + 'px', top: tagPopover.top + 'px' }"
        @click.stop
      >
        <div class="sp-tag-popover__search">
          <input v-model="tagSearch" class="sp-tag-popover__input" placeholder="搜索标签..." @keydown.escape="closeTagPopover"/>
        </div>
        <div class="sp-tag-popover__list">
          <div
            v-for="tag in filteredTags"
            :key="tag.id"
            :class="['sp-tag-popover__item', { active: isTagSelected(tagPopover.task, tag.id!) }]"
            @click="toggleTaskTag(tagPopover.task, tag); closeTagPopover()"
          >
            <span class="sp-tag-popover__dot" :style="{ background: tag.color }"></span>
            <span>{{ tag.name }}</span>
            <span v-if="isTagSelected(tagPopover.task, tag.id!)" class="sp-tag-popover__check">✓</span>
          </div>
          <div v-if="!filteredTags.length" class="sp-tag-popover__empty">{{ tagSearch ? '无匹配标签' : '暂无可用标签' }}</div>
        </div>
      </div>
    </Teleport>

    <!-- Context menu -->
    <div v-if="ctxMenu" class="sp-ctxmenu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }" @click.stop>
      <!-- Row context menu -->
      <template v-if="ctxMenu.type === 'row'">
        <button class="sp-ctxmenu__item" @click="ctxInsertAbove">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 5 5 12"/></svg>
          在上方插入行
        </button>
        <button class="sp-ctxmenu__item" @click="ctxInsertBelow">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="5 12 12 19 19 12"/></svg>
          在下方插入行
        </button>
        <div class="sp-ctxmenu__sep"></div>
        <button class="sp-ctxmenu__item" @click="ctxCopy">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
          复制 Ctrl+C
        </button>
        <button class="sp-ctxmenu__item" @click="ctxCut">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="6" cy="6" r="3"/><circle cx="18" cy="18" r="3"/><line x1="8.6" y1="8.6" x2="15.4" y2="15.4"/></svg>
          剪切 Ctrl+X
        </button>
        <button class="sp-ctxmenu__item" @click="ctxPaste" :class="{ 'sp-ctxmenu__item--disabled': !clipboard.length }">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/></svg>
          粘贴 Ctrl+V
          <span v-if="clipboardMode === 'cut'" class="sp-ctxmenu__hint">覆盖</span>
        </button>
        <button v-if="clipboardMode === 'cut' && clipboard.length" class="sp-ctxmenu__item" @click="ctxPasteInsert">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M16 4h2a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><line x1="12" y1="11" x2="12" y2="17"/><line x1="9" y1="14" x2="15" y2="14"/></svg>
          插入剪切单元格
        </button>
        <div class="sp-ctxmenu__sep"></div>
        <button class="sp-ctxmenu__item sp-ctxmenu__item--danger" @click="ctxDeleteRow">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/></svg>
          删除行 Del
        </button>
      </template>
      <!-- Column header context menu -->
      <template v-if="ctxMenu.type === 'header'">
        <button class="sp-ctxmenu__item" @click="setColAlign(ctxMenu.colKey!, 'left')">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="15" y2="12"/><line x1="3" y1="18" x2="18" y2="18"/></svg>
          左对齐
        </button>
        <button class="sp-ctxmenu__item" @click="setColAlign(ctxMenu.colKey!, 'center')">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="6" y1="12" x2="18" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          居中
        </button>
        <button class="sp-ctxmenu__item" @click="setColAlign(ctxMenu.colKey!, 'right')">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="9" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
          右对齐
        </button>
        <div class="sp-ctxmenu__sep"></div>
        <button class="sp-ctxmenu__item" @click="toggleColWrap(ctxMenu.colKey!)">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 7 6 7 18"/><polyline points="7 14 3 18 7 18"/><line x1="11" y1="6" x2="21" y2="6"/><line x1="11" y1="12" x2="21" y2="12"/><line x1="11" y1="18" x2="21" y2="18"/></svg>
          {{ colWraps[ctxMenu.colKey!] ? '取消自动换行' : '自动换行' }}
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
/*
 * Superbox Spreadsheet — Element Plus Flat Design
 * Palette: Analytics Dashboard (#1E40AF deep / #409EFF primary / #F8FAFC bg)
 * Principles: Flat, no heavy shadows, clean borders, typography-focused, 150-200ms transitions
 */

/* ── Container ── */
.spreadsheet {
  display: flex;
  flex-direction: column;
  height: 100%;
  outline: none;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

/* ── Filter badge (top-right, same height as header) ── */
.sp-filter-badge {
  position: fixed;
  top: 8px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  font-size: 12px;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(64,158,255,0.12);
}
.sp-filter-badge__text { color: #409EFF; font-weight: 500; }
.sp-filter-badge__clear {
  background: none; border: none; color: #409EFF; cursor: pointer;
  font-size: 12px; padding: 2px 6px; border-radius: 2px;
  transition: background 0.15s ease;
}
.sp-filter-badge__clear:hover { background: #d9ecff; }

/* ── Table wrapper ── */
.sp-table-wrap {
  flex: 1;
  overflow: auto;
  background: #ffffff;
}
.sp-table-wrap::-webkit-scrollbar { width: 7px; height: 7px; }
.sp-table-wrap::-webkit-scrollbar-track { background: #f5f7fa; }
.sp-table-wrap::-webkit-scrollbar-thumb { background: #c0c4cc; border-radius: 4px; }
.sp-table-wrap::-webkit-scrollbar-thumb:hover { background: #909399; }
.sp-table-wrap::-webkit-scrollbar-corner { background: #f5f7fa; }

.sp-table {
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
  font-size: 13px;
}

/* ── Headers ── */
.sp-th {
  position: sticky;
  top: 0;
  background: #f5f7fa;
  padding: 0;
  border-bottom: 1px solid #dcdfe6;
  border-right: 1px solid #dcdfe6;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  text-align: center;
  z-index: 2;
}
.sp-th:last-child { border-right: none; }
.sp-th--rownum { z-index: 3; }
.sp-th--data { cursor: context-menu; }
.sp-th__content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 9px 11px;
  gap: 6px;
}
.sp-th__filter-btn {
  background: none; border: none; color: #c0c4cc; cursor: pointer;
  padding: 3px 4px; border-radius: 2px; line-height: 1;
  display: inline-flex; align-items: center; justify-content: center;
  transition: color 0.15s ease, background 0.15s ease;
}
.sp-th__filter-btn:hover { color: #909399; background: #e9ecf1; }
.sp-th__filter-btn.active { color: #ffffff; background: #409EFF; }

/* Column resize handle */
.sp-th__resize {
  position: absolute; right: 0; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; z-index: 1; transition: background 0.12s ease;
}
.sp-th__resize:hover, .sp-th__resize:active { background: #409EFF; }

/* ── Filter dropdown ── */
.sp-filter-dd {
  position: absolute; top: calc(100% + 3px); left: 0; min-width: 148px;
  background: #ffffff; border: 1px solid #e4e7ed; border-radius: 3px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08); padding: 6px 0; z-index: 10;
  animation: filterIn 0.15s ease;
}
@keyframes filterIn { from { opacity:0; transform:translateY(-3px) } to { opacity:1; transform:translateY(0) } }
.sp-filter-dd__item {
  display: flex; align-items: center; gap: 8px; padding: 6px 14px;
  font-size: 12px; color: #606266; cursor: pointer; margin: 0;
  transition: all 0.12s ease;
}
.sp-filter-dd__item:hover { background: #f5f7fa; color: #409EFF; }
.sp-filter-dd__item input[type="radio"] { accent-color: #409EFF; }
.sp-filter-dd__item--all { border-bottom: 1px solid #ebeef5; font-weight: 500; }
.sp-filter-dd__search { padding: 6px 10px; border-bottom: 1px solid #ebeef5; }
.sp-filter-dd__input {
  width: 100%; padding: 5px 8px; border: 1px solid #dcdfe6; border-radius: 3px;
  font-size: 12px; outline: none; font-family: inherit; box-sizing: border-box;
  transition: border-color 0.15s ease;
}
.sp-filter-dd__input:focus { border-color: #409EFF; }
.sp-filter-dd__actions { display: flex; gap: 6px; justify-content: flex-end; padding: 6px 10px; border-top: 1px solid #ebeef5; }
.sp-filter-dd__btn {
  padding: 3px 12px; border: 1px solid #dcdfe6; border-radius: 3px;
  background: #ffffff; font-size: 12px; color: #606266; cursor: pointer;
  transition: all 0.12s ease; font-family: inherit;
}
.sp-filter-dd__btn:hover { color: #409EFF; border-color: #c6e2ff; background: #ecf5ff; }
.sp-filter-dd__btn--confirm { background: #409EFF; color: #ffffff; border-color: #409EFF; }
.sp-filter-dd__btn--confirm:hover { background: #66b1ff; border-color: #66b1ff; color: #ffffff; }

/* ── Text filter panel ── */
.sp-filter-panel {
  width: 220px; padding: 10px;
}
.sp-filter-panel__input {
  width: 100%; padding: 7px 10px; border: 1px solid #dcdfe6;
  border-radius: 3px; font-size: 12px; outline: none; font-family: inherit;
  transition: border-color 0.15s ease; box-sizing: border-box; margin-bottom: 8px;
}
.sp-filter-panel__input:focus { border-color: #409EFF; }
.sp-filter-panel__list {
  max-height: 160px; overflow-y: auto; margin-bottom: 8px;
  border: 1px solid #ebeef5; border-radius: 3px;
}
.sp-filter-panel__item {
  padding: 5px 10px; font-size: 12px; color: #606266; cursor: pointer;
  transition: all 0.12s ease; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sp-filter-panel__item:hover { background: #f5f7fa; color: #409EFF; }
.sp-filter-panel__item.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.sp-filter-panel__empty {
  padding: 20px 10px; text-align: center; font-size: 12px; color: #c0c4cc;
}
.sp-filter-panel__actions {
  display: flex; gap: 6px; justify-content: flex-end;
}
.sp-filter-panel__btn {
  padding: 4px 12px; border: 1px solid #dcdfe6; border-radius: 3px;
  background: #ffffff; font-size: 12px; color: #606266; cursor: pointer;
  transition: all 0.12s ease; font-family: inherit;
}
.sp-filter-panel__btn:hover { color: #409EFF; border-color: #c6e2ff; background: #ecf5ff; }
.sp-filter-panel__btn--confirm {
  background: #409EFF; color: #ffffff; border-color: #409EFF;
}
.sp-filter-panel__btn--confirm:hover { background: #66b1ff; border-color: #66b1ff; color: #ffffff; }

/* ── Rows ── */
.sp-row { min-height: 36px; transition: background 0.12s ease; }
.sp-row:hover { background: #f5f7fa; }
.sp-row:nth-child(even) { background: #fafbfd; }
.sp-row:nth-child(even):hover { background: #f0f2f5; }
.sp-row--cancelled .sp-td { color: #c0c4cc; text-decoration: line-through; text-decoration-color: #dcdfe6; }
.sp-row--resized .sp-td { height: inherit; overflow: hidden !important; }

/* ── Cells ── */
.sp-td {
  padding: 6px 10px; border-bottom: 1px solid #dcdfe6; border-right: 1px solid #dcdfe6;
  font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap; cursor: default; position: relative;
  transition: background 0.12s ease;
}
.sp-td:last-child { border-right: none; }
.sp-td--rownum { text-align: center; font-size: 11px; color: #c0c4cc; font-weight: 500; font-variant-numeric: tabular-nums; }
.sp-td--has-resize { position: relative; }
.sp-cell-wrap { overflow: hidden; line-height: 1.6; }
.sp-cell-text { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.6; }

/* Wrapped cells */
.sp-td--wrap .sp-cell-wrap {
  white-space: normal;
  word-break: break-word;
}
.sp-td--wrap .sp-cell-text {
  white-space: normal;
  word-break: break-word;
  overflow: visible;
}

/* Row resize handle */
.sp-row__resize {
  position: absolute; left: 0; right: 0; bottom: 0; height: 5px;
  cursor: row-resize; z-index: 1; transition: background 0.12s ease;
}
.sp-row__resize:hover { background: #409EFF; }

.sp-cell-input {
  width: calc(100% + 20px); margin: -6px -10px; padding: 6px 10px;
  border: 1px solid #409EFF; border-radius: 2px; font-size: 13px;
  font-family: inherit; outline: none; background: #ffffff; color: #303133;
  height: 36px; box-sizing: border-box; z-index: 1; position: relative;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.12);
}
/* ElSelect in edit mode — match cell height */
.sp-cell-select {
  width: calc(100% + 20px) !important; margin: -6px -10px;
  --el-select-border-color-hover: #409EFF;
  --el-select-input-focus-border-color: #409EFF;
}
.sp-cell-select :deep(.el-select__wrapper) {
  border-radius: 2px; min-height: 36px; height: 36px;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.12);
}
/* ElDatePicker in edit mode */
.sp-cell-date {
  width: calc(100% + 20px) !important; margin: -6px -10px;
}
.sp-cell-date :deep(.el-input__wrapper) {
  border-radius: 2px; min-height: 36px; height: 36px;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.12);
}
/* Popper dropdown styles — blue-white theme */
.sp-cell-select-popper {
  --el-color-primary: #409EFF;
  --el-fill-color-light: #ecf5ff;
}
.sp-cell-date-popper {
  --el-color-primary: #409EFF;
}

/* ── Tag badges & popover ── */
.sp-tag-badge {
  display: inline-flex; align-items: center; position: relative;
  padding: 3px 14px 3px 8px; margin: 2px 2px;
  border-radius: 3px; font-size: 11px; color: #ffffff; font-weight: 500;
  white-space: nowrap; vertical-align: middle;
}
.sp-tag-badge:hover { opacity: 0.85; }
.sp-tag-badge__x {
  position: absolute; top: 0px; right: 2px;
  font-size: 10px; font-weight: 700; line-height: 1; cursor: pointer;
  color: rgba(255,255,255,0.75); opacity: 0; transition: opacity 0.12s ease;
  text-shadow: 0 1px 2px rgba(0,0,0,0.4);
}
.sp-tag-badge:hover .sp-tag-badge__x { opacity: 1; }
.sp-tag-badge__x:hover { color: #ffffff; }
.sp-tag-add {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; border: 1px dashed #dcdfe6; border-radius: 3px;
  font-size: 14px; color: #c0c4cc; cursor: pointer; vertical-align: middle;
  transition: all 0.12s ease; margin: 1px 2px;
}
.sp-tag-add:hover { border-color: #409EFF; color: #409EFF; background: #ecf5ff; }
.sp-tag-popover {
  position: fixed; min-width: 180px; max-height: 240px;
  background: #ffffff; border: 1px solid #e4e7ed; border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08); z-index: 200; overflow-y: auto;
}
.sp-tag-popover__search { padding: 6px 8px; border-bottom: 1px solid #ebeef5; }
.sp-tag-popover__input {
  width: 100%; padding: 5px 8px; border: 1px solid #dcdfe6; border-radius: 3px;
  font-size: 12px; outline: none; font-family: inherit; box-sizing: border-box;
  transition: border-color 0.15s ease;
}
.sp-tag-popover__input:focus { border-color: #409EFF; }
.sp-tag-popover__list { padding: 4px 0; max-height: 200px; overflow-y: auto; }
.sp-tag-popover__item {
  display: flex; align-items: center; gap: 8px; padding: 7px 14px;
  font-size: 12px; color: #606266; cursor: pointer; transition: all 0.12s ease;
}
.sp-tag-popover__item:hover { background: #f5f7fa; }
.sp-tag-popover__item.active { background: #ecf5ff; color: #409EFF; }
.sp-tag-popover__dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.sp-tag-popover__check { margin-left: auto; font-size: 12px; color: #409EFF; }
.sp-tag-popover__empty { padding: 20px; text-align: center; font-size: 12px; color: #c0c4cc; }

/* ── Select & Date editing (Element Plus blue-white style) ── */
.sp-cell-select,
.sp-cell-date {
  width: calc(100% + 20px); margin: -6px -10px; padding: 6px 10px;
  border: 1px solid #409EFF; border-radius: 2px; font-size: 13px;
  font-family: inherit; outline: none; background: #ffffff; color: #303133;
  height: 36px; box-sizing: border-box; z-index: 1; position: relative;
  box-shadow: 0 0 0 2px rgba(64,158,255,0.12);
  cursor: pointer;
  -webkit-appearance: none; appearance: none;
}
.sp-cell-select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23909399' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 8px center; padding-right: 26px;
}
.sp-cell-date::-webkit-calendar-picker-indicator { cursor: pointer; opacity: 0.7; }
.sp-cell-date::-webkit-calendar-picker-indicator:hover { opacity: 1; }
/* Remove old unused popper styles */
.sp-cell-select-popper,
.sp-cell-date-popper { display: none; }

/* ── Status tags ── */
.status-tag {
  display: inline-flex; align-items: center; font-size: 11px; padding: 2px 10px;
  border-radius: 3px; font-weight: 500; line-height: 1.5; border: 1px solid;
}
.status--not-started { background:#f4f4f5; color:#909399; border-color:#e9e9eb; }
.status--in-progress { background:#ecf5ff; color:#409EFF; border-color:#d9ecff; }
.status--in-progress::before { content:''; display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:6px; flex-shrink:0; background:#409EFF; animation:pulse-dot 1.5s ease-in-out infinite; }
.status--completed { background:#f0f9eb; color:#67C23A; border-color:#e1f3d8; }
.status--completed::before { content:''; display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:6px; flex-shrink:0; background:#67C23A; }
.status--cancelled { background:#fafafa; color:#c0c4cc; border-color:#ebeef5; }
@keyframes pulse-dot { 0%,100%{opacity:1} 50%{opacity:.4} }

/* ── Priority tags ── */
.priority-tag {
  display: inline-flex; align-items: center; font-size: 11px; padding: 2px 10px;
  border-radius: 3px; font-weight: 500; border: 1px solid #e9e9eb;
  background: #f4f4f5; color: #909399;
}
.priority--important { background:#fef0f0; color:#F56C6C; border-color:#fbc4c4; font-weight:600; }

/* ── Empty ── */
.sp-empty { text-align: center; padding: 64px 20px; font-size: 13px; color: #c0c4cc; }

/* ── Context menu ── */
.sp-ctxmenu {
  position: fixed; min-width: 180px; background: #ffffff; border: 1px solid #e4e7ed;
  border-radius: 3px; box-shadow: 0 2px 12px rgba(0,0,0,.08); padding: 5px 0;
  z-index: 100; animation: ctxIn 0.12s ease;
}
@keyframes ctxIn { from{opacity:0;transform:scale(.97)} to{opacity:1;transform:scale(1)} }
.sp-ctxmenu__item {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 8px 16px; border: none; background: none; text-align: left;
  font-size: 12px; color: #606266; cursor: pointer;
  transition: all 0.12s ease; font-family: inherit; line-height: 1.4;
}
.sp-ctxmenu__item:hover { background: #f5f7fa; color: #409EFF; }
.sp-ctxmenu__item--danger { color: #F56C6C; }
.sp-ctxmenu__item--danger:hover { background: #fef0f0; color: #F56C6C; }
.sp-ctxmenu__item--disabled { opacity: .4; pointer-events: none; }
.sp-ctxmenu__hint { font-size: 10px; color: #909399; margin-left: auto; }
.sp-ctxmenu__sep { height: 1px; background: #e4e7ed; margin: 4px 0; }
</style>
