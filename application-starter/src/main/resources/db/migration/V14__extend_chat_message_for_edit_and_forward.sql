ALTER TABLE chat_message
    ADD COLUMN edited_at TIMESTAMP NULL AFTER created_at,
    ADD COLUMN edit_version BIGINT NOT NULL DEFAULT 1 AFTER edited_at,
    ADD COLUMN mentions JSON NULL AFTER metadata,
    ADD COLUMN forwarded_from JSON NULL AFTER mentions;
