-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ========== User ==========
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    display_name VARCHAR(128),
    avatar_url VARCHAR(512),
    wx_openid VARCHAR(128),
    preferred_language VARCHAR(10) DEFAULT 'zh-CN',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========== Tag ==========
CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    color VARCHAR(7) DEFAULT '#409EFF',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========== Task ==========
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'todo',
    priority VARCHAR(10) DEFAULT 'medium',
    due_date DATE,
    parent_task_id BIGINT REFERENCES task(id),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE task_tag (
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);

CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_due_date ON task(due_date);
CREATE INDEX idx_task_parent ON task(parent_task_id);

-- ========== Knowledge ==========
CREATE TABLE knowledge_folder (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT REFERENCES knowledge_folder(id) ON DELETE SET NULL,
    sort_order INTEGER DEFAULT 0,
    entry_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_folder_parent ON knowledge_folder(parent_id);

CREATE TABLE knowledge_entry (
    id BIGSERIAL PRIMARY KEY,
    folder_id BIGINT REFERENCES knowledge_folder(id) ON DELETE SET NULL,
    title VARCHAR(512) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT,
    file_path VARCHAR(1024),
    file_type VARCHAR(32),
    file_size BIGINT,
    content_text TEXT,
    chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_knowledge_folder ON knowledge_entry(folder_id);
CREATE INDEX idx_knowledge_source ON knowledge_entry(source_type);

CREATE TABLE knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES knowledge_entry(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    embedding VECTOR(1536),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chunk_entry ON knowledge_chunk(entry_id);

CREATE TABLE knowledge_tag (
    knowledge_id BIGINT NOT NULL REFERENCES knowledge_entry(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (knowledge_id, tag_id)
);

-- ========== Chat ==========
CREATE TABLE conversation (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(256) NOT NULL DEFAULT '新对话',
    model VARCHAR(64) NOT NULL DEFAULT 'gpt-4o-mini',
    system_prompt TEXT,
    message_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    reasoning_content TEXT,
    sources JSONB,
    token_count INTEGER,
    model VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_message_conversation ON chat_message(conversation_id, created_at);

-- ========== Full-text search ==========
ALTER TABLE task ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, ''))) STORED;
CREATE INDEX idx_task_search ON task USING GIN (search_vector);

ALTER TABLE knowledge_entry ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content_text, ''))) STORED;
CREATE INDEX idx_knowledge_search ON knowledge_entry USING GIN (search_vector);
