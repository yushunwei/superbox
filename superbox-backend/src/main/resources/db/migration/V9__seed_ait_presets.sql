-- AI Translate: system preset prompt templates and translator roles

-- Preset prompt templates
INSERT INTO ait_prompt_template (user_id, name, category, system_prompt, is_preset, sort_order)
VALUES
(NULL, '通用翻译', 'general', '你是一个专业的翻译助手。请将以下{source_lang}文本准确翻译成{target_lang}。保持原文的语气和风格，对专业术语保持一致性。\n\n待翻译文本：\n{text}', true, 1),
(NULL, '技术文档翻译', 'tech', '你是一个技术文档翻译专家。请将以下{source_lang}技术文档翻译成{target_lang}。注意：保留代码片段和变量名不变，技术术语使用行业标准译法，保持文档结构。\n\n待翻译文本：\n{text}', true, 2),
(NULL, '文学翻译', 'literary', '你是一个文学翻译家。请将以下{source_lang}文本翻译成{target_lang}。注重文学性和可读性，传达原文的意境和情感。\n\n待翻译文本：\n{text}', true, 3),
(NULL, '口语翻译', 'casual', '你是一个口语翻译助手。请将以下{source_lang}文本翻译成自然的{target_lang}口语表达。使用日常用语，让翻译听起来像母语者的自然对话。\n\n待翻译文本：\n{text}', true, 4);

-- Preset translator roles (default_prompt_id references corresponding prompt templates)
INSERT INTO ait_translator_role (user_id, name, description, default_prompt_id, model, temperature, max_tokens, is_preset, sort_order)
VALUES
(NULL, '专业译者', '通用翻译角色，适合大多数翻译场景，平衡准确性和可读性', (SELECT id FROM ait_prompt_template WHERE category = 'general' AND is_preset = true LIMIT 1), 'openai', 0.3, 4096, true, 1),
(NULL, '技术文档译者', '专精技术文档翻译，保留代码和变量名，使用行业标准术语', (SELECT id FROM ait_prompt_template WHERE category = 'tech' AND is_preset = true LIMIT 1), 'openai', 0.1, 8192, true, 2),
(NULL, '文学译者', '注重文学性和可读性，擅长传达原文的意境和情感', (SELECT id FROM ait_prompt_template WHERE category = 'literary' AND is_preset = true LIMIT 1), 'claude-sonnet-4-6', 0.7, 4096, true, 3),
(NULL, '口语翻译', '自然口语化翻译，让译文像母语者的日常对话', (SELECT id FROM ait_prompt_template WHERE category = 'casual' AND is_preset = true LIMIT 1), 'deepseek-chat', 0.5, 2048, true, 4);
