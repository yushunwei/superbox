import apiClient from '@/api'

export interface Task {
  id: number
  title: string
  description: string
  status: 'not_started' | 'in_progress' | 'completed' | 'cancelled'
  priority: 'normal' | 'important'
  planStartDate: string | null
  planEndDate: string | null
  executor: string | null
  collaborators: string | null
  completedAt: string | null
  remarks: string | null
  parentTaskId: number | null
  sortOrder: number
  tags: { id: number; name: string; color: string }[]
  createdAt: string
  updatedAt: string
}

export interface TaskQuery {
  status?: string
  priority?: string
  keyword?: string
  executor?: string
  tagId?: string
  page?: number
  size?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export const taskApi = {
  list: (params?: TaskQuery) =>
    apiClient.get<{ data: PageResult<Task> }>('/task-manager', { params }),

  getById: (id: number) =>
    apiClient.get<{ data: Task }>(`/task-manager/${id}`),

  create: (data: Partial<Task>) =>
    apiClient.post<{ data: Task }>('/task-manager', data),

  update: (id: number, data: Partial<Task>) =>
    apiClient.put<{ data: Task }>(`/task-manager/${id}`, data),

  updateCell: (id: number, field: string, value: unknown) =>
    apiClient.patch<{ data: Task }>(`/task-manager/${id}/cell`, { field, value }),

  delete: (id: number) =>
    apiClient.delete(`/task-manager/${id}`),

  batchDelete: (ids: number[]) =>
    apiClient.post('/task-manager/batch-delete', { ids }),

  insertRow: (afterId?: number | null) =>
    apiClient.post<{ data: Task }>('/task-manager/insert-row', { afterId }),

  batchPaste: (afterId: number | null, rows: Partial<Task>[]) =>
    apiClient.post<{ data: Task[] }>('/task-manager/batch-paste', { afterId, rows }),

  updateSortOrders: (items: { id: number; sortOrder: number }[]) =>
    apiClient.put('/task-manager/sort-orders', items),

  updateStatus: (id: number, status: string) =>
    apiClient.patch<{ data: Task }>(`/task-manager/${id}/status`, { status }),

  promoteToKnowledge: (id: number, folderId?: number) =>
    apiClient.post<{ data: { knowledgeId: number } }>(`/task-manager/${id}/promote-to-knowledge`, { folderId }),

  setTags: (id: number, tagIds: number[]) =>
    apiClient.put<{ data: Task }>(`/task-manager/${id}/tags`, { tagIds }),

  removeTag: (id: number, tagId: number) =>
    apiClient.delete(`/task-manager/${id}/tags/${tagId}`),
}
