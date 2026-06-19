export interface ModelConfig {
  id?: number
  providerName: string
  apiKey?: string
  baseUrl?: string
  modelName: string
  displayName?: string
  apiFormat?: string
  isDefault: boolean
  isActive: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ProviderType {
  providerName: string
  displayName: string
}

export interface ModelTestRequest {
  providerName: string
  apiKey: string
  baseUrl: string
  modelName: string
}

export interface ModelTestResponse {
  success: boolean
  message: string
  responseTimeMs: number
}
