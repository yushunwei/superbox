CREATE TABLE IF NOT EXISTS spreadsheet_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    page_key VARCHAR(64) NOT NULL,
    col_widths JSONB,
    col_aligns JSONB,
    col_wraps JSONB,
    row_heights JSONB,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, page_key)
);
