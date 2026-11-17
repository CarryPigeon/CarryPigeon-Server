-- 持锁分配独立事件序号；消息创建和后续撤回均进入同一频道变更流。
CREATE TABLE chat_message_event_stream (
    channel_id BIGINT NOT NULL PRIMARY KEY,
    last_sequence BIGINT NOT NULL,
    expired_through BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_message_event (
    event_id BIGINT NOT NULL PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    event_sequence BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    payload JSON NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    delivered_at TIMESTAMP(3) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message_event_channel_sequence (channel_id, event_sequence),
    INDEX idx_message_event_message (channel_id, message_id, event_type),
    INDEX idx_message_event_pending (delivered_at, next_attempt_at, event_id),
    INDEX idx_message_event_retention (created_at, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
