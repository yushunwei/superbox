<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { chatApi, streamChat, type Conversation, type ChatMessage } from '@/api/modules/chat'

const conversations = ref<Conversation[]>([])
const messages = ref<ChatMessage[]>([])
const activeConvId = ref<number | null>(null)
const prompt = ref('')
const streaming = ref(false)
const streamingText = ref('')
const streamingReasoning = ref('')
const reasoningEnabled = ref(false)
const ragEnabled = ref(true)
const selectedModel = ref('gpt-4o-mini')
const showSidebar = ref(false)
const scrollViewRef = ref<any>(null)

onMounted(() => {
  fetchConversations()
})

async function fetchConversations() {
  const res = await chatApi.listConversations()
  conversations.value = res.data
}

async function selectConv(id: number) {
  activeConvId.value = id
  showSidebar.value = false
  const res = await chatApi.getMessages(id)
  messages.value = res.data
  scrollToBottom()
}

function newChat() {
  activeConvId.value = null
  messages.value = []
  streamingText.value = ''
  streamingReasoning.value = ''
}

async function send() {
  if (!prompt.value.trim() || streaming.value) return

  if (!activeConvId.value) {
    const res = await chatApi.createConversation({ model: selectedModel.value })
    activeConvId.value = res.data.id
    conversations.value.unshift(res.data)
  }

  const userMsg: ChatMessage = {
    id: Date.now(),
    conversationId: activeConvId.value,
    role: 'user',
    content: prompt.value,
    createdAt: new Date().toISOString(),
  }
  messages.value.push(userMsg)
  const text = prompt.value
  prompt.value = ''

  streaming.value = true
  streamingText.value = ''
  streamingReasoning.value = ''

  streamChat(
    activeConvId.value,
    text,
    selectedModel.value,
    reasoningEnabled.value,
    ragEnabled.value,
    {
      onReasoning: (delta) => {
        streamingReasoning.value += delta
      },
      onAnswer: (delta) => {
        streamingText.value += delta
        scrollToBottom()
      },
      onSources: () => {},
      onDone: () => {
        const assistantMsg: ChatMessage = {
          id: Date.now() + 1,
          conversationId: activeConvId.value!,
          role: 'assistant',
          content: streamingText.value,
          reasoningContent: streamingReasoning.value || undefined,
          createdAt: new Date().toISOString(),
        }
        messages.value.push(assistantMsg)
        streamingText.value = ''
        streamingReasoning.value = ''
        streaming.value = false
        fetchConversations()
        scrollToBottom()
      },
      onError: (err) => {
        streaming.value = false
        uni.showToast({ title: err, icon: 'error' })
      },
    },
  )
}

function scrollToBottom() {
  nextTick(() => {
    // uni-app scroll-view scroll-into-view would be used here
  })
}
</script>

<template>
  <view class="chat-page">
    <!-- Main Chat -->
    <view class="chat-main">
      <!-- Header -->
      <view class="chat-header">
        <button class="sidebar-toggle" @tap="showSidebar = !showSidebar">☰</button>
        <text class="chat-title">{{ activeConvId ? '对话中' : 'AI 问答' }}</text>
        <button class="new-btn" @tap="newChat">＋</button>
      </view>

      <!-- Messages -->
      <scroll-view
        class="chat-messages"
        scroll-y
        :scroll-into-view="'msg-' + (messages.length - 1)"
      >
        <view v-if="messages.length === 0 && !streaming" class="chat-empty">
          <text class="empty-icon">💬</text>
          <text class="empty-title">开始新对话</text>
          <text class="empty-desc">基于知识库进行智能问答，支持深度推理和多模型切换</text>
        </view>

        <view
          v-for="(msg, i) in messages"
          :key="msg.id"
          :id="'msg-' + i"
          :class="['chat-bubble', msg.role === 'user' ? 'chat-bubble--user' : 'chat-bubble--ai']"
        >
          <text class="bubble-content">{{ msg.content }}</text>
          <view v-if="msg.reasoningContent" class="reasoning-block">
            <text class="reasoning-label">💭 深度推理</text>
            <text class="reasoning-text">{{ msg.reasoningContent }}</text>
          </view>
        </view>

        <!-- Streaming -->
        <view v-if="streaming" class="chat-bubble chat-bubble--ai">
          <view v-if="streamingReasoning" class="reasoning-block">
            <text class="reasoning-label">💭 深度推理中</text>
            <text class="reasoning-text">{{ streamingReasoning }}</text>
          </view>
          <text class="bubble-content streaming">
            {{ streamingText || '思考中...' }}<text v-if="streamingText" class="cursor">|</text>
          </text>
        </view>
      </scroll-view>

      <!-- Input -->
      <view class="chat-input-bar">
        <view class="input-toggles">
          <text
            :class="['toggle', { active: reasoningEnabled }]"
            @tap="reasoningEnabled = !reasoningEnabled"
          >💭</text>
          <text
            :class="['toggle', { active: ragEnabled }]"
            @tap="ragEnabled = !ragEnabled"
          >📚</text>
        </view>
        <input
          v-model="prompt"
          class="chat-input"
          placeholder="输入问题..."
          placeholder-style="color:#6e7681"
          @confirm="send"
        />
        <button class="send-btn" @tap="send" :disabled="streaming">
          {{ streaming ? '■' : '➤' }}
        </button>
      </view>
    </view>

    <!-- Sidebar Overlay -->
    <view v-if="showSidebar" class="sidebar-overlay" @tap="showSidebar = false">
      <view class="sidebar" @tap.stop="">
        <view class="sidebar-header">
          <text class="sidebar-title">对话列表</text>
        </view>
        <view
          v-for="conv in conversations"
          :key="conv.id"
          :class="['sidebar-item', { active: activeConvId === conv.id }]"
          @tap="selectConv(conv.id)"
        >
          <text class="sidebar-item__title">{{ conv.title }}</text>
          <text class="sidebar-item__meta">{{ conv.messageCount }} 条</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.chat-page {
  height: 100vh;
  display: flex;
  background: #0d1117;
}
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #161b22;
  border-bottom: 1rpx solid rgba(255,255,255,0.06);
}
.sidebar-toggle, .new-btn {
  background: none;
  border: none;
  color: #8b949e;
  font-size: 32rpx;
  padding: 8rpx;
}
.chat-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #e6edf3;
}

