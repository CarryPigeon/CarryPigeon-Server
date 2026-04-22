CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    conversation_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    payload JSON NULL,
    metadata JSON NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_message_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_message_channel_message ON chat_message (channel_id, message_id DESC);
