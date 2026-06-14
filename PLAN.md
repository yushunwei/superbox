# Superbox — 个人高效工作台 策划方案

## Context

基于 CLAUDE.md 中定义的技术栈（Vue 3 + Spring Boot + PostgreSQL），从零构建个人工作台。第一阶段聚焦三个核心能力：**任务管理**、**知识库**、**AI 智能问答**（基于知识库的 RAG + 通用对话，借鉴 DeepSeek 交互体验）。后续可扩展更多应用。需支持 **Web 端 + 微信小程序端**。

---

## 一、产品设计

### 1.1 核心理念

**主页即入口，点击即进入，一键即返回。**

```
┌──────────────────────────────────────────────┐
│                  🏠 主页                      │
│                                              │
│  🔍 全局搜索 (Ctrl+K)           🌐 中/EN     │
│  ┌────────────────────────────────────────┐  │
│  │  搜索任务、知识、或直接提问...           │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌─ ⚡ 快速捕获 ──────────────────────────┐  │
│  │  [输入内容...]     [→ 任务] [→ 知识]   │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌─────────────┐ ┌─────────────┐ ┌───────┐  │
│  │ 📋 任务管理  │ │ 📚 知识库   │ │ 💬    │  │
│  │   3 待办     │ │  12 条目    │ │AI 问答│  │
│  └─────────────┘ └─────────────┘ └───────┘  │
│                                              │
│  📌 最近动态                                  │
│  · 更新了任务「完成报告」        3分钟前     │
│  · AI 问答：讨论了 GNN 模型      1小时前     │
│  · 新增知识「Transformer 架构」  2小时前     │
└──────────────────────────────────────────────┘
          │ 点击任意应用卡片
          ▼
┌──────────────────────────────────────────────┐
│ ← 返回主页    任务管理              [+]      │  ← 极简顶栏
├──────────────────────────────────────────────┤
│  应用全屏内容区域                              │
└──────────────────────────────────────────────┘
```

### 1.2 AI 智能问答 — 核心应用

借鉴 DeepSeek / ChatGPT 等主流大模型产品的交互体验，提供基于知识库的 RAG 问答 + 通用 AI 对话能力：

```
┌──────────────────────────────────────────────────┐
│ ← 返回主页   AI 问答    [模型: DeepSeek R1 ▼] [⚙]│
├────────────┬─────────────────────────────────────┤
│ 对话列表    │  对话区                              │
│ ──────────│                                      │
│ ● GNN讨论  │  ┌────────────────────────────────┐ │
│   今天      │  │ 用户: Transformer 和 GNN 的区别? │ │
│ ● 代码审查  │  └────────────────────────────────┘ │
│   昨天      │                                      │
│ ● 知识问答  │  ┌────────────────────────────────┐ │
│   周三      │  │ 🤖 AI:                          │ │
│            │  │                                  │ │
│ + 新对话   │  │ 💭 深度推理 (可折叠):             │ │
│            │  │  1. 分析 Transformer → 自注意力  │ │
│            │  │  2. 分析 GNN → 图结构消息传递     │ │
│            │  │  3. 对比核心差异 → 数据结构不同   │ │
│            │  │ ────────────────────────────     │ │
│            │  │ 📝 回答:                         │ │
│            │  │ Transformer 处理序列数据，基于    │ │
│            │  │ 自注意力机制；GNN 处理图结构...   │ │
│            │  │                                  │ │
│            │  │ 📎 来源: [Transformer详解] [GNN] │ │
│            │  └────────────────────────────────┘ │
│            │                                      │
│            │  ┌────────────────────────────────┐ │
│            │  │ 输入框         [深度推理] [@知识库]│ │
│            │  └────────────────────────────────┘ │
└────────────┴─────────────────────────────────────┘
```

**核心功能**：
- 多轮对话，会话列表管理，历史持久化
- 流式输出（SSE），打字机效果逐字渲染
- 深度推理模式：展示 Chain-of-Thought 推理链路，可折叠
- 多模型切换：GPT-4o / DeepSeek R1 / Claude Sonnet 等商业模型
- **@知识库**：一键切换 RAG 模式，回答自动标注来源
- 复制/重新生成/停止生成

### 1.3 知识库设计

知识库是个人知识管理（PKM）的核心，支持三种知识摄入方式 + 目录组织 + AI 智能检索。

