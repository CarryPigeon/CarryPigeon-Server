CREATE TABLE chat_channel (
    id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_chat_channel_default_type ON chat_channel (is_default, type);

INSERT INTO chat_channel (id, conversation_id, name, type, is_default, created_at, updated_at)
VALUES (1, 1, 'public', 'public', TRUE, '2026-04-22 00:00:00', '2026-04-22 00:00:00');
