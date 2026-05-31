CREATE TABLE chat_channel_read_state (
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    last_read_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (channel_id, account_id)
);

CREATE INDEX idx_chat_channel_read_state_account_id
    ON chat_channel_read_state (account_id);
