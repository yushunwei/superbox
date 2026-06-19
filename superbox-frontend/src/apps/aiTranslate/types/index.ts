// Language codes
export type LangCode = 'de' | 'es' | 'it' | 'en' | 'pt' | 'zh' | 'fr' | 'hu' | 'nl' | 'no' | 'ro' | 'ru'

export interface Language {
  code: LangCode
  name: string        // English name like "German"
  nativeName: string  // native name like "Deutsch", "中文"
}

// Translation request/response
export interface TextTranslateRequest {
  text: string
  sourceLang: LangCode
  targetLang: LangCode
  model?: string
  promptTemplateId?: number
  roleId?: number
}

export interface TextTranslateResponse {
  translatedText: string
  model: string
  sourceLang: string
  targetLang: string
}

export interface TranslateTask {
  id: number
  fileName: string
  fileSize: number
  fileType: string
  sourceLang: string
  targetLang: string
  model: string
  status: 'queued' | 'processing' | 'completed' | 'failed'
  progress: number
  totalSegments: number
  completedSegments: number
  errorMsg?: string
  createdAt: string
  updatedAt: string
}

export interface GlossaryEntry {
  id: number
  sourceLang: string
  targetLang: string
  sourceTerm: string
  targetTerm: string
  category: string
  note?: string
  createdAt: string
  updatedAt: string
}

export interface PromptTemplate {
  id: number
  name: string
  category: string
  systemPrompt: string
  isPreset: boolean
  sortOrder: number
}

export interface TranslatorRole {
  id: number
  name: string
  description: string
  defaultPromptId?: number
  model: string
  temperature: number
  maxTokens: number
  isPreset: boolean
  sortOrder: number
}

export interface TranslateSettings {
  defaultModel: string
  defaultSourceLang: string
  defaultTargetLang: string
  temperature: number
  maxTokens: number
  pdfFormat: 'overlay' | 'dual_column'
  wordFormat: 'keep_style' | 'plain_text'
  doc2xEnabled: boolean
}

export interface ModelInfo {
  name: string
  model: string
  provider: string
  available: boolean
  isDefault: boolean
}

// Pagination
export interface PageResult<T> {
  code: number
  message: string
  data: {
    records: T[]
    total: number
    page: number
    size: number
  }
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
