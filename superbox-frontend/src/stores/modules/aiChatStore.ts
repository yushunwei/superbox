import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi, streamChat, type Conversation, type ChatMessage, type ChatStreamEvent } from '@/api/modules/aiChat'

export const useChatStore = defineStore('aiChat', () => {
  const conversations = ref<Conversation[]>([])
  const activeConversationId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const streaming = ref(false)
  const reasoningEnabled = ref(false)
  const ragEnabled = ref(true)
  const selectedModel = ref('gpt-4o-mini')
  const abortController = ref<AbortController | null>(null)
  const streamingReasoning = ref('')
  const streamingAnswer = ref('')
  const streamingSources = ref<ChatStreamEvent['sources']>(null)

  async function fetchConversations() {
    const res = await chatApi.listConversations()
    conversations.value = res.data
  }

  async function createConversation(title?: string) {
    const res = await chatApi.createConversation({ title, model: selectedModel.value })
    conversations.value.unshift(res.data)
    return res.data
  }

  async function deleteConversation(id: number) {
    await chatApi.deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (activeConversationId.value === id) {
      activeConversationId.value = null
      messages.value = []
    }
  }

  async function selectConversation(id: number) {
    activeConversationId.value = id
    const res = await chatApi.getMessages(id)
    messages.value = res.data
  }

  function newChat() {
    activeConversationId.value = null
    messages.value = []
    streamingReasoning.value = ''
    streamingAnswer.value = ''
    streamingSources.value = null
  }

  async function sendMessage(prompt: string) {
    if (streaming.value || !prompt.trim()) return

    if (!activeConversationId.value) {
      const conv = await createConversation()
      activeConversationId.value = conv.id
    }

    const userMsg: ChatMessage = {
      id: Date.now(),
      conversationId: activeConversationId.value,
      role: 'user',
      content: prompt,
      createdAt: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    streaming.value = true
    streamingReasoning.value = ''
    streamingAnswer.value = ''
    streamingSources.value = null

    abortController.value = streamChat(
      activeConversationId.value,
      prompt,
      selectedModel.value,
      reasoningEnabled.value,
      ragEnabled.value,
      (event: ChatStreamEvent) => {
        if (event.type === 'reasoning') {
          streamingReasoning.value += event.delta || ''
        } else if (event.type === 'answer') {
          streamingAnswer.value += event.delta || ''
        } else if (event.type === 'sources') {
          streamingSources.value = event.sources || null
        }
      },
      () => {
        const assistantMsg: ChatMessage = {
          id: Date.now() + 1,
          conversationId: activeConversationId.value!,
          role: 'assistant',
          content: streamingAnswer.value,
          reasoningContent: streamingReasoning.value || undefined,
          sources: streamingSources.value ? JSON.stringify(streamingSources.value) : undefined,
          createdAt: new Date().toISOString(),
        }
        messages.value.push(assistantMsg)
        streamingReasoning.value = ''
        streamingAnswer.value = ''
        streamingSources.value = null
        streaming.value = false
        fetchConversations()
      },
      (error: string) => {
        streaming.value = false
        streamingAnswer.value = error
      },
    )
  }

  function stopStreaming() {
    abortController.value?.abort()
    streaming.value = false
  }

  return {
    conversations, activeConversationId, messages, streaming,
    reasoningEnabled, ragEnabled, selectedModel,
    streamingReasoning, streamingAnswer, streamingSources,
    fetchConversations, createConversation, deleteConversation,
    selectConversation, newChat, sendMessage, stopStreaming,
  }
})
