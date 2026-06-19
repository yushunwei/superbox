CREATE TABLE IF NOT EXISTS aim_model_config (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    api_key VARCHAR(512),
    base_url VARCHAR(512),
    model_name VARCHAR(128) NOT NULL,
    display_name VARCHAR(128),
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_aim_model_default ON aim_model_config(user_id) WHERE is_default = true;
CREATE INDEX IF NOT EXISTS idx_aim_model_user ON aim_model_config(user_id, created_at DESC);
