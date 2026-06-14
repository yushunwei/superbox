-- Tag category table
CREATE TABLE IF NOT EXISTS tag_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(7) DEFAULT '#409EFF',
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(name)
);

-- Add category_id to existing label table
ALTER TABLE label ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES tag_category(id) ON DELETE SET NULL;
ALTER TABLE label ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;

-- Insert a default category
INSERT INTO tag_category (name, color, sort_order) VALUES ('默认分类', '#409EFF', 0)
ON CONFLICT (name) DO NOTHING;
