import apiClient from '@/api'

export interface KnowledgeFolder {
  id: number
  name: string
  parentId: number | null
  sortOrder: number
  entryCount: number
  children: KnowledgeFolder[]
  createdAt: string
  updatedAt: string
}

export interface KnowledgeChunk {
  id: number
  entryId: number
  chunkIndex: number
  content: string
  tokenCount: number
}

export interface KnowledgeEntry {
  id: number
  folderId: number | null
  title: string
  sourceType: 'manual' | 'file_upload' | 'imported_task'
  sourceId: number | null
  filePath: string | null
  fileType: string | null
  fileSize: number | null
  contentText: string | null
  chunkCount: number
  tags: { id: number; name: string; color: string }[]
  chunks: KnowledgeChunk[]
  createdAt: string
  updatedAt: string
}

export interface KnowledgeQuery {
  folderId?: number
  keyword?: string
  page?: number
  size?: number
}

export const knowledgeFolderApi = {
  tree: () => apiClient.get<{ data: KnowledgeFolder[] }>('/knowledge-base/folders'),

  getById: (id: number) => apiClient.get<{ data: KnowledgeFolder }>(`/knowledge-base/folders/${id}`),

  create: (data: { name: string; parentId?: number | null; sortOrder?: number }) =>
    apiClient.post<{ data: KnowledgeFolder }>('/knowledge-base/folders', data),

  update: (id: number, data: { name?: string; parentId?: number | null; sortOrder?: number }) =>
    apiClient.put<{ data: KnowledgeFolder }>(`/knowledge-base/folders/${id}`, data),

  delete: (id: number) => apiClient.delete(`/knowledge-base/folders/${id}`),
}

export const knowledgeEntryApi = {
  list: (params?: KnowledgeQuery) =>
    apiClient.get<{ data: { records: KnowledgeEntry[]; total: number; page: number; size: number } }>('/knowledge-base', { params }),

  getById: (id: number) =>
    apiClient.get<{ data: KnowledgeEntry }>(`/knowledge-base/${id}`),

  createManual: (data: { title: string; content: string; folderId?: number | null; tagIds?: number[] }) =>
    apiClient.post<{ data: KnowledgeEntry }>('/knowledge-base/manual', data),

  upload: (file: File, folderId?: number) => {
    const form = new FormData()
    form.append('file', file)
    if (folderId) form.append('folderId', String(folderId))
    return apiClient.post<{ data: KnowledgeEntry }>('/knowledge-base/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  update: (id: number, data: { title: string; content?: string; folderId?: number | null; tagIds?: number[] }) =>
    apiClient.put<{ data: KnowledgeEntry }>(`/knowledge-base/${id}`, data),

  delete: (id: number) => apiClient.delete(`/knowledge-base/${id}`),
}
