import { api } from '@/api/request'

export interface Task {
  id: number
  title: string
  description: string
  status: 'not_started' | 'in_progress' | 'completed' | 'cancelled'
  priority: 'normal' | 'important'
  planStartDate: string
  planEndDate: string
  executor: string
  collaborators: string
  completedAt: string
  remarks: string
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
  page?: number
  size?: number
}

export const taskApi = {
  list: (params?: TaskQuery) =>
    api.get<{ code: number; message: string; data: { records: Task[]; total: number; page: number; size: number } }>('/task-manager', params as any),

  getById: (id: number) =>
    api.get<{ code: number; message: string; data: Task }>(`/task-manager/${id}`),

  create: (data: { title: string; description?: string; priority?: string; planEndDate?: string; planStartDate?: string; tagIds?: number[] }) =>
    api.post<{ code: number; message: string; data: Task }>('/task-manager', data),

  update: (id: number, data: any) =>
    api.put<{ code: number; message: string; data: Task }>(`/task-manager/${id}`, data),

  delete: (id: number) =>
    api.delete(`/task-manager/${id}`),

  updateStatus: (id: number, status: string) =>
    api.patch<{ code: number; message: string; data: Task }>(`/task-manager/${id}/status`, { status }),
}
