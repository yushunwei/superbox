import { api } from '@/api/request'

export interface UserInfo {
  id: number
  username: string
  displayName: string
  avatarUrl: string
  preferredLanguage: string
}

export const authApi = {
  login: (data: { username: string; password: string }) =>
    api.post<{ code: number; data: { token: string } }>('/auth/login', data),

  wxLogin: (code: string) =>
    api.post<{ code: number; data: { token: string } }>('/auth/wx-login', { code }),

  getMe: () =>
    api.get<{ code: number; data: UserInfo }>('/auth/me'),
}
