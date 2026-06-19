import apiClient from '@/api'
import type { ModelConfig, ModelTestRequest } from '../types'

export const modelManagerApi = {
  listModels: () =>
    apiClient.get('/model-manager/models'),

  getModel: (id: number) =>
    apiClient.get(`/model-manager/models/${id}`),

  createModel: (data: Partial<ModelConfig>) =>
    apiClient.post('/model-manager/models', data),

  updateModel: (id: number, data: Partial<ModelConfig>) =>
    apiClient.put(`/model-manager/models/${id}`, data),

  deleteModel: (id: number) =>
    apiClient.delete(`/model-manager/models/${id}`),

  setDefault: (id: number) =>
    apiClient.put(`/model-manager/models/${id}/default`),

  testConnection: (data: ModelTestRequest) =>
    apiClient.post('/model-manager/models/test', data),
}
