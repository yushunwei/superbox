import { defineStore } from 'pinia'
import { ref } from 'vue'
import { taskApi, type Task, type TaskQuery } from '@/api/modules/taskManager'

export const useTaskStore = defineStore('taskManager', () => {
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const page = ref(1)
  const size = ref(50)
  const filters = ref<TaskQuery>({ page: 1, size: 50 })

  async function fetchTasks(query?: TaskQuery) {
    loading.value = true
    try {
      if (query !== undefined) {
        filters.value = {
          page: query.page ?? 1,
          size: query.size ?? 50,
          status: query.status || undefined,
          priority: query.priority || undefined,
          keyword: query.keyword || undefined,
          executor: query.executor || undefined,
          tagId: query.tagId,
        }
      }
      const res = await taskApi.list(filters.value)
      tasks.value = res.data.records
      total.value = res.data.total
      page.value = res.data.page
    } finally {
      loading.value = false
    }
  }

  async function fetchTask(id: number) {
    const res = await taskApi.getById(id)
    currentTask.value = res.data
    return currentTask.value
  }

  async function createTask(data: Partial<Task>) {
    const res = await taskApi.create(data)
    await fetchTasks()
    return res.data
  }

  async function updateTask(id: number, data: Partial<Task>) {
    const res = await taskApi.update(id, data)
    if (currentTask.value?.id === id) currentTask.value = res.data
    await fetchTasks()
    return res.data
  }

  async function updateCell(id: number, field: string, value: unknown) {
    const res = await taskApi.updateCell(id, field, value)
    const idx = tasks.value.findIndex(t => t.id === id)
    if (idx >= 0) tasks.value[idx] = res.data
    if (currentTask.value?.id === id) currentTask.value = res.data
    return res.data
  }

  async function deleteTask(id: number) {
    await taskApi.delete(id)
    if (currentTask.value?.id === id) currentTask.value = null
    await fetchTasks()
  }

  async function batchDeleteTasks(ids: number[]) {
    await taskApi.batchDelete(ids)
    await fetchTasks()
  }

  async function insertRow(afterId?: number | null) {
    const res = await taskApi.insertRow(afterId)
    await fetchTasks()
    return res.data
  }

  async function batchPaste(afterId: number | null, rows: Partial<Task>[]) {
    const res = await taskApi.batchPaste(afterId, rows)
    await fetchTasks()
    return res.data
  }

  async function updateSortOrders(items: { id: number; sortOrder: number }[]) {
    await taskApi.updateSortOrders(items)
    await fetchTasks()
  }

  async function changeStatus(id: number, status: string) {
    const res = await taskApi.updateStatus(id, status)
    const idx = tasks.value.findIndex(t => t.id === id)
    if (idx >= 0) tasks.value[idx] = res.data
    if (currentTask.value?.id === id) currentTask.value = res.data
    return res.data
  }

  async function promoteToKnowledge(id: number, folderId?: number) {
    return taskApi.promoteToKnowledge(id, folderId)
  }

  async function setTaskTags(id: number, tagIds: number[]) {
    const res = await taskApi.setTags(id, tagIds)
    const idx = tasks.value.findIndex(t => t.id === id)
    if (idx >= 0) tasks.value[idx] = res.data
    return res.data
  }

  async function removeTaskTag(id: number, tagId: number) {
    await taskApi.removeTag(id, tagId)
    // Reload to get updated tags
    await fetchTasks()
  }

  return {
    tasks, currentTask, loading, total, page, size, filters,
    fetchTasks, fetchTask, createTask, updateTask, updateCell,
    deleteTask, batchDeleteTasks, insertRow, batchPaste, updateSortOrders,
    changeStatus, promoteToKnowledge, setTaskTags, removeTaskTag,
  }
})
