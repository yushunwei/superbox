# 数据库表结构

> 数据库：PostgreSQL（含 pgvector 扩展）
> 迁移工具：Flyway（`superbox-backend/src/main/resources/db/migration/`）

---

## 模块概览

| 模块 | 表数 | 前缀/命名特征 |
|------|------|-------------|
| 公共 | 3 | `sys_`, `tag`, `tag_category` |
| 任务管理 | 3 | `task`, `task_tag`, `spreadsheet_settings` |
| 知识库 | 4 | `knowledge_` |
| AI 对话 | 2 | `ai_chat_` |
| AI 翻译 | 6 | `ait_` |
| 模型管理 | 1 | `aim_` |

---

## 1. 公共模块

### sys_user — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 用户ID |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 用户名 |
| password_hash | VARCHAR(256) | NOT NULL | 密码哈希 |
| display_name | VARCHAR(128) | | 显示名称 |
| avatar_url | VARCHAR(512) | | 头像URL |
| wx_openid | VARCHAR(128) | | 微信OpenID |
| preferred_language | VARCHAR(10) | DEFAULT 'zh-CN' | 偏好语言 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

### tag — 标签表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 标签ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 标签名称 |
| color | VARCHAR(7) | DEFAULT '#409EFF' | 标签颜色(hex) |
| category_id | BIGINT | FK → tag_category.id, ON DELETE SET NULL | 所属分类 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |

### tag_category — 标签分类表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 分类ID |
| name | VARCHAR(64) | NOT NULL, UNIQUE | 分类名称 |
| color | VARCHAR(7) | DEFAULT '#409EFF' | 分类颜色 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |

---

## 2. 任务管理模块

### task — 任务表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 任务ID |
| title | VARCHAR(256) | NOT NULL | 任务标题 |
| description | TEXT | | 任务描述 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'not_started' | 状态：not_started / in_progress / completed |
| priority | VARCHAR(10) | DEFAULT 'normal' | 优先级：normal / important |
| due_date | DATE | | 截止日期 |
| plan_start_date | DATE | | 计划开始日期 |
| plan_end_date | DATE | | 计划结束日期 |
| executor | VARCHAR(128) | | 执行人 |
| collaborators | VARCHAR(512) | | 协作人（逗号分隔） |
| parent_task_id | BIGINT | FK → task.id | 父任务ID |
| sort_order | INT | DEFAULT 0 | 排序 |
| completed_at | TIMESTAMPTZ | | 完成时间 |
| remarks | TEXT | | 备注 |
| search_vector | TSVECTOR | GENERATED | 全文搜索向量 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

索引：`idx_task_status`(status), `idx_task_due_date`(due_date), `idx_task_parent`(parent_task_id), `idx_task_search` GIN(search_vector)

### task_tag — 任务-标签关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| task_id | BIGINT | PK, FK → task.id ON DELETE CASCADE | 任务ID |
| tag_id | BIGINT | PK, FK → tag.id ON DELETE CASCADE | 标签ID |

### spreadsheet_settings — 表格视图设置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 设置ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| page_key | VARCHAR(64) | NOT NULL | 页面标识 |
| col_widths | JSONB | | 列宽配置 |
| col_aligns | JSONB | | 列对齐配置 |
| col_wraps | JSONB | | 列换行配置 |
| row_heights | JSONB | | 行高配置 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

唯一约束：`UNIQUE(user_id, page_key)`

---

## 3. 知识库模块

### knowledge_folder — 知识库文件夹表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 文件夹ID |
| name | VARCHAR(128) | NOT NULL | 文件夹名称 |
| parent_id | BIGINT | FK → knowledge_folder.id, ON DELETE SET NULL | 父文件夹ID |
| sort_order | INT | DEFAULT 0 | 排序 |
| entry_count | INT | DEFAULT 0 | 条目数量（冗余计数） |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

索引：`idx_folder_parent`(parent_id)

### knowledge_entry — 知识条目表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 条目ID |
| folder_id | BIGINT | FK → knowledge_folder.id, ON DELETE SET NULL | 所属文件夹 |
| title | VARCHAR(512) | NOT NULL | 标题 |
| source_type | VARCHAR(20) | NOT NULL | 来源类型 |
| source_id | BIGINT | | 来源ID |
| file_path | VARCHAR(1024) | | 文件路径 |
| file_type | VARCHAR(32) | | 文件类型 |
| file_size | BIGINT | | 文件大小(bytes) |
| content_text | TEXT | | 内容文本 |
| chunk_count | INT | DEFAULT 0 | 分块数量 |
| search_vector | TSVECTOR | GENERATED | 全文搜索向量 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

索引：`idx_knowledge_folder`(folder_id), `idx_knowledge_source`(source_type), `idx_knowledge_search` GIN(search_vector)

