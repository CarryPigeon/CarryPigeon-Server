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
