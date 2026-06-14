-- Rename label → tag (unify naming)
ALTER TABLE label RENAME TO tag;
ALTER TABLE task_label RENAME TO task_tag;
ALTER TABLE task_tag RENAME COLUMN label_id TO tag_id;
ALTER TABLE knowledge_label RENAME TO knowledge_tag;
ALTER TABLE knowledge_tag RENAME COLUMN label_id TO tag_id;
ALTER TABLE knowledge_tag RENAME COLUMN entry_id TO knowledge_id;
