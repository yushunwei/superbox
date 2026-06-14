-- Add new columns to task table
ALTER TABLE task ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE task ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE task ADD COLUMN IF NOT EXISTS executor VARCHAR(128);
ALTER TABLE task ADD COLUMN IF NOT EXISTS collaborators VARCHAR(512);
ALTER TABLE task ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE task ADD COLUMN IF NOT EXISTS remarks TEXT;

-- Migrate old status values
UPDATE task SET status = 'not_started' WHERE status = 'todo';
UPDATE task SET status = 'completed' WHERE status = 'done';

-- Migrate old priority values
UPDATE task SET priority = 'normal' WHERE priority IN ('low', 'medium');
UPDATE task SET priority = 'important' WHERE priority IN ('high', 'urgent');
