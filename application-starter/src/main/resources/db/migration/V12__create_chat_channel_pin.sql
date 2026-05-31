CREATE TABLE chat_channel_pin (
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_by_account_id BIGINT NOT NULL,
    note VARCHAR(200) NOT NULL DEFAULT '',
    pinned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (channel_id, message_id)
);

CREATE INDEX idx_chat_channel_pin_channel_id_pinned_at
    ON chat_channel_pin (channel_id, pinned_at DESC);
