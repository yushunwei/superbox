import apiClient from '@/api'

export interface TagCategory {
  id?: number
  name: string
  color: string
  sortOrder?: number
  createdAt?: string
}

export interface Tag {
  id?: number
  name: string
  color: string
  categoryId?: number | null
  sortOrder?: number
  createdAt?: string
}

export const tagCategoryApi = {
  list: () => apiClient.get<{ data: TagCategory[] }>('/tag-categories'),
  get: (id: number) => apiClient.get<{ data: TagCategory }>(`/tag-categories/${id}`),
  create: (data: Partial<TagCategory>) => apiClient.post<{ data: TagCategory }>('/tag-categories', data),
  update: (id: number, data: Partial<TagCategory>) => apiClient.put<{ data: TagCategory }>(`/tag-categories/${id}`, data),
  delete: (id: number) => apiClient.delete(`/tag-categories/${id}`),
}

export const tagApi = {
  list: (categoryId?: number) =>
    apiClient.get<{ data: Tag[] }>('/tags', { params: categoryId != null ? { categoryId } : {} }),
  get: (id: number) => apiClient.get<{ data: Tag }>(`/tags/${id}`),
  create: (data: Partial<Tag>) => apiClient.post<{ data: Tag }>('/tags', data),
  update: (id: number, data: Partial<Tag>) => apiClient.put<{ data: Tag }>(`/tags/${id}`, data),
  delete: (id: number) => apiClient.delete(`/tags/${id}`),
}