### knowledge_chunk — 知识分块表（向量化）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 分块ID |
| entry_id | BIGINT | NOT NULL, FK → knowledge_entry.id ON DELETE CASCADE | 所属条目 |
| chunk_index | INT | NOT NULL | 分块序号 |
| content | TEXT | NOT NULL | 分块内容 |
| token_count | INT | | Token 数量 |
| embedding | VECTOR(1536) | | 向量嵌入(1536维) |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |

索引：`idx_chunk_entry`(entry_id)

### knowledge_tag — 知识-标签关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| knowledge_id | BIGINT | PK, FK → knowledge_entry.id ON DELETE CASCADE | 条目ID |
| tag_id | BIGINT | PK, FK → tag.id ON DELETE CASCADE | 标签ID |

---

## 4. AI 对话模块

### ai_chat_conversation — 对话表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 对话ID |
| title | VARCHAR(256) | NOT NULL, DEFAULT '新对话' | 对话标题 |
| model | VARCHAR(64) | NOT NULL, DEFAULT 'gpt-4o-mini' | 模型名称 |
| system_prompt | TEXT | | 系统提示词 |
| message_count | INT | DEFAULT 0 | 消息数量（冗余计数） |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

### ai_chat_message — 对话消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 消息ID |
| conversation_id | BIGINT | NOT NULL, FK → ai_chat_conversation.id ON DELETE CASCADE | 所属对话 |
| role | VARCHAR(16) | NOT NULL | 角色：user / assistant / system |
| content | TEXT | NOT NULL | 消息内容 |
| reasoning_content | TEXT | | 推理过程内容 |
| sources | JSONB | | 引用来源 |
| token_count | INT | | Token 消耗量 |
| model | VARCHAR(64) | | 使用的模型 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |

索引：`idx_ai_msg_conversation`(conversation_id, created_at)

---

## 5. AI 翻译模块

### ait_translate_task — 文档翻译任务表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 任务ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| file_name | VARCHAR(255) | | 文件名 |
| file_size | BIGINT | | 文件大小(bytes) |
| file_path | VARCHAR(500) | | 源文件路径 |
| output_path | VARCHAR(500) | | 输出文件路径 |
| file_type | VARCHAR(10) | | 文件类型(txt/pdf/docx等) |
| source_lang | VARCHAR(10) | | 源语言 |
| target_lang | VARCHAR(10) | | 目标语言 |
| model | VARCHAR(50) | | 翻译模型 |
| prompt_template_id | BIGINT | | 提示词模板ID |
| role_id | BIGINT | | 翻译角色ID |
| status | VARCHAR(20) | DEFAULT 'queued' | 状态：queued / processing / completed / failed |
| progress | INT | DEFAULT 0 | 进度(0-100) |
| total_segments | INT | | 总段落数 |
| completed_segments | INT | DEFAULT 0 | 已完成段落数 |
| error_msg | VARCHAR(1000) | | 错误信息 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

索引：`idx_ait_task_user_time`(user_id, created_at DESC), `idx_ait_task_status`(status)

### ait_translate_segment — 文档翻译段落表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 段落ID |
| task_id | BIGINT | NOT NULL, FK → ait_translate_task.id | 所属任务 |
| segment_index | INT | | 段落序号 |
| source_text | TEXT | | 原文 |
| translated_text | TEXT | | 译文 |
| segment_type | VARCHAR(20) | | 段落类型 |
| char_count | INT | | 字符数 |
| status | VARCHAR(20) | DEFAULT 'pending' | 状态：pending / translating / completed / failed |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |

索引：`idx_ait_segment_task`(task_id)

### ait_glossary_entry — 术语表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 条目ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| source_lang | VARCHAR(10) | | 源语言 |
| target_lang | VARCHAR(10) | | 目标语言 |
| source_term | VARCHAR(500) | NOT NULL | 源术语 |
| target_term | VARCHAR(500) | NOT NULL | 目标术语 |
| category | VARCHAR(100) | | 分类 |
| note | VARCHAR(1000) | | 备注 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

索引：`idx_ait_glossary_user_lang`(user_id, source_lang, target_lang)
唯一约束：`idx_ait_glossary_unique`(user_id, source_lang, target_lang, source_term)

### ait_prompt_template — 提示词模板表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 模板ID |
| user_id | BIGINT | 可空 | 用户ID（NULL=系统预设） |
| name | VARCHAR(100) | NOT NULL | 模板名称 |
| category | VARCHAR(50) | | 分类(general/tech/literary/casual等) |
| system_prompt | TEXT | NOT NULL | 系统提示词内容 |
| is_preset | BOOLEAN | DEFAULT false | 是否系统预设 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

### ait_translator_role — 翻译角色表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 角色ID |
| user_id | BIGINT | 可空 | 用户ID（NULL=系统预设） |
| name | VARCHAR(100) | NOT NULL | 角色名称 |
| description | VARCHAR(500) | | 角色描述 |
| default_prompt_id | BIGINT | | 默认提示词模板ID |
| model | VARCHAR(50) | | 默认模型 |
| temperature | DOUBLE PRECISION | DEFAULT 0.3 | 温度参数(0-1) |
| max_tokens | INT | DEFAULT 4096 | 最大输出token数 |
| is_preset | BOOLEAN | DEFAULT false | 是否系统预设 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

