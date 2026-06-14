import apiClient from '@/api'

export interface SpreadsheetSettings {
  id?: number
  userId?: number
  pageKey: string
  colWidths: Record<string, number> | null
  colAligns: Record<string, string> | null
  colWraps: Record<string, boolean> | null
  rowHeights: Record<string, number> | null
}

export const spreadsheetSettingsApi = {
  get: (pageKey: string) =>
    apiClient.get<{ data: SpreadsheetSettings }>('/spreadsheet/settings', { params: { pageKey } }),

  save: (settings: {
    pageKey: string
    colWidths?: Record<string, number>
    colAligns?: Record<string, string>
    colWraps?: Record<string, boolean>
    rowHeights?: Record<string, number>
  }) => apiClient.put('/spreadsheet/settings', settings),
}
