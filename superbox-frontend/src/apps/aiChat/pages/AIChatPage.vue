<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/modules/aiChatStore'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

const { t } = useI18n()
const store = useChatStore()

const messageText = ref('')
const reasoningExpanded = ref<Record<number, boolean>>({})
const messagesContainer = ref<HTMLElement | null>(null)
const modelsLoading = ref(false)
const availableModels = ref<string[]>([])

onMounted(async () => {
  await store.fetchConversations()
  try {
    const res = await import('@/api/modules/aiChat').then(m => m.chatApi.getModels())
    availableModels.value = res.data
  } catch { /* use defaults */ }
})

watch(() => store.streamingAnswer, scrollToBottom)
watch(() => store.streamingReasoning, scrollToBottom)

function scrollToBottom() {
  nextTick(() => {
    const el = messagesContainer.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function handleSend() {
  const text = messageText.value.trim()
  if (!text || store.streaming) return
  messageText.value = ''
  await store.sendMessage(text)
  scrollToBottom()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

async function handleNewChat() {
  store.newChat()
}

async function handleSelectConv(id: number) {
  store.newChat()
  await store.selectConversation(id)
}

async function handleDeleteConv(id: number) {
  try {
    const { ElMessageBox } = await import('element-plus')
    await ElMessageBox.confirm('确定删除此对话？', '确认', { type: 'warning' })
    await store.deleteConversation(id)
  } catch { /* cancelled */ }
}

function toggleReasoning(msgId: number) {
  reasoningExpanded.value[msgId] = !reasoningExpanded.value[msgId]
}

function getModelLabel(m: string) {
  const map: Record<string, string> = {
    'gpt-4o': 'GPT-4o', 'gpt-4o-mini': 'GPT-4o Mini',
    'deepseek-chat': 'DeepSeek V3', 'deepseek-reasoner': 'DeepSeek R1',
    'claude-sonnet-4-6': 'Claude Sonnet', 'qwen2.5': 'Qwen 2.5',
  }
  return map[m] || m
}
</script>

<template>
  <div class="chat-page">
    <!-- Conversation Sidebar -->
    <aside class="chat-sidebar">
      <div class="chat-sidebar__header">
        <button class="new-chat-btn" @click="handleNewChat">＋ {{ t('aiChat.newChat') }}</button>
      </div>
      <div class="conv-list">
        <div
          v-for="conv in store.conversations"
          :key="conv.id"
          :class="['conv-item', { active: store.activeConversationId === conv.id }]"
          @click="handleSelectConv(conv.id)"
        >
          <div class="conv-item__title">{{ conv.title }}</div>
          <div class="conv-item__meta">
            {{ conv.messageCount }} 条消息
            <button class="conv-item__del" @click.stop="handleDeleteConv(conv.id)">🗑</button>
          </div>
        </div>
        <div v-if="store.conversations.length === 0" class="conv-empty">
          暂无对话
        </div>
      </div>

      <!-- Model selector -->
      <div class="chat-sidebar__footer">
        <select v-model="store.selectedModel" class="model-select">
          <option v-for="m in availableModels" :key="m" :value="m">{{ getModelLabel(m) }}</option>
        </select>
      </div>
    </aside>

    <!-- Main Chat -->
    <div class="chat-main">
      <!-- Messages -->
      <div ref="messagesContainer" class="chat-messages" v-if="store.messages.length || store.streaming">
        <div
          v-for="msg in store.messages"
          :key="msg.id"
          :class="['chat-msg', `chat-msg--${msg.role}`]"
        >
          <div class="chat-msg__avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="chat-msg__body">
            <div class="chat-msg__bubble">{{ msg.content }}</div>

            <!-- Reasoning block -->
            <div v-if="msg.role === 'assistant' && msg.reasoningContent" class="reasoning-block">
              <div
                class="reasoning-block__header"
                @click="toggleReasoning(msg.id)"
              >
                💭 深度推理 {{ reasoningExpanded[msg.id] ? '▲' : '▼' }}
              </div>
              <div v-if="reasoningExpanded[msg.id]" class="reasoning-block__body">
                {{ msg.reasoningContent }}
              </div>
            </div>
          </div>
        </div>

        <!-- Streaming message -->
        <div v-if="store.streaming" class="chat-msg chat-msg--assistant">
          <div class="chat-msg__avatar">🤖</div>
          <div class="chat-msg__body">
            <div v-if="store.streamingReasoning" class="reasoning-block streaming">
              <div class="reasoning-block__header">💭 深度推理中...</div>
              <div class="reasoning-block__body">{{ store.streamingReasoning }}</div>
            </div>
            <div v-if="store.streamingAnswer" class="chat-msg__bubble streaming-text">
              {{ store.streamingAnswer }}<span class="cursor-blink">|</span>
            </div>
            <div v-if="!store.streamingAnswer && !store.streamingReasoning" class="chat-msg__bubble">
              思考中<span class="dot-pulse">...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else class="chat-empty">
        <div class="chat-empty__icon">💬</div>
        <div class="chat-empty__title">{{ t('aiChat.emptyTitle') }}</div>
        <div class="chat-empty__desc">{{ t('aiChat.emptyDesc') }}</div>
        <div class="suggestions">
          <button
            v-for="s in ['Transformer 和 GNN 的区别？', '简述 Spring Boot 自动配置原理', '如何使用 RAG 提升问答质量？']"
            :key="s"
            class="suggestion-chip"
            @click="messageText = s; handleSend()"
          >{{ s }}</button>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <div class="chat-input-row">
          <textarea
            v-model="messageText"
            :placeholder="t('aiChat.inputPlaceholder')"
            rows="1"
            :disabled="store.streaming"
            @keydown="handleKeydown"
          ></textarea>
          <button
            v-if="!store.streaming"
            class="send-btn"
            :disabled="!messageText.trim()"
            @click="handleSend"
          >➤</button>
          <button
            v-else
            class="stop-btn"
            @click="store.stopStreaming"
          >■</button>
        </div>
        <div class="chat-input-actions">
          <button :class="['toggle-btn', { active: store.reasoningEnabled }]" @click="store.reasoningEnabled = !store.reasoningEnabled">
            💭 {{ t('aiChat.deepReasoning') }}
          </button>
          <button :class="['toggle-btn', { active: store.ragEnabled }]" @click="store.ragEnabled = !store.ragEnabled">
            📚 @{{ t('aiChat.atKnowledge') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page { display:flex; height:100%; }

/* Sidebar */
.chat-sidebar {
  width:220px; flex-shrink:0; background:#f8fafc;
  border-right:1px solid #e2e8f0; display:flex; flex-direction:column;
}
.chat-sidebar__header { padding:12px; border-bottom:1px solid #e2e8f0; }
.new-chat-btn {
  background:#ffffff; border:1px solid #e2e8f0; color:#64748b; font-size:13px;
  width:100%; cursor:pointer; padding:7px 0; border-radius:8px; transition: all .2s;
}
.new-chat-btn:hover { border-color:#3b82f6; color:#3b82f6; background:#eff6ff; }

.conv-list { flex:1; overflow-y:auto; padding:6px; }
.conv-item {
  padding:10px 12px; border-radius:8px; cursor:pointer;
  font-size:13px; transition: background .15s; margin-bottom:2px;
}
.conv-item:hover { background:#f1f5f9; }
.conv-item.active { background:#eff6ff; }
.conv-item__title { color:#334155; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; margin-bottom:2px; }
.conv-item.active .conv-item__title { color:#1e293b; font-weight:500; }
.conv-item__meta { font-size:11px; color:#94a3b8; display:flex; justify-content:space-between; align-items:center; }
.conv-item__del {
  background:none; border:none; cursor:pointer; font-size:10px;
  opacity:0; transition:opacity .15s;
}
.conv-item:hover .conv-item__del { opacity:1; }
.conv-empty { text-align:center; padding:20px; font-size:12px; color:#94a3b8; }

.chat-sidebar__footer { padding:10px; border-top:1px solid #e2e8f0; }
.model-select {
  width:100%; padding:6px 8px; border-radius:6px; border:1px solid #e2e8f0;
  background:#ffffff; color:#1e293b; font-size:12px; outline:none;
}
.model-select:focus { border-color:#3b82f6; }

/* Main chat area */
.chat-main { flex:1; display:flex; flex-direction:column; overflow:hidden; background:#ffffff; }
.chat-messages { flex:1; overflow-y:auto; padding:20px; display:flex; flex-direction:column; gap:16px; }
.chat-msg { display:flex; gap:10px; max-width:80%; }
.chat-msg--user { align-self:flex-end; flex-direction:row-reverse; }
.chat-msg--assistant { align-self:flex-start; }
.chat-msg__avatar {
  width:32px; height:32px; border-radius:50%; flex-shrink:0;
  display:flex; align-items:center; justify-content:center; font-size:14px;
}
.chat-msg--user .chat-msg__avatar { background:#eff6ff; }
.chat-msg--assistant .chat-msg__avatar { background:#ecfdf5; }
.chat-msg__bubble {
  padding:10px 14px; border-radius:10px; font-size:13px; line-height:1.7;
  white-space:pre-wrap; word-break:break-word;
}
.chat-msg--user .chat-msg__bubble {
  background:#eff6ff; color:#1e293b; border-bottom-right-radius:4px;
}
.chat-msg--assistant .chat-msg__bubble {
  background:#f8fafc; color:#334155; border:1px solid #e2e8f0; border-bottom-left-radius:4px;
}

.cursor-blink { animation:blink 1s step-end infinite; color:#3b82f6; }
@keyframes blink { 50%{ opacity:0; } }
.dot-pulse { animation:pulse 1.5s infinite; }
@keyframes pulse { 0%,100%{ opacity:1; } 50%{ opacity:0.3; } }

/* Reasoning block */
.reasoning-block {
  margin-top:8px; border:1px solid #e2e8f0; border-radius:8px;
  background:#f8fafc; overflow:hidden;
}
.reasoning-block.streaming { border-color:#bfdbfe; }
.reasoning-block__header {
  padding:8px 12px; font-size:12px; color:#64748b; cursor:pointer;
  user-select:none; display:flex; align-items:center; gap:6px;
}
.reasoning-block__header:hover { background:#f1f5f9; }
.reasoning-block__body {
  padding:10px 14px; font-size:12px; color:#64748b;
  border-top:1px solid #e2e8f0; line-height:1.6;
  white-space:pre-wrap; max-height:200px; overflow-y:auto;
}

/* Empty state */
.chat-empty { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:16px; text-align:center; }
.chat-empty__icon { font-size:48px; opacity:0.3; }
.chat-empty__title { font-size:18px; font-weight:600; color:#1e293b; }
.chat-empty__desc { font-size:13px; color:#94a3b8; max-width:400px; line-height:1.7; }
.suggestions { display:flex; flex-wrap:wrap; gap:8px; justify-content:center; margin-top:8px; }
.suggestion-chip {
  padding:6px 14px; border-radius:20px; border:1px solid #e2e8f0;
  background:#ffffff; font-size:12px; color:#64748b; cursor:pointer;
  transition: all .2s;
}
.suggestion-chip:hover { border-color:#3b82f6; color:#3b82f6; background:#eff6ff; }

/* Input area */
.chat-input-area { padding:14px 20px; border-top:1px solid #e2e8f0; background:#f8fafc; }
.chat-input-row { display:flex; gap:8px; align-items:flex-end; }
.chat-input-row textarea {
  flex:1; padding:10px 14px; background:#ffffff;
  border:1px solid #e2e8f0; border-radius:10px;
  color:#1e293b; font-size:13px; resize:none; outline:none;
  font-family:inherit; min-height:40px; max-height:100px;
  transition: border-color .2s;
}
.chat-input-row textarea:focus { border-color:#3b82f6; box-shadow:0 0 0 3px rgba(59,130,246,0.12); }
.send-btn, .stop-btn {
  width:40px; height:40px; border-radius:10px; border:none;
  font-size:16px; cursor:pointer; transition: background .2s;
  flex-shrink:0; display:flex; align-items:center; justify-content:center;
}
.send-btn { background:#3b82f6; color:#fff; }
.send-btn:hover { background:#2563eb; }
.send-btn:disabled { background:#bfdbfe; color:#fff; cursor:not-allowed; }
.stop-btn { background:#ef4444; color:#fff; }
.stop-btn:hover { background:#dc2626; }

.chat-input-actions { display:flex; gap:8px; align-items:center; padding-top:8px; }
.toggle-btn {
  padding:4px 10px; border-radius:20px; border:1px solid #e2e8f0;
  font-size:12px; cursor:pointer; background:#ffffff; color:#64748b;
  display:flex; align-items:center; gap:4px; transition: all .2s;
}
.toggle-btn:hover { border-color:#bfdbfe; color:#1e293b; }
.toggle-btn.active { background:#eff6ff; border-color:#3b82f6; color:#3b82f6; }
</style>