**知识库布局**：

```
┌──────────────────────────────────────────────┐
│ ← 返回主页    知识库          [+新增] [🔍]   │
├──────────┬───────────────────────────────────┤
│ 目录树    │  内容区                            │
│ ──────── │                                    │
│ 📁 技术   │  📁 机器学习 (12 条目)              │
│  📁 ML   │    ├─ Transformer 架构详解          │
│    📄 ... │    ├─ GNN 入门指南                  │
│  📁 后端  │    └─ ...                          │
│ 📁 产品   │                                    │
│ 📁 生活   │  或: 选中某条目 → 查看详细内容       │
│          │                                    │
│ + 新建目录│                                    │
└──────────┴───────────────────────────────────┘
```

**三种知识摄入方式**：

**① 知识目录（文件夹）管理**：
- 支持无限层级目录树，拖拽排序/移动
- 目录可重命名、删除（目录下条目自动移至父级）
- 每个目录统计条目数量
- 类似文件管理器体验：左侧树 + 右侧内容

**② 文件上传解析**：

| 支持格式 | 解析库 | 说明 |
|--------|------|------|
| PDF | Apache PDFBox | 逐页提取文本 |
| DOCX | Apache POI XWPF | 提取段落文本 |
| TXT | 直接读取 | UTF-8 文本 |
| Markdown (.md) | commonmark-java | 解析后提取纯文本 |
| HTML | Jsoup | 去除标签，提取正文 |
| CSV | OpenCSV | 解析为结构化文本 |

- 支持拖拽上传、批量上传
- 上传后自动解析 → 分块 → 向量化（异步）
- 最大单文件 20MB，显示解析进度

**③ 手工输入新知识**：
- 富文本/Markdown 编辑器
- 填写标题 + 正文内容
- 选择所属目录
- 可选添加标签（全局标签系统）

**知识条目操作**：查看详情（全文 + 分块高亮）、编辑、删除、移动到其他目录、关联标签、关联任务。

### 1.4 数据互通

- 任务 → 知识库：已完成的任務可"提升"为知识条目
- AI 问答 ↔ 知识库：对话中启用 RAG，自动检索知识库内容增强回答
- 全局搜索：PostgreSQL 全文检索，跨任务、知识条目

---

## 二、前端架构

> **实施注意**：编写前端页面代码时，需使用 `frontend-design` 技能确保页面样式美观、交互流畅、避免千篇一律的 AI 风格。设计稿参考 `design-mockup.html`（深色主题、毛玻璃卡片、主页启动器模式）。

### 2.1 项目结构（Monorepo）

```
superbox/
  packages/
    shared/                    # 跨端共享
      api/                     # Axios 实例 + SSE + API 模块
        modules/ (auth.ts, task.ts, knowledge.ts, chat.ts, search.ts, tag.ts)
      stores/modules/          # user, search, tag, home
      types/                   # 共享类型
      utils/ (sse.ts, token.ts)
    web/                       # Web 前端 (Vite + Vue 3 + Element Plus)
      src/
        components/
          home/                # HomePage, AppCardGrid, GlobalSearch, QuickCapture, RecentTimeline
          shell/               # AppShell (返回按钮 + 标题 + 内容区)
          task/                # TaskCard, TaskForm, KanbanBoard, StatusBadge
          knowledge/           # FolderTree, FileUploader, KnowledgeCard, KnowledgeEditor
          chat/                # ChatLayout, ConversationList, ChatMessage, ReasoningBlock,
                               #   ModelSelector, ChatInput, StreamingText
        composables/           # useSSE, useChat, useI18n, useDebounce
        i18n/locales/          # zh-CN.ts, en-US.ts
        router/                # index.ts, routes.ts
        apps/
          index.ts             # ★ 应用清单（主页卡片由此驱动）
          taskManager/         # pages/, stores/taskStore.ts, types/
          knowledgeBase/       # pages/, stores/knowledgeStore.ts, types/
          aiChat/              # pages/AIChatPage.vue, stores/chatStore.ts, types/
    miniapp/                   # 微信小程序 (uni-app Vue 3)
      src/pages/               # index, task, knowledge, chat, mine
      manifest.json, pages.json
```

### 2.2 路由设计

