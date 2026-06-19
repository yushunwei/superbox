ALTER TABLE aim_model_config ADD COLUMN IF NOT EXISTS api_format VARCHAR(32);

-- Backfill: Ollama patterns
UPDATE aim_model_config SET api_format = 'OLLAMA'
WHERE base_url IS NOT NULL
  AND (lower(base_url) LIKE '%ollama%' OR base_url LIKE '%:11434%')
  AND api_format IS NULL;

-- Backfill: Anthropic-compatible patterns
UPDATE aim_model_config SET api_format = 'ANTHROPIC_COMPATIBLE'
WHERE base_url IS NOT NULL
  AND lower(base_url) LIKE '%/anthropic%'
  AND api_format IS NULL;

-- Backfill: remaining → OpenAI-compatible (default)
UPDATE aim_model_config SET api_format = 'OPENAI_COMPATIBLE'
WHERE api_format IS NULL;
