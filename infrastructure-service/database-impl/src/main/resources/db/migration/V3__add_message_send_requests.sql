-- 请求 ID 是客户端生成的 UUID，使用文本语义，不转换为数值业务 ID。
-- 幂等记录不自动过期，频道删除时需一并清理；消息缺失时仍拒绝重新创建同一请求。
CREATE TABLE chat_message_send_request (
    account_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    message_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, channel_id, request_id),
    KEY idx_message_send_request_channel (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发送请求幂等身份';