```
/login                         # 公开
/                              # ★ 主页 (HomePage)
/app/tasks                     # 任务 (AppShell)
/app/tasks/:id                 # 任务详情
/app/knowledge                 # 知识库列表
/app/knowledge/upload          # 上传/录入
/app/knowledge/:id             # 知识条目详情
/app/chat                      # AI 问答
/app/chat/:conversationId      # 特定对话
```

- `/` 渲染 HomePage，无需 AppShell
- `/app/*` 由 AppShell 包裹（顶栏返回按钮 + 标题）

### 2.3 应用注册

```typescript
// src/apps/index.ts
export const apps: AppManifest[] = [
  { id: 'taskManager',   icon: 'List',         label: '任务管理', statKey: 'pendingCount' },
  { id: 'knowledgeBase', icon: 'Collection',    label: '知识库',   statKey: 'entryCount' },
  { id: 'aiChat',        icon: 'ChatDotRound',  label: 'AI 问答',  wide: true },
];
```

HomePage 遍历此数组渲染应用卡片。

### 2.4 AppShell

所有应用的通用壳：顶栏（48px）含 ← 返回按钮 + 应用标题 + 操作区 slot。`router.push('/')` 回到主页。

### 2.5 状态管理

- 全局：user, search, tag, home
- 应用级（懒加载）：taskStore, knowledgeStore, chatStore

### 2.6 AI 问答核心设计

**SSE 流式接收**（`shared/utils/sse.ts`）：

```typescript
streamChat(params: ChatRequest,
  onReasoning: (delta: string) => void,   // 推理增量
  onAnswer: (delta: string) => void,      // 回答增量
  onSources: (sources: Source[]) => void,  // RAG引用
  onDone: (stats: TokenStats) => void
): AbortController
```

**chatStore 核心状态**：conversations[], activeConversationId, messages (按会话分组), isStreaming, reasoningCollapsed, selectedModel, ragEnabled

---

## 三、后端架构

### 3.1 项目结构（单 Maven 模块）

```
superbox-backend/src/main/java/com/superbox/
  SuperboxApplication.java
  config/        # SecurityConfig, CorsConfig, RedisConfig, AiConfig, AsyncConfig
  common/        # Result, PageResult, BusinessException, GlobalExceptionHandler
  security/      # JwtTokenFilter, JwtTokenProvider, WxAuthProvider
  app/
    auth/        # controller, service, entity(User), mapper
    task/        # controller, service, entity(Task), mapper, dto
    knowledge/   # controller, service, entity(KnowledgeEntry/Chunk), mapper, dto
    chat/        # controller(ChatController), service(ConversationService, ChatService,
                 #   ModelRouterService), provider(AiModelProvider接口+多实现),
                 #   entity(Conversation/ChatMessage), mapper, dto
    search/      # controller, service (全文检索)
    tag/         # controller, service, entity(Tag), mapper
```

### 3.2 数据库设计

| 表 | 说明 | 关键字段 |
|---|---|---|
| `sys_user` | 用户 | username, password_hash, wx_openid, preferred_language |
| `task` | 任务 | title, description, status(todo/in_progress/done), priority, due_date, sort_order |
| `tag` | 全局标签 | name, color |
| `task_tag / knowledge_tag` | 标签关联 | task_id/knowledge_id + tag_id |
| **`knowledge_folder`** | **知识目录(树)** | **name, parent_id(NULL为根), sort_order, entry_count** |
| `knowledge_entry` | 知识条目 | **folder_id**, title, source_type(manual/file_upload/imported_task), content_text, file_path, file_type, file_size |
| `knowledge_chunk` | 知识分块(RAG) | entry_id, chunk_index, content, embedding(pgvector VECTOR(1536)) |
| `conversation` | AI 对话 | title, model, system_prompt |
| `chat_message` | 对话消息 | conversation_id, role, content, reasoning_content, sources(JSONB), token_count |

全文检索：task 和 knowledge_entry 增加 `search_vector TSVECTOR` 生成列 + GIN 索引。

### 3.3 API 设计（`/api/v1/`）

**Auth**: `POST /auth/login` | `POST /auth/wx-login` | `GET /auth/me`

**Task**: `GET/POST /tasks` | `GET/PUT/DELETE /tasks/{id}` | `PATCH /tasks/{id}/status` | `POST /tasks/{id}/promote-to-knowledge`

