import { api } from '@/api/request'

export interface KnowledgeEntry {
  id: number
  folderId: number | null
  title: string
  sourceType: string
  contentText: string | null
  fileType: string | null
  fileSize: number | null
  chunkCount: number
  tags: { id: number; name: string; color: string }[]
  createdAt: string
  updatedAt: string
}

export const knowledgeApi = {
  list: (params?: any) =>
    api.get<{ code: number; data: { records: KnowledgeEntry[]; total: number; page: number; size: number } }>('/knowledge', params),

  getById: (id: number) =>
    api.get<{ code: number; data: KnowledgeEntry }>(`/knowledge/${id}`),

  getFolders: () =>
    api.get<{ code: number; data: { id: number; name: string; parentId: number | null; children: any[] }[] }>('/knowledge/folders'),
}
