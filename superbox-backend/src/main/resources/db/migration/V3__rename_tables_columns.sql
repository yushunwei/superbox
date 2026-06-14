-- Rename tag → label
ALTER TABLE tag RENAME TO label;

-- Rename task_tag → task_label
ALTER TABLE task_tag RENAME TO task_label;
ALTER TABLE task_label RENAME COLUMN tag_id TO label_id;

-- Rename knowledge_tag → knowledge_label
ALTER TABLE knowledge_tag RENAME TO knowledge_label;
ALTER TABLE knowledge_label RENAME COLUMN tag_id TO label_id;
ALTER TABLE knowledge_label RENAME COLUMN knowledge_id TO entry_id;

-- Rename conversation → ai_chat_conversation
ALTER TABLE conversation RENAME TO ai_chat_conversation;

-- Rename chat_message → ai_chat_message
ALTER TABLE chat_message RENAME TO ai_chat_message;
ALTER INDEX idx_message_conversation RENAME TO idx_ai_msg_conversation;
