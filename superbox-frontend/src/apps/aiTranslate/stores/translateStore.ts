import { defineStore } from 'pinia'
import { ref } from 'vue'
import type {
  Language, ModelInfo, TranslateTask, GlossaryEntry,
  PromptTemplate, TranslatorRole, TranslateSettings,
} from '../types'
import { translateApi, glossaryApi, promptApi, roleApi, settingsApi } from '../api/translateApi'

export const useTranslateStore = defineStore('aiTranslate', () => {
  // ── State ──
  const languages = ref<Language[]>([])
  const models = ref<ModelInfo[]>([])
  const tasks = ref<TranslateTask[]>([])
  const tasksTotal = ref(0)
  const currentTask = ref<TranslateTask | null>(null)
  const glossary = ref<GlossaryEntry[]>([])
  const glossaryTotal = ref(0)
  const prompts = ref<PromptTemplate[]>([])
  const roles = ref<TranslatorRole[]>([])
  const settings = ref<TranslateSettings | null>(null)

  // Loading flags
  const languagesLoading = ref(false)
  const modelsLoading = ref(false)
  const tasksLoading = ref(false)
  const glossaryLoading = ref(false)
  const promptsLoading = ref(false)
  const rolesLoading = ref(false)
  const settingsLoading = ref(false)

  // ── Languages ──
  async function fetchLanguages() {
    languagesLoading.value = true
    try {
      const res = await translateApi.getLanguages()
      languages.value = (res as any).data
    } finally { languagesLoading.value = false }
  }

  // ── Models ──
  async function fetchModels() {
    modelsLoading.value = true
    try {
      const res = await translateApi.getModels()
      models.value = (res as any).data
    } finally { modelsLoading.value = false }
  }

  // ── Tasks ──
  async function fetchTasks(page = 1, size = 20) {
    tasksLoading.value = true
    try {
      const res = await translateApi.getTasks(page, size)
      tasks.value = (res as any).data.records
      tasksTotal.value = (res as any).data.total
    } finally { tasksLoading.value = false }
  }

  async function fetchTask(id: number) {
    const res = await translateApi.getTask(id)
    currentTask.value = (res as any).data
    return currentTask.value
  }

  async function deleteTask(id: number) {
    await translateApi.deleteTask(id)
    if (currentTask.value?.id === id) currentTask.value = null
  }

  async function retryTask(id: number) {
    await translateApi.retryTask(id)
  }

  function downloadTask(id: number) {
    return translateApi.downloadTask(id)
  }

  // ── Glossary ──
  async function fetchGlossary(params: {
    page?: number; size?: number; sourceLang?: string; targetLang?: string; keyword?: string
  } = {}) {
    glossaryLoading.value = true
    try {
      const res = await glossaryApi.list(params)
      glossary.value = (res as any).data.records
      glossaryTotal.value = (res as any).data.total
    } finally { glossaryLoading.value = false }
  }

  async function createGlossaryEntry(data: Partial<GlossaryEntry>) {
    const res = await glossaryApi.create(data)
    return (res as any).data
  }

  async function updateGlossaryEntry(id: number, data: Partial<GlossaryEntry>) {
    const res = await glossaryApi.update(id, data)
    return (res as any).data
  }

  async function deleteGlossaryEntry(id: number) {
    await glossaryApi.delete(id)
  }

  async function importGlossaryCsv(formData: FormData) {
    const res = await glossaryApi.importCsv(formData)
    return (res as any).data
  }

  function exportGlossaryCsv(params: { sourceLang?: string; targetLang?: string } = {}) {
    return glossaryApi.exportCsv(params)
  }

  // ── Prompts ──
  async function fetchPrompts(category?: string) {
    promptsLoading.value = true
    try {
      const res = await promptApi.list(category)
      prompts.value = (res as any).data
    } finally { promptsLoading.value = false }
  }

  async function createPrompt(data: Partial<PromptTemplate>) {
    const res = await promptApi.create(data)
    return (res as any).data
  }

  async function updatePrompt(id: number, data: Partial<PromptTemplate>) {
    const res = await promptApi.update(id, data)
    return (res as any).data
  }

  async function deletePrompt(id: number) {
    await promptApi.delete(id)
  }

  // ── Roles ──
  async function fetchRoles() {
    rolesLoading.value = true
    try {
      const res = await roleApi.list()
      roles.value = (res as any).data
    } finally { rolesLoading.value = false }
  }

  async function createRole(data: Partial<TranslatorRole>) {
    const res = await roleApi.create(data)
    return (res as any).data
  }

  async function updateRole(id: number, data: Partial<TranslatorRole>) {
    const res = await roleApi.update(id, data)
    return (res as any).data
  }

  async function deleteRole(id: number) {
    await roleApi.delete(id)
  }

  // ── Settings ──
  async function fetchSettings() {
    settingsLoading.value = true
    try {
      const res = await settingsApi.get()
      settings.value = (res as any).data
    } finally { settingsLoading.value = false }
  }

  async function saveSettings(data: Partial<TranslateSettings>) {
    const res = await settingsApi.save(data)
    settings.value = (res as any).data
    return settings.value
  }

  return {
    // State
    languages, models, tasks, tasksTotal, currentTask,
    glossary, glossaryTotal, prompts, roles, settings,
    languagesLoading, modelsLoading, tasksLoading,
    glossaryLoading, promptsLoading, rolesLoading, settingsLoading,
    // Actions
    fetchLanguages, fetchModels,
    fetchTasks, fetchTask, deleteTask, retryTask, downloadTask,
    fetchGlossary, createGlossaryEntry, updateGlossaryEntry, deleteGlossaryEntry,
    importGlossaryCsv, exportGlossaryCsv,
    fetchPrompts, createPrompt, updatePrompt, deletePrompt,
    fetchRoles, createRole, updateRole, deleteRole,
    fetchSettings, saveSettings,
  }
})
