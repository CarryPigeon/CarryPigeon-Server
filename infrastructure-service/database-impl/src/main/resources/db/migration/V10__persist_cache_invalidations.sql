-- 与缓存覆盖的业务写入同事务登记，提交后删除失败可跨进程重启恢复。
CREATE TABLE cache_invalidation_task (
    task_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(4096) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_cache_invalidation_due (next_attempt_at, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
