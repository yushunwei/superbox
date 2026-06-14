import { api } from '@/api/request'

export interface Conversation {
  id: number
  title: string
  model: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  role: 'user' | 'assistant'
  content: string
  reasoningContent?: string
  sources?: string
  model?: string
  createdAt: string
}

export const chatApi = {
  listConversations: () =>
    api.get<{ code: number; data: Conversation[] }>('/chat/conversations'),

  createConversation: (data: { title?: string; model?: string }) =>
    api.post<{ code: number; data: Conversation }>('/chat/conversations', data),

  deleteConversation: (id: number) =>
    api.delete(`/chat/conversations/${id}`),

  getMessages: (id: number) =>
    api.get<{ code: number; data: ChatMessage[] }>(`/chat/conversations/${id}/messages`),

  getModels: () =>
    api.get<{ code: number; data: string[] }>('/chat/models'),
}

export function streamChat(
  conversationId: number,
  prompt: string,
  model: string,
  reasoningEnabled: boolean,
  ragEnabled: boolean,
  callbacks: {
    onReasoning: (delta: string) => void
    onAnswer: (delta: string) => void
    onSources: (sources: any[]) => void
    onDone: () => void
    onError: (err: string) => void
  }
) {
  const token = uni.getStorageSync('token') || ''
  const task = uni.request({
    url: `http://localhost:8080/api/v1/chat/conversations/${conversationId}/send`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: { prompt, model, reasoningEnabled, ragEnabled },
    enableChunked: true,
    responseType: 'text',
    success: () => {},
    fail: (err) => callbacks.onError(err.errMsg),
  })

  // Note: uni.request with enableChunked only works on certain platforms.
  // For full SSE support, consider using wx.request with enableChunked in WeChat.
  task.onChunkReceived((res: any) => {
    const text = res.data
    const lines = text.split('\n')
    for (const line of lines) {
      if (line.startsWith('event:')) continue
      if (line.startsWith('data:')) {
        try {
          const event = JSON.parse(line.substring(5).trim())
          if (event.type === 'reasoning') callbacks.onReasoning(event.delta || '')
          else if (event.type === 'answer') callbacks.onAnswer(event.delta || '')
          else if (event.type === 'sources') callbacks.onSources(event.sources || [])
          else if (event.type === 'done') callbacks.onDone()
          else if (event.type === 'error') callbacks.onError(event.delta || '')
        } catch { /* skip */ }
      }
    }
  })

  return {
    abort: () => task.abort(),
  }
}
