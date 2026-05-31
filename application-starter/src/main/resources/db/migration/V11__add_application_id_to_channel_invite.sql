ALTER TABLE chat_channel_invite
    ADD COLUMN application_id BIGINT NULL;

CREATE UNIQUE INDEX idx_chat_channel_invite_application_id
    ON chat_channel_invite (application_id);
