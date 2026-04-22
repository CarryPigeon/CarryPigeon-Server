ALTER TABLE chat_message
    RENAME COLUMN content TO body;

ALTER TABLE chat_message
    ADD COLUMN preview_text VARCHAR(255) NOT NULL DEFAULT '' AFTER body,
    ADD COLUMN searchable_text TEXT NOT NULL AFTER preview_text;

UPDATE chat_message
SET preview_text = body,
    searchable_text = body;
