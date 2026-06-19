CREATE TABLE IF NOT EXISTS ait_translate_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    default_model VARCHAR(50) DEFAULT 'gpt-4o-mini',
    default_source_lang VARCHAR(10) DEFAULT 'zh',
    default_target_lang VARCHAR(10) DEFAULT 'en',
    temperature DOUBLE PRECISION DEFAULT 0.3,
    max_tokens INT DEFAULT 4096,
    pdf_format VARCHAR(20) DEFAULT 'overlay',
    word_format VARCHAR(20) DEFAULT 'keep_style',
    doc2x_enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ait_settings_user ON ait_translate_settings(user_id);
