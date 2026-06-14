import apiClient from '@/api'

export interface Conversation {
  id: number
  title: string
  model: string
  systemPrompt?: string
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
  tokenCount?: number
  model?: string
  createdAt: string
}

export interface SourceRef {
  entryId: number
  title: string
  score: number
}

export interface ChatStreamEvent {
  type: 'reasoning' | 'answer' | 'sources' | 'done' | 'error'
  delta?: string
  sources?: SourceRef[]
  messageId?: number
  tokenCount?: number
}

export const chatApi = {
  listConversations: () =>
    apiClient.get<{ data: Conversation[] }>('/ai-chat/conversations'),

  getConversation: (id: number) =>
    apiClient.get<{ data: Conversation }>(`/ai-chat/conversations/${id}`),

  createConversation: (data: { title?: string; model?: string }) =>
    apiClient.post<{ data: Conversation }>('/ai-chat/conversations', data),

  deleteConversation: (id: number) =>
    apiClient.delete(`/ai-chat/conversations/${id}`),

  getMessages: (conversationId: number) =>
    apiClient.get<{ data: ChatMessage[] }>(`/ai-chat/conversations/${conversationId}/messages`),

  getModels: () =>
    apiClient.get<{ data: string[] }>('/ai-chat/models'),
}

export function streamChat(
  conversationId: number,
  prompt: string,
  model: string,
  reasoningEnabled: boolean,
  ragEnabled: boolean,
  onEvent: (event: ChatStreamEvent) => void,
  onDone: () => void,
  onError: (error: string) => void,
): AbortController {
  const controller = new AbortController()
  const token = localStorage.getItem('token') || ''

  fetch(`/api/v1/ai-chat/conversations/${conversationId}/send`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ prompt, model, reasoningEnabled, ragEnabled }),
    signal: controller.signal,
  }).then(async (response) => {
    if (!response.ok) {
      onError(`HTTP ${response.status}`)
      return
    }
    const reader = response.body?.getReader()
    if (!reader) { onError('No response body'); return }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          continue
        }
        if (line.startsWith('data:')) {
          const json = line.substring(5).trim()
          try {
            const event = JSON.parse(json) as ChatStreamEvent
            onEvent(event)
            if (event.type === 'done') {
              onDone()
              return
            }
          } catch { /* skip malformed */ }
        }
      }
    }
    onDone()
  }).catch((err) => {
    if (err.name !== 'AbortError') {
      onError(err.message)
    }
  })

  return controller
}
