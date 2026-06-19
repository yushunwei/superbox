-- AI Translate: core tables
CREATE TABLE IF NOT EXISTS ait_translate_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    file_path VARCHAR(500),
    output_path VARCHAR(500),
    file_type VARCHAR(10),
    source_lang VARCHAR(10),
    target_lang VARCHAR(10),
    model VARCHAR(50),
    prompt_template_id BIGINT,
    role_id BIGINT,
    status VARCHAR(20) DEFAULT 'queued',
    progress INT DEFAULT 0,
    total_segments INT,
    completed_segments INT DEFAULT 0,
    error_msg VARCHAR(1000),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ait_task_user_time ON ait_translate_task(user_id, created_at DESC);
CREATE INDEX idx_ait_task_status ON ait_translate_task(status);

CREATE TABLE IF NOT EXISTS ait_translate_segment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES ait_translate_task(id),
    segment_index INT,
    source_text TEXT,
    translated_text TEXT,
    segment_type VARCHAR(20),
    char_count INT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ait_segment_task ON ait_translate_segment(task_id);

CREATE TABLE IF NOT EXISTS ait_glossary_entry (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_lang VARCHAR(10),
    target_lang VARCHAR(10),
    source_term VARCHAR(500) NOT NULL,
    target_term VARCHAR(500) NOT NULL,
    category VARCHAR(100),
    note VARCHAR(1000),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ait_glossary_user_lang ON ait_glossary_entry(user_id, source_lang, target_lang);
CREATE UNIQUE INDEX idx_ait_glossary_unique ON ait_glossary_entry(user_id, source_lang, target_lang, source_term);

CREATE TABLE IF NOT EXISTS ait_prompt_template (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    system_prompt TEXT NOT NULL,
    is_preset BOOLEAN DEFAULT false,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ait_translator_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    default_prompt_id BIGINT,
    model VARCHAR(50),
    temperature DOUBLE PRECISION DEFAULT 0.3,
    max_tokens INT DEFAULT 4096,
    is_preset BOOLEAN DEFAULT false,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
