import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ModelConfig, ProviderType, ModelTestRequest } from '../types'
import { modelManagerApi } from '../api'

export const useModelManagerStore = defineStore('modelManager', () => {
  const models = ref<ModelConfig[]>([])
  const providerTypes = ref<ProviderType[]>([])
  const loading = ref(false)

  async function fetchModels() {
    loading.value = true
    try {
      const res = await modelManagerApi.listModels()
      const data = (res as any).data
      models.value = data.userModels || []
      providerTypes.value = data.providerTypes || []
    } catch {
      // fetch 失败不覆盖已有列表数据
    } finally {
      loading.value = false
    }
  }

  async function createModel(dto: Partial<ModelConfig>) {
    const res = await modelManagerApi.createModel(dto)
    const created = (res as any).data
    await fetchModels()
    return created
  }

  async function updateModel(id: number, dto: Partial<ModelConfig>) {
    const res = await modelManagerApi.updateModel(id, dto)
    const updated = (res as any).data
    await fetchModels()
    return updated
  }

  async function deleteModel(id: number) {
    await modelManagerApi.deleteModel(id)
    await fetchModels()
  }

  async function setDefault(id: number) {
    await modelManagerApi.setDefault(id)
    await fetchModels()
  }

  async function testConnection(dto: ModelTestRequest) {
    const res = await modelManagerApi.testConnection(dto)
    return (res as any).data
  }

  return {
    models, providerTypes, loading,
    fetchModels, createModel, updateModel, deleteModel, setDefault, testConnection,
  }
})