**Knowledge Folder**:

| Method | Path | Description |
|---|---|---|
| GET | `/knowledge/folders` | 完整目录树 (返回嵌套JSON) |
| POST | `/knowledge/folders` | 创建目录 (parentId可选) |
| PUT | `/knowledge/folders/{id}` | 重命名/移动目录 |
| DELETE | `/knowledge/folders/{id}` | 删除目录 (子条目归入父级) |
| PATCH | `/knowledge/folders/sort` | 批量更新排序 |

**Knowledge Entry**: `GET/POST /knowledge` | `GET/PUT/DELETE /knowledge/{id}` | `POST /knowledge/upload` (multipart, 含 folderId) | `PATCH /knowledge/{id}/move` (移动到指定目录)

**Chat（★）**:

| Method | Path | Description |
|---|---|---|
| GET | `/chat/conversations` | 对话列表 |
| POST | `/chat/conversations` | 创建新对话 |
| DELETE | `/chat/conversations/{id}` | 删除对话 |
| GET | `/chat/conversations/{id}/messages` | 消息历史 |
| POST | `/chat/conversations/{id}/send` | 发送消息 → **SSE 流式返回** |
| GET | `/chat/models` | 可用模型列表 |

SSE 事件流：
```
event: reasoning  | data: {"delta": "首先分析..."}
event: answer     | data: {"delta": "Transformers和GNN..."}
event: sources    | data: [{"entryId":12,"title":"Transformer详解","score":0.94}]
event: done       | data: {"messageId":42,"tokenCount":512}
```

**Tag**: `GET/POST /tags` | `PUT/DELETE /tags/{id}`

**Search**: `GET /search?q=&type=task,knowledge&page=&size=`

### 3.4 AI 多模型架构

```java
public interface AiModelProvider {
    Flux<ChatStreamEvent> chatStream(String systemPrompt,
        List<ChatMessage> history, ChatOptions options); // 流式对话
    List<Float> embed(String text);  // 嵌入向量(RAG用)
    String translate(String text, String from, String to);
}

@Service
public class ModelRouterService {
    // "gpt-4o" / "deepseek-reasoner" / "claude-sonnet-4-6" → 对应 Provider
    public AiModelProvider route(String modelId) { ... }
}
```

配置模型列表（`application.yml`）：GPT-4o、GPT-4o-mini、DeepSeek V3、DeepSeek R1（深度推理）、Claude Sonnet 4.6、Ollama 本地模型。

### 3.5 AI 问答核心流程（ChatService）

```
1. 接收请求 (conversationId, prompt, model, reasoning, ragEnabled)
2. 保存用户消息 → chat_message
3. 构建消息历史 (最近15轮)
4. if ragEnabled:
     embed(用户问题) → pgvector 检索 top-5 分块
     → 注入 System Prompt: "参考以下知识库内容回答..."
5. ModelRouterService.route(modelId) → 获取 Provider
6. 创建 SseEmitter (超时5分钟)
7. provider.chatStream() → 流式推送 reasoning/answer/sources/done
8. 保存 assistant 消息 (含 reasoning + sources) → DB
9. 用户发送 /stop → 中断 SSE，保存部分结果
```

### 3.6 知识库摄入流程

**文件上传**：
```
上传文件(指定 folderId) → 存盘(storage/files/YYYY/MM/uuid-name)
  → 创建 knowledge_entry(folder_id, source_type=file_upload)
  → 异步: 文本解析(PDFBox/POI/Tika/CommonMark/Jsoup)
  → 分块(500 token, 50重叠) → embed() 向量化
  → 存入 knowledge_chunk → 更新 entry.chunk_count
  → 更新 folder.entry_count
```

**手工输入**：
```
填写标题+内容+目录+标签 → 创建 knowledge_entry(source_type=manual)
  → 同步分块+向量化（内容较少） → 存入 chunks
```

**从任务提升**：
```
已完成任务 → 选择目标目录 → 创建 knowledge_entry(source_type=imported_task)
  → 任务标题→标题, 任务描述→内容 → 分块+向量化
```

### 3.7 微信小程序

- 认证：`wx.login()` → code → 后端换 openid → 生成 JWT
- 技术：uni-app (Vue 3) 共享 API/Store/Types
- TabBar：主页 | AI问答 | 任务 | 我的
- 后端增加 `sys_user.wx_openid`、微信 session_key 缓存（Redis）

