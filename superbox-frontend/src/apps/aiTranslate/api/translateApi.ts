import apiClient from '@/api'
import type {
  TextTranslateRequest, TextTranslateResponse,
  TranslateTask, GlossaryEntry, PromptTemplate,
  TranslatorRole, TranslateSettings, ModelInfo, Language,
  PageResult, ApiResult,
} from '../types'

// ── Text Translate ──
export const translateApi = {
  text: (req: TextTranslateRequest) =>
    apiClient.post<ApiResult<TextTranslateResponse>>('/ai-translate/text', req),

  uploadDocument: (formData: FormData) =>
    apiClient.post<ApiResult<{ taskId: number; status: string }>>('/ai-translate/document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  getTasks: (page = 1, size = 20) =>
    apiClient.get<PageResult<TranslateTask>>('/ai-translate/tasks', { params: { page, size } }),

  getTask: (id: number) =>
    apiClient.get<ApiResult<TranslateTask>>(`/ai-translate/tasks/${id}`),

  downloadTask: (id: number) =>
    apiClient.get(`/ai-translate/tasks/${id}/download`, { responseType: 'blob' }),

  deleteTask: (id: number) =>
    apiClient.delete(`/ai-translate/tasks/${id}`),

  retryTask: (id: number) =>
    apiClient.post<ApiResult<null>>(`/ai-translate/tasks/${id}/retry`),

  getModels: () =>
    apiClient.get<ApiResult<ModelInfo[]>>('/ai-translate/models'),

  getLanguages: () =>
    apiClient.get<ApiResult<Language[]>>('/ai-translate/languages'),
}

// ── Glossary ──
export const glossaryApi = {
  list: (params: { page?: number; size?: number; sourceLang?: string; targetLang?: string; keyword?: string } = {}) =>
    apiClient.get<PageResult<GlossaryEntry>>('/ai-translate/glossary', { params }),

  create: (data: Partial<GlossaryEntry>) =>
    apiClient.post<ApiResult<GlossaryEntry>>('/ai-translate/glossary', data),

  update: (id: number, data: Partial<GlossaryEntry>) =>
    apiClient.put<ApiResult<GlossaryEntry>>(`/ai-translate/glossary/${id}`, data),

  delete: (id: number) =>
    apiClient.delete(`/ai-translate/glossary/${id}`),

  importCsv: (formData: FormData) =>
    apiClient.post<ApiResult<{ imported: number }>>('/ai-translate/glossary/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  exportCsv: (params: { sourceLang?: string; targetLang?: string } = {}) =>
    apiClient.get('/ai-translate/glossary/export', { params, responseType: 'blob' }),
}

// ── Prompts ──
export const promptApi = {
  list: (category?: string) =>
    apiClient.get<ApiResult<PromptTemplate[]>>('/ai-translate/prompts', {
      params: category ? { category } : {},
    }),

  get: (id: number) =>
    apiClient.get<ApiResult<PromptTemplate>>(`/ai-translate/prompts/${id}`),

  create: (data: Partial<PromptTemplate>) =>
    apiClient.post<ApiResult<PromptTemplate>>('/ai-translate/prompts', data),

  update: (id: number, data: Partial<PromptTemplate>) =>
    apiClient.put<ApiResult<PromptTemplate>>(`/ai-translate/prompts/${id}`, data),

  delete: (id: number) =>
    apiClient.delete(`/ai-translate/prompts/${id}`),
}

// ── Roles ──
export const roleApi = {
  list: () =>
    apiClient.get<ApiResult<TranslatorRole[]>>('/ai-translate/roles'),

  create: (data: Partial<TranslatorRole>) =>
    apiClient.post<ApiResult<TranslatorRole>>('/ai-translate/roles', data),

  update: (id: number, data: Partial<TranslatorRole>) =>
    apiClient.put<ApiResult<TranslatorRole>>(`/ai-translate/roles/${id}`, data),

  delete: (id: number) =>
    apiClient.delete(`/ai-translate/roles/${id}`),
}

// ── Settings ──
export const settingsApi = {
  get: () =>
    apiClient.get<ApiResult<TranslateSettings>>('/ai-translate/settings'),

  save: (data: Partial<TranslateSettings>) =>
    apiClient.put<ApiResult<TranslateSettings>>('/ai-translate/settings', data),
}
