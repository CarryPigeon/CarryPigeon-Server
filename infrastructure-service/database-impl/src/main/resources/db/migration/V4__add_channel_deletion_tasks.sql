-- 频道主记录在请求事务中移除，关联数据由持久化任务分批清理；保留完成记录支持所有者幂等重试。
CREATE TABLE chat_channel_deletion (
    channel_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (channel_id),
    KEY idx_channel_deletion_pending (completed_at, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可恢复频道清理任务';
