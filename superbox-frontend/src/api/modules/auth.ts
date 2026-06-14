import apiClient from '@/api'

export interface LoginParams {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  displayName: string
  avatarUrl: string
  preferredLanguage: string
}

export const authApi = {
  login: (params: LoginParams) =>
    apiClient.post<{ data: { token: string } }>('/auth/login', params),

  wxLogin: (code: string) =>
    apiClient.post<{ data: { token: string } }>('/auth/wx-login', { code }),

  getMe: () =>
    apiClient.get<{ data: UserInfo }>('/auth/me'),
}