.chat-messages {
  flex: 1;
  padding: 24rpx;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-title { font-size: 34rpx; font-weight: 600; color: #e6edf3; margin-bottom: 12rpx; }
.empty-desc { font-size: 26rpx; color: #8b949e; text-align: center; line-height: 1.6; max-width: 500rpx; }

.chat-bubble {
  margin-bottom: 24rpx;
  max-width: 85%;
}
.chat-bubble--user {
  align-self: flex-end;
  margin-left: auto;
}
.chat-bubble--ai {
  align-self: flex-start;
}
.bubble-content {
  display: inline-block;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-bubble--user .bubble-content {
  background: rgba(88,166,255,0.15);
  color: #e6edf3;
  border-bottom-right-radius: 4rpx;
}
.chat-bubble--ai .bubble-content {
  background: rgba(28,35,51,0.8);
  border: 1rpx solid rgba(255,255,255,0.08);
  color: #e6edf3;
  border-bottom-left-radius: 4rpx;
}
.streaming {
  color: #c9d1d9;
}
.cursor {
  animation: blink 1s step-end infinite;
  color: #58a6ff;
}
@keyframes blink { 50%{ opacity:0; } }

.reasoning-block {
  margin-top: 12rpx;
  padding: 16rpx;
  background: rgba(255,255,255,0.02);
  border: 1rpx solid rgba(255,255,255,0.06);
  border-radius: 12rpx;
}
.reasoning-label { font-size: 22rpx; color: #6e7681; display: block; margin-bottom: 8rpx; }
.reasoning-text { font-size: 24rpx; color: #8b949e; line-height: 1.6; }

.chat-input-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx;
  background: #161b22;
  border-top: 1rpx solid rgba(255,255,255,0.06);
}
.input-toggles { display: flex; gap: 8rpx; }
.toggle {
  padding: 8rpx;
  font-size: 28rpx;
  opacity: 0.3;
}
.toggle.active { opacity: 1; }
.chat-input {
  flex: 1;
  padding: 20rpx;
  background: #1c2333;
  border: 1rpx solid rgba(255,255,255,0.1);
  border-radius: 12rpx;
  color: #e6edf3;
  font-size: 28rpx;
}
.send-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #58a6ff;
  color: #fff;
  font-size: 28rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
.send-btn[disabled] { opacity: 0.3; }

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 100;
}
.sidebar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 500rpx;
  background: #161b22;
  padding: 40rpx 0;
}
.sidebar-header {
  padding: 0 24rpx 20rpx;
  border-bottom: 1rpx solid rgba(255,255,255,0.06);
}
.sidebar-title { font-size: 30rpx; font-weight: 600; color: #e6edf3; }
.sidebar-item {
  padding: 24rpx;
  border-bottom: 1rpx solid rgba(255,255,255,0.04);
}
.sidebar-item.active { background: rgba(88,166,255,0.08); }
.sidebar-item__title { display: block; font-size: 28rpx; color: #e6edf3; margin-bottom: 4rpx; }
.sidebar-item__meta { font-size: 22rpx; color: #6e7681; }
</style>
