-- Message domain
-- Canonical message schema. Domain-specific fields are stored only in data.

CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    domain VARCHAR(128) NOT NULL,
    domain_version VARCHAR(32) NOT NULL,
    data JSON NOT NULL,
    send_time DATETIME(6) NOT NULL,
    mentions JSON NOT NULL,
    preview VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_message_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_message_channel_message
    ON chat_message (channel_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_sender_message_id
    ON chat_message (channel_id, sender_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_domain_message_id
    ON chat_message (channel_id, domain, message_id DESC);

CREATE TABLE chat_message_idempotency (
    account_id BIGINT NOT NULL,
    operation VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    idempotency_key VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    message_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (account_id, operation, idempotency_key),
    CONSTRAINT fk_chat_message_idempotency_account FOREIGN KEY (account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_message_idempotency_message FOREIGN KEY (message_id) REFERENCES chat_message (message_id)
);

CREATE INDEX idx_chat_message_idempotency_message
    ON chat_message_idempotency (message_id);

ALTER TABLE chat_channel_pin
    ADD CONSTRAINT fk_chat_channel_pin_message FOREIGN KEY (message_id) REFERENCES chat_message (message_id);

CREATE TABLE chat_mention (
    mention_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    from_account_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_account_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    is_read BOOLEAN NOT NULL,
    PRIMARY KEY (mention_id),
    CONSTRAINT fk_chat_mention_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_mention_message FOREIGN KEY (message_id) REFERENCES chat_message (message_id),
    CONSTRAINT fk_chat_mention_from_account FOREIGN KEY (from_account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_mention_target_account FOREIGN KEY (target_account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_mention_target_account_id_created_at
    ON chat_mention (target_account_id, created_at DESC);

CREATE INDEX idx_chat_mention_channel_id_created_at
    ON chat_mention (channel_id, created_at DESC);

CREATE INDEX idx_chat_mention_target_account_read_mention_id
    ON chat_mention (target_account_id, is_read, mention_id DESC);

CREATE INDEX idx_chat_mention_target_account_channel_mention_id
    ON chat_mention (target_account_id, channel_id, mention_id DESC);
