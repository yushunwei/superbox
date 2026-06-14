import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  knowledgeFolderApi, knowledgeEntryApi,
  type KnowledgeFolder, type KnowledgeEntry, type KnowledgeQuery,
} from '@/api/modules/knowledgeBase'

export const useKnowledgeStore = defineStore('knowledgeBase', () => {
  const folders = ref<KnowledgeFolder[]>([])
  const entries = ref<KnowledgeEntry[]>([])
  const currentEntry = ref<KnowledgeEntry | null>(null)
  const loading = ref(false)
  const total = ref(0)
  const filters = ref<KnowledgeQuery>({ page: 1, size: 20 })

  async function fetchFolders() {
    const res = await knowledgeFolderApi.tree()
    folders.value = res.data
  }

  async function createFolder(data: { name: string; parentId?: number | null }) {
    await knowledgeFolderApi.create(data)
    await fetchFolders()
  }

  async function deleteFolder(id: number) {
    await knowledgeFolderApi.delete(id)
    await fetchFolders()
  }

  async function fetchEntries(query?: KnowledgeQuery) {
    loading.value = true
    try {
      if (query) filters.value = { ...filters.value, ...query }
      const res = await knowledgeEntryApi.list(filters.value)
      entries.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchEntry(id: number) {
    const res = await knowledgeEntryApi.getById(id)
    currentEntry.value = res.data
    return currentEntry.value
  }

  async function createManual(data: Parameters<typeof knowledgeEntryApi.createManual>[0]) {
    const res = await knowledgeEntryApi.createManual(data)
    await fetchFolders()
    await fetchEntries()
    return res.data
  }

  async function uploadFile(file: File, folderId?: number) {
    const res = await knowledgeEntryApi.upload(file, folderId)
    await fetchFolders()
    await fetchEntries()
    return res.data
  }

  async function updateEntry(id: number, data: Parameters<typeof knowledgeEntryApi.update>[1]) {
    const res = await knowledgeEntryApi.update(id, data)
    if (currentEntry.value?.id === id) currentEntry.value = res.data
    await fetchEntries()
    return res.data
  }

  async function deleteEntry(id: number) {
    await knowledgeEntryApi.delete(id)
    if (currentEntry.value?.id === id) currentEntry.value = null
    await fetchEntries()
    await fetchFolders()
  }

  return {
    folders, entries, currentEntry, loading, total, filters,
    fetchFolders, createFolder, deleteFolder,
    fetchEntries, fetchEntry, createManual, uploadFile, updateEntry, deleteEntry,
  }
})