### 3.8 Redis 用途

- JWT 黑名单（登出时）
- AI 对话频率限制（防止误操作高频调用）
- 微信 session_key 缓存

---

## 四、开发路线图

### Phase 1：平台地基（1-2周）

**后端**：Spring Boot + PostgreSQL/Redis + Flyway(`sys_user`, `tag`) + JWT + 微信登录 + 统一响应

**前端**：Vue 3 脚手架 + Element Plus/Pinia/Router/i18n + HomePage（搜索+应用卡片+快速捕获+动态） + LoginPage + AppShell + Axios

**验收**：登录 → 主页三张应用卡片 → AppShell 进出 → 语言切换

### Phase 2：任务管理（1周）

**后端**：task 表 + CRUD API + 状态变更 + 提升到知识库

**前端**：TaskListPage(列表/看板+拖拽) + TaskDetailPage + 状态角标

**验收**：CRUD 正常 → 看板拖拽 → 提升到知识库

### Phase 3：知识库（1-2周）

**后端**：pgvector + knowledge_folder/entry/chunk 表 + 目录CRUD + 文件解析(PDFBox/POI/Tika/CommonMark/Jsoup) + 分块 + 向量化 + 上传API + 手动录入API + 任务提升API

**前端**：KnowledgeListPage(左侧目录树+右侧条目列表) + FolderTree(无限层级+拖拽排序) + KnowledgeUploadPage(拖拽上传+选择目录) + KnowledgeEditPage(Markdown手动录入+目录选择) + KnowledgeDetailPage + FileUploader 组件 + 解析进度展示

**验收**：创建多层级目录 → 上传 PDF/DOCX/TXT/MD 等格式 → 自动解析分块向量化 → 手动录入知识 → pgvector 检索验证相似度 → 目录树拖拽排序

### Phase 4：AI 智能问答（2-3周）

**后端**：conversation/chat_message 表 + ModelRouter + ChatService(流式+RAG编排) + SSE endpoint + 多Provider实现

**前端**：AIChatPage + ConversationList + ChatMessage + ReasoningBlock + ModelSelector + ChatInput + StreamingText + SSE流式渲染

**验收**：多轮对话 → 流式输出 → 深度推理折叠 → 多模型切换 → RAG 回答带来源

### Phase 5：微信小程序（2-3周）

uni-app 搭建 → 微信登录 → 任务/知识库/AI问答页面 → TabBar → 语音/拍照输入

**验收**：编译通过 → 登录 → 核心操作 → AI 流式对话

### Phase 6：打磨上线（1-2周）

AI 频率限制 → 异步文件摄入 → 搜索排序优化 → Docker Compose 一键部署

---

## 五、关键决策

| 决策点 | 选择 | 原因 |
|---|---|---|
| 交互架构 | 主页启动器 + 全屏应用 | 简洁直观，无传统侧边栏，聚焦核心操作 |
| 后端结构 | 单 Maven 模块 | 个人应用，类少，拆模块过度设计 |
| 向量存储 | PostgreSQL pgvector | 无需独立向量数据库，个人规模足够 |
| 流式对话 | SSE (SseEmitter) | 比 WebSocket 简单，单向推送够用 |
| 深度推理 | SSE reasoning 事件 | 推理过程与回答分阶段流式返回 |
| AI 模型 | Provider 接口 + ModelRouter | 多商业模型运行时切换 |
| 跨端 | uni-app + shared 包 | Web/小程序共享逻辑，UI 各端适配 |
| 认证 | JWT + 微信登录 | 无状态，Web 用户名密码 / 小程序 wx.login |

---

## 六、验证方案

1. **Phase 1**：`docker-compose up` → 登录 → 主页三张卡片 → 进出应用
2. **Phase 2**：任务 CRUD + 看板拖拽 + 提升到知识库
3. **Phase 3**：上传 PDF → 验证 pgvector 向量 → 相似度检索
4. **Phase 4**：多轮对话 → 流式输出 → 深度推理(折叠/展开) → 模型切换 → @知识库 RAG 带来源
5. **Phase 5**：微信开发者工具 → 登录 → AI 流式对话
6. **Phase 6**：完整部署 → 全流程回归
