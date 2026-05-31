CREATE TABLE chat_notification_server_preference (
    account_id BIGINT NOT NULL,
    mode VARCHAR(32) NOT NULL,
    muted_until BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (account_id)
);

CREATE TABLE chat_notification_channel_preference (
    account_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    mode VARCHAR(32) NOT NULL,
    muted_until BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (account_id, channel_id),
    CONSTRAINT fk_chat_notification_channel_preference_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id)
);

CREATE INDEX idx_chat_notification_channel_preference_account_id
    ON chat_notification_channel_preference (account_id, channel_id);
