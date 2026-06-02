-- Message domain
-- Source: V6, V7, V13, V14, V17

CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    conversation_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    preview_text VARCHAR(255) NOT NULL DEFAULT '',
    searchable_text TEXT NOT NULL,
    payload JSON NULL,
    metadata JSON NULL,
    mentions JSON NULL,
    forwarded_from JSON NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP NULL,
    edit_version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_message_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_message_channel_message
    ON chat_message (channel_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_sender_message_id
    ON chat_message (channel_id, sender_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_type_message_id
    ON chat_message (channel_id, message_type, message_id DESC);

CREATE TABLE chat_mention (
    mention_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    from_account_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_account_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_read BOOLEAN NOT NULL,
    PRIMARY KEY (mention_id)
);

CREATE INDEX idx_chat_mention_target_account_id_created_at
    ON chat_mention (target_account_id, created_at DESC);

CREATE INDEX idx_chat_mention_channel_id_created_at
    ON chat_mention (channel_id, created_at DESC);

CREATE INDEX idx_chat_mention_target_account_read_mention_id
    ON chat_mention (target_account_id, is_read, mention_id DESC);

CREATE INDEX idx_chat_mention_target_account_channel_mention_id
    ON chat_mention (target_account_id, channel_id, mention_id DESC);