### ait_translate_settings — 翻译设置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 设置ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| default_model | VARCHAR(50) | DEFAULT 'gpt-4o-mini' | 默认翻译模型 |
| default_source_lang | VARCHAR(10) | DEFAULT 'zh' | 默认源语言 |
| default_target_lang | VARCHAR(10) | DEFAULT 'en' | 默认目标语言 |
| temperature | DOUBLE PRECISION | DEFAULT 0.3 | 温度参数 |
| max_tokens | INT | DEFAULT 4096 | 最大输出token数 |
| pdf_format | VARCHAR(20) | DEFAULT 'overlay' | PDF翻译格式 |
| word_format | VARCHAR(20) | DEFAULT 'keep_style' | Word翻译格式 |
| doc2x_enabled | BOOLEAN | DEFAULT false | 是否启用Doc2X |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

唯一约束：`idx_ait_settings_user`(user_id)

---

## 6. 模型管理模块

### aim_model_config — 模型配置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PK | 配置ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| provider_name | VARCHAR(50) | NOT NULL | 提供商名称(openai/deepseek等) |
| api_key | VARCHAR(512) | | API Key |
| base_url | VARCHAR(512) | | API Base URL |
| api_format | VARCHAR(32) | | API 格式(OPENAI_COMPATIBLE/ANTHROPIC_COMPATIBLE/OLLAMA)，保存时自动检测 |
| model_name | VARCHAR(128) | NOT NULL | 模型名称 |
| display_name | VARCHAR(128) | | 显示名称 |
| is_default | BOOLEAN | DEFAULT false | 是否默认模型 |
| is_active | BOOLEAN | DEFAULT true | 是否启用 |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | 更新时间 |

唯一约束：`idx_aim_model_default`(user_id) WHERE is_default = true（每用户仅一个默认）
索引：`idx_aim_model_user`(user_id, created_at DESC)

---

## 索引汇总

| 表 | 索引名 | 类型 | 列 |
|------|------|------|------|
| task | idx_task_status | BTREE | status |
| task | idx_task_due_date | BTREE | due_date |
| task | idx_task_parent | BTREE | parent_task_id |
| task | idx_task_search | GIN | search_vector |
| knowledge_folder | idx_folder_parent | BTREE | parent_id |
| knowledge_entry | idx_knowledge_folder | BTREE | folder_id |
| knowledge_entry | idx_knowledge_source | BTREE | source_type |
| knowledge_entry | idx_knowledge_search | GIN | search_vector |
| knowledge_chunk | idx_chunk_entry | BTREE | entry_id |
| ai_chat_message | idx_ai_msg_conversation | BTREE | (conversation_id, created_at) |
| ait_translate_task | idx_ait_task_user_time | BTREE | (user_id, created_at DESC) |
| ait_translate_task | idx_ait_task_status | BTREE | status |
| ait_translate_segment | idx_ait_segment_task | BTREE | task_id |
| ait_glossary_entry | idx_ait_glossary_user_lang | BTREE | (user_id, source_lang, target_lang) |
| ait_glossary_entry | idx_ait_glossary_unique | UNIQUE | (user_id, source_lang, target_lang, source_term) |
| ait_translate_settings | idx_ait_settings_user | UNIQUE | user_id |
| aim_model_config | idx_aim_model_default | UNIQUE(partial) | user_id WHERE is_default=true |
| aim_model_config | idx_aim_model_user | BTREE | (user_id, created_at DESC) |

---

## 迁移历史

| 版本 | 文件 | 变更 |
|------|------|------|
| V1 | V1__init_schema.sql | 初始建表：sys_user, tag, task, task_tag, knowledge_*, conversation, chat_message, 全文搜索 |
| V2 | V2__seed_data.sql | 种子数据：5个默认标签 |
| V3 | V3__rename_tables_columns.sql | 重命名 tag→label, conversation→ai_chat_conversation 等 |
| V4 | V4__task_enhance_columns.sql | task 表增加计划日期、执行人、备注等字段；状态值迁移 |
| V5 | V5__spreadsheet_settings.sql | 新增 spreadsheet_settings 表 |
| V6 | V6__tag_category.sql | 新增 tag_category 表；label 增加 category_id |
| V7 | V7__rename_label_to_tag.sql | 重命名 label→tag（统一命名） |
| V8 | V8__create_ait_translate_tables.sql | AI 翻译核心表：ait_translate_task/segment/glossary_entry/prompt_template/translator_role |
| V9 | V9__seed_ait_presets.sql | AI 翻译预设数据：4个提示词模板 + 4个翻译角色 |
| V10 | V10__create_ait_settings_table.sql | AI 翻译设置表：ait_translate_settings |
| V11 | V11__create_aim_model_tables.sql | 模型管理表：aim_model_config |
| V12 | V12__add_aim_model_config_api_format.sql | aim_model_config 增加 api_format 列并回填 |
